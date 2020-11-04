package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * Defines methods used by the XmlSchema target for creating a Schematron
 * Schema.
 * 
 * @author Reinhard Erstling
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public interface SchematronSchema {

    /**
     * Get the 'codeListValuePattern', first looking for an according tagged value
     * on the given code list. If that tagged value is blank ( <code>null</code> or
     * whitespace only) then look for an (XmlSchema) target parameter
     * 'defaultCodeListValuePattern'. If that also does not result in a non-empty
     * value, us the given default pattern.
     *
     * @param codelist       tbd
     * @param defaultPattern pattern to use if the lookup via tagged value and
     *                       target parameter did not yield a non-empty result
     * @return tbd
     */
    public String determineCodeListValuePattern(ClassInfo codelist, String defaultPattern);

    /**
     * Auxiliary method to find out the full, namespace adorned name of a class from
     * the the mapping or the model. As a side effect the method makes the namespace
     * also known to the Schematron schema, appending another &lt;ns&gt; element if
     * necessary.
     *
     * <p>
     * The method considers the mapping in first priority. If the class turns out to
     * map to a basic type, <i>null</i> is returned instead of a name.
     *
     * @param ci ClassInfo object
     * @return Element name of class
     */
    public String getAndRegisterXmlName(ClassInfo ci);

    /**
     * Auxiliary method to find out the full, namespace adorned name of a property
     * from the model. As a side effect the method makes the namespace also known to
     * the Schematron schema, appending another &lt;ns&gt; element if necessary.
     * Takes into account XsdPropertyMapEntry defined in the configuration.
     *
     * @param pi PropertyInfo object
     * @return Element name of property (a QName: {ns prefix}:{property name})
     */
    public String getAndRegisterXmlName(PropertyInfo pi);

    /**
     * Determine the namespace abbreviation and target namespace uri for the given
     * class. If the configuration (via PackageInfo elements) or the schema of the
     * class do not define a target namespace, the namespace mappings defined for
     * the XmlSchema target in the configuration will be consulted.
     *
     * @param ci ClassInfo object to fetch the xmlns and namespace uri from
     */
    public void registerNamespace(ClassInfo ci);

    /**
     * This auxiliary method registers a namespace prefix with the Schematron
     * schema. It adds another &lt;ns&gt; element when the namespace occurs the
     * first time
     *
     * @param xmlns Namespace prefix
     */
    public void registerNamespace(String xmlns);

    /**
     * This special variant of the method above considers the class object to
     * determine the full namespace uri. If this is not successful it resorts to the
     * method above, which uses the namespace mapping mechanism in the Options
     * object.
     *
     * @param xmlns Namespace prefix
     * @param ci    ClassInfo object to fetch the namespace uri from
     */
    public void registerNamespace(String xmlns, ClassInfo ci);

    /**
     * This auxiliary method registers a namespace (prefix and namespace proper)
     * with the Schematron schema. It adds another &lt;ns&gt; element when the
     * namespace occurs the first time.
     *
     * @param xmlns Namespace prefix
     * @param ns    Namespace proper
     */
    public void registerNamespace(String xmlns, String ns);

    /**
     * Set the attribute 'queryBinding' of the schematron document, for example to
     * 'xslt2'.
     *
     * @param qb tbd
     */
    public void setQueryBinding(String qb);

    /**
     * This function serializes the generated Schematron schema to the given
     * directory. The document name is derived from the name of the GML application
     * schema. Serialization takes place only if at least one rule has been
     * generated.
     *
     * @param outputDirectory tbd
     */
    public void write(String outputDirectory);

    /**
     * Add an assertion statement embodied in an XpathFragment object and output it
     * as a Schematron &lt;assert&gt; element, which is contained in a proper
     * &lt;rule&gt; context. &lt;let&gt; elements are searched for identities and
     * are merged including the necessary name corrections in the text.
     * <p>
     * NOTE: Does NOT add assertions to subtypes of the given class.
     *
     * @param ci    ClassInfo object, which is context to the constraint.
     * @param xpath Assertion embodied in an XpathFragment object.
     * @param text  Explanatory text concerning the assertion
     */
    public void addAssertion(ClassInfo ci, XpathFragment xpath, String text);

    /**
     * Add an assertion statement embodied in an XpathFragment object and output it
     * as a Schematron &lt;assert&gt; element, which is contained in a proper
     * &lt;rule&gt; context. &lt;let&gt; elements are searched for identities and
     * are merged including the necessary name corrections in the text.
     *
     * @param ci                             ClassInfo object, which is context to
     *                                       the constraint.
     * @param addToSubtypesInSelectedSchemas - <code>true</code> if the assertion
     *                                       statement shall also be added to
     *                                       subtypes of ci, else <code>false</code>
     * @param xpath                          Assertion embodied in an XpathFragment
     *                                       object.
     * @param text                           Explanatory text concerning the
     *                                       assertion
     */
    public void addAssertion(ClassInfo ci, boolean addToSubtypesInSelectedSchemas, XpathFragment xpath, String text);

    /**
     * Add another OCL constraint and translate it into a Schematron &lt;assert&gt;,
     * which is subsequently appended to the Schematron document within the proper
     * &lt;rule&gt; context.
     *
     * @param ci ClassInfo object, which is context to the constraint.
     * @param c  OCL constraint. Must be invariant.
     */
    public void addAssertion(ClassInfo ci, OclConstraint c);

    /**
     * Add an assertion statement - that will result by translating the given OCL
     * constraint, which is defined for a property, to an XpathFragment object - and
     * output it as a Schematron &lt;assert&gt; element. Does not add an assertion
     * to abstract or suppressed classes.
     *
     * <p>
     * The rule context is a class, which is determined by parameter cib. This
     * supports cases in which the context class is a subtype of the class that owns
     * the property: b:subtype/a:property instead of a:owner/a:property.
     *
     * <p>
     * &lt;let&gt; elements are searched for identities and are merged including the
     * necessary name corrections in the text.
     *
     * @param c                              OCL constraint that shall be translated
     *                                       to the check of a Schematron assertion
     * @param cib                            ClassInfo object, which defines the
     *                                       rule context. Can be <code>null</code>,
     *                                       then the class that owns the property
     *                                       for which the OCL constraint is defined
     *                                       provides the rule context.
     * @param addToSubtypesInSelectedSchemas true to add the assertion to direct and
     *                                       indirect subtypes of cib (or the class
     *                                       that owns the property for which the
     *                                       OCL constraint is defined) that are in
     *                                       the schemas selected for processing
     */
    public void addAssertionForPropertyConstraint(OclConstraint c, ClassInfo cib,
	    boolean addToSubtypesInSelectedSchemas);

    /**
     * Add an assertion statement embodied in an XpathFragment object and output it
     * as a Schematron &lt;assert&gt; element. Does not add an assertion to abstract
     * or suppressed classes.
     *
     * <p>
     * The rule context is the property element, prefixed by the element that
     * represents ci. This supports cases in which the context class is a subtype of
     * the class that owns the property: b:subtype/a:property instead of
     * a:owner/a:property.
     *
     * <p>
     * &lt;let&gt; elements are searched for identities and are merged including the
     * necessary name corrections in the text.
     *
     * @param cib                            ClassInfo object, which is base of the
     *                                       rule context. Can be <code>null</code>,
     *                                       then the class that owns the property
     *                                       is the base of the rule context.
     * @param pi                             Property that completes the context
     * @param addToSubtypesInSelectedSchemas true to add the assertion to direct and
     *                                       indirect subtypes of cib (or the class
     *                                       that owns pi) that are in the schemas
     *                                       selected for processing
     * @param xpath                          Assertion embodied in an XpathFragment
     *                                       object.
     * @param text                           Explanatory text concerning the
     *                                       assertion
     */
    public void addAssertionForExplicitProperty(ClassInfo cib, PropertyInfo pi, boolean addToSubtypesInSelectedSchemas,
	    XpathFragment xpath, String text);

    /**
     * @return <code>true</code> if the Schematron schema has at least one rule
     *         (with assertion(s)), else <code>false</code>
     */
    public boolean hasRules();
    
    /**
     * @return the file name of this Schematron schema
     */
    public String getFileName();
}
