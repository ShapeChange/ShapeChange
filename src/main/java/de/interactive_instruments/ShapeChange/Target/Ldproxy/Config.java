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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ldproxy;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.simple.JSONObject;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;

/**
 * This target generates an ldproxy configuration for the feature types in
 * the selected application schemas. ldproxy is a software for publishing 
 * spatial data via a Web API according the WFS 3.0 approach, consistent
 * with the W3C/OGC Spatial Data on the Web Best Practices and the OpenAPI
 * specification.
 * <p>
 * Supported are datasets that are stored in PostgreSQL/PostGIS. A number
 * of options are supported for the mapping from the application schema to
 * the SQL DDL in the database.
 * 
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @see <a href="https://interactive-instruments.github.io/ldproxy/">ldproxy</a>
 * @see <a href="https://cdn.rawgit.com/opengeospatial/WFS_FES/3.0.0-draft.1/docs/17-069.html">WFS 3.0, Core (Draft)</a>
 * @see <a href="https://www.w3.org/TR/sdw-bp/">W3C/OGC Spatial Data on the Web Best Practices</a>
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md">OpenAPI Specification</a>
 */
public class Config implements SingleTarget, MessageSource {

	// TODO: document on shapechange.net, based on Javadoc comments 

	private static boolean initialised = false;

	/**
	 * The directory where the configuration file(s) will be created.
	 */
	private static String outputDirectory = null;
	
	/**
	 * The filename of the ldproxy configuration file, without the file extension.
	 * ".json" will be added, when the file is written.
	 */
	private static String outputFilename = null;
	
	/**
	 * @see ConfigConstants#PARAM_SERVICE_ID
	 */
	private static String srvid = null;

	/**
	 * @see ConfigConstants#PARAM_SERVICE_LABEL
	 */
	private static String srvlabel = null;

	/**
	 * @see ConfigConstants#PARAM_SERVICE_DESC
	 */
	private static String srvdesc = null;

	/**
	 * @see ConfigConstants#PARAM_PRIMARY_KEY_FIELD
	 */
	private static String primaryKeyField = null;

	/**
	 * @see ConfigConstants#PARAM_FOREIGN_KEY_SUFFIX
	 */
	private static String foreignKeySuffix = null;

	/**
	 * @see ConfigConstants#PARAM_MAX_LENGTH
	 */
	private static Integer maxLength = null;

	/**
	 * @see ConfigConstants#PARAM_GEOMETRY_TABLE
	 */
	private static String geometryTable = null;

	/**
	 * @see ConfigConstants#PARAM_GEOMETRY_FIELD
	 */
	private static String geometryField = null;

	/**
	 * @see ConfigConstants#PARAM_NTOM_TABLE_TEMPLATE
	 */
	private static String templateNtoM = null;

	/**
	 * @see ConfigConstants#PARAM_1TON_TABLE_TEMPLATE
	 */
	private static String template1toN = null;

	/**
	 * @see ConfigConstants#PARAM_ROOT_FEATURE_TABLE
	 */
	private static String rootFeatureTable = null;

	/**
	 * @see ConfigConstants#PARAM_ROOT_COLLECTION_FIELD
	 */
	private static String rootCollectionField = null;

	/**
	 * @see ConfigConstants#PARAM_FILTERS
	 */
	private static Set<String> filterableFields = null;
	
	/**
	 * The JSON object representing the main configuration file.
	 */
	private static JSONObject cfgobj = null;
	
	/**
	 * The "featureTypes" object in {@link #cfgobj}. We need this
	 * as a variable as we add information for each feature type
	 * that is processed.
	 */
	private static JSONObject collections = null;
	
	/**
	 * The "featureProvider.mappings" object in {@link #cfgobj}.
	 * We need this as a variable as we add information for each 
	 * feature type that is processed.
	 */
	private static JSONObject mappings = null;
	
	/**
	 * The writer to export the main configuration file.
	 */
	private static FileWriter writer = null;

