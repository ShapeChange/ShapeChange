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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.SBVR.Sbvr2FolParser;
import de.interactive_instruments.ShapeChange.SBVR.SbvrConstants;
import de.interactive_instruments.ShapeChange.SBVR.SbvrRuleLoader;

public abstract class ModelImpl implements Model {

	/*
	 * flags whether postprocessing/validation has been executed
	 */
	protected boolean postprocessed = false;

	/*
	 * the list of tagged values specified by ISO 19109 (2015)
	 * 
	 * note: designation is not included, it is considered to be the same as
	 * alias
	 */
	protected static String[] iso19109Tags = { "language", "definition",
			"description" };

	/*
	 * the list of tagged values specified by the GML encoding rule
	 */
	protected static String[] gmlTags = { "targetNamespace", "xmlns", "version",
			"xsdDocument", "gmlProfileSchema", "sequenceNumber",
			"noPropertyType", "byValuePropertyType", "isCollection",
			"asDictionary", "inlineOrByReference", "isMetadata",
			"defaultCodeSpace", "xmlSchemaType", "documentation", "resourceURI",
			"codeList" };
	// TODO clean-up list

	/*
	 * the list of tagged values specified by the JSON encoding rule
	 */
	protected static String[] jsonTags = { "jsonBaseURI", "jsonLayerTableURI",
			"jsonDirectory" };

	/*
	 * the list of tagged values specified by the ArcGIS encoding rule
	 */
	protected static String[] arcgisTags = { "HasZ", "HasM", "fieldType" };

	/*
	 * the list of tagged values specified by other encoding rules
	 */
	protected static String[] shapeChangeTags = { "xsdEncodingRule",
			"xsdAsAttribute", "xsdDerivation", "gmlAsGroup", "length",
			"maxLength", "base", "rangeMinimum", "rangeMaximum", "default",
			"nilReasonAllowed", "gmlImplementedByNilReason", "primaryCode",
			"secondaryCode", "oclExpressions", "schPatterns", "unitMeasure",
			"voidable", "alias", "gmlAsCharacterString", "gmlMixin", "nillable",
			"suppress", "codeListValuePattern", "codeListRepresentation",
			"uomResourceURI", "uomResourceValuePattern",
			"uomResourceRepresentation", "physicalQuantity",
			"recommendedMeasure", "noncomparableMeasure", "asXMLAttribute",
			"soft-typed", "parent", "AAA:Kennung", "AAA:Datum",
			"AAA:Organisation", "AAA:Modellart", "AAA:Profile",
			"AAA:Grunddatenbestand", "AAA:Nutzungsart",
			"AAA:Nutzungsartkennung", "AAA:objektbildend", "AAA:Themen",
			"AAA:Revisionsnummer", "reverseRoleNAS", "allowedTypesNAS",
			"gmlArrayProperty", "gmlListProperty", "example",
			"dataCaptureStatement", "legalBasis", "profiles", "name", "infoURL",
			"broaderListedValue", "skosConceptSchemeSubclassName", "size",
			"omitWhenFlattened", "maxOccurs", "isFlatTarget", "Title",
			"formrows", "formcols", "validate", "Reiter", "generationDateTime",
			"ontologyName", "alwaysVoid", "neverVoid", "appliesTo",
			"vocabulary", "associativeTable", "jsonEncodingRule",
			"sqlEncodingRule", "status", "geometry",
			"oneToManyReferenceColumnName", "dissolveAssociation", "precision",
			"scale", "numericType", "toFeatureType", "toCodelist", "sqlUnique",
			"codelistType", "sqlOnUpdate", "sqlOnDelete", "shortName",
			"codeListSource", "codeListSourceCharset",
			"codeListSourceRepresentation", "codeListRestriction",
			"arcgisDefaultSubtype", "arcgisSubtypeCode", "arcgisUsedBySubtypes",
			"arcgisSubtypeInitialValues", "codeListXML", "reportable" };

	/*
	 * temporary storage for validating the names of the XML Schema documents to
	 * be created when processing the model
	 */
	HashSet<String> xsdDocNames;

	/*
	 * temporary storage for validating the names of classes in an application
	 * schema
	 */
	HashSet<String> classNames;

	/*
	 * List of allowed tags of tagged values
	 */
	protected HashSet<String> allowedTags = null;

	@Override
	public void postprocessAfterLoadingAndValidate() {

		if (postprocessed)
			return;

		if (options().parameter("checkingConstraints") == null || options()
				.parameter("checkingConstraints").equalsIgnoreCase("enabled")) {
			postprocessFolConstraints();
		}

		xsdDocNames = new HashSet<String>();
		classNames = new HashSet<String>();

		Options options = options();
		for (PackageInfo pi : schemas("")) {
			if (options.skipSchema(pi))
				continue;
			classNames.clear();
			postprocessPackage(pi, true);
		}
		postprocessed = true;
	}

