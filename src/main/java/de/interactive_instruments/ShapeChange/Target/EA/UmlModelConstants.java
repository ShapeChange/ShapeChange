/**
 * ShapeChange - processing application schemas for geographic information
 *
 * <p>This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application Schema from a UML
 * model and translates it into a GML Application Schema or other implementation representations.
 *
 * <p>Additional information about the software can be found at http://shapechange.net/
 *
 * <p>(c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.EA;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class UmlModelConstants {

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: String (with regular expression)
     *
     * <p>
     * Default Value: none
     *
     * <p>
     * Explanation: A tagged value that matches the regular expression defined by
     * this parameter will not be written to the EA repository.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_IGNORE_TAGGED_VALUES_REGEX = "ignoreTaggedValuesRegex";

    /**
     * Optional (default is the current run directory) - The path to the folder in
     * which the resulting UML model will be created.
     */
    public static final String PARAM_OUTPUT_DIR = "outputDirectory";

    /**
     * Optional (default is determined by the EA process) - Value for the field
     * 'Author' of an EA element.
     */
    public static final String PARAM_EA_AUTHOR = "eaAuthor";

    /**
     * Optional (default is determined by the EA process) - Value for the field
     * 'Status' of an EA element.
     */
    public static final String PARAM_EA_STATUS = "eaStatus";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: String
     *
     * <p>
     * Default Value: ShapeChangeExport.qea
     *
     * <p>
     * Explanation: The Enterprise architect EA repository file to which the application
     * schema(s) are written.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_MODEL_FILENAME = "modelFilename";
    public static final String PARAM_MODEL_FILENAME_DEFAULT = "ShapeChangeExport.qea";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: false
     *
     * <p>
     * Explanation: Can be used to prevent the addition of the timestamp to the new
     * class view package that is added to the model by ShapeChange.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_OMIT_OUTPUT_PACKAGE_DATETIME = "omitOutputPackageDateTime";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: false
     *
     * <p>
     * Explanation: Can be used to prevent the addition of a new
     * class view package as child of the root model (package). That new package is typically 
     * added by ShapeChange to store the output of the target execution.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_OMIT_OUTPUT_PACKAGE = "omitOutputPackage";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: String
     *
     * <p>
     * Default Value: ShapeChangeOutput
     *
     * <p>
     * Explanation: Define the name of the output package that will be added by ShapeChange 
     * as child of the root model package (unless parameter {@value #PARAM_OMIT_OUTPUT_PACKAGE} is set to true). 
     * The current date and time will be added to that name (unless parameter {@value #PARAM_OMIT_OUTPUT_PACKAGE_DATETIME} is set to true).
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_OUTPUT_PACKAGE_NAME = "outputPackageName";

    
    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: false
     *
     * <p>
     * Explanation: If set to true, the package hierarchy within schemas selected
     * for processing, and also above such schemas, is preserved.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_PRESERVE_PACKAGE_HIERARCHY = "preservePackageHierarchy";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: String
     *
     * <p>
     * Default Value: none
     *
     * <p>
     * Explanation: Path to the EA template file (can be local or an online
     * resource).
     *
     * <p>
     * If the output file (location and name are defined by the parameters
     * outputDirectory and modelFilename) does not exist, the default behavior of
     * this target is to create a new EA repository.
     *
     * <p>
     * However, if writing the model requires a specific UML Profile / MDG to be
     * available, this would fail if it is not configured in the EA environment
     * where ShapeChange is executed. In that situation, you would want the UML
     * Profile / MDG loaded into the EA repository to which the model is written.
     * Such a repository can be provided as a template, and configured to be used by
     * ShapeChange via the parameter eaTemplate.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_EA_TEMPLATE = "eaTemplate";
    public static final String PARAM_EAP_TEMPLATE = "eapTemplate";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: false
     *
     * <p>
     * Explanation: If set to true, then ownership of an association role will be
     * encoded. A role is either owned by the association or by a class (the class
     * at the other end of the association). In a UML class diagram, ownership by
     * the class is depicted by small filled dot at the association role.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_INCLUDE_ASSOCIATIONEND_OWNERSHIP = "includeAssociationEndOwnership";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: false
     *
     * <p>
     * Explanation: Set this parameter to true, to merge any comment defined for an
     * OCL constraint into the constraint text. ShapeChange supports comments in OCL
     * constraints within java-like comment delimiters: \/* and *\/. Comments may be
     * added to or defined for an OCL constraint via an external source, such as a
     * model transformation or in SCXML via the &lt;description&gt; element of an
     * &lt;OclConstraint&gt; element. Merging means that any comment which is not
     * already contained in the text of the OCL constraint will be prepended to the
     * constraint text, within java-like comment delimiters.
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_MERGE_CONSTRAINT_COMMENTS_INTO_TEXT = "mergeConstraintCommentsIntoText";

    /**
     * Alias: none
     *
     * <p>
     * Required / Optional: optional
     *
     * <p>
     * Type: Boolean
     *
     * <p>
     * Default Value: true
     *
     * <p>
     * Explanation: true, if stereotypes from UML profiles (defined using stereotype
     * map entries) shall automatically be synchronized at the end of processing,
     * else false
     *
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_SYNCH_STEREOTYPES = "synchronizeStereotypes";
}
