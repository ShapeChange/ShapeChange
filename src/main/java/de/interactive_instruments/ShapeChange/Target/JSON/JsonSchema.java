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

package de.interactive_instruments.ShapeChange.Target.JSON;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class JsonSchema implements Target, MessageSource {

	// TODO convert to more fine-grained info.matches() logic
	
	public static final String PARAM_SKIP_NOT_IMPLEMENTED_CHECK = "skipNotImplementedCheck";
	private static final String JSON_SCHEMA_URI_DRAFT_03 = "http://json-schema.org/draft-03/schema#";
	private static final String JSON_SCHEMA_URI_DRAFT_04 = "http://json-schema.org/draft-04/schema#";

	private class Context {
		protected String links = null;
		protected boolean first = true;
		protected BufferedWriter writer = null;
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

	/**
	 * <p>Initialize target generation for the JSON Schema output.</p> 
	 * @param pi UML Package represented by PackageInfo interface
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
		
		outputDirectory = options.parameter(this.getClass().getName(),"outputDirectory");
		if (outputDirectory==null)
			outputDirectory = options.parameter(".");

		String s = options.parameter(this.getClass().getName(),"includeDocumentation");
		if (s!=null && s.equalsIgnoreCase("false"))
			includeDocumentation = false;
		
		schemaURI = options.parameter(this.getClass().getName(),"jsonSchemaURI");
		if (schemaURI==null)
			schemaURI = JSON_SCHEMA_URI_DRAFT_03;

		baseURI = pi.taggedValue("jsonBaseURI");
		if (baseURI==null)
			baseURI = options.parameter(this.getClass().getName(),"jsonBaseURI");
		if (baseURI==null)
			baseURI = "FIXME";
		
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

	public void process(ClassInfo ci) {
		
		int cat = ci.category();
				
		if (options.matchesEncRule(ci.encodingRule("json"),"geoservices")) {
			if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN) {
				return;
			}
		} else if (options.matchesEncRule(ci.encodingRule("json"),"geoservices_extended")) {
			if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN && 
				cat != Options.DATATYPE && cat != Options.UNION) {
				return;
			}
		}
		
		Context ctx = new Context();
		try {
			
			if (!diagnosticsOnly)
				ctx.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(subDirectoryFile, ci.name()+".json")), "UTF-8"));
			
			write(ctx,"{");
			newLine(ctx);
			write(ctx,"\t"+"\"$schema\":\""+schemaURI+"\",");
			newLine(ctx);
			write(ctx,"\t"+"\"id\":\""+baseURI+"/"+subdir+"/"+ci.name()+".json\",");
			newLine(ctx);
			String s = ci.aliasName();
			write(ctx,"\t"+"\"title\":\""+(s==null||s.isEmpty()?ci.name():s)+"\",");
			newLine(ctx);
			String s2 = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
			if (ci.globalId() != null) {
				write(ctx, "\t" + "\"description\":\"" + ci.globalId() + "\",");
				newLine(ctx);
			} else if (includeDocumentation && !s2.isEmpty()) {
				write(ctx,"\t"+"\"description\":\""+escape(s2).trim()+"\",");
				newLine(ctx);				
			}
			write(ctx,"\t"+"\"type\":\"object\",");
			newLine(ctx);
			
			write(ctx,"\t"+"\"properties\":{");
			newLine(ctx);

			// add entityType for features and objects
			if (cat==Options.FEATURE || cat==Options.OBJECT) {
				write(ctx,"\t\t"+"\"entityType\":{");
				newLine(ctx);
				write(ctx,"\t\t\t"+"\"title\":\"feature/object type\",");
				newLine(ctx);
				write(ctx,"\t\t\t"+"\"type\":\"string\",");
				newLine(ctx);
				write(ctx,"\t\t\t"+"\"default\":\""+(s==null||s.isEmpty()?ci.name():s)+"\"");
				newLine(ctx);
				write(ctx,"\t\t},");
				newLine(ctx);
			}

			// add geometry for features and objects
			if (cat==Options.FEATURE || cat==Options.OBJECT) {
				String geomType = determineGeometryType(ci);
				if (geomType!=null) {
					write(ctx,"\t\t"+"\"geometry\":{");
					newLine(ctx);
					write(ctx,"\t\t\t"+"\"$ref\":\""+geomType+"\"");
					newLine(ctx);
					write(ctx,"\t\t},");
					newLine(ctx);
				}
			} else if (cat==Options.DATATYPE || cat==Options.UNION) {
				verifyNoGeometry(ci);
			}
			
			write(ctx,"\t\t"+"\"attributes\":{");
			newLine(ctx);
			write(ctx,"\t\t\t"+"\"title\":\"feature attributes\",");
			newLine(ctx);
			write(ctx,"\t\t\t"+"\"type\":\"object\",");
			newLine(ctx);
			write(ctx,"\t\t\t"+"\"properties\":{");
			newLine(ctx);
			
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
							ctx = ProcessProperties(ctx, cix);
					}
				}
			}
			
			ctx = ProcessProperties(ctx, ci);
			
			newLine(ctx);
			write(ctx,"\t\t\t}");
			if (JSON_SCHEMA_URI_DRAFT_04.equals(schemaURI)) {
				// TODO add required keyword: way of defining required properties is different for JSON Schema draft v04
			}
			newLine(ctx);
			write(ctx,"\t\t}");
			
	        newLine(ctx);
			write(ctx,"\t}");
			
			if (ctx.links!=null) {
				write(ctx,",");
				newLine(ctx);
				String[] lines = ctx.links.split("\n");
				for (int i=0; i<lines.length; i++) {
					write(ctx,lines[i]);
					newLine(ctx);
				}
				write(ctx,"\t]");
			}
			
			newLine(ctx);
			write(ctx,"}");
			newLine(ctx);			
			
			if (ctx.writer!=null) {
				ctx.writer.close();
				result.addResult(getTargetID(), subDirectoryFile.getPath(), ci.name()+".json", ci.qname());
			}
			
		} catch( IOException e ) {
			// Opening the file went wrong, skip class
			result.addError( this, 11, ci.name()+".json" );
			return;
		}
	}
	
	private void write(Context ctx, String text) throws IOException {
		if (!diagnosticsOnly && ctx!=null && ctx.writer!=null) {
			ctx.writer.write(text);
		}		
	}

	private void newLine(Context ctx) throws IOException {
		if (!diagnosticsOnly && ctx!=null && ctx.writer!=null) {
			ctx.writer.newLine();
		}		
	}

	private Context ProcessProperties(Context ctx, ClassInfo ci) throws IOException {
		return ProcessProperties(ctx, ci, null, ci.category()!=Options.UNION);
	}
	
	private Context ProcessProperties(Context ctx, ClassInfo ci, String propertyPrefix, boolean required) throws IOException {
		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
			PropertyInfo propi = j.next();
			
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
			String enums = null;
			boolean nillable = false;
			boolean flatten = false;
			// First handle well-known ones
			MapEntry me = options.targetTypeMapEntry(getClass().getName(), ti.name, propi.encodingRule("json"));
			if (me!=null) {
				if (me.p2.equalsIgnoreCase("geometry")) {
					result.addDebug(this, 10003, propi.inClass().name(), propi.name());
					continue;
				} else if (me.p1.startsWith("ref:")) {
					ref = me.p1.substring(4);
				} else {
					type = me.p1;
					if (me.p2.startsWith("format:"))
						format = me.p2.substring(7);
				}
			}
			
			ClassInfo cix = null;
			if (type==null && ref==null) {
				// Handle well-known type prefixes from base models that we know are not supported by JSON 
				// or a well-known JSON schema
				cix = model.classById(ti.id);
				if (cix==null) {
					if (options.matchesEncRule(propi.encodingRule("json"),"geoservices")) {
						result.addWarning(this, 103, propi.inClass().name(), propi.name(), ti.name);
						type = "string";
					} else if (options.matchesEncRule(propi.encodingRule("json"),"geoservices_extended")) {
						result.addWarning(this, 105, propi.inClass().name(), propi.name(), ti.name);
						type = "any";
					}
				} else {
					int cat = cix.category();
					if (cat==Options.CODELIST) {
						if (options.matchesEncRule(cix.encodingRule("json"),"geoservices")) {
							type = "string";
						} else if (options.matchesEncRule(cix.encodingRule("json"),"geoservices_extended")) {
							type = "string";
							format = "uri";
						}					
					} else if (cat==Options.ENUMERATION) {
						type = "string";
						if (cix.properties().isEmpty()) {
							result.addWarning(this, 107, cix.name());
						} else {
						enums = "[";
						boolean fst = true;
						for (Iterator<PropertyInfo> k = cix.properties().values().iterator(); k.hasNext();) {
							PropertyInfo propix = k.next();
							if (fst)
								fst = false;
							else
								enums += ",";
							enums += "\""+propix.name()+"\"";
						}
						enums += "]";
						}
					} else if (cat==Options.FEATURE || cat==Options.OBJECT || cat==Options.MIXIN) {
						if (options.matchesEncRule(cix.encodingRule("json"),"geoservices")) {
							type = "integer";
							String lyrURI = cix.taggedValue("jsonLayerTableURI");
							if (lyrURI!=null) {
								if (ctx.links==null) {
									ctx.links = "\t\"links\":[\n";
								} else {
									ctx.links += ",\n";
								}
								ctx.links += "\t\t{\n";
								ctx.links += "\t\t\t\"rel\":\"related\",\n";
								ctx.links += "\t\t\t\"href\":\""+lyrURI+"/{#/attributes/"+propi.name()+"}?f=json\"\n";
								ctx.links += "\t\t}";
							}
						} else if (options.matchesEncRule(cix.encodingRule("json"),"geoservices_extended")) {
							type = "string";
							format = "uri";
						}					
					} else if (cat==Options.DATATYPE || cat==Options.UNION) {
						
						if (options.matchesEncRule(cix.encodingRule("json"),"geoservices")) {
							
							flatten = true;
							verifyNoGeometry(cix);
							
						} else if (options.matchesEncRule(cix.encodingRule("json"),"geoservices_extended")) {
							
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
			
			if (options.matchesEncRule(propi.encodingRule("json"),"geoservices_extended") && propi.voidable())
				nillable = true; 
			
			int repeat = 1;
			boolean array = false;
			if (type!=null || ref!=null || flatten) {
				if (m.maxOccurs>1) {
					if (options.matchesEncRule(propi.encodingRule("json"),"geoservices")) {
						repeat = 3;
					} else if (options.matchesEncRule(propi.encodingRule("json"),"geoservices_extended")) {
						array = true;
					}
				}
				
				// TODO support for pattern
				
				if (flatten && cix!=null) {
					for (int i = 1; i <= repeat; i++) {
						String n = nam;
						if (repeat>1)
							n = n+"-"+i;
						ProcessProperties(ctx, cix, n, required && m.minOccurs>0 && cix.category()!=Options.UNION);
					}
				} else {
					for (int i = 1; i <= repeat; i++) {
						if (!ctx.first) {
							write(ctx,",");
							newLine(ctx);
						} else
							ctx.first = false;
						String n = nam;
						if (repeat>1)
							n = n+"-"+i;
						write(ctx,"\t\t\t\t"+"\""+n+"\":{");
						newLine(ctx);
						String s = propi.aliasName();
						write(ctx,"\t\t\t\t\t"+"\"title\":\""+(s==null||s.isEmpty()?propi.name():s)+"\",");
						newLine(ctx);
						String s2 = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
						if (propi.globalId() != null) {
							write(ctx, "\t\t\t\t\t" + "\"description\":\"" + propi.globalId() + "\",");
							newLine(ctx);
						} else if (includeDocumentation && !s2.isEmpty()) {
							write(ctx,"\t\t\t\t\t"+"\"description\":\""+escape(s2).trim()+"\",");
							newLine(ctx);				
						}
						if (array) {
							if (nillable && i==1)
								write(ctx,"\t\t\t\t\t"+"\"type\":[\"array\",\"null\"],");
							else
								write(ctx,"\t\t\t\t\t"+"\"type\":\"array\",");
							newLine(ctx);
							write(ctx,"\t\t\t\t\t"+"\"items\":{");
							newLine(ctx);
							if (ref!=null) {
								write(ctx,"\t\t\t\t\t\t"+"\"$ref\":\""+ref+"\"");
								newLine(ctx);							
							} else {
								write(ctx,"\t\t\t\t\t\t"+"\"type\":\""+type+"\"");
								if (format!=null) {
									write(ctx,",");
									newLine(ctx);							
									write(ctx,"\t\t\t\t\t\t"+"\"format\":\""+format+"\"");
								}
								if (enums!=null) {
									write(ctx,",");
									newLine(ctx);							
									write(ctx,"\t\t\t\t\t\t"+"\"enum\":"+enums+"");
								}
							}
							newLine(ctx);
							write(ctx,"\t\t\t\t\t}");
								if (m.minOccurs>0 && required) {
									write(ctx,",");
									newLine(ctx);																							
								write(ctx,"\t\t\t\t\t"+"\"minItems\":"+m.minOccurs);
								}
								newLine(ctx);		
							
						} else {
							if (ref!=null) {
								write(ctx,"\t\t\t\t\t"+"\"$ref\":\""+ref+"\"");
								newLine(ctx);							
							} else {
								if (nillable && i==1)
									write(ctx,"\t\t\t\t\t"+"\"type\":[\""+type+"\",\"null\"]");
								else
									write(ctx,"\t\t\t\t\t"+"\"type\":\""+type+"\"");
								if (format!=null) {
									write(ctx,",");
									newLine(ctx);							
									write(ctx,"\t\t\t\t\t"+"\"format\":\""+format+"\"");
								}
								if (enums!=null) {
									write(ctx,",");
									newLine(ctx);							
									write(ctx,"\t\t\t\t\t"+"\"enum\":"+enums+"");
								}
								if (JSON_SCHEMA_URI_DRAFT_03.equals(schemaURI) && m.minOccurs>0 && i==1 && required) {
									write(ctx,",");
									newLine(ctx);																							
									write(ctx,"\t\t\t\t\t"+"\"required\":true");
								}
								newLine(ctx);															
							}						
						}
						write(ctx,"\t\t\t\t}");
						
						if (nillable && i==1) {
							write(ctx,",");
							newLine(ctx);
							write(ctx,"\t\t\t\t"+"\""+n+"_nullReason\":{");
							newLine(ctx);
							write(ctx,"\t\t\t\t\t"+"\"title\":\"Reason for null value in property "+(s==null||s.isEmpty()?propi.name():s)+"\",");
							newLine(ctx);
							write(ctx,"\t\t\t\t\t"+"\"type\":\"string\"");
							newLine(ctx);															
							write(ctx,"\t\t\t\t}");
						}
					}				
				}
			} else {
				// TODO 2016-09-26: is the message misleading? The property is not encoded at all.
				result.addWarning(this, 103, propi.inClass().name(), propi.name(), ti.name);
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

		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
			PropertyInfo propi = j.next();
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
		MapEntry me = options.targetTypeMapEntry(getClass().getName(), type, propi.encodingRule("json"));
		if (me!=null && me.p2.equalsIgnoreCase("geometry")) {
			if (m.maxOccurs>1)
				result.addWarning(this,102,propi.name(),propi.inClass().name());
			if (geomType==null) {
				// remove "ref:" prefix
				geomType = me.p1.substring(4);
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

		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
			PropertyInfo propi = j.next();
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
		MapEntry me = options.targetTypeMapEntry(getClass().getName(), type, propi.encodingRule("json"));
		if (me!=null && me.p2.equalsIgnoreCase("geometry")) {
			result.addWarning(this,106,propi.name(),propi.inClass().name());
		}
	}	
	
	/**
	 * <p>See https://tools.ietf.org/html/rfc7159: characters that must be escaped quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)</p>
	 * <p>Control characters that are likely to occur in EA models, especially when using memo tagged values: \n, \r and \t.</p>
	 */
	private String escape(String s2) {
		return StringUtils.replaceEach(s2, new String[]{"\"", "\\", "\n", "\r", "\t"} , new String[]{"\\\"", "\\\\", CharUtils.unicodeEscaped('\n'),  CharUtils.unicodeEscaped('\r'), CharUtils.unicodeEscaped('\t')});
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
	
	/**
	 * This is the message text provision proper. It returns a message for a number.
	 * @param mnr Message number
	 * @return Message text or null
	 */
	protected String messageText( int mnr ) {
		switch( mnr ) {
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
			return "??No JSON representation known for type '$3$' of property '$2$' in class '$1$'; 'any' will be used.";
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

		}
		return null;
	}
	
	public int getTargetID(){
		return TargetIdentification.JSON.getId();
	}
}
