/**
 * ShapeChange - processing application schemas for geographic information
 *
 * <p>This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application Schema from a UML
 * model and translates it into a GML Application Schema or other implementation representations.
 *
 * <p>Additional information about the software can be found at http://shapechange.net/
 *
 * <p>(c) 2002-2022 interactive instruments GmbH, Bonn, Germany
 *
 * <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Contact: interactive instruments GmbH Bundeskanzlerplatz 2d 53113 Bonn Germany
 */
package de.interactive_instruments.shapechange.core.target.diff;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class DiffTargetConstants {

    /**
     * Optional (default is the current run directory) - The path to the folder in
     * which the resulting XML file will be created.
     */
    public static final String PARAM_OUTPUT_DIR = "outputDirectory";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String
     * <p>
     * Default Value: none
     * <p>
     * Explanation: If the target parameter
     * {@value #PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING} is set, and the
     * connection requires a username and password, set the username with this
     * target parameter.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_REFERENCE_MODEL_USER = "referenceModelUsername";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String
     * <p>
     * Default Value: none
     * <p>
     * Explanation: If the target parameter
     * {@value #PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING} is set, and the
     * connection requires a username and password, set the password with this
     * target parameter.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_REFERENCE_MODEL_PWD = "referenceModelPassword";

    public static final String PARAM_REFERENCE_MODEL_TYPE = "referenceModelType";

    /**
     * Alias: none
     * <p>
     * Required / Optional: required
     * <p>
     * Type: String
     * <p>
     * Default Value: none
     * <p>
     * Explanation: This parameter provides the connection info to the UML model
     * that shall be used as reference.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING = "referenceModelFileNameOrConnectionString";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String (with comma separated values)
     * <p>
     * Default Value: _all types of model differences supported by the target_
     * <p>
     * Explanation: Comma-separated list of names of diff element types. The diff
     * result will only provide information on these types of differences (in
     * addition to a possibly existing schema version difference).
     * <p>
     * The following diff element types are currently supported: SELF, NAME,
     * DOCUMENTATION, MULTIPLICITY, VALUETYPE, INITIALVALUE, CLASS, SUPERTYPE,
     * SUBPACKAGE, PROPERTY, ENUM, STEREOTYPE, TAG, ALIAS, DEFINITION, DESCRIPTION,
     * PRIMARYCODE, GLOBALIDENTIFIER, LEGALBASIS, DATACAPTURESTATEMENT, EXAMPLE,
     * LANGUAGE
     * <p>
     * Applies to Rule(s): none - default behavior
     */
    public static final String PARAM_DIFF_ELEMENT_TYPES = "diffElementTypes";

    public static final String PARAM_TAG_PATTERN = "tagPattern";
    public static final String DEFAULT_TAG_PATTERN = ".*";

    public static final String PARAM_INCLUDE_MODEL_DATA = "includeModelData";

    public static final String PARAM_PRINT_MODEL_ELEMENT_PATHS = "printModelElementPaths";

    public static final String PARAM_AAA_MODEL = "aaaModel";

    public static final String PARAM_RELEVANTE_MODELLARTEN = "relevanteModellarten";

    public static final String PARAM_TAGS_TO_SPLIT = "tagsToSplit";
}