	private void postprocessFolConstraints() {

		/*
		 * First order logic expressions can be parsed from different sources.
		 * For those where the parser does not need to be set up per constraint,
		 * we can create them outside of the following loops.
		 */
		Sbvr2FolParser sbvrParser = new Sbvr2FolParser(this);

		for (PackageInfo pi : schemas("")) {

			if (options().skipSchema(pi))
				continue;

			for (ClassInfo ci : this.classes(pi)) {

				List<Constraint> cons = ci.constraints();

				if (cons != null) {

					// sort the constraints by name
					Collections.sort(cons, new Comparator<Constraint>() {
						@Override
						public int compare(Constraint o1, Constraint o2) {
							return o1.name().compareTo(o2.name());
						}
					});
				}

				for (Constraint con : cons) {

					if (con instanceof FolConstraint) {

						FolConstraint folCon = (FolConstraint) con;

						if (folCon.sourceType()
								.equals(SbvrConstants.FOL_SOURCE_TYPE)) {

							folCon.setComments(new String[] { folCon.text() });

							FolExpression folExpr = sbvrParser.parse(folCon);

							if (folExpr != null) {
								folCon.setFolExpression(folExpr);
							} else {
								/*
								 * the parser already logged why the expression
								 * was not created
								 */
							}

						} else {

							/*
							 * Apparently a new source for FOL constraints
							 * exists - add parsing it here; in the meantime,
							 * log this as an error
							 */
							MessageContext ctx = this.result().addError(null,
									38, folCon.sourceType());
							ctx.addDetail(null, 39, folCon.name(), folCon
									.contextModelElmt().fullNameInSchema());
						}
					}
				}
			}
		}
	}

	@Override
	public void loadInformationFromExternalSources() {

		// do not execute this once the model has been postprocessed
		if (postprocessed)
			return;

		Options options = options();
		ShapeChangeResult result = result();

		// ============================================================
		// load SBVR constraint info from excel file
		// NOTE: can also be done via ConstraintLoader transformation

		String sbvrFileLocation = options()
				.parameter(Options.PARAM_CONSTRAINT_EXCEL_FILE);

		if (sbvrFileLocation != null) {

			/*
			 * if no sbvr file is provided, the loader will simply not contain
			 * any sbvr rules
			 */
			SbvrRuleLoader sbvrLoader = new SbvrRuleLoader(sbvrFileLocation,
					options, result, this);

			for (PackageInfo pi : selectedSchemas()) {

				sbvrLoader.loadSBVRRulesAsConstraints(pi);
			}
		}
	}

	@Override
	public SortedSet<PackageInfo> selectedSchemas() {
		SortedSet<PackageInfo> res = new TreeSet<PackageInfo>();

		Options options = options();
		for (PackageInfo pi : schemas("")) {
			if (!options.skipSchema(pi))
				res.add(pi);
		}
		return res;
	}

	@Override
	public SortedSet<? extends ClassInfo> selectedSchemaClasses() {

		SortedSet<ClassInfo> res = new TreeSet<ClassInfo>();

		for (PackageInfo selectedSchema : selectedSchemas()) {

			SortedSet<ClassInfo> cisOfSelectedSchema = this
					.classes(selectedSchema);

			for (ClassInfo ci : cisOfSelectedSchema) {

				res.add(ci);
			}
		}

		return res;
	}

