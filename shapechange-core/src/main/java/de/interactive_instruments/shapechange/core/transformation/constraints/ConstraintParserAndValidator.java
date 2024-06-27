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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.transformation.constraints;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TransformerConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.fol.FolExpression;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Constraint;
import de.interactive_instruments.shapechange.core.model.FolConstraint;
import de.interactive_instruments.shapechange.core.model.OclConstraint;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericClassInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.model.generic.GenericOclConstraint;
import de.interactive_instruments.shapechange.core.model.generic.GenericPropertyInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericTextConstraint;
import de.interactive_instruments.shapechange.core.sbvr.Sbvr2FolParser;
import de.interactive_instruments.shapechange.core.sbvr.SbvrConstants;
import de.interactive_instruments.shapechange.core.transformation.Transformer;

/**
 * Parses and validates constraints contained in the model (more specifically:
 * the schemas selected for processing) to ensure that they are valid in the
 * context of that model.
 * <p>
 * This can be useful if the model has been modified - especially through the
 * Profiler transformation - and in case that constraints have not been parsed
 * and validated yet. The latter is the case for profile constraints and FOL
 * constraints.
 * <p>
 * Via the ConstraintParserAndValidator, the functionality to parse/validate
 * constraints is available in a single piece of code that can be executed at
 * any place in the transformation process of ShapeChange.
 * <p>
 * By default, the transformation parses and valdiates all types of constraints.
 * If a profile constraint is encountered, it is converted according to its type
 * before parsing and validating it. The transformation parameters named
 * <i>xxx</i>ConstraintTypeRegex influence to which type the constraint is
 * converted.
 * <p>
 * NOTE: profile constraints have not been implemented yet
 * 
 * @author Johannes Echterhoff
 *
 */
public class ConstraintParserAndValidator implements Transformer {

	// public static final String PARAM_OCL_TYPE_REGEX_NAME =
	// "oclConstraintTypeRegex";
	// public static final String PARAM_FOL_TYPE_REGEX_NAME =
	// "folConstraintTypeRegex";
	//
	// protected String ocl_type_regex = "OCL|Invariant";
	// protected String fol_type_regex = "(" + SbvrConstants.FOL_SOURCE_TYPE +
	// ")";

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		/*
		 * NOTE retrieving these regexes is only needed for dealing with profile
		 * constraints, because for an OclConstraint or FolConstraint the type
		 * is clear, and all other constraint types (unless it were a profile
		 * constraint) must be TextConstraints.
		 */
		// if (trfConfig.hasParameter(PARAM_OCL_TYPE_REGEX_NAME)) {
		// this.ocl_type_regex = trfConfig
		// .getParameterValue(PARAM_OCL_TYPE_REGEX_NAME);
		// }
		//
		// if (trfConfig.hasParameter(PARAM_FOL_TYPE_REGEX_NAME)) {
		// this.fol_type_regex = trfConfig
		// .getParameterValue(PARAM_FOL_TYPE_REGEX_NAME);
		// }

		Sbvr2FolParser sbvrParser = new Sbvr2FolParser(m);

