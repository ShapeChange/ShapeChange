/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109
 * Application Schema from a UML model and translates it into a
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.GeoPackage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.platform.commons.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.TargetUtil;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.attributes.AttributesColumn;
import mil.nga.geopackage.attributes.AttributesTable;
import mil.nga.geopackage.contents.Contents;
import mil.nga.geopackage.contents.ContentsDao;
import mil.nga.geopackage.contents.ContentsDataType;
import mil.nga.geopackage.srs.SpatialReferenceSystem;
import mil.nga.geopackage.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.extension.CrsWktExtension;
import mil.nga.geopackage.extension.rtree.RTreeIndexExtension;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureTable;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.schema.SchemaExtension;
import mil.nga.geopackage.extension.schema.columns.DataColumns;
import mil.nga.geopackage.extension.schema.columns.DataColumnsDao;
import mil.nga.geopackage.extension.schema.constraints.DataColumnConstraintType;
import mil.nga.geopackage.extension.schema.constraints.DataColumnConstraints;
import mil.nga.geopackage.extension.schema.constraints.DataColumnConstraintsDao;
import mil.nga.sf.GeometryType;

/**
 * Creates a GeoPackage template for a set of application schemas.
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GeoPackageTemplate implements SingleTarget, MessageSource {

    protected static Model model = null;

    protected static boolean initialised = false;
    protected static boolean diagnosticsOnly = false;
    protected static int numberOfEncodedSchemas = 0;

    /**
     * NOTE: If not set via the configuration, the default applies which is
     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE}.
     */
    protected static String documentationTemplate = null;
    /**
     * NOTE: If not set via the configuration, the default applies which is
     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_NOVALUE}.
     */
    protected static String documentationNoValue = null;

    protected static String outputDirectory = null;
    protected static String outputFilename = null;

    protected static List<ClassInfo> cisToProcess = new ArrayList<ClassInfo>();

    protected static List<SpatialReferenceSystem> srsDefs = new ArrayList<>();
    protected static int organizationCoordSysId = 4326;
    protected static String srsOrganization = "EPSG";

    protected static String idColumnName;
    protected static byte gpkgM = 0;
    protected static byte gpkgZ = 0;

    protected ShapeChangeResult result = null;
    protected Options options = null;

    protected PackageInfo schema = null;
    protected boolean schemaNotEncoded = false;

    // initialised and used during writeAll
    protected SchemaExtension schemaExtension = null;
    
    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;

	diagnosticsOnly = diagOnly;

	if (!isEncoded(schema)) {

	    schemaNotEncoded = true;
	    result.addInfo(this, 7, schema.name());
	    return;
	} else {
	    numberOfEncodedSchemas++;
	}

	if (!initialised) {
	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    // create output directory, if necessary
	    if (!diagnosticsOnly) {

		// Check whether we can use the given output directory
		File outputDirectoryFile = new File(outputDirectory);
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
		    outputDirectoryFile.mkdirs();
		    exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
		    result.addFatalError(this, 4, outputDirectory);
		    return;
		}

		outputFilename = options.parameter(this.getClass().getName(), "outputFilename");
		if (outputFilename == null) {

		    PackageInfo mainAppSchema = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o,
			    r);

		    if (mainAppSchema == null) {
			outputFilename = schema.name();
		    } else {
			outputFilename = mainAppSchema.name();
		    }
		}
		outputFilename = outputFilename.replace("/", "_").replace(" ", "_");
		if (!outputFilename.endsWith(".gpkg")) {
		    outputFilename = outputFilename + ".gpkg";
		}

		File outputFile = new File(outputDirectory, outputFilename);
		if (outputFile.exists()) {
		    result.addInfo(this, 503, outputFilename, outputDirectory);
		    try {
			FileUtils.forceDelete(outputFile);
			result.addInfo(this, 504);
		    } catch (IOException e) {
			result.addInfo(null, 600, e.getMessage());
			e.printStackTrace(System.err);
		    }
		}

		idColumnName = options.parameterAsString(this.getClass().getName(),
			GeoPackageConstants.PARAM_ID_COLUMN_NAME, GeoPackageConstants.DEFAULT_ID_COLUMN_NAME, false,
			true);

		gpkgM = options.parameterAsByte(this.getClass().getName(), GeoPackageConstants.PARAM_GPKGM, (byte) 0);
		gpkgZ = options.parameterAsByte(this.getClass().getName(), GeoPackageConstants.PARAM_GPKGZ, (byte) 0);

		organizationCoordSysId = options.parameterAsInteger(this.getClass().getName(),
			GeoPackageConstants.PARAM_ORGANIZATION_COORD_SYS_ID, 4326);

		srsOrganization = options.parameterAsString(this.getClass().getName(),
			GeoPackageConstants.PARAM_SRS_ORGANIZATION, "EPSG", false, true);

		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(),
			GeoPackageConstants.PARAM_DOCUMENTATION_TEMPLATE);
		documentationNoValue = options.parameter(this.getClass().getName(),
			GeoPackageConstants.PARAM_DOCUMENTATION_NOVALUE);

		/*
		 * identify SRS definitions in advancedProcessConfigurations element of target
		 * configuration
		 */
		if (options.getCurrentProcessConfig().getAdvancedProcessConfigurations() != null) {
		    srsDefs = parseGeoPackageSrsDefinitions(
			    options.getCurrentProcessConfig().getAdvancedProcessConfigurations());
		}
	    }
	}
    }

    public static List<SpatialReferenceSystem> parseGeoPackageSrsDefinitions(Element apcs) {

	List<SpatialReferenceSystem> results = new ArrayList<>();

	// identify GeoPackageSrsDefinition elements
	List<Element> gpkgSrsDefEs = new ArrayList<Element>();

	NodeList sdNl = apcs.getElementsByTagName("GeoPackageSrsDefinition");

	if (sdNl != null && sdNl.getLength() != 0) {
	    for (int k = 0; k < sdNl.getLength(); k++) {
		Node n = sdNl.item(k);
		if (n.getNodeType() == Node.ELEMENT_NODE) {

		    gpkgSrsDefEs.add((Element) n);
		}
	    }
	}

	for (int i = 0; i < gpkgSrsDefEs.size(); i++) {

	    Element gpkgSrsDefE = gpkgSrsDefEs.get(i);

	    SpatialReferenceSystem srs = new SpatialReferenceSystem();
	    srs.setSrsName(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "srsName"));
	    srs.setSrsId(Integer.parseInt(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "srsId")));
	    srs.setOrganization(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "organization"));
	    srs.setOrganizationCoordsysId(Integer
		    .parseInt(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "organizationCoordSysId")));
	    srs.setDefinition(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "definition"));
	    srs.setDescription(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "description"));
	    srs.setDefinition_12_063(XMLUtil.getTrimmedTextContentOfFirstElement(gpkgSrsDefE, "definition_12_063"));
	    results.add(srs);
	}

	return results;
    }

    public void process(ClassInfo ci) {

	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (!isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	result.addDebug(this, 3, ci.name());

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	if ((ci.category() == Options.OBJECT && ci.matches(GeoPackageConstants.RULE_TGT_GPKG_CLS_OBJECTTYPE))
		|| ci.category() == Options.FEATURE || ci.category() == Options.ENUMERATION
		|| ci.category() == Options.CODELIST) {

	    boolean propExists = false;
	    for (PropertyInfo pi : ci.propertiesAll()) {
		if (isEncoded(pi)) {
		    propExists = true;
		} else {
		    MessageContext mc = result.addInfo(this, 19, pi.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullNameInSchema());
		    }
		}
	    }

	    if (!propExists) {
		result.addInfo(this, 104, ci.name());
	    } else {
		cisToProcess.add(ci);
	    }

	} else {

	    result.addInfo(this, 17, ci.name());
	}
    }

    @Override
    public void write() {

	// nothing to do here (this is a SingleTarget)
    }

    @Override
    public String getTargetName() {
	return "GeoPackage";
    }

    @Override
    public String getTargetIdentifier() {
	return "gpkg";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "geopackage";
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	/*
	 * GeoPackage encoding rules
	 */

	/*
	 * NOTE: This rule has been added to encoding rule 'notEncoded', which is part
	 * of StandardRules.xml
	 */
	r.addRule("rule-gpkg-all-notEncoded");

	// add all rules
	r.addRule("rule-gpkg-cls-identifierStereotype");
	r.addRule("rule-gpkg-cls-objecttype");

	// now declare rule sets	
	ProcessRuleSet geopackagePrs = new ProcessRuleSet("geopackage","*",new TreeSet<>(Stream.of(
		"rule-gpkg-cls-objecttype").collect(Collectors.toSet())));
	r.addRuleSet(geopackagePrs);

    }

    public static boolean isEncoded(Info i) {

	if (i.matches(GeoPackageConstants.RULE_TGT_GPKG_ALL_NOTENCODED)
		&& i.encodingRule("gpkg").equalsIgnoreCase("notencoded")) {
	    return false;
	} else {
	    return true;
	}
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	this.result = r;
	this.options = r.options();

	if (diagnosticsOnly || numberOfEncodedSchemas == 0) {
	    return;
	}

	// create output file
	File geopackageFile = new File(outputDirectory, outputFilename);
	GeoPackageManager.create(geopackageFile);

	try (GeoPackage geoPackage = GeoPackageManager.open(geopackageFile, true);) {

	    // initialise SRS definitions
	    SpatialReferenceSystemDao srsDao = geoPackage.getSpatialReferenceSystemDao();

	    if (srsDefs.stream().anyMatch(srs -> StringUtils.isNotBlank(srs.getDefinition_12_063()))) {
		CrsWktExtension wktExt = new CrsWktExtension(geoPackage);
		wktExt.getOrCreate();
	    }

	    for (SpatialReferenceSystem srs : srsDefs) {
		srsDao.create(srs);
	    }

	    SpatialReferenceSystem srs = srsDao.getOrCreateCode(srsOrganization, organizationCoordSysId);

	    ContentsDao contentsDao = geoPackage.getContentsDao();

	    RTreeIndexExtension rTreeIndexExtension = new RTreeIndexExtension(geoPackage);
	    boolean createSpatialIndexes = options.parameterAsBoolean(this.getClass().getName(),
		    GeoPackageConstants.PARAM_CREATE_SPATIAL_INDEXES, false);

	    // create gpkg_data_columns table
	    schemaExtension = new SchemaExtension(geoPackage);
	    schemaExtension.createDataColumnsTable();
	    
	    /*
	     * Create data column constraints for enumerations.
	     */
	    for (ClassInfo ci : cisToProcess) {

		if (ci.category() == Options.ENUMERATION) {

		    /*
		     * NOTE: Schema extension is automatically registered by GeoPackage API
		     */
		    createEnumColumnConstraints(geoPackage, ci);
		}
	    }

	    boolean geometryColumnsTableCreated = false;

	    // create tables
	    for (ClassInfo ci : cisToProcess) {

		if (ci.category() == Options.ENUMERATION) {
		    // already encoded as data column constraints
		    continue;
		}

		if (ci.category() == Options.CODELIST) {
		    result.addWarning(this, 113, ci.name());
		    continue;
		}

		// class is a feature type, object type, or data type

		/*
		 * Check class properties for geometric properties and properties with max
		 * multiplicity greater than 1. Create data columns table if necessary.
		 */
		int numberOfGeometryProperties = 0;
		PropertyInfo geometryPi = null;
		byte m = 0;
		byte z = 0;
		PropertyInfo identifierPi = null;

		for (PropertyInfo pi : ci.propertiesAll()) {

		    if (!isEncoded(pi)) {
			continue;
		    }

		    if (isGeometryTypedProperty(pi)) {
			numberOfGeometryProperties++;
			geometryPi = pi;

			m = getMValue(pi);
			z = getZValue(pi);

			if (!geometryColumnsTableCreated) {
			    geoPackage.createGeometryColumnsTable();
			    geometryColumnsTableCreated = true;
			}
		    }

		    if (pi.cardinality().maxOccurs > 1) {
			MessageContext mc = result.addWarning(this, 103, pi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
		    }

		    if (pi.isAttribute() && pi.stereotype("identifier")
			    && ci.matches(GeoPackageConstants.RULE_TGT_GPKG_CLS_IDENTIFIER_STEREOTYPE)) {
			identifierPi = pi;
			GeoPackageDataType gpkgType = mapGeoPackageDataType(pi);
			if (gpkgType != GeoPackageDataType.INTEGER) {
			    MessageContext mc = result.addInfo(this, 110, pi.name(), ci.name(), pi.typeInfo().name);
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullNameInSchema());
			    }
			}
		    }
		}

		String identifierColumnName = identifierPi == null ? normalize(idColumnName)
			: normalize(identifierPi.name());

		if (numberOfGeometryProperties > 1) {
		    MessageContext mc = result.addWarning(this, 100, ci.name(), geometryPi.name());
		    if (mc != null) {
			mc.addDetail(this, 2, ci.fullNameInSchema());
		    }
		}

		String tableName = normalize(ci.name());

		Contents contents = new Contents();
		contents.setTableName(tableName);
		contents.setDescription(ci.derivedDocumentation(GeoPackageTemplate.documentationTemplate,
			GeoPackageTemplate.documentationNoValue));

		if (geometryPi != null) {

		    // create feature table

		    contents.setSrs(srs);
		    contents.setDataType(ContentsDataType.FEATURES);

		    List<FeatureColumn> columns = new ArrayList<>();

		    columns.add(FeatureColumn.createPrimaryKeyColumn(identifierColumnName));

		    for (PropertyInfo pi : ci.propertiesAll()) {

			if (!isEncoded(pi) || identifierPi == pi) {
			    continue;
			}

			String columnName = normalize(pi.name());

			if (isGeometryTypedProperty(pi)) {

			    if (pi != geometryPi) {

				MessageContext mc = result.addWarning(this, 109, ci.name(), geometryPi.name(),
					pi.name());
				if (mc != null) {
				    mc.addDetail(this, 1, pi.fullNameInSchema());
				}

			    } else {

				GeometryType gpkgType = mapGeometryTypedProperty(pi);

				if (gpkgType == null) {
				    MessageContext mc = result.addWarning(this, 101, pi.name(), pi.typeInfo().name);
				    if (mc != null) {
					mc.addDetail(this, 1, pi.fullNameInSchema());
				    }
				    gpkgType = GeometryType.GEOMETRY;
				}

				columns.add(FeatureColumn.createGeometryColumn(columnName, gpkgType,
					pi.cardinality().minOccurs != 0, null));

				GeometryColumns gc = new GeometryColumns();
				gc.setColumnName(columnName);
				gc.setSrs(srs);
				gc.setGeometryType(gpkgType);
				gc.setContents(contents);
				gc.setM(m);
				gc.setZ(z);

				GeometryColumnsDao geometryColumnsDao = geoPackage.getGeometryColumnsDao();
				geometryColumnsDao.create(gc);
			    }

			} else {

			    if (pi.categoryOfValue() == Options.CODELIST
				    || pi.categoryOfValue() == Options.ENUMERATION) {

				columns.add(FeatureColumn.createColumn(columnName, GeoPackageDataType.TEXT,
					pi.cardinality().minOccurs != 0, pi.initialValue()));

			    } else {

				GeoPackageDataType gpkgType = mapGeoPackageDataType(pi);

				if (gpkgType == null) {
				    MessageContext mc = result.addWarning(this, 102, pi.name(), pi.typeInfo().name);
				    if (mc != null) {
					mc.addDetail(this, 1, pi.fullNameInSchema());
				    }
				    gpkgType = GeoPackageDataType.TEXT;
				}

				columns.add(FeatureColumn.createColumn(columnName, gpkgType,
					pi.cardinality().minOccurs != 0, pi.initialValue()));
			    }
			}

			// create entry in gpkg_data_columns for any property, so we can record its
			// human-readable name and description (GeoPackage Schema extension)
			createDataColumnsEntry(pi, contents, geoPackage);

		    }

		    FeatureTable table = new FeatureTable(tableName, columns);
		    geoPackage.createFeatureTable(table);
		    if (createSpatialIndexes) {
			rTreeIndexExtension.create(table);
		    }

		} else {

		    // create attribute table
		    contents.setDataType(ContentsDataType.ATTRIBUTES);

		    List<AttributesColumn> columns = new ArrayList<>();

		    columns.add(AttributesColumn.createPrimaryKeyColumn(identifierColumnName));

		    for (PropertyInfo pi : ci.propertiesAll()) {

			if (!isEncoded(pi) || identifierPi == pi) {
			    continue;
			}

			String columnName = normalize(pi.name());

			if (pi.categoryOfValue() == Options.CODELIST || pi.categoryOfValue() == Options.ENUMERATION) {

			    columns.add(AttributesColumn.createColumn(columnName, GeoPackageDataType.TEXT,
				    pi.cardinality().minOccurs != 0, pi.initialValue()));

			} else {

			    GeoPackageDataType gpkgType = mapGeoPackageDataType(pi);

			    if (gpkgType == null) {
				MessageContext mc = result.addWarning(this, 102, pi.name(), pi.typeInfo().name);
				if (mc != null) {
				    mc.addDetail(this, 1, pi.fullNameInSchema());
				}
				gpkgType = GeoPackageDataType.TEXT;
			    }

			    columns.add(AttributesColumn.createColumn(columnName, gpkgType,
				    pi.cardinality().minOccurs != 0, pi.initialValue()));
			}

			// create entry in gpkg_data_columns for any property, so we can record its
			// human-readable name and description (GeoPackage Schema extension)
			createDataColumnsEntry(pi, contents, geoPackage);
		    }

		    AttributesTable table = new AttributesTable(tableName, columns);
		    geoPackage.createAttributesTable(table);
		}

		contentsDao.create(contents);

	    }

	    result.addResult(getTargetName(), outputDirectory, outputFilename, null);

	} catch (SQLException e) {
	    result.addError(this, 108, e.getMessage());
	}

    }

    private byte getZValue(PropertyInfo pi) {

	String tv = pi.taggedValue("gpkgZ");

	if (StringUtils.isBlank(tv)) {
	    return gpkgZ;
	} else {

	    switch (tv.trim()) {
	    case "0":
		return 0;
	    case "1":
		return 1;
	    case "2":
		return 2;

	    default:
		MessageContext mc = result.addWarning(this, 111, pi.name(), pi.inClass().name(), tv.trim(), "" + gpkgZ);
		if (mc != null) {
		    mc.addDetail(this, 1, pi.fullNameInSchema());
		}
		return gpkgZ;
	    }
	}
    }

    private byte getMValue(PropertyInfo pi) {

	String tv = pi.taggedValue("gpkgM");

	if (StringUtils.isBlank(tv)) {
	    return gpkgM;
	} else {

	    switch (tv.trim()) {
	    case "0":
		return 0;
	    case "1":
		return 1;
	    case "2":
		return 2;

	    default:
		MessageContext mc = result.addWarning(this, 112, pi.name(), pi.inClass().name(), tv.trim(), "" + gpkgM);
		if (mc != null) {
		    mc.addDetail(this, 1, pi.fullNameInSchema());
		}
		return gpkgM;
	    }
	}
    }

    /**
     * Create entry in table gpkg_data_columns. Column constraint_name is only
     * filled out if the property has an enumeration as data type.
     */
    private void createDataColumnsEntry(PropertyInfo pi, Contents contents, GeoPackage geoPackage) {

	// add entry in data columns table
	DataColumns dc = new DataColumns();

	// Note: setting contents also sets table_name
	dc.setContents(contents);
	dc.setColumnName(normalize(pi.name()));
	// Do not set name, because it would have to be UNIQUE
	dc.setDescription(pi.derivedDocumentation(GeoPackageTemplate.documentationTemplate,
		GeoPackageTemplate.documentationNoValue));
	if (pi.aliasName() != null) {
	    dc.setTitle(pi.aliasName().trim());
	}
	// Encoding of code list values is currently not supported by the target, so
	// only check for enumerations
	if (pi.categoryOfValue() == Options.ENUMERATION) {
	    dc.setConstraintName(normalize(pi.typeInfo().name));
	}
	DataColumnsDao dataColumnsDao = schemaExtension.getDataColumnsDao();
	try {
	    dataColumnsDao.create(dc);
	} catch (SQLException e) {
	    MessageContext mc = result.addError(this, 107, pi.name(), e.getMessage());
	    if (mc != null) {
		mc.addDetail(this, 1, pi.fullNameInSchema());
	    }
	}
    }

    private boolean valueTypeHasEnums(PropertyInfo pi) {

	ClassInfo typeCi = pi.typeClass();

	if (typeCi != null && typeCi.category() == Options.ENUMERATION) {
	    for (PropertyInfo tpi : typeCi.propertiesAll()) {
		if (isEncoded(tpi)) {
		    return true;
		}
	    }
	}

	return false;
    }

    protected void createEnumColumnConstraints(GeoPackage geoPackage, ClassInfo ci) {

	// create/update data columns constraints table
	schemaExtension.createDataColumnConstraintsTable();

	DataColumnConstraintsDao dataColumnConstraintsDao = schemaExtension.getDataColumnConstraintsDao();

	String constraintName = normalize(ci.name());

	try {
	    for (PropertyInfo pi : ci.propertiesAll()) {
		DataColumnConstraints dcc = new DataColumnConstraints();
		dcc.setConstraintName(constraintName);
		dcc.setConstraintType(DataColumnConstraintType.ENUM);
		dcc.setValue(pi.name());
		dcc.setDescription(pi.derivedDocumentation(GeoPackageTemplate.documentationTemplate,
			GeoPackageTemplate.documentationNoValue));
		dataColumnConstraintsDao.create(dcc);
	    }
	} catch (SQLException e) {
	    result.addError(this, 105, ci.name(), e.getMessage());
	}

    }

    private GeoPackageDataType mapGeoPackageDataType(PropertyInfo pi) {

	ProcessMapEntry me = pi.options().targetMapEntry(pi.typeInfo().name, pi.encodingRule("gpkg"));

	if (me == null) {
	    return null;
	} else {

	    switch (me.getTargetType().toUpperCase(Locale.ENGLISH)) {
	    case "BLOB":
		return GeoPackageDataType.BLOB;
	    case "BOOLEAN":
		return GeoPackageDataType.BOOLEAN;
	    case "DATE":
		return GeoPackageDataType.DATE;
	    case "DATETIME":
		return GeoPackageDataType.DATETIME;
	    case "DOUBLE":
		return GeoPackageDataType.DOUBLE;
	    case "FLOAT":
		return GeoPackageDataType.FLOAT;
	    case "INT":
		return GeoPackageDataType.INT;
	    case "INTEGER":
		return GeoPackageDataType.INTEGER;
	    case "MEDIUMINT":
		return GeoPackageDataType.MEDIUMINT;
	    case "REAL":
		return GeoPackageDataType.REAL;
	    case "SMALLINT":
		return GeoPackageDataType.SMALLINT;
	    case "TEXT":
		return GeoPackageDataType.TEXT;
	    case "TINYINT":
		return GeoPackageDataType.TINYINT;
	    default:
		return null;
	    }
	}
    }

    private GeometryType mapGeometryTypedProperty(PropertyInfo pi) {

	ProcessMapEntry me = pi.options().targetMapEntry(pi.typeInfo().name, pi.encodingRule("gpkg"));

	if (me == null) {
	    return null;
	} else {

	    switch (me.getTargetType().toUpperCase(Locale.ENGLISH)) {
	    case "CIRCULARSTRING":
		return GeometryType.CIRCULARSTRING;
	    case "COMPOUNDCURVE":
		return GeometryType.COMPOUNDCURVE;
	    case "CURVE":
		return GeometryType.CURVE;
	    case "CURVEPOLYGON":
		return GeometryType.CURVEPOLYGON;
	    case "GEOMETRY":
		return GeometryType.GEOMETRY;
	    case "GEOMETRYCOLLECTION":
		return GeometryType.GEOMETRYCOLLECTION;
	    case "LINESTRING":
		return GeometryType.LINESTRING;
	    case "MULTICURVE":
		return GeometryType.MULTICURVE;
	    case "MULTILINESTRING":
		return GeometryType.MULTILINESTRING;
	    case "MULTIPOINT":
		return GeometryType.MULTIPOINT;
	    case "MULTIPOLYGON":
		return GeometryType.MULTIPOLYGON;
	    case "MULTISURFACE":
		return GeometryType.MULTISURFACE;
	    case "POINT":
		return GeometryType.POINT;
	    case "POLYGON":
		return GeometryType.POLYGON;
	    case "POLYHEDRALSURFACE":
		return GeometryType.POLYHEDRALSURFACE;
	    case "SURFACE":
		return GeometryType.SURFACE;
	    case "TIN":
		return GeometryType.TIN;
	    case "TRIANGLE":
		return GeometryType.TRIANGLE;
	    default:
		return null;
	    }
	}
    }

    private String normalize(String name) {
	return name.trim().toLowerCase().replaceAll("\\s", "_");
    }

    private boolean isGeometryTypedProperty(PropertyInfo pi) {
	String typeName = pi.typeInfo().name;
	return typeName.startsWith("GM_") || typeName.equalsIgnoreCase("DirectPosition");
    }

    @Override
    public void reset() {

	model = null;

	initialised = false;
	diagnosticsOnly = false;

	documentationTemplate = null;
	documentationNoValue = null;

	outputDirectory = null;
	outputFilename = null;

	numberOfEncodedSchemas = 0;

	idColumnName = null;

	gpkgM = 0;
	gpkgZ = 0;

	cisToProcess = new ArrayList<ClassInfo>();
	srsDefs = new ArrayList<>();
	organizationCoordSysId = 0;
	srsOrganization = "EPSG";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: class GeoPackageTemplate";
	case 1:
	    return "Property '$1$'";
	case 2:
	    return "Class '$1$'";
	case 3:
	    return "Processing class '$1$'.";
	case 4:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 5:
	    return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";
	case 6:
	    return "";
	case 7:
	    return "Schema '$1$' is not encoded.";
	case 8:
	    return "Class '$1$' is not encoded.";

	case 16:
	    return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that it will not be represented in the GeoPackage template.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 19:
	    return "Property '$1$' is not encoded.";

	case 25:
	    return "Value of configuration parameter '$1$' is '$2$'. The file does not exist, is a directory, or cannot be read.";

	case 100:
	    return "Class '$1$' has more than one geometry properties. All geometric properties except '$2$' will be ignored.";
	case 101:
	    return "Property '$1$' has geometry value type '$2$', for which no map entry is configured that maps that type to one of the geometry types recognized by GeoPackage. The property will be encoded with type GEOMETRY.";
	case 102:
	    return "Property '$1$' has value type '$2$', for which no map entry is configured that maps that type to one of the data types recognized by GeoPackage. The property will be encoded with type TEXT.";
	case 103:
	    return "Property '$1$' has max multiplicity greater than 1. This is currently not supported by the target. The property will be encoded as if it had max multiplicity = 1.";
	case 104:
	    return "Class '$1$' has no relevant properties. It will be ignored.";
	case 105:
	    return "SQL Exception occurred while creating data column constraints for enumeration '$1$'. Exception message is: $2$";
	case 106:
	    return "";
	case 107:
	    return "SQL Exception occurred while creating a data columns table entry for property '$1$'. Exception message is: $2$";
	case 108:
	    return "SQL Exception occurred while creating the GeoPackage template. Exception message is: $1$";
	case 109:
	    return "Class '$1$' has multiple geometric properties. Only property '$2$' will be encoded. Property '$3$' is ignored.";
	case 110:
	    return "Property '$1$' of class '$2$' is an identifier property with type '$3$'. It will be encoded with GeoPackage data type INTEGER.";
	case 111:
	    return "Property '$1$' of class '$2$' has tagged value gpkgZ with unrecognized value '$3$'. Using value defined by target parameter gpkgZ (which is '$4$').";
	case 112:
	    return "Property '$1$' of class '$2$' has tagged value gpkgM with unrecognized value '$3$'. Using value defined by target parameter gpkgM (which is '$4$').";
	case 113:
	    return "Ignoring values in code list '$1$'. Encoding of code list values is currently not supported by the target.";

	case 503:
	    return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
	case 504:
	    return "File has been deleted.";

	default:
	    return "(" + GeoPackageTemplate.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
