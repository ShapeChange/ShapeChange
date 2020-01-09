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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.JSON;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

public class JsonSchema implements Target, MessageSource {

	// TODO convert to more fine-grained info.matches() logic
	
	public static final String PARAM_SKIP_NOT_IMPLEMENTED_CHECK = "skipNotImplementedCheck";
	private static final String JSON_SCHEMA_URI_DRAFT_2019_09 = "https://json-schema.org/draft/2019-09/schema";

	private class Context {
		protected ArrayList<Object> links = new ArrayList<>();
		protected TreeMap<String,Object> object = new TreeMap<>();
		protected File file = null;
		protected boolean addDefsLink = false;
	}
	
	// geometry type per feature type
	private HashMap<String,String> contexts = new HashMap<String,String>(); 
	
	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private String outputDirectory = null;
	private File outputDirectoryFile = null;
	private File subDirectoryFile = null;
	private String subdir = null;
	private String baseURI = null;
	private String schemaURI = null;
	private boolean diagnosticsOnly;
	private boolean includeDocumentation = true;
	private boolean skipNotImplementedCheck = false;
	private String documentationTemplate = null;
	private String documentationNoValue = null;
	private Gson gson = null;

	/**
	 * <p>Initialize target generation for the JSON Schema output.</p> 
	 * @param p UML Package represented by PackageInfo interface
	 * @param m Model represented by Model interface
	 * @param r Result class for diagnostics output
	 * @param diagOnly Flag requesting to suppress any output
	 */
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly) throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;
		
		if (!isEncoded(pi)) {
			
			result.addInfo(this, 7, pi.name());
			return;
		}
		
		outputDirectory = options.parameter(this.getClass().getName(),"outputDirectory");
		if (outputDirectory==null)
			outputDirectory = options.parameter(".");

		String s = options.parameter(this.getClass().getName(),"includeDocumentation");
		if (s!=null && s.equalsIgnoreCase("false"))
			includeDocumentation = false;
		
		s = options.parameter(this.getClass().getName(),"prettyPrint");
		if (s!=null && s.equalsIgnoreCase("false"))
			gson = new Gson();
		else
			gson = new GsonBuilder().setPrettyPrinting().create();

		schemaURI = options.parameter(this.getClass().getName(),"jsonSchemaURI");
		if (schemaURI==null)
			schemaURI = JSON_SCHEMA_URI_DRAFT_2019_09;

		baseURI = pi.taggedValue("jsonBaseURI");
		if (baseURI==null)
			baseURI = options.parameter(this.getClass().getName(),"jsonBaseURI");
		if (baseURI==null)
			baseURI = "http://example.com/FIXME";
		
		String skipNotImplemented_tmp = options.parameter(this.getClass().getName(),PARAM_SKIP_NOT_IMPLEMENTED_CHECK);
		if(skipNotImplemented_tmp != null && skipNotImplemented_tmp.trim().equalsIgnoreCase("true")) {
			skipNotImplementedCheck = true;
		}
		
		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");

		result.addDebug(this, 10001, pi.name());

		if(!this.diagnosticsOnly){

			// Check whether we can use the given output directory
			outputDirectoryFile = new File( outputDirectory );
			boolean exi = outputDirectoryFile.exists();
			if( !exi ) {
				outputDirectoryFile.mkdirs();
				exi = outputDirectoryFile.exists();
			}
			boolean dir = outputDirectoryFile.isDirectory();
			boolean wrt = outputDirectoryFile.canWrite();
			boolean rea = outputDirectoryFile.canRead();
			if( !exi || !dir || !wrt || !rea ) {
				result.addFatalError(this, 12, outputDirectory);		
				throw new ShapeChangeAbortException();
			}
			
			subdir = pi.taggedValue("jsonDirectory");
			if (subdir == null)
				subdir = pi.xmlns();
			if (subdir == null)
				subdir = "default";
			
			// Construct a File for the application schema 
			// being a subdirectory under the main output
			subDirectoryFile = new File( outputDirectoryFile, subdir );
			try {
				// Make sure it is a directory
				subDirectoryFile.mkdirs();
				// Check if we have the necessary access
				dir = subDirectoryFile.isDirectory();
				wrt = subDirectoryFile.canWrite();
				rea = subDirectoryFile.canRead();
				if( !dir || !wrt || !rea ) {
					result.addFatalError(this, 12, subDirectoryFile.getName());		
					throw new ShapeChangeAbortException();
				}
			} catch ( Exception e ) {
				// Something went wrong with the io concerning the directory
				result.addFatalError(this, 12, subDirectoryFile.getName());
				result.addFatalError(this, 10, e.getMessage());					
				throw new ShapeChangeAbortException();
			}
		} else {
			result.addInfo(this, 10002);			
		}
	}
	
	private boolean notImplemented(String cname) {
		if (cname.startsWith("TP_") ||
				cname.startsWith("TM_") ||
				cname.startsWith("MD_") ||
				cname.startsWith("CI_") ||
				cname.startsWith("DQ_") ||
				cname.startsWith("CV_") ||
				cname.startsWith("OM_") ||
				cname.startsWith("SF_") ||
				cname.startsWith("SC_") ||
				cname.startsWith("SV_"))
			return true;
			
		return false;
	}
	
	public static boolean isEncoded(Info i) {

		if (i.matches("rule-json-all-notEncoded")
				&& i.encodingRule("json").equalsIgnoreCase("notencoded")) {
			
			return false;
			
		} else {
			
			return true;
		}
	}

	public void process(ClassInfo ci) {
		
		int cat = ci.category();
				
		if (!isEncoded(ci)) {
			result.addInfo(this,8,ci.name());
			return;
		}
		
		if (encRuleIsGeoservices(ci)) {
			if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN) {
				return;
			}
		} else if (encRuleIsGeoservicesExtended(ci)) {
			if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN && 
				cat != Options.DATATYPE && cat != Options.UNION) {
				return;
			}
		} else if (encRuleIsGeoJson(ci)) {
			if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN && 
				cat != Options.DATATYPE && cat != Options.UNION) {
				return;
			}
		}
		
		Context ctx = new Context();
		try {
			
			if (!diagnosticsOnly) {
				ctx.file = new File(subDirectoryFile, ci.name()+".json");
			}
			
			ctx.object.put("$schema", schemaURI);
			ctx.object.put("$id", baseURI+"/"+subdir+"/"+ci.name()+".json");
			ctx.object.put("type", "object");
			String s = ci.aliasName();
			ctx.object.put("title", (s==null||s.isEmpty()?ci.name():s));
			String s2 = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
			if (includeDocumentation && !s2.isEmpty()) {
				ctx.object.put("description", s2.trim());
			}

			TreeMap<String,Object> properties = new TreeMap<>();
			ctx.object.put("properties", properties);
			
			ArrayList<String> required = new ArrayList<>();
			ctx.object.put("required", required);
			
			// add general properties for features and objects
			if (cat==Options.FEATURE || cat==Options.OBJECT) {
				if (encRuleIsGeoJson(ci)) {
					TreeMap<String,Object> type = new TreeMap<>();
					type.put("type", "string");
					ArrayList<String> enumArray = new ArrayList<>();
					enumArray.add("Feature");
					type.put("enum", enumArray);
					properties.put("type", type);
					required.add("type");
					ArrayList<String> stringOrNumberArray = new ArrayList<>();
					stringOrNumberArray.add("string");
					stringOrNumberArray.add("number");
					TreeMap<String,Object> stringOrNumber = new TreeMap<>();
					stringOrNumber.put("type", stringOrNumberArray);
					properties.put("id", stringOrNumber);
				} else if (encRuleIsGeoservices(ci) || encRuleIsGeoservicesExtended(ci)) {
					TreeMap<String,Object> entityType = new TreeMap<>();
					entityType.put("type", "string");
					entityType.put("default", (s==null||s.isEmpty()?ci.name():s));
					properties.put("entityType", entityType);
				}
			}

			// add geometry for features and objects
			if (cat==Options.FEATURE || cat==Options.OBJECT) {
				TreeMap<String,Object> geometry = new TreeMap<>();
				String geomType = determineGeometryType(ci);
				if (geomType!=null) {
					ArrayList<Object> oneOf = new ArrayList<>(); 
					TreeMap<String,Object> typeNull = new TreeMap<>();
					typeNull.put("type", "null");
					oneOf.add(typeNull);
					TreeMap<String,Object> ref = new TreeMap<>();
					ref.put("$ref", geomType);
					oneOf.add(ref);
					geometry.put("oneOf", oneOf);
				} else {
					geometry.put("type", "null");					
				}
				properties.put("geometry", geometry);
				required.add("geometry");
			} else if (cat==Options.DATATYPE || cat==Options.UNION) {
				verifyNoGeometry(ci);
			}
			
			TreeMap<String,Object> featureProperties = new TreeMap<>();
			ArrayList<String> featurePropertiesRequired = new ArrayList<>();
			TreeMap<String,Object> featurePropertiesMap = new TreeMap<>();
			if (encRuleIsGeoJson(ci)) {
				if (cat==Options.FEATURE || cat==Options.OBJECT) {
					TreeMap<String,Object> oneOf = new TreeMap<>();
					properties.put("properties", oneOf);
					ArrayList<Object> typeArray = new ArrayList<>(); 
					oneOf.put("oneOf", typeArray);
					TreeMap<String,Object> typeNull = new TreeMap<>();
					typeNull.put("type", "null");
					typeArray.add(typeNull);
					typeArray.add(featureProperties);
					required.add("properties");
					featureProperties.put("title", "feature properties");
					featureProperties.put("type", "object");
					featureProperties.put("required", featurePropertiesRequired);
					featureProperties.put("properties", featurePropertiesMap);
				} else if (cat==Options.DATATYPE || cat==Options.UNION) {
					featurePropertiesRequired = required;
					featurePropertiesMap = properties;
				}
			} else if (encRuleIsGeoservices(ci) || encRuleIsGeoservicesExtended(ci)) {
				if (cat==Options.FEATURE || cat==Options.OBJECT) {
					featureProperties.put("title", "feature attributes");
					featureProperties.put("type", "object");
					featureProperties.put("properties", featureProperties);
					properties.put("attributes", featureProperties);
					featureProperties.put("required", featurePropertiesRequired);
					featureProperties.put("properties", featurePropertiesMap);
				} else if ((cat==Options.DATATYPE || cat==Options.UNION) && encRuleIsGeoservicesExtended(ci)) {
					featurePropertiesRequired = required;
					featurePropertiesMap = properties;
				}
			}
			
			SortedSet<String> st = ci.supertypes();
			if (st != null) {
				for (Iterator<String> i = st.iterator(); i.hasNext();) {
					String sid = i.next();
					ClassInfo cix = model.classById(sid);
					if (cix != null) {
						String cn = cix.name();
						if (!skipNotImplementedCheck && notImplemented(cn))
							result.addWarning(this, 104, ci.name(), cn);
						else
							ctx = ProcessProperties(ctx, cix, featurePropertiesMap, featurePropertiesRequired);
					}
				}
			}
			
			ctx = ProcessProperties(ctx, ci, featurePropertiesMap, featurePropertiesRequired);
			
			if (!ctx.links.isEmpty()) {
				ctx.object.put("links", ctx.links);
			}
			
			if (ctx.addDefsLink) {
				TreeMap<String,Object> defs = new TreeMap<>();
				ctx.object.put("$defs", defs);

				TreeMap<String,Object> link = new TreeMap<>();
				link.put("type", "object");
				ArrayList<String> linkRequired = new ArrayList<>();
				linkRequired.add("href");
				linkRequired.add("title");
				link.put("required", linkRequired);
				TreeMap<String,Object> linkProperties = new TreeMap<>();
				link.put("properties", linkProperties);
				TreeMap<String,Object> typeString = new TreeMap<>();
				typeString.put("type", "string");
				linkProperties.put("href", typeString);
				linkProperties.put("title", typeString);
				linkProperties.put("rel", typeString);
				linkProperties.put("hreflang", typeString);
				linkProperties.put("type", typeString);
				defs.put("link", link);
			}
			
			if (ctx.file!=null) {

				OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(ctx.file)), StandardCharsets.UTF_8);
				writer.write(gson.toJson(ctx.object));
				writer.flush();
				writer.close();
				result.addResult(getTargetName(), subDirectoryFile.getPath(), ci.name()+".json", ci.qname());
			}
			
		} catch( IOException e ) {
			// Opening the file went wrong, skip class
			result.addError( this, 11, ci.name()+".json" );
			return;
		}
	}
	
	private boolean encRuleIsGeoservices(Info i) {
		return options.getRuleRegistry().matchesEncRule(i.encodingRule("json"),"geoservices");
	}

	private boolean encRuleIsGeoservicesExtended(Info i) {
		return options.getRuleRegistry().matchesEncRule(i.encodingRule("json"),"geoservices_extended");
	}

	private boolean encRuleIsGeoJson(Info i) {
		return options.getRuleRegistry().matchesEncRule(i.encodingRule("json"),"geojson");
	}

	private Context ProcessProperties(Context ctx, ClassInfo ci, TreeMap<String,Object> propertiesMap, ArrayList<String> propertiesRequired) throws IOException {
		return ProcessProperties(ctx, ci, null, ci.category()!=Options.UNION, propertiesMap, propertiesRequired);
	}
	
	private Context ProcessProperties(Context ctx, ClassInfo ci, String propertyPrefix, boolean required, TreeMap<String,Object> propertiesMap, ArrayList<String> propertiesRequired) throws IOException {
		
		for (PropertyInfo propi : ci.properties().values()) {
			
			if(!isEncoded(propi)) {
				MessageContext mc = result.addInfo(this,9,propi.name());
				if(mc != null) {
					mc.addDetail(this, 20000, propi.fullNameInSchema());
				}
				continue;
			}
			
			Type ti = propi.typeInfo();
			Multiplicity m = propi.cardinality();

			String nam = null;
			if (propertyPrefix!=null)
				nam = propertyPrefix+"."+propi.name();
			else
				nam = propi.name();

			String type = null;
			String format = null;
			String ref = null;
			List<String> enums = new ArrayList<>();
			boolean nillable = false;
			boolean flatten = false;
			// First handle well-known ones
			ProcessMapEntry me = options.targetMapEntry(ti.name, propi.encodingRule("json"));
			if (me!=null) {
				if ("geometry".equalsIgnoreCase(me.getParam())) {
					result.addDebug(this, 10003, propi.inClass().name(), propi.name());
					continue;
				} else if (me.getTargetType().startsWith("ref:")) {
					ref = me.getTargetType().substring(4);
				} else {
					type = me.getTargetType();
					if (me.hasParam() && me.getParam().startsWith("format:"))
						format = me.getParam().substring(7);
				}
			}
			
			ClassInfo cix = null;
			if (type==null && ref==null) {
				// Handle well-known type prefixes from base models that we know are not supported by JSON 
				// or a well-known JSON schema
				cix = model.classById(ti.id);
				if (cix==null) {
					if (encRuleIsGeoservices(propi)) {
						result.addWarning(this, 103, propi.inClass().name(), propi.name(), ti.name);
						type = "string";
					} else if (encRuleIsGeoservicesExtended(propi) || encRuleIsGeoJson(propi)) {
						result.addWarning(this, 105, propi.inClass().name(), propi.name(), ti.name);
						type = "any";
					}
				} else {
					int cat = cix.category();
					if (cat==Options.CODELIST) {
						if (encRuleIsGeoservices(propi)) {
							type = "string";
						} else if (encRuleIsGeoservicesExtended(propi)) {
							type = "string";
							format = "uri";
						} else if (encRuleIsGeoJson(propi)) {
							ctx.addDefsLink = true;
							ref = "#/$defs/link";
						}					
					} else if (cat==Options.ENUMERATION) {
						type = "string";
						if (cix.properties().isEmpty()) {
							result.addWarning(this, 107, cix.name());
						} else {
							for (PropertyInfo propix : cix.properties().values()) {
								if(isEncoded(propix)) {
									enums.add(propix.name());
								}
							}
						}
					} else if (cat==Options.FEATURE || cat==Options.OBJECT || cat==Options.MIXIN) {
						if (encRuleIsGeoservices(propi)) {
							type = "integer";
							String lyrURI = cix.taggedValue("jsonLayerTableURI");
							if (lyrURI!=null) {
								TreeMap<String,Object> link = new TreeMap<>();
								link.put("href", lyrURI+"/{#/attributes/"+propi.name()+"}?f=json");
								link.put("rel", "related");
								ctx.links.add(link);
							}
						} else if (encRuleIsGeoservicesExtended(propi)) {
							type = "string";
							format = "uri";
						} else if (encRuleIsGeoJson(propi)) {
							ctx.addDefsLink = true;
							ref = "#/$defs/link";
						}					
					} else if (cat==Options.DATATYPE || cat==Options.UNION) {						
						if (encRuleIsGeoservices(propi)) {
							flatten = true;
							verifyNoGeometry(cix);							
						} else if (encRuleIsGeoservicesExtended(propi) || encRuleIsGeoJson(propi)) {
							PackageInfo rootPackage = cix.pkg().rootPackage();
							
							String refBaseURI = null;
							if(rootPackage != null) 
								refBaseURI = rootPackage.taggedValue("jsonBaseURI");
							if (refBaseURI == null)
								refBaseURI = baseURI;
							
							String refDir = null;
							if(rootPackage != null)
								refDir = rootPackage.taggedValue("jsonDirectory");
							if (refDir==null && rootPackage != null)
								refDir = rootPackage.xmlns();
							if (refDir==null)
								refDir = "default";
							
							ref = refBaseURI+"/"+refDir+"/"+cix.name()+".json";
						}					
					}
				}
			}
			
			if ((encRuleIsGeoservicesExtended(propi) || encRuleIsGeoJson(propi)) && propi.voidable())
				nillable = true; 
			
			int repeat = 1;
			boolean array = false;
			if (m.maxOccurs>1) {
				if (encRuleIsGeoservices(propi)) {
					repeat = 3;
				} else if (encRuleIsGeoservicesExtended(propi) || encRuleIsGeoJson(propi)) {
					array = true;
				}
			}
			
			// TODO support for pattern
			
			if (flatten && cix!=null) {
				for (int i = 1; i <= repeat; i++) {
					String n = nam;
					if (repeat>1)
						n = n+"_"+i;
					ProcessProperties(ctx, cix, n, required && m.minOccurs>0 && cix.category()!=Options.UNION, propertiesMap, propertiesRequired);
				}
			} else {
				for (int i = 1; i <= repeat; i++) {
					String n = nam;
					if (repeat>1)
						n = n+"-"+i;
					TreeMap<String,Object> property = new TreeMap<>();
					propertiesMap.put(n, property);
					String s = propi.aliasName();
					property.put("title", (s==null||s.isEmpty()?propi.name():s));
					String s2 = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
					if (includeDocumentation && !s2.isEmpty()) {
						property.put("description", s2.trim());
					}
					if (array) {
						if (nillable && i==1) {
							ArrayList<String> types = new ArrayList<>();
							types.add("null");
							types.add("array");
							property.put("type", types);
						} else
							property.put("type", "array");
						TreeMap<String,Object> items = new TreeMap<>();
						property.put("items", items);
						if (ref!=null) {
							items.put("$ref", ref);
						} else {
							items.put("type", type);
							if (format!=null) {
								items.put("format", format);
							}
							if (!enums.isEmpty()) {
								ArrayList<String> enumArray = new ArrayList<>();
								enumArray.addAll(enums);
								items.put("enum", enumArray);
							}
						}
						if (m.minOccurs>0 && required) {
							items.put("minItems", m.minOccurs);
							propertiesRequired.add(n);
						}
						if (m.maxOccurs<Integer.MAX_VALUE) {
							items.put("maxItems", m.maxOccurs);
						}							
					} else {
						if (ref!=null) {
							property.put("$ref", ref);
							if (m.minOccurs>0 && i==1 && required) {
								propertiesRequired.add(n);
							}
						} else {
							if (nillable && i==1) {
								ArrayList<String> types = new ArrayList<>();
								types.add("null");
								types.add(type);
								property.put("type", types);
							} else {
								if (type==null) {
									type = "string";
									result.addWarning(this, 103, propi.inClass().name(), propi.name(), ti.name);
								}
								property.put("type", type);
							}
							if (format!=null) {
								property.put("format", format);
							}
							if (!enums.isEmpty()) {
								ArrayList<String> enumArray = new ArrayList<>();
								enumArray.addAll(enums);
								property.put("enum", enumArray);
							}
							if (m.minOccurs>0 && i==1 && required) {
								propertiesRequired.add(n);
							}
						}						
					}
					
					if (nillable && i==1 && encRuleIsGeoservicesExtended(propi)) {
						TreeMap<String,Object> nullReason = new TreeMap<>();
						propertiesMap.put(n+"_nullReason", nullReason);
						nullReason.put("title", "Reason for null value in property "+(s==null||s.isEmpty()?propi.name():s));
						nullReason.put("type", "string");
					}
				}				
			}
		}
		return ctx;
	}
	
	private String determineGeometryType(ClassInfo ci) {
		if (ci==null)
			return null;
		if (ci.pkg()==null) {
			return null;
		}
		if (contexts.containsKey(ci.qname())) {
			// already processed
			return contexts.get(ci.qname()); 
		}

		String geomType = null;
		ClassInfo cibase = ci.baseClass();
		if (cibase!=null) {
			geomType = contexts.get(cibase.qname());
			contexts.put(ci.qname(), geomType);
		}

		for (PropertyInfo propi : ci.properties().values()) {

			if(!isEncoded(propi)) {
				continue;
			}
			geomType = determineGeometryType(ci, propi);
		}
		return geomType;
	}		

	private String determineGeometryType(ClassInfo ci, PropertyInfo propi) {
		String geomType = contexts.get(ci.qname());

		if (!propi.isNavigable())
			return geomType;
		if (propi.isRestriction())
			return geomType;
		
		Multiplicity m = propi.cardinality();
		if (m.maxOccurs<1)
			return geomType;

		String type = propi.typeInfo().name;
		ProcessMapEntry me = options.targetMapEntry(type, propi.encodingRule("json"));
		if (me!=null && "geometry".equalsIgnoreCase(me.getParam())) {
			if (m.maxOccurs>1)
				result.addWarning(this,102,propi.name(),propi.inClass().name());
			if (geomType==null) {
				// remove "ref:" prefix
				geomType = me.getTargetType().substring(4);
				contexts.put(ci.qname(), geomType);
			} else {
				result.addWarning(this,101,propi.name(),propi.inClass().name());
			}
		}
		return geomType;
	}	
	
	private void verifyNoGeometry(ClassInfo ci) {
		if (ci==null)
			return;
		if (ci.pkg()==null) {
			return;
		}

		for (PropertyInfo propi : ci.properties().values()) {
			if(!isEncoded(propi)) {
				continue;
			}
			verifyNoGeometry(ci, propi);
		}
	}		

	private void verifyNoGeometry(ClassInfo ci, PropertyInfo propi) {
		if (!propi.isNavigable())
			return;
		if (propi.isRestriction())
			return;
		
		Multiplicity m = propi.cardinality();
		if (m.maxOccurs<1)
			return;

		String type = propi.typeInfo().name;
		ProcessMapEntry me = options.targetMapEntry(type, propi.encodingRule("json"));
		if (me!=null && "geometry".equalsIgnoreCase(me.getParam())) {
			result.addWarning(this,106,propi.name(),propi.inClass().name());
		}
	}	
	
	public void write() {
	}

	/** 
	 * <p>This method returns messages belonging to the JSON Schema target by their
	 * message number. The organization corresponds to the logic in module 
	 * ShapeChangeResult. All functions in that class, which require an message
	 * number can be redirected to the function at hand.</p>
	 * @param mnr Message number
	 * @return Message text, including $x$ substitution points.
	 */
	public String message( int mnr ) {
		// Get the message proper and return it with an identification prefixed
		String mess = messageText( mnr );
		if( mess==null ) return null;
		String prefix = "";
		if( mess.startsWith("??") ) {
			prefix = "??";
			mess = mess.substring( 2 );
		}
		return prefix + "JSON Schema Target: " + mess;
	}
	
	@Override
	public void registerRulesAndRequirements(RuleRegistry r) {
		/*
		 * JSON encoding rules
		 */
		r.addRule("rule-json-all-notEncoded");

		r.addExtendsEncRule("geoservices", "*");
		r.addExtendsEncRule("geoservices_extended", "*");
		r.addExtendsEncRule("geojson", "*");
	}
	
	@Override
	public String getDefaultEncodingRule() {
		return "geojson";
	}
	
	/**
	 * This is the message text provision proper. It returns a message for a number.
	 * @param mnr Message number
	 * @return Message text or null
	 */
	protected String messageText( int mnr ) {
		switch( mnr ) {
		case 7:
			return "Schema '$1$' is not encoded.";
		case 8: 
			return "Class '$1$' is not encoded.";
		case 9: 
			return "Property '$1$' is not encoded.";
		case 10:
			return "System error: Exception raised '$1$'. '$2$'";
		case 11:
			return "Error opening or writing to file '$1$'. The class is skipped.";
		case 12:
			return "Directory named '$1$' does not exist or is not accessible.";
			
		case 100: 
			return "??Unknown geometry type '$1$' in property '$2$' in class '$3$'. This geometry property will be ignored.";
		case 101: 
			return "??More than one geometry property specified for type '$2$'. The geometry property '$1$' will be ignored.";
		case 102: 
			return "??The geometry property '$1$' in type '$2$' has a multiplicity greater than one, the multiplicity will be ignored.";
		case 103:
			return "??No JSON representation known for type '$3$' of property '$2$' in class '$1$'; 'string' will be used.";
		case 104:
			return "??No JSON representation known for type '$2$' which is a supertype of '$1$'. The supertype is ignored.";
		case 105:
			return "??No JSON representation known for type '$3$' of property '$2$' in class '$1$'; 'string'/'object' will be used.";
		case 106: 
			return "??A geometry property is specified for data type '$2$', but data types may not have geometry properties. The geometry property '$1$' will be ignored.";
		case 107:
			return "No enumeration values specified in enumeration '$1$'. Schema attribute enum will not be generated.";

		case 10001:
			return "Generating JSON schemas for application schema $1$.";
		case 10002:
			return "Diagnostics-only mode. All output to files is suppressed.";
		case 10003:
			return "??Property '$2$' in class '$1$' is a geometry property and will be ignored.";			

		case 20000:
			return "Context: $1$";
		}
		return null;
	}
	
	@Override
	public String getTargetName(){
		return "JSON Schema";
	}
	
	@Override
	public String getTargetIdentifier() {
	    return "json";
	}

}