	@Override
	public SortedSet<? extends PropertyInfo> selectedSchemaProperties() {

		SortedSet<? extends ClassInfo> selCis = this.selectedSchemaClasses();

		SortedSet<PropertyInfo> res = new TreeSet<PropertyInfo>();

		for (ClassInfo selCi : selCis) {

			for (PropertyInfo pi : selCi.properties().values()) {

				res.add(pi);
			}
		}

		return res;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public SortedSet<PackageInfo> allPackagesFromSelectedSchemas() {

		SortedSet<PackageInfo> result = new TreeSet<PackageInfo>();

		for (PackageInfo selSchema : selectedSchemas()) {

			result.add(selSchema);
			result.addAll(selSchema.containedPackagesInSameTargetNamespace());
		}

		return result;
	}

	private void postprocessPackage(PackageInfo pi, boolean processClasses) {
		if (pi == null)
			return;

		pi.postprocessAfterLoadingAndValidate();

		if (pi.matches("req-xsd-pkg-xsdDocument-unique")) {
			String xsdDocName = pi.xsdDocument();
			if (xsdDocName != null && !xsdDocName.trim().isEmpty()) {
				if (xsdDocNames.contains(xsdDocName)) {
					MessageContext mc = result().addError(null, 162,
							xsdDocName);
					if (mc != null)
						mc.addDetail(null, 400, "Package", pi.fullName());
				} else
					xsdDocNames.add(xsdDocName);
			}
		}

		if (processClasses) {
			for (ClassInfo ci : classes(pi)) {
				postprocessClass(ci);
			}
		}

		for (PackageInfo pi2 : pi.containedPackages()) {
			if (!pi2.isSchema())
				postprocessPackage(pi2, false);
		}
	}

	private void postprocessClass(ClassInfo ci) {
		if (ci == null)
			return;

		ci.postprocessAfterLoadingAndValidate();

		if (ci.matches("req-xsd-cls-name-unique")) {
			String className = ci.name();
			if (classNames.contains(className)) {
				MessageContext mc = result().addError(null, 163, className,
						ci.pkg().targetNamespace());
				if (mc != null)
					mc.addDetail(null, 400, "Package", ci.fullName());
			} else
				classNames.add(className);
		}

		for (PropertyInfo propi : ci.properties().values()) {
			postprocessProperty(propi);
		}

		// TODO currently there is no way to get all operations of a class, so
		// we cannot validate them right now
	}

	private void postprocessProperty(PropertyInfo propi) {
		if (propi == null)
			return;

		propi.postprocessAfterLoadingAndValidate();

		if (!propi.isAttribute()) {
			postprocessAssociation(propi.association());
		}
	}

	private void postprocessAssociation(AssociationInfo ai) {
		if (ai == null)
			return;

		ai.postprocessAfterLoadingAndValidate();
	}

	// Tagged values normalization. This returns the tag given or a
	// de-deprecated tag or null.
	public String normalizeTaggedValue(String tag) {

		// If not yet done, set up the tagged values, which we allow
		if (allowedTags == null) {
			allowedTags = new HashSet<String>(100);
			for (String s : iso19109Tags)
				allowedTags.add(s);
			for (String s : gmlTags)
				allowedTags.add(s);
			for (String s : jsonTags)
				allowedTags.add(s);
			for (String s : arcgisTags)
				allowedTags.add(s);
			for (String s : shapeChangeTags)
				allowedTags.add(s);
			for (String s : options().parameter("representTaggedValues")
					.split("\\,"))
				allowedTags.add(s.trim());
			for (String s : options().parameter("addTaggedValues").split("\\,"))
				allowedTags.add(s.trim());
		}

		// Note: UML tools may have their own way of naming tagged values.
		// Therefore, tool specific mappings are included here, too. Currently
		// specific
		// support exists for Rational Rose and Enterprise Architect.
		if (tag.startsWith("RationalRose$UGAS:")) {
			tag = tag.substring(18);
		} else if (tag.startsWith("RationalRose$ShapeChange:")) {
			tag = tag.substring(25);
		} else if (tag.startsWith("RationalRose$")) {
			tag = tag.substring(13);
		} else if (tag.contains("::")) {
			// tagged value is qualified - remove name space
			tag = tag.substring(tag.lastIndexOf("::") + 2);
		}

		// Now check tag aliases provided in the configuration
		tag = options().normalizeTag(tag);

		// So, if it's one of these just return the argument ...
		if (allowedTags.contains(tag))
			return tag;
		// Now allow for some deprecated stuff
		if (tag.equals("xmlNamespace"))
			return "targetNamespace";
		if (tag.equals("xmlNamespaceAbbreviation"))
			return "xmlns";
		if (tag.equals("xsdName"))
			return "xsdDocument";
		if (tag.equals("asGroup"))
			return "gmlAsGroup";
		if (tag.equals("implementedByNilReason"))
			return "gmlImplementedByNilReason";

		// TBD: add input parameter to allow any tag

		// None of these, return null
		return null;
	}

	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString, String user,
			String pwd) throws ShapeChangeAbortException {

		throw new ShapeChangeAbortException(
				"Initialization of repository with username and password not supported for this type of input model.");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean isInSelectedSchemas(ClassInfo ci) {

		SortedSet<? extends PackageInfo> selectedSchemas = this
				.selectedSchemas();

		if (selectedSchemas == null || selectedSchemas.isEmpty()) {

			return false;

		} else {

			for (PackageInfo selectedSchema : selectedSchemas) {

				if (ci.inSchema(selectedSchema)) {
					return true;
				}
			}

			// ci is not part of any of the selected schemas
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public PackageInfo schemaPackage(ClassInfo ci) {

		PackageInfo p = ci.pkg();

		do {
			if (p.isSchema() || p.isAppSchema()) {
				return p;
			} else {
				p = p.owner();
			}
		} while (p != null);

		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public SortedSet<PackageInfo> packages(PackageInfo pkg) {

		SortedSet<PackageInfo> result = new TreeSet<PackageInfo>();

		if (pkg.targetNamespace() != null) {

			SortedSet<PackageInfo> allPackages = this.packages();

			for (PackageInfo pi : allPackages) {
				if (pi.targetNamespace() != null
						&& pi.targetNamespace().equals(pkg.targetNamespace())) {
					result.add(pi);
				}
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public SortedSet<PackageInfo> schemas(String name) {

		SortedSet<PackageInfo> res = new TreeSet<PackageInfo>();

		for (PackageInfo pi : packages()) {

			if (pi.isSchema()) {
				if (name != null && !name.equals("")) {
					if (pi.name().equals(name)) {

						res.add(pi);
					}
				} else {
					res.add(pi);
				}
			}
		}
		return res;
	}

	@Override
	public ClassInfo classByIdOrName(Type typeInfo) {
		if (typeInfo == null) {
			return null;
		} else {
			ClassInfo result = this.classById(typeInfo.id);
			if (result == null) {
				result = this.classByName(typeInfo.name);
			}
			return result;
		}
	}
}