	/**
	 * The model that is processed.
	 */
	private static Model model = null;

	/**
	 * The ShapeChange configuration for this process.
	 */
	private Options options = null;
	
	/**
	 * The log where messages will be written.
	 */
	private ShapeChangeResult result = null;

	@SuppressWarnings("unchecked")
	@Override
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		options = o;
		result = r;

		try {

			if (!initialised) {

				initialised = true;

				model = m;

				outputDirectory = options.parameter(Config.class.getName(),
						"outputDirectory");
				if (outputDirectory == null)
					outputDirectory = options.parameter("outputDirectory");
				if (outputDirectory == null)
					outputDirectory = ".";

				outputFilename = options.parameter(Config.class.getName(),
						"outputFilename");
				if (outputFilename == null)
					outputFilename = "ldproxy_config";

				String jsonName = outputFilename + ".json";

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
					result.addFatalError(this, 6, outputDirectory);
					throw new ShapeChangeAbortException();
				}

				// If a dry run is requested, simply do not create the writer. Everything will be
				// executed as normal, but nothing will be written.
				if (!diagOnly) {
					writer = new FileWriter(outputDirectory + "/" + jsonName);
				}
				
				// We select an arbitrary schema from the set of selected schemas to derive defaults
				// for some target parameters.
				PackageInfo first = (model.allPackagesFromSelectedSchemas().isEmpty() ? null : model.allPackagesFromSelectedSchemas().first());
				
				// Establish the parameter values. Use defaults where no value is provided in the
				// configuration.
				srvid = options.parameter(Config.class.getName(), ConfigConstants.PARAM_SERVICE_ID);
				if (srvid == null)
					srvid = ( first != null ? first.xmlns() : null );
				if (srvid == null)
					srvid = "fixme";

				srvlabel = options.parameter(Config.class.getName(), ConfigConstants.PARAM_SERVICE_LABEL);
				if (srvlabel == null)
					srvlabel = ( first != null ? ((first.aliasName()!=null && first.aliasName().length()>0) ? first.aliasName() : first.name()) : "Some Dataset" );
				
				srvdesc = options.parameter(Config.class.getName(), ConfigConstants.PARAM_SERVICE_DESC);
				if (srvdesc == null)
					srvdesc = ( first != null ? (first.documentation()!=null ? first.documentation() : first.definition()) : "" );
				
				primaryKeyField = options.parameter(Config.class.getName(), ConfigConstants.PARAM_PRIMARY_KEY_FIELD);
				if (primaryKeyField == null)
					primaryKeyField = "id";
				
				foreignKeySuffix = options.parameter(Config.class.getName(), ConfigConstants.PARAM_FOREIGN_KEY_SUFFIX);
				if (foreignKeySuffix == null)
					foreignKeySuffix = "_id";

				geometryTable = options.parameter(Config.class.getName(), ConfigConstants.PARAM_GEOMETRY_TABLE);
				if (geometryTable == null)
					geometryTable = "geom";
				
				geometryField = options.parameter(Config.class.getName(), ConfigConstants.PARAM_GEOMETRY_FIELD);
				if (geometryField == null)
					geometryField = "geom";
				
				templateNtoM = options.parameter(Config.class.getName(), ConfigConstants.PARAM_NTOM_TABLE_TEMPLATE);
				if (templateNtoM == null)
					templateNtoM = "{{class}}_2_{{property}}";
				
				template1toN = options.parameter(Config.class.getName(), ConfigConstants.PARAM_1TON_TABLE_TEMPLATE);
				if (template1toN == null)
					template1toN = "{{class}}_{{property}}";

				rootFeatureTable = options.parameter(Config.class.getName(), ConfigConstants.PARAM_ROOT_FEATURE_TABLE);
				if (rootFeatureTable == null)
					rootFeatureTable = "root";
				
				rootCollectionField = options.parameter(Config.class.getName(), ConfigConstants.PARAM_ROOT_COLLECTION_FIELD);
				if (rootCollectionField == null)
					rootCollectionField = "collection";

				filterableFields = new TreeSet<String>();
				String s  = options.parameter(Config.class.getName(), ConfigConstants.PARAM_FILTERS);
				if (s != null) {
					String[] sarr = s.split(",");
					for (String s2 : sarr) {
						filterableFields.add(s2.trim());
					}
				}
								
				s = options.parameter(Config.class.getName(), ConfigConstants.PARAM_MAX_LENGTH);
				if (s != null)
					maxLength = new Integer(s);
				if (maxLength == null)
					maxLength = 60;
				
				// Initialize the JSON object that will become the configuration
				cfgobj = new JSONObject();		
				
				// Populate with metadata from the configuration or the model
				cfgobj.put("id", srvid);
				cfgobj.put("label", srvlabel);
				cfgobj.put("description", srvdesc);

				// This will be published as a WFS 3 service that should be started when ldproxy starts
				cfgobj.put("serviceType", "WFS3");
				cfgobj.put("shouldStart", true);

				// Set timestamps to the current time
				long now = System.currentTimeMillis();
				// use a "secret" override to set this to a fixed value in order to support unit tests
				if (options.parameter(Config.class.getName(), "_unitTestOverride")!=null)
					now = 1531327916566L;
				cfgobj.put("createdAt", now);
				cfgobj.put("lastModified", now);

				// Create a collections object. This will be populated when the feature types are processed.
				collections = new JSONObject();
				cfgobj.put("featureTypes", collections);
				
				// Create a feature provider object. Currently only PostgreSQL/PostGIS is supported, so 
				// this information is pre-configured. The database information and the credentials 
				// need to be updated manually. 
				JSONObject featureProvider = new JSONObject();
				cfgobj.put("featureProvider", featureProvider);				
				featureProvider.put("providerType", "PGIS");
				JSONObject connectionInfo = new JSONObject();
				featureProvider.put("connectionInfo", connectionInfo);
				connectionInfo.put("host", "FIXME");
				connectionInfo.put("database", "FIXME");
				connectionInfo.put("user", "FIXME");
				connectionInfo.put("password", "FIXME-base64encoded");

				// Create a mappings object, which will be populated when the feature types are processed.
				mappings = new JSONObject();
				featureProvider.put("mappings", mappings);				
			}

		} catch (Exception e) {

			String msg = e.getMessage();
			if (msg != null) {
				result.addError(msg);
			}
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void reset() {

		initialised = false;

		model = null;

		foreignKeySuffix = null;
		primaryKeyField = null;
		maxLength = null;
		templateNtoM = null;
		template1toN = null;
		rootFeatureTable = null;
		rootCollectionField = null;
		filterableFields = null;
		
		outputDirectory = null;
		outputFilename = null;
		writer = null;

		cfgobj = null;
		collections = null;
		mappings = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(ClassInfo ci) {
		
		// This target processes the feature types of the selected schemas and
		// generates the ldproxy JSON configuration. All other classifiers can
		// be ignored. Where their information is needed, it will be accessed when
		// processing the feature type properties.		
		if (ci.category()!=Options.FEATURE)
			return;
		
		// The name of a table for the feature type must be based on the class name
		String tabname = deriveName(ci);
		
		// The name for the collection is the same as the table name.
		String colname = tabname;
		
		// The label for the class uses the alias, if available, otherwise the model name.
		String alias = ci.aliasName();
		String label = (alias!=null && alias.length()>0) ? alias : ci.name();
		
		// Add non-abstract feature types to the list of collections served by the API
		if (ci.matches(ConfigConstants.RULE_TGT_LDP_CLS_NAFT_AS_COLLECTION) &&
			!ci.isAbstract()) {

			// Set the collection name
			JSONObject featuretype = new JSONObject();
			collections.put(colname, featuretype);
			
			// Add descriptors. The id must be the same as the JSON property above
			featuretype.put("id", colname);
			featuretype.put("label", label);
			featuretype.put("description", ci.documentation());
			
			// We do not know the spatial or temporal extents of the dataset, so we use
			// default values. These need to be changed manually. 
			JSONObject extent = new JSONObject();
			featuretype.put("extent", extent);
			JSONObject spatial = new JSONObject();
			extent.put("spatial", spatial);
			spatial.put("xmin", -180);
			spatial.put("xmax", 180);
			spatial.put("ymin", -90);
			spatial.put("ymax", 90);
			JSONObject temporal = new JSONObject();
			extent.put("temporal", temporal);
			temporal.put("start", null);
			temporal.put("end", null);
		}

		if (ci.isAssocClass() != null) {
			// Currently no rule exists for association classes
			result.addWarning(this, 101, ci.name());
		}

		// Add non-abstract feature type tables to the mappings. Abstract
		// feature types, object types or data types are not considered 
		// here as the mappings are organised per published feature type.
		if (ci.matches(ConfigConstants.RULE_TGT_LDP_CLS_TABLE_PER_FT) &&
			!ci.isAbstract()) {
			JSONObject featuretype = new JSONObject();
			mappings.put(colname, featuretype);
			JSONObject path = new JSONObject();
			
			// The mapping path starts with the feature table.
			String basepath = "/"+tabname;
			featuretype.put(basepath, path);

			JSONObject general = new JSONObject();
			path.put("general", general);
			general.put("mappingType", "GENERIC_PROPERTY");
			general.put("enabled", true);
			
			// Add default schema.org mapping in HTML
			JSONObject html = new JSONObject();
			path.put("text/html", html);
			// {{Full Name}} references the label
			html.put("name", "{{Full Name}}");
			html.put("mappingType", "MICRODATA_PROPERTY");
			html.put("itemType", "http://schema.org/Place");
			
			// Nothing to do for GeoJSON
			
			// Typically, each table, including a feature type table, will have a primary key field
			if (ci.matches(ConfigConstants.RULE_TGT_LDP_CLS_ID_FIELD)) {
				createIdProperty(featuretype, basepath);
			}
			
			// Add mappings for all supertype properties
			if (!ci.supertypes().isEmpty()) {
				for (String sid : ci.supertypes()) {
					processSupertypeProperties(ci, model.classById(sid), featuretype, basepath);
				}
			}			

			// Add mappings for all properties
			if (!ci.properties().isEmpty()) {
				for (PropertyInfo pi : ci.properties().values()) {
					processProperty(pi, featuretype, basepath, null, tabname);
				}
			}			
		}
	}

	@Override
	public void write() {
		// Nothing to do here, since this is a SingleTarget
	}

	@Override
	public String getTargetName(){
		return "ldproxy Configuration";
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		result = r;
		options = r.options();

		try {

			// If diagOnly was selected, writer will be 'null'
			if (writer!=null) {
				writer.write(cfgobj.toJSONString());
				writer.flush();
				writer.close();
			}
			
		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);

		} finally {

			// Close the writer
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
					e.printStackTrace(System.err);
				}
			}

			// Release the model - do NOT close it here
			model = null;
		}
	}

	/**
	 * Derive a table or field name from the name of a model element 
	 * @param i	the model element
	 * @return	name of the table/field in the database for the model element
	 * @see ConfigConstants#RULE_TGT_LDP_ALL_NAMES_LOWERCASE
	 * @see ConfigConstants#RULE_TGT_LDP_ALL_NAMES_MAXLENGTH
	 * @see ConfigConstants#PARAM_MAX_LENGTH
	 */
	private String deriveName(Info i) {
		String result = i.name();
		if (i.matches(ConfigConstants.RULE_TGT_LDP_ALL_NAMES_LOWERCASE)) {
			result = result.toLowerCase();
		}
		if (i.matches(ConfigConstants.RULE_TGT_LDP_ALL_NAMES_MAXLENGTH) && result.length()>maxLength) {
			result = result.substring(0, maxLength);
		}
		return result;
	}
	
	/**
	 * Derive the name of an intermediate table capable of representing 
	 * a property in the model with a many-to-many relation
	 * @param tabname	name of the table representing the class that the property belongs to
	 * @param fieldname	name of the field representing a single value of the property 
	 * @return	name of the intermediate table in the database
	 * @see ConfigConstants#RULE_TGT_LDP_PROP_DT_AS_NTOM
	 * @see ConfigConstants#RULE_TGT_LDP_PROP_FT_AS_NTOM
	 * @see ConfigConstants#PARAM_NTOM_TABLE_TEMPLATE
	 * @see ConfigConstants 
	 */
	private String deriveNameNtoM(String tabname, String fieldname) {
		String result = templateNtoM.replace("{{class}}", tabname).replace("{{property}}",fieldname);
		return result;
	}
	
	/**
	 * Derive the name of a related table capable of representing 
	 * a property in the model with a 1-to-many relation
	 * @param tabname	name of the table representing the class that the property belongs to
	 * @param fieldname	name of the field representing a single value of the property 
	 * @return	name of the related table in the database
	 * @see ConfigConstants#RULE_TGT_LDP_PROP_MV_AS_1TON
	 * @see ConfigConstants#PARAM_1TON_TABLE_TEMPLATE 
	 */
	private String deriveName1toN(String tabname, String fieldname) {
		String result = template1toN.replace("{{class}}", tabname).replace("{{property}}",fieldname);
		return result;
	}
	
	/**
	 * Different approaches are possible how to represent properties of supertypes. Two options
	 * are currently supported. The default is that all properties are "copied down" to the
	 * non-abstract feature types. The second approach is to have a separate table per feature
	 * type, including abstract supertypes (see {@link ConfigConstants#RULE_TGT_LDP_CLS_TABLE_PER_FT}). 
	 * @param ci	the non-abstract feature type 
	 * @param superci	the supertype of {@link ci} that will be processed
	 * @param featuretype	the JSON object in the mappings section for {@link ci}
	 * @param basepath	the mapping for each property is described by a path in the database; 
	 * the path starts at the table of {@link ci} and mainly includes joins; this parameter
	 * includes the path to the current position and will be extended with additional path
	 * elements for the properties 
	 */
	private void processSupertypeProperties(ClassInfo ci, ClassInfo superci, JSONObject featuretype, String basepath) {
		if (superci==null)
			return;

		String tabname;
		String path;
		if (ci.matches(ConfigConstants.RULE_TGT_LDP_CLS_TABLE_PER_FT)) {
			// The supertype has its own table
			tabname = deriveName(superci);
			path = basepath + "/[" + primaryKeyField + "=" + primaryKeyField + "]" + tabname;
		} else {
			// The supertype properties are part of the table of the non-abstract feature type
			tabname = deriveName(ci);
			path = basepath;
		}

		
		// Add mappings for all properties
		if (!superci.properties().isEmpty()) {
			for (PropertyInfo pi : superci.properties().values()) {
				processProperty(pi, featuretype, path, null, tabname);
			}
		}

		// Recursively add mappings for all supertype properties
		if (superci.matches(ConfigConstants.RULE_TGT_LDP_CLS_TABLE_PER_FT) &&
			!superci.supertypes().isEmpty()) {
			for (String sid : superci.supertypes()) {
				processSupertypeProperties(ci, model.classById(sid), featuretype, basepath);
			}
		}		
	} 
	
	/**
	 * Each table in the database must have a primary key that is used in
	 * joins. This operation creates this field, which is in addition to the
	 * fields derived from the application schemas.
	 * @param featuretype	the JSON object in the mappings section for current feature type	
	 * @param basepath	the mapping for each property is described by a path in the database; 
	 * the path starts at the table of current feature type and mainly includes joins; this 
	 * parameter includes the path to the current position and will be extended with additional 
	 * path elements for the primary key field
	 * @see ConfigConstants#PARAM_PRIMARY_KEY_FIELD
	 */
	@SuppressWarnings("unchecked")
	private void createIdProperty(JSONObject featuretype, String basepath) {

		String fieldname = primaryKeyField;
		
		JSONObject path = new JSONObject();
		String proppath = basepath+"/"+fieldname;
		featuretype.put(proppath, path);
		
		JSONObject general = new JSONObject();
		path.put("general", general);
		general.put("mappingType", "GENERIC_PROPERTY");
		general.put("name", fieldname);
		general.put("enabled", true);
		general.put("filterable", false);
		general.put("type", "ID");
		
		// Add default schema.org mapping in HTML
		JSONObject html = new JSONObject();
		path.put("text/html", html);
		html.put("mappingType", "MICRODATA_PROPERTY");
		html.put("name", "Id");
		html.put("type", "ID");
		html.put("showInCollection", true);
		
		// Add default GeoJSON mapping for the id
		JSONObject json = new JSONObject();
		path.put("application/geo+json", json);
		json.put("mappingType", "GEO_JSON_PROPERTY");
		json.put("type", "ID");
	}
	
	/**
	 * 
	 * @param pi	the property to process
	 * @param featuretype	the JSON object in the mappings section for current feature type
	 * @param basepath	the mapping for each property is described by a path in the database; 
	 * the path starts at the table of current feature type and mainly includes joins; this 
	 * parameter includes the path to the current position and will be extended with additional 
	 * path elements for the property
	 * @param basename	when complex data structures (data types, nested objects) or properties
	 * with a maximum multiplicity greater than one are included in the application schema, we
	 * need to deal with additional structures and these properties are in a JSON encoding either
	 * flattened or represented as objects and/or arrays; the name of a property, therefore,
	 * can be a JSON path expression; this parameter includes the JSON path to the current
	 * position and will be extended to with additional path elements for the property   
	 * @param tabname	name of the current table; if the property is a direct property of the
	 * feature type, this is the table of the feature type; otherwise it is a table along the
	 * joins in the basepath
	 */
	@SuppressWarnings("unchecked")
	private void processProperty(PropertyInfo pi, JSONObject featuretype, String basepath, String basename, String tabname) {

		// The default name of a field for the property is the property name in 
		// lower case characters.
		String field = deriveName(pi);

		// The default label for the class
		String alias = pi.aliasName();
		String label = (alias!=null && alias.length()>0) ? alias : pi.name();
		
		// NOTE: Currently no other rules exist to deviate from the defaults
		
		// Determine the type information
		String category = "VALUE";
		String htmltype = "STRING";
		String htmlformat = null;
		String htmlgeometrytype = null;
		String jsontype = "STRING";
		String jsongeometrytype = null;
		String targettabname = null;
		String pattern = null;
		Type ti = pi.typeInfo();
		if (ti != null) {
			ProcessMapEntry me = options.targetMapEntry(ti.name, pi.encodingRule("ldp"));
			if (me!=null) {
				// First handle well-known ones
				if (me.hasTargetType()) {
					htmltype = me.getTargetType();
					jsontype = (htmltype.equalsIgnoreCase("DATE") ? "STRING" : htmltype); 
					if (me.hasParam() && me.getParam().length()>0) {
						Map<String,String> params = new HashMap<String,String>();
						String[] paramarr = me.getParam().split(";");
						for (String param : paramarr) {
							String[] entry = param.split(":",2);
							if (entry.length==2) {
								params.put(entry[0].trim(), entry[1].trim());
							}
						}
						if (params.containsKey("category"))
							category = params.get("category");
						if (params.containsKey("htmlformat"))
							htmlformat = params.get("htmlformat");
						if (params.containsKey("htmlgeometry"))
							htmlgeometrytype = params.get("htmlgeometry");
						if (params.containsKey("jsongeometry"))
							jsongeometrytype = params.get("jsongeometry");
					}
				}
			} else {
				// Default mapping. The type of the property should be a code list value,
				// a data type or a feature type.
				ClassInfo cix = model.classById(ti.id);
				if (cix!=null) {
					if (cix.category()==Options.DATATYPE || cix.category()==Options.OBJECT) {
						if (pi.matches(ConfigConstants.RULE_TGT_LDP_PROP_DT_AS_NTOM)) {
							String dttabname = deriveName(cix);
							String dtbasepath = basepath + "/["+primaryKeyField+"="+ tabname + foreignKeySuffix + "]" + deriveNameNtoM(tabname, field) + "/[" + field + foreignKeySuffix + "=" + primaryKeyField + "]" + dttabname;
							// Add mappings for all properties
							if (!cix.properties().isEmpty()) {
								for (PropertyInfo pix : cix.properties().values()) {
									processProperty(pix, featuretype, dtbasepath, (basename!=null ? basename+"." : "") + field+"["+field+"]", dttabname);
								}
							}
						} else {
							MessageContext m = result.addError(this, 2);
							m.addDetail(this, 99, pi.name(), pi.inClass().name(), cix.name());
						}
						return;
					} else if (cix.category()==Options.FEATURE) {
						if (pi.matches(ConfigConstants.RULE_TGT_LDP_PROP_FT_AS_NTOM)) {
							category = "REFERENCE";
							if (cix.matches(ConfigConstants.RULE_TGT_LDP_CLS_TABLE_PER_FT)) {
								// TODO Should we limit this to the non-abstract link targets?
								basepath = basepath + "/[" + primaryKeyField + "="+ tabname + foreignKeySuffix + "]" + deriveNameNtoM(tabname, field) + "/[" + field + foreignKeySuffix + "=" + primaryKeyField + "]" + rootFeatureTable;
							} else if (!cix.isAbstract()) {
								targettabname = deriveName(cix);
								// TODO This could also stop at the intermediate table as we have the id in that table already
								basepath = basepath + "/[" + primaryKeyField + "="+ tabname + foreignKeySuffix + "]" + deriveNameNtoM(tabname, field) + "/" + field + foreignKeySuffix + "=" + primaryKeyField + "]" + targettabname;
							} else {
								MessageContext m = result.addError(this, 4);
								m.addDetail(this, 99, pi.name(), pi.inClass().name(), cix.name());
								return;
							}
						} else {
							MessageContext m = result.addError(this, 3);
							m.addDetail(this, 99, pi.name(), pi.inClass().name(), cix.name());
							return;
						}
					} else if ((cix.category()==Options.CODELIST || cix.category()==Options.ENUMERATION) && pi.matches(ConfigConstants.RULE_TGT_LDP_PROP_CL_AS_STRING)) {
						// Nothing to do, the default works.
					} else if (cix.category()==Options.UNION) {
						MessageContext m = result.addError(this, 5);
						m.addDetail(this, 99, pi.name(), pi.inClass().name(), cix.name());
						return;
					} else {
						// Nothing to do, we assume the property is a string field. Generate a warning.
						MessageContext m = result.addWarning(this, 102, cix.name());
						m.addDetail(this, 99, pi.name(), pi.inClass().name(), cix.name());						
					}
				}
			}
		}
		
		JSONObject path = new JSONObject();
		String proppath = basepath+"/"+field;
		String fieldname = (basename!=null ? basename+"."+field : field);

		// Special cases
		if (category.equalsIgnoreCase("REFERENCE")) {
			if (targettabname==null) {
				fieldname = fieldname + "[" + rootFeatureTable + "]"; 
				proppath = basepath + "/" + primaryKeyField + ":" + rootCollectionField;
				pattern = "{{serviceUrl}}/collections/{{" + rootCollectionField + "}}/items/{{" + primaryKeyField + "}}";
			} else {
				fieldname = fieldname + "[" + targettabname + "]"; 
				proppath = basepath + "/" + primaryKeyField;
				pattern = "{{serviceUrl}}/collections/" + targettabname + "/items/{{" + primaryKeyField + "}}";
			}
		} else if (category.equalsIgnoreCase("SPATIAL")) {
			// For geometries, multiple values are not supported
			if (pi.cardinality().maxOccurs > 1) {
				MessageContext m = result.addError(this, 1);
				m.addDetail(this, 99, pi.name(), pi.inClass().name(), pi.typeInfo().name);
			}
			if (pi.matches(ConfigConstants.RULE_TGT_LDP_PROP_GEOMETRY_TABLE)) {
				proppath = basepath + "/[" + geometryField + "=" + primaryKeyField + "]" + geometryTable + "/" + geometryField;
			}
		} else if (pi.cardinality().maxOccurs > 1) {			
			// If we are here, the value type is simple and we need to address multiplicity.
			// Currently this is oNeo specific, if necessary add a rule to support other mechanisms.
			proppath = basepath + "/[" + primaryKeyField + "="+ tabname + foreignKeySuffix + "]" + deriveName1toN(tabname, field) + "/" + field;
			fieldname = fieldname + "[" + deriveName1toN(tabname, field) + "]"; 
		}

		featuretype.put(proppath, path);
			
		JSONObject general = new JSONObject();
		path.put("general", general);
		general.put("mappingType", "GENERIC_PROPERTY");
		general.put("name", fieldname);
		general.put("enabled", true);
		boolean filterable = false;
		if (category.equalsIgnoreCase("SPATIAL"))
			filterable = true;
		else if (filterableFields.contains(tabname+"."+field))
			filterable = true;
		general.put("filterable", filterable);
		general.put("type", category);
		if (pattern!=null)
			general.put("pattern", pattern);
				
		// Add default schema.org mapping in HTML
		JSONObject html = new JSONObject();
		path.put("text/html", html);
		html.put("mappingType", "MICRODATA_PROPERTY");
		html.put("name", label);
		html.put("type", htmltype);
		if (htmlformat!=null)
			html.put("format", htmlformat);
		if (htmlgeometrytype!=null)
			html.put("geometryType", htmlgeometrytype);
		html.put("showInCollection", (htmltype.equalsIgnoreCase("GEOMETRY") ? false : true));
		
		// Add default GeoJSON mapping for the id
		JSONObject json = new JSONObject();
		path.put("application/geo+json", json);
		json.put("mappingType", "GEO_JSON_PROPERTY");
		json.put("type", jsontype);
		if (jsongeometrytype!=null)
			json.put("geometryType", jsongeometrytype);		
	}	
		
	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "A geometry property has a maximum multiplicity greater than '1'. This is not supported by this target. A maxmimum multiplicity of '1' is used.";
		
		case 2:
			return "No rule is specified how to handle properties with a value that is a data type. The property is ignored.";
			
		case 3:
			return "No rule is specified how to handle properties with a value that is a feature type. The property is ignored.";
			
		case 4:
			return "No rule is specified how to handle feature associations that involve non-abstract features, i.e. the link targets are spread across multiple tables. The property is ignored.";
			
		case 5:
			return "No rule is specified how to handle properties with a value that is a union data type. The property is ignored.";

		case 6:
			return "Directory named '$1$' does not exist or is not accessible.";

		case 99:
			return "Context: class InfoImpl (subtype: PropertyInfo). Name: '$1$'. In class: '$2$'. Value type: '$3$'.";

		case 101:
			return "Association classes are currently not supported. Association characteristics of '$1$' are not considered.";

		case 102:
			return "No mapping has been specified for the value type '$1$' of a property. We assume the type is implemented as a string field in the database.";
			
		default:
			return "(Config.java) Unknown message with number: " + mnr;
		}

	}
}
