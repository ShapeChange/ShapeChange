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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.SBVR;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.FOL.ClassCall;
import de.interactive_instruments.ShapeChange.FOL.PropertyCall;
import de.interactive_instruments.ShapeChange.FOL.SchemaCall;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.antlr.ShapeChangeAntlr.SbvrParserHelper;

/**
 * @author Johannes Echterhoff
 *
 */
public class SbvrUtil {

	/**
	 * Copies the schema call, creating new objects for the schema call itself
	 * as well as the next element (recursively). Does not copy the schema
	 * element referenced by the schema call, or the variable context - but sets
	 * them to the same reference as in the original schema call.
	 * 
	 * @param sc tbd
	 * @return tbd
	 */
	public static SchemaCall copy(SchemaCall sc) {

		if (sc == null) {
			return null;
		} else {

			SchemaCall result;

			if (sc instanceof PropertyCall) {

				PropertyCall pc = new PropertyCall();
				pc.setSchemaElement(((PropertyCall) sc).getSchemaElement());
				result = pc;

			} else {

				ClassCall cc = new ClassCall();
				cc.setSchemaElement(((ClassCall) sc).getSchemaElement());
				result = cc;
			}

			result.setNameInSbvr(sc.getNameInSbvr());

			if (sc.hasVariableContext()) {
				result.setVariableContext(sc.getVariableContext());
			}

			if (sc.hasNextElement()) {
				result.setNextElement(copy(sc.getNextElement()));
			}

			return result;
		}
	}

	/**
	 * Identifies the nouns contained in the model, more specifically in the
	 * selected schemas. Each class and property name is considered to be a
	 * noun.
	 * <p>
	 * For AIXM schemas, the following nouns are also recognized (even if they
	 * do not occur as such in the selected schemas):
	 * 
	 * <ul>
	 * <li>{feature type name}TimeSlice</li>
	 * <li>timeSlice</li>
	 * <li>interpretation</li>
	 * <li>sequenceNumber</li>
	 * <li>correctionNumber</li>
	 * <li>timeSliceMetadata</li>
	 * <li>featureLifetime</li>
	 * <li>validTime</li>
	 * <li>featureMetadata</li>
	 * </ul>
	 * 
	 * @param m tbd
	 * @return verbs contained in the selected schemas (including 'has' and
	 *         'have')
	 */
	public static SortedSet<String> identifyNouns(Model m) {

		SortedSet<String> nouns = new TreeSet<String>();

		SortedSet<? extends PackageInfo> schemas = m.selectedSchemas();

		if (schemas != null) {

			// identify processing specific nouns and verbs

			for (PackageInfo schema : schemas) {

				SortedSet<ClassInfo> classes = m.classes(schema);

				if (classes != null) {

					for (ClassInfo ci : classes) {

						nouns.add(ci.name());

						if (m.options().isAIXM()
								&& ci.category() == Options.FEATURE) {
							nouns.add(ci.name() + "TimeSlice");
							nouns.add("timeSlice");
							nouns.add("interpretation");
							nouns.add("sequenceNumber");
							nouns.add("correctionNumber");
							nouns.add("timeSliceMetadata");
							nouns.add("featureLifetime");
							nouns.add("validTime");
							nouns.add("featureMetadata");
						}

						SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

						if (pis != null) {

							for (PropertyInfo pi : pis.values()) {

								nouns.add(pi.name());
							}
						}
					}
				}
			}
		}

		return nouns;
	}

	/**
	 * Identifies the verbs contained in the model, more specifically in the
	 * selected schemas. Each association name is considered to be a verb. In
	 * addition, 'has' and 'have' are always recongized as verbs.
	 * 
	 * @param m tbd
	 * @return verbs contained in the selected schemas (including 'has' and
	 *         'have')
	 */
	public static SortedSet<String> identifyVerbs(Model m) {

		TreeSet<String> verbs = new TreeSet<String>();

		SortedSet<? extends PackageInfo> schemas = m.selectedSchemas();

		if (schemas != null) {

			// add general verbs
			verbs.add("has");
			verbs.add("have");

			// identify processing specific verbs

			for (PackageInfo schema : schemas) {

				SortedSet<ClassInfo> classes = m.classes(schema);

				if (classes != null) {

					for (ClassInfo ci : classes) {

						SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

						if (pis != null) {

							for (PropertyInfo pi : pis.values()) {

								if (!pi.isAttribute()) {

									AssociationInfo ai = pi.association();
									if (ai.name() != null
											&& ai.name().length() > 0) {
										verbs.add(ai.name());
									}
								}
							}
						}
					}
				}
			}
		}

		return verbs;
	}

	/**
	 * @param m tbd
	 * @return a parser helper with nouns and verbs initialized with information
	 *         identified from the given model (using
	 *         {@link #identifyNouns(Model)} and {@link #identifyVerbs(Model)}).
	 */
	public static SbvrParserHelper createParserHelper(Model m) {

		SbvrParserHelper helper = new SbvrParserHelper();

		helper.nouns = SbvrUtil.identifyNouns(m);
		helper.verbs = SbvrUtil.identifyVerbs(m);

		return helper;
	}

	public static void printErrors(List<SbvrErrorInfo> errors,
			String sbvrRuleText, ShapeChangeResult result, boolean asWarnings) {
		
		String msg;

		for (SbvrErrorInfo err : errors) {
			
			msg = SbvrConstants.INDENTATION_FOR_MESSAGE_DETAILS
					+ err.getErrorCategory() + ": " + err.getErrorMessage();
			
			if(asWarnings) {
				result.addWarning(msg);
			} else {
				result.addError(msg);
			}

			if (err.hasOffendingTextInfo()) {

				msg = SbvrConstants.INDENTATION_FOR_MESSAGE_DETAILS
						+ SbvrConstants.RULE_MESSAGE_PREFIX + sbvrRuleText;
				
				if(asWarnings) {
					result.addWarning(msg);
				} else {
					result.addError(msg);
				}
				
				StringBuilder sb = new StringBuilder();

				int start = err.getOffendingTextStartIndex();
				int stop = err.getOffendingTextStopIndex();

				for (int i = 0; i < start
						+ SbvrConstants.INDENTATION_FOR_MESSAGE_DETAILS
								.length()
						+ SbvrConstants.RULE_MESSAGE_PREFIX.length(); i++)
					sb.append(" ");

				if (start >= 0 && stop >= 0) {
					for (int i = start; i <= stop; i++)
						sb.append("^");
				}
				
				msg = sb.toString();
				
				if(asWarnings) {
					result.addWarning(msg);
				} else {
					result.addError(msg);
				}
			}
		}
	}
}