		/*
		 * Handle actual constraints
		 */
		for (PackageInfo pkg : m.selectedSchemas()) {

			for (ClassInfo tmp : m.classes(pkg)) {

				/*
				 * Cast should be safe, because all classes of 'pkg' are
				 * GenericClassInfos.
				 */
				GenericClassInfo genCi = (GenericClassInfo) tmp;

				/*
				 * Ignore constraints on AIXM <<extension>> types
				 */
				if (genCi.category() == Options.AIXMEXTENSION) {
					continue;
				}

				List<Constraint> ciCons = genCi.directConstraints();

				if (ciCons != null) {

					// sort the constraints by name
					Collections.sort(ciCons, ConstraintComparators.NAME);

					Vector<Constraint> newConstraints = new Vector<Constraint>();

					for (Constraint con : ciCons) {

						if (con instanceof OclConstraint) {

							OclConstraint oclCon = (OclConstraint) con;

							newConstraints.add(parse(oclCon, genCi));

						} else if (con instanceof FolConstraint) {

							FolConstraint folCon = (FolConstraint) con;

							newConstraints
									.add(parse(folCon, sbvrParser, genCi, r));
						} else {

							// for all other cases, simply add the constraint
							newConstraints.add(con);
						}
					}

					genCi.setDirectConstraints(newConstraints);

				}

				// check constraints on properties
				if (genCi.properties() != null) {

					for (PropertyInfo pi : genCi.properties().values()) {

						/*
						 * Cast should be safe, because all properties of
						 * 'genCi' are GenericPropertyInfos.
						 */
						GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

						List<Constraint> piCons = genPi.constraints();

						if (piCons != null) {

							// sort the constraints by name
							Collections.sort(piCons,
									ConstraintComparators.NAME);

							Vector<Constraint> newConstraints = new Vector<Constraint>();

							for (Constraint con : piCons) {

								if (con instanceof OclConstraint) {

									OclConstraint oclCon = (OclConstraint) con;

									newConstraints.add(parse(oclCon, genPi));

								} else {

									/*
									 * For all other cases, simply add the
									 * constraint.
									 * 
									 * 2016-07-12 JE: at the moment,
									 * FolConstraints are only created with
									 * classes as context element. Therefore
									 * there is no need to handle FolConstraints
									 * here.
									 */
									newConstraints.add(con);
								}
							}
						}
					}
				}
			}
		}
	}

	public static Constraint parse(FolConstraint con, Sbvr2FolParser parser,
			GenericClassInfo genCi, ShapeChangeResult r) {

		if (con.sourceType().equals(SbvrConstants.FOL_SOURCE_TYPE)) {

			con.mergeComments(new String[] { con.text() });
			
			FolExpression folExpr = parser.parse(con);

			if (folExpr != null) {

				con.setFolExpression(folExpr);
				return con;

			} else {
				/*
				 * The parser already logged why the expression was not created;
				 * use a text constraint as fallback.
				 */
				return new GenericTextConstraint(genCi, con);
			}

		} else {

			/*
			 * Apparently a new source for FOL constraints exists - add parsing
			 * it here; in the meantime, log this as an error and create a text
			 * constraint as fallback.
			 */
			MessageContext ctx = r.addError(null, 38, con.sourceType());
			ctx.addDetail(null, 39, con.name(),
					con.contextModelElmt().fullNameInSchema());

			return new GenericTextConstraint(genCi, con);
		}
	}

	public static Constraint parse(OclConstraint con, GenericClassInfo genCi) {

		GenericOclConstraint validated = new GenericOclConstraint(genCi, con);

		if (validated.syntaxTree() != null) {
			/*
			 * Parsing succeeded
			 */
			return validated;

		} else {

			/*
			 * The reason why parsing the constraint failed has already been
			 * logged; use a text constraint as fallback.
			 */
			GenericTextConstraint fallback = new GenericTextConstraint(genCi,
					con);
			return fallback;
		}
	}

	public static Constraint parse(OclConstraint con,
			GenericPropertyInfo genPi) {

		GenericOclConstraint validated = new GenericOclConstraint(genPi, con);

		if (validated.syntaxTree() != null) {
			/*
			 * Parsing succeeded
			 */
			return validated;

		} else {

			/*
			 * The reason why parsing the constraint failed has already been
			 * logged; use a text constraint as fallback.
			 */
			GenericTextConstraint fallback = new GenericTextConstraint(genPi,
					con);
			return fallback;
		}
	}

	public static class ConstraintComparators {

		public static Comparator<Constraint> NAME = new Comparator<Constraint>() {
			@Override
			public int compare(Constraint o1, Constraint o2) {
				return o1.name().compareTo(o2.name());
			}
		};
	}
}
