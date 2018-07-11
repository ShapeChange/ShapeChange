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
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 *
 */
public class Config implements SingleTarget, MessageSource {

	private static boolean initialised = false;

	private static String outputDirectory = null;
	private static String outputFilename = null;
	private static String srvid = null;
	private static String srvlabel = null;
	private static String srvdesc = null;
	private static String primaryKeyField = null;
	private static String foreignKeySuffix = null;
	private static Integer maxLength = null;
	private static String geometryTable = null;
	private static String geometryField = null;
	private static String templateNtoM = null;
	private static String template1toN = null;
	private static String rootFeatureTable = null;
	private static String rootCollectionField = null;
	private static Set<String> filterableFields = null;
	private static JSONObject cfgobj = null;
	private static JSONObject collections = null;
	private static JSONObject mappings = null;
	private static FileWriter writer = null;

	private static Model model = null;

	private Options options = null;
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
					result.addFatalError(this, 12, outputDirectory);
					throw new ShapeChangeAbortException();
				}

				writer = new FileWriter(outputDirectory + "/" + jsonName);

				PackageInfo first = (model.allPackagesFromSelectedSchemas().isEmpty() ? null : model.allPackagesFromSelectedSchemas().first());
				
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
					processSupertypeProperties(ci, model.classById(sid), featuretype, null, basepath);
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
		// nothing to do here, since this is a SingleTarget
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

			writer.write(cfgobj.toJSONString());
			writer.flush();
			writer.close();

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);

		} finally {

			// close writer
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

			// release model - do NOT close it here
			model = null;
		}
	}

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
	
	private String deriveNameNtoM(String tabname, String fieldname) {
		String result = templateNtoM.replace("{{class}}", tabname).replace("{{property}}",fieldname);
		return result;
	}
	
	private String deriveName1toN(String tabname, String fieldname) {
		String result = template1toN.replace("{{class}}", tabname).replace("{{property}}",fieldname);
		return result;
	}
	
	private void processSupertypeProperties(ClassInfo ci, ClassInfo superci, JSONObject featuretype, String basename, String basepath) {
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
				processProperty(pi, featuretype, path, basename, tabname);
			}
		}

		// Recursively add mappings for all supertype properties
		if (superci.matches(ConfigConstants.RULE_TGT_LDP_CLS_TABLE_PER_FT) &&
			!superci.supertypes().isEmpty()) {
			for (String sid : superci.supertypes()) {
				processSupertypeProperties(ci, model.classById(sid), featuretype, basename, basepath);
			}
		}		
	} 
	
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
					} else if (cix.category()==Options.CODELIST && pi.matches(ConfigConstants.RULE_TGT_LDP_PROP_CL_AS_STRING)) {
						// Nothing to do, the default works.
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
			html.put("geometryType", jsongeometrytype);		
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
