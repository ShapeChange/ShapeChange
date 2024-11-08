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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.model.Stereotypes;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ModelElementSelectionInfo implements MessageSource {

    public enum ModelElementType {
	ASSOCIATION, CLASS, PACKAGE, PROPERTY, ATTRIBUTE, ASSOCIATIONROLE
    };

    private Boolean isValid = null;

    private ModelElementType modelElementType = null;
    private String modelElementStereotype = null;
    private Pattern modelElementStereotypePattern = null;
    private String modelElementName = null;
    private Pattern modelElementNamePattern = null;
    private String modelElementOwnerName = null;
    private Pattern modelElementOwnerNamePattern = null;
    private String modelElementOwnerStereotype = null;
    private Pattern modelElementOwnerStereotypePattern = null;
    private String propertyValueTypeStereotype = null;
    private Pattern propertyValueTypeStereotypePattern = null;
    private String propertyValueTypeName = null;
    private Pattern propertyValueTypeNamePattern = null;
    private String applicationSchemaName = null;
    private Pattern applicationSchemaNamePattern = null;

    /**
     * Default model element selection, with <code>null</code> for all filter
     * criteria. Means that this ModelElementSelectionInfo will select any model
     * element.
     */
    public ModelElementSelectionInfo() {
	super();
    }

    public ModelElementSelectionInfo(ModelElementType modelElementType, String modelElementStereotype,
	    String modelElementName, String modelElementOwnerName, String modelElementOwnerStereotype,
	    String propertyValueTypeStereotype, String propertyValueTypeName, String applicationSchemaName) {

	super();
	this.modelElementType = modelElementType;
	this.modelElementStereotype = modelElementStereotype;
	this.modelElementName = modelElementName;
	this.modelElementOwnerName = modelElementOwnerName;
	this.modelElementOwnerStereotype = modelElementOwnerStereotype;
	this.propertyValueTypeStereotype = propertyValueTypeStereotype;
	this.propertyValueTypeName = propertyValueTypeName;
	this.applicationSchemaName = applicationSchemaName;
    }

    public void validate() throws ModelElementSelectionParseException {

	if (isValid != null && isValid) {
	    return;
	}

	List<String> compileErrors = new ArrayList<>();

	if (this.modelElementName != null) {
	    try {
		this.modelElementNamePattern = Pattern.compile(this.modelElementName);
	    } catch (PatternSyntaxException e) {
		compileErrors.add("@modelElementName - invalid value (" + modelElementName + "): " + e.getMessage());
	    }
	}

	if (this.modelElementOwnerName != null) {
	    try {
		modelElementOwnerNamePattern = Pattern.compile(modelElementOwnerName);
	    } catch (PatternSyntaxException e) {
		compileErrors.add(
			"@modelElementOwnerName - invalid value (" + modelElementOwnerName + "): " + e.getMessage());
	    }
	}

	if (this.modelElementOwnerStereotype != null) {
	    try {
		modelElementOwnerStereotypePattern = Pattern.compile(modelElementOwnerStereotype);
	    } catch (PatternSyntaxException e) {
		compileErrors.add("@modelElementOwnerStereotype - invalid value (" + modelElementOwnerStereotype + "): "
			+ e.getMessage());
	    }
	}

	if (this.modelElementStereotype != null) {
	    try {
		modelElementStereotypePattern = Pattern.compile(modelElementStereotype);
	    } catch (PatternSyntaxException e) {
		compileErrors.add(
			"@modelElementStereotype - invalid value (" + modelElementStereotype + "): " + e.getMessage());
	    }
	}

	if (this.propertyValueTypeStereotype != null) {
	    try {
		propertyValueTypeStereotypePattern = Pattern.compile(propertyValueTypeStereotype);
	    } catch (PatternSyntaxException e) {
		compileErrors.add("@propertyValueTypeStereotype - invalid value (" + propertyValueTypeStereotype + "): "
			+ e.getMessage());
	    }
	}

	if (this.propertyValueTypeName != null) {
	    try {
		propertyValueTypeNamePattern = Pattern.compile(propertyValueTypeName);
	    } catch (PatternSyntaxException e) {
		compileErrors.add(
			"@propertyValueTypeName - invalid value (" + propertyValueTypeName + "): " + e.getMessage());
	    }
	}

	if (this.applicationSchemaName != null) {
	    try {
		applicationSchemaNamePattern = Pattern.compile(applicationSchemaName);
	    } catch (PatternSyntaxException e) {
		compileErrors.add(
			"@applicationSchemaName - invalid value (" + applicationSchemaName + "): " + e.getMessage());
	    }
	}

	if (!compileErrors.isEmpty()) {
	    isValid = false;
	    throw new ModelElementSelectionParseException(
		    "Compiling regular expression(s) of model element selection attribute(s) did not succeed. Issues: "
			    + StringUtils.join(compileErrors, ", "));
	} else {
	    isValid = true;
	}
    }

    public Pattern getModelElementOwnerStereotypePattern() {
	validate();
	return modelElementOwnerStereotypePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementStereotype attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementStereotypePattern() {
	validate();
	return modelElementStereotypePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         propertyValueTypeStereotype attribute, or <code>null</code> if this
     *         filter criterium was not set in the configuration.
     */
    public Pattern getPropertyValueTypeStereotypePattern() {
	validate();
	return propertyValueTypeStereotypePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementNamePattern() {
	validate();
	return modelElementNamePattern;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         modelElementOwnerName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getModelElementOwnerNamePattern() {
	validate();
	return modelElementOwnerNamePattern;
    }

    /**
     * @return the value defined by the modelElementType attribute, or
     *         <code>null</code> if this filter criterium was not set in the
     *         configuration.
     */
    public ModelElementType getModelElementType() {
	validate();
	return modelElementType;
    }

    /**
     * @return the pattern representing the regular expression of the
     *         applicationSchemaName attribute, or <code>null</code> if this filter
     *         criterium was not set in the configuration.
     */
    public Pattern getApplicationSchemaNamePattern() {
	validate();
	return applicationSchemaNamePattern;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementName attribute, else <code>false</code>
     */
    public boolean hasModelElementNamePattern() {
	validate();
	return modelElementNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementOwnerName attribute, else <code>false</code>
     */
    public boolean hasModelElementOwnerNamePattern() {
	validate();
	return modelElementOwnerNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementStereotype attribute, else <code>false</code>
     */
    public boolean hasModelElementStereotypePattern() {
	validate();
	return modelElementStereotypePattern != null;
    }

    public boolean hasModelElementOwnerStereotypePattern() {
	validate();
	return modelElementOwnerStereotypePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         propertyValueTypeStereotype attribute, else <code>false</code>
     */
    public boolean hasPropertyValueTypeStereotypePattern() {
	validate();
	return propertyValueTypeStereotypePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         propertyValueTypeName attribute, else <code>false</code>
     */
    public boolean hasPropertyValueTypeNamePattern() {
	validate();
	return propertyValueTypeNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         applicationSchemaName attribute, else <code>false</code>
     */
    public boolean hasApplicationSchemaNamePattern() {
	validate();
	return applicationSchemaNamePattern != null;
    }

    /**
     * @return <code>true</code> if this configuration entry has a value for the
     *         modelElementType attribute, else <code>false</code>
     */
    public boolean hasModelElementType() {
	validate();
	return modelElementType != null;
    }

    /**
     * Determines if the given info type matches the filter criteria defined by this
     * object.
     * 
     * @param infoType tbd
     * @return <code>true</code> if the info type matches the filter criteria,
     *         otherwise <code>false</code>.
     */
    public boolean matches(Info infoType) {

	validate();

	Options options = infoType.options();
	ShapeChangeResult result = infoType.result();

	boolean modelElementStereotypeMatch = true;
	boolean propertyValueTypeStereotypeMatch = true;
	boolean propertyValueTypeNameMatch = true;
	boolean modelElementNameMatch = true;
	boolean modelElementOwnerNameMatch = true;
	boolean modelElementOwnerStereotypeMatch = true;
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

	if (hasModelElementOwnerStereotypePattern()) {

	    modelElementOwnerStereotypeMatch = false;

	    Info owner = null;

	    if (infoType instanceof PropertyInfo) {
		owner = ((PropertyInfo) infoType).inClass();
	    } else if (infoType instanceof ClassInfo) {
		owner = ((ClassInfo) infoType).pkg();
	    } else if (infoType instanceof PackageInfo) {
		PackageInfo pkg = (PackageInfo) infoType;
		if (pkg.owner() != null) {
		    owner = pkg.owner();
		}
	    }

	    if (owner != null) {

		Stereotypes stereotypes = owner.stereotypes();

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

		    Matcher matcher = modelElementOwnerStereotypePattern.matcher(stereotype);

		    if (matcher.matches()) {
			modelElementOwnerStereotypeMatch = true;
			result.addDebug(this, 100, stereotype, modelElementOwnerStereotypePattern.pattern());
			break;
		    } else {
			result.addDebug(this, 101, stereotype, modelElementOwnerStereotypePattern.pattern());
		    }
		}

	    } else {
		/*
		 * Info type has no specific owner (could be an association or root package).
		 * Assume that filter criteria matches.
		 */
		modelElementOwnerStereotypeMatch = true;
	    }
	}

	if (hasPropertyValueTypeStereotypePattern() && infoType instanceof PropertyInfo) {

	    PropertyInfo pi = (PropertyInfo) infoType;

	    /*
	     * only perform the check for properties that actually have a value type name
	     */
	    if (StringUtils.isNotBlank(pi.typeInfo().name)) {

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
		} else {
		    MessageContext mc = result.addWarning(this, 104,
			    StringUtils.defaultIfBlank(pi.typeInfo().name, "NOT_FOUND"), pi.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullName());
		    }
		}
	    }
	}

	if (hasPropertyValueTypeNamePattern() && infoType instanceof PropertyInfo) {

	    PropertyInfo pi = (PropertyInfo) infoType;

	    if (StringUtils.isNotBlank(pi.typeInfo().name)) {

		propertyValueTypeNameMatch = false;

		Matcher matcher = propertyValueTypeNamePattern.matcher(pi.typeInfo().name);

		if (matcher.matches()) {
		    propertyValueTypeNameMatch = true;
		    result.addDebug(this, 100, pi.typeInfo().name, propertyValueTypeNamePattern.pattern());
		} else {
		    result.addDebug(this, 101, pi.typeInfo().name, propertyValueTypeNamePattern.pattern());
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
		&& modelElementOwnerStereotypeMatch && propertyValueTypeStereotypeMatch && propertyValueTypeNameMatch
		&& modelElementTypeMatch && applicationSchemaNameMatch;
    }

    public static ModelElementSelectionInfo parse(Element element) {

	String modelElementName = null;

	if (element.hasAttribute("modelElementName")) {
	    modelElementName = element.getAttribute("modelElementName");
	}

	String modelElementOwnerName = null;

	if (element.hasAttribute("modelElementOwnerName")) {
	    modelElementOwnerName = element.getAttribute("modelElementOwnerName");
	}

	String modelElementOwnerStereotype = null;

	if (element.hasAttribute("modelElementOwnerStereotype")) {
	    modelElementOwnerStereotype = element.getAttribute("modelElementOwnerStereotype");
	}

	String modelElementStereotype = null;

	if (element.hasAttribute("modelElementStereotype")) {
	    modelElementStereotype = element.getAttribute("modelElementStereotype");
	}

	String propertyValueTypeStereotype = null;

	if (element.hasAttribute("propertyValueTypeStereotype")) {
	    propertyValueTypeStereotype = element.getAttribute("propertyValueTypeStereotype");
	}

	String propertyValueTypeName = null;

	if (element.hasAttribute("propertyValueTypeName")) {
	    propertyValueTypeName = element.getAttribute("propertyValueTypeName");
	}

	String applicationSchemaName = null;

	if (element.hasAttribute("applicationSchemaName")) {
	    applicationSchemaName = element.getAttribute("applicationSchemaName");
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

	return new ModelElementSelectionInfo(modelElementType, modelElementStereotype, modelElementName,
		modelElementOwnerName, modelElementOwnerStereotype, propertyValueTypeStereotype, propertyValueTypeName,
		applicationSchemaName);
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

	case 1:
	    return "Context: $1$";
	case 100:
	    return "??'$1$' matches regex '$2$'";
	case 101:
	    return "??'$1$' does not match regex '$2$'";
	case 102:
	    return "Could not find application schema for Info type '$1$'";
	case 103:
	    return "Class type of Info object '$1$' not recognized by logic to determine the name of its application schema";
	case 104:
	    return "??Could not find value type '$1$' of property '$2$' in the model. The propertyValueTypeStereotype filter criterium cannot be evaluated (and defaults to true, i.e., it matches).";
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
