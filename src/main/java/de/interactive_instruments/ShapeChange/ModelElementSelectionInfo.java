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
package de.interactive_instruments.ShapeChange;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class ModelElementSelectionInfo implements MessageSource {

    public enum ModelElementType {
	ASSOCIATION, CLASS, PACKAGE, PROPERTY, ATTRIBUTE, ASSOCIATIONROLE
    };

    private ModelElementType modelElementType = null;
    private Pattern modelElementStereotypePattern = null;
    private Pattern modelElementNamePattern = null;
    private Pattern modelElementOwnerNamePattern = null;
    private Pattern propertyValueTypeStereotypePattern = null;
    private Pattern applicationSchemaNamePattern = null;

    /**
     * Default model element selection, with <code>null</code> for all filter
     * criteria. Means that this ModelElementSelectionInfo will select any model
     * element.
     */
    public ModelElementSelectionInfo() {
	super();
    }

    public ModelElementSelectionInfo(ModelElementType modelElementType, Pattern modelElementStereotypePattern,
	    Pattern modelElementNamePattern, Pattern modelElementOwnerNamePattern,
	    Pattern propertyValueTypeStereotypePattern, Pattern applicationSchemaNamePattern) {

	super();
	this.modelElementType = modelElementType;
	this.modelElementStereotypePattern = modelElementStereotypePattern;
	this.modelElementNamePattern = modelElementNamePattern;
	this.modelElementOwnerNamePattern = modelElementOwnerNamePattern;
	this.propertyValueTypeStereotypePattern = propertyValueTypeStereotypePattern;
	this.applicationSchemaNamePattern = applicationSchemaNamePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementStereotype attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementStereotypePattern() {
	return modelElementStereotypePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         propertyValueTypeStereotype attribute, or <code>null</code> if this
     *         filter criterium was not set in the configuration.
     */
    public Pattern getPropertyValueTypeStereotypePattern() {
	return propertyValueTypeStereotypePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementNamePattern() {
	return modelElementNamePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementOwnerName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementOwnerNamePattern() {
	return modelElementOwnerNamePattern;
    }

    /**
     * @return the value defined by the modelElementType attribute, or
     *         <code>null</code> if this filter criterium was not set in the
     *         configuration.
     */
    public ModelElementType getModelElementType() {
	return modelElementType;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         applicationSchemaName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getApplicationSchemaNamePattern() {
	return applicationSchemaNamePattern;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementName attribute, else <code>false</code>
     */
    public boolean hasModelElementNamePattern() {
	return modelElementNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementOwnerName attribute, else <code>false</code>
     */
    public boolean hasModelElementOwnerNamePattern() {
	return modelElementOwnerNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementStereotype attribute, else <code>false</code>
     */
    public boolean hasModelElementStereotypePattern() {
	return modelElementStereotypePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         propertyValueTypeStereotype attribute, else <code>false</code>
     */
    public boolean hasPropertyValueTypeStereotypePattern() {
	return propertyValueTypeStereotypePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         applicationSchemaName attribute, else <code>false</code>
     */
    public boolean hasApplicationSchemaNamePattern() {
	return applicationSchemaNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementType attribute, else <code>false</code>
     */
    public boolean hasModelElementType() {
	return modelElementType != null;
    }

    /**
     * Determines if the given info type matches the filter criteria defined by this
     * object.
     * 
     * @param infoType
     * @return <code>true</code> if the info type matches the filter criteria,
     *         otherwise <code>false</code>.
     */
    public boolean matches(Info infoType) {

	Options options = infoType.options();
	ShapeChangeResult result = infoType.result();

	boolean modelElementStereotypeMatch = true;
	boolean propertyValueTypeStereotypeMatch = true;
	boolean modelElementNameMatch = true;
	boolean modelElementOwnerNameMatch = true;
	boolean applicationSchemaNameMatch = true;
	boolean modelElementTypeMatch = true;

	if (hasModelElementType()) {

	    modelElementTypeMatch = false;

	    if ((infoType instanceof AssociationInfo && modelElementType.equals(ModelElementType.ASSOCIATION))
		    || (infoType instanceof ClassInfo && modelElementType.equals(ModelElementType.CLASS))
		    || (infoType instanceof PackageInfo && modelElementType.equals(ModelElementType.PACKAGE))
		    || (infoType instanceof PropertyInfo && modelElementType.equals(ModelElementType.PROPERTY))
		    || (infoType instanceof PropertyInfo && modelElementType.equals(ModelElementType.ATTRIBUTE)
			    && ((PropertyInfo) infoType).isAttribute())
		    || (infoType instanceof PropertyInfo && modelElementType.equals(ModelElementType.ASSOCIATIONROLE)
			    && !((PropertyInfo) infoType).isAttribute())) {
		modelElementTypeMatch = true;
	    }
	}

	if (hasModelElementStereotypePattern()) {

	    modelElementStereotypeMatch = false;

	    Stereotypes stereotypes = infoType.stereotypes();

	    // TBD: what if a model element has no stereotype?
	    // stereotypes in info types have been normalized
	    if (stereotypes.isEmpty()) {

		String stereotype = null;

		if (infoType instanceof PropertyInfo)
		    stereotype = "";
		else if (infoType instanceof ClassInfo)
		    stereotype = "";
		else if (infoType instanceof PackageInfo)
		    stereotype = "";

		stereotypes = options.stereotypesFactory();
		stereotypes.add(stereotype);
	    }

	    for (String stereotype : stereotypes.asArray()) {

		Matcher matcher = modelElementStereotypePattern.matcher(stereotype);

		if (matcher.matches()) {
		    modelElementStereotypeMatch = true;
		    result.addDebug(this, 100, stereotype, modelElementStereotypePattern.pattern());
		    break;
		} else {
		    result.addDebug(this, 101, stereotype, modelElementStereotypePattern.pattern());
		}
	    }
	}

	if (hasPropertyValueTypeStereotypePattern() && infoType instanceof PropertyInfo) {

	    PropertyInfo pi = (PropertyInfo) infoType;

	    /*
	     * Try to get the value type from the model
	     */
	    Model model = pi.model();

	    ClassInfo valueType = null;
	    if (pi.typeInfo().id != null) {
		valueType = model.classById(pi.typeInfo().id);
	    }
	    if (valueType == null && pi.typeInfo().name != null) {
		valueType = model.classByName(pi.typeInfo().name);
	    }

	    if (valueType != null) {

		propertyValueTypeStereotypeMatch = false;

		Stereotypes stereotypes = valueType.stereotypes();

		// TBD: what if a model element has no stereotype?
		// stereotypes in info types have been normalized
		if (stereotypes.isEmpty()) {
		    stereotypes = options.stereotypesFactory();
		    stereotypes.add("");
		}

		for (String stereotype : stereotypes.asArray()) {

		    Matcher matcher = propertyValueTypeStereotypePattern.matcher(stereotype);

		    if (matcher.matches()) {
			propertyValueTypeStereotypeMatch = true;
			result.addDebug(this, 100, stereotype, propertyValueTypeStereotypePattern.pattern());
			break;
		    } else {
			result.addDebug(this, 101, stereotype, propertyValueTypeStereotypePattern.pattern());
		    }
		}
	    }
	}

	if (hasModelElementNamePattern()) {

	    modelElementNameMatch = false;

	    Matcher matcher = modelElementNamePattern.matcher(infoType.name());
	    if (matcher.matches()) {
		modelElementNameMatch = true;
		result.addDebug(this, 100, infoType.name(), modelElementNamePattern.pattern());
	    } else {
		result.addDebug(this, 101, infoType.name(), modelElementNamePattern.pattern());
	    }
	}

	if (hasModelElementOwnerNamePattern()) {

	    modelElementOwnerNameMatch = false;

	    String ownerName = null;

	    if (infoType instanceof PropertyInfo) {
		ownerName = ((PropertyInfo) infoType).inClass().name();
	    } else if (infoType instanceof ClassInfo) {
		ownerName = ((ClassInfo) infoType).pkg().name();
	    } else if (infoType instanceof PackageInfo) {
		PackageInfo pkg = (PackageInfo) infoType;
		if (pkg.owner() != null) {
		    ownerName = pkg.owner().name();
		}
	    }

	    if (ownerName != null) {

		Matcher matcher = modelElementOwnerNamePattern.matcher(ownerName);

		if (matcher.matches()) {
		    modelElementOwnerNameMatch = true;
		    result.addDebug(this, 100, ownerName, modelElementOwnerNamePattern.pattern());
		} else {
		    result.addDebug(this, 101, ownerName, modelElementOwnerNamePattern.pattern());
		}

	    } else {
		/*
		 * Info type has no specific owner (could be an association or root package).
		 * Assume that filter criteria matches.
		 */
		modelElementOwnerNameMatch = true;
	    }
	}

	if (hasApplicationSchemaNamePattern()) {

	    applicationSchemaNameMatch = false;

	    SortedSet<String> applicationSchemaNames = determineApplicationSchemaName(infoType);

	    for (String applicationSchemaName : applicationSchemaNames) {
		
		Matcher matcher = applicationSchemaNamePattern.matcher(applicationSchemaName);

		if (matcher.matches()) {
		    applicationSchemaNameMatch = true;
		    result.addDebug(this, 100, applicationSchemaName, applicationSchemaNamePattern.pattern());
		} else {
		    result.addDebug(this, 101, applicationSchemaName, applicationSchemaNamePattern.pattern());
		}
	    }
	}

	return modelElementStereotypeMatch && modelElementNameMatch && modelElementOwnerNameMatch
		&& propertyValueTypeStereotypeMatch && modelElementTypeMatch && applicationSchemaNameMatch;
    }

    public static ModelElementSelectionInfo parse(Element element) {

	Pattern modelElementNamePattern = null;

	if (element.hasAttribute("modelElementName")) {
	    String modelElementName = element.getAttribute("modelElementName");
	    modelElementNamePattern = Pattern.compile(modelElementName);
	}

	Pattern modelElementOwnerNamePattern = null;

	if (element.hasAttribute("modelElementOwnerName")) {
	    String modelElementOwnerName = element.getAttribute("modelElementOwnerName");
	    modelElementOwnerNamePattern = Pattern.compile(modelElementOwnerName);
	}

	Pattern modelElementStereotypePattern = null;

	if (element.hasAttribute("modelElementStereotype")) {

	    String modelElementStereotype = element.getAttribute("modelElementStereotype");
	    modelElementStereotypePattern = Pattern.compile(modelElementStereotype);
	}

	Pattern propertyValueTypeStereotypePattern = null;

	if (element.hasAttribute("propertyValueTypeStereotype")) {

	    String propertyValueTypeStereotype = element.getAttribute("propertyValueTypeStereotype");
	    propertyValueTypeStereotypePattern = Pattern.compile(propertyValueTypeStereotype);
	}

	Pattern applicationSchemaNamePattern = null;

	if (element.hasAttribute("applicationSchemaName")) {

	    String applicationSchemaName = element.getAttribute("applicationSchemaName");
	    applicationSchemaNamePattern = Pattern.compile(applicationSchemaName);
	}

	ModelElementType modelElementType = null;

	if (element.hasAttribute("modelElementType")) {

	    String mdeValue = element.getAttribute("modelElementType");

	    if (mdeValue.equalsIgnoreCase("Association")) {
		modelElementType = ModelElementType.ASSOCIATION;
	    } else if (mdeValue.equalsIgnoreCase("Class")) {
		modelElementType = ModelElementType.CLASS;
	    } else if (mdeValue.equalsIgnoreCase("Package")) {
		modelElementType = ModelElementType.PACKAGE;
	    } else if (mdeValue.equalsIgnoreCase("PROPERTY")) {
		modelElementType = ModelElementType.PROPERTY;
	    } else if (mdeValue.equalsIgnoreCase("Attribute")) {
		modelElementType = ModelElementType.ATTRIBUTE;
	    } else if (mdeValue.equalsIgnoreCase("AssociationRole")) {
		modelElementType = ModelElementType.ASSOCIATIONROLE;
	    }
	}

	return new ModelElementSelectionInfo(modelElementType, modelElementStereotypePattern, modelElementNamePattern,
		modelElementOwnerNamePattern, propertyValueTypeStereotypePattern, applicationSchemaNamePattern);
    }

    /**
     * @param infoType
     * @return can be empty but not <code>null</code>
     */
    private SortedSet<String> determineApplicationSchemaName(Info infoType) {

	ShapeChangeResult result = infoType.result();

	SortedSet<PackageInfo> pis = new TreeSet<>();

	if (infoType instanceof PackageInfo) {

	    pis.add((PackageInfo) infoType);

	} else if (infoType instanceof ClassInfo) {

	    ClassInfo ci = (ClassInfo) infoType;
	    pis.add(ci.pkg());

	} else if (infoType instanceof PropertyInfo) {

	    PropertyInfo propI = (PropertyInfo) infoType;
	    pis.add(propI.inClass().pkg());

	} else if (infoType instanceof AssociationInfo) {

	    AssociationInfo ai = (AssociationInfo) infoType;
	    if (ai.assocClass() != null) {
		pis.add(ai.assocClass().pkg());
	    }
	    pis.add(ai.end1().inClass().pkg());
	    pis.add(ai.end2().inClass().pkg());

	} else {
	    result.addWarning(this, 103, infoType.name());
	}

	SortedSet<String> asNames = new TreeSet<>();

	for (PackageInfo pi : pis) {
	    PackageInfo piAS = identifyApplicationSchema(pi);
	    if (piAS != null) {
		asNames.add(piAS.name());
	    }
	}

	if (asNames.isEmpty()) {
	    result.addWarning(this, 102, infoType.name());
	}

	return asNames;
    }

    private PackageInfo identifyApplicationSchema(PackageInfo pi) {

	if (pi.isAppSchema()) {

	    return pi;

	} else {

	    if (pi.owner() != null) {
		return identifyApplicationSchema(pi.owner());
	    } else {
		return null;
	    }
	}
    }

    @Override
    public String message(int mnr) {

	/*
	 * NOTE: A leading ?? in a message text suppresses multiple appearance of a
	 * message in the output.
	 */
	switch (mnr) {

	case 100:
	    return "??'$1$' matches regex '$2$'";
	case 101:
	    return "??'$1$' does not match regex '$2$'";
	case 102:
	    return "Could not find application schema for Info type '$1$'";
	case 103:
	    return "Class type of Info object '$1$' not recognized by logic to determine the name of its application schema";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
