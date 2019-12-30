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

package de.interactive_instruments.ShapeChange.Target;

import de.interactive_instruments.ShapeChange.Converter;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;

public interface Target {

    /**
     * Call upon the target to register all relevant conversion and standard
     * encoding rules as well as requirements. This method will automatically be
     * called by ShapeChange in order to populate the rule registry. If the target
     * does not define any such rules or requirements, nothing needs to be loaded.
     * <p>
     * Example use:
     * <ul>
     * <li>A rule must first be added by calling
     * {@link RuleRegistry#addRule(String)} - e.g.
     * <code>r.addRule("req-xsd-pkg-xsdDocument-unique");</code></li>
     * <li>The rule can then be associated with an encoding rule by calling
     * {@link RuleRegistry#addRule(String, String)} - e.g.
     * <code>r.addRule("req-xsd-pkg-xsdDocument-unique", "*");</code> NOTE: '*'
     * always represents the core, i.e. global, encoding rule.</li>
     * <li>You can also register an encoding rule that extends another encoding rule
     * by calling {@link RuleRegistry#addExtendsEncRule(String, String)} - e.g.
     * <code>r.addExtendsEncRule("iso19136_2007", "*")</code>; NOTE: the
     * default encoding rule (returned by {@link #getDefaultEncodingRule()}), if
     * other than '*', must be added this way</li>
     * </ul>
     * 
     * @param r the registry to which the rules and requirements shall be added
     */
    public void registerRulesAndRequirements(RuleRegistry r);

    /**
     * Allows a target to perform the necessary initialization routines before
     * processing.
     * <p>
     * Will be called by the {@link Converter} for each selected schema (see
     * {@link Model#selectedSchemas()} and {@link PackageInfo#isSchema()}).
     * 
     * @param pi       a schema from the model selected via the configuration (see
     *                 {@link Model#selectedSchemas()}) - not necessarily always an
     *                 application schema
     * @param m        the model
     * @param o        the options object for this configuration
     * @param r        the results object to report log messages
     * @param diagOnly a flag to request a dry run only, no output will be created
     * @throws ShapeChangeAbortException tbd
     * @see Model#selectedSchemas()
     * @see PackageInfo#isSchema()
     */
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException;

    /**
     * The converter will call this method for each class belonging to the package
     * given during initialization (see {@link #initialise}).
     * <p>
     * NOTE: will be called not only for the classes directly contained in the
     * package, but also all sub-packages belonging to the same targetNamespace!
     * 
     * @param ci tbd
     */
    public void process(ClassInfo ci);

    public void write();

    /**
     * @return human readable name for the target, primarily used in log messages
     */
    public String getTargetName();

    /**
     * @return The identifier of the target, as used in conversion rules (e.g. 'xsd'
     *         for the XmlSchema target, with conversion rule identifiers having the
     *         structure 'rule-xsd-[...]'). Note: case matters ('xsD' is not equal
     *         to 'xsd')!
     */
    public String getTargetIdentifier();

    /**
     * @return name of the default encoding rule to be used for this target;
     *         typically defaults to '*' (if <code>null</code> is returned, it will
     *         be considered equal to '*'); if not '*', then the encoding rule must
     *         be added via {@link #registerRulesAndRequirements(RuleRegistry)}
     */
    public String getDefaultEncodingRule();

};