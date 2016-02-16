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

package de.interactive_instruments.ShapeChange.Transformation.Constraints;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.SBVR.Sbvr2FolParser;
import de.interactive_instruments.ShapeChange.SBVR.SbvrConstants;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * Parses First Order Logic expressions from constraints contained in the model.
 * This is especially useful if constraints have been loaded via the
 * ConstraintLoader transformation (which does not parse the constraints).
 * <p>
 * At the moment parsing is only supported for FOL constraints of type 'SBVR'.
 * 
 * @author Johannes Echterhoff
 *
 */
public class FolConstraintParsing implements Transformer {

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		/*
		 * First order logic expressions can be parsed from different sources.
		 * For those where the parser does not need to be set up per constraint,
		 * we can create them outside of the following loops.
		 */
		Sbvr2FolParser sbvrParser = new Sbvr2FolParser(m);

		for (PackageInfo pi : m.selectedSchemas()) {

			for (ClassInfo ci : m.classes(pi)) {

				/*
				 * Ignore constraints on AIXM <<extension>> types
				 */
				if (ci.category() == Options.AIXMEXTENSION) {
					continue;
				}

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

						if (folCon.sourceType().equals(
								SbvrConstants.FOL_SOURCE_TYPE)) {

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
							MessageContext ctx = r.addError(null, 38,
									folCon.sourceType());
							ctx.addDetail(null, 39, folCon.name(), folCon
									.contextModelElmt().fullNameInSchema());
						}
					}
				}
			}
		}
	}

}
