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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * Helper class to determine information about special properties of a
 * given class.
 * 
 * NOTE: Property override is taken into account. Even though ldproxy does not
 * fully support inheritance, it does support shallow merge of schemas, where
 * properties of a subsequent schema override properties with same name of a
 * prior schema.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSpecialPropertiesInfo {

    private PropertyInfo identifierPiOfCi = null;
    private boolean multipleIdentifierPisEncountered = false;
    private boolean encodeAdditionalIdentifierProp = false;

    private PropertyInfo defaultGeometryPiOfCi = null;
    private boolean multipleDefaultGeometriesEncountered = false;
    private PropertyInfo defaultInstantPiOfCi = null;
    private boolean multipleDefaultInstantsEncountered = false;
    private PropertyInfo defaultIntervalStartPiOfCi = null;
    private boolean multipleDefaultIntervalStartsEncountered = false;
    private PropertyInfo defaultIntervalEndPiOfCi = null;
    private boolean multipleDefaultIntervalEndsEncountered = false;

    private MessageSource msgSource;
    private ShapeChangeResult result;
    private Ldproxy2Target target;

    private ClassInfo ci;

    public LdpSpecialPropertiesInfo(ClassInfo ci, Ldproxy2Target target) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;
	this.ci = ci;

	determineIdentifierInfos();
	determineDefaultGeometryInfos();
	determineDefaultInstantInfos();
	determineDefaultIntervalStartInfos();
	determineDefaultIntervalEndInfos();
    }

    private void determineDefaultIntervalEndInfos() {

	PropertyInfo defaultIntervalEndPi = null;
	List<PropertyInfo> allDefaultIntervalEndProperties = defaultIntervalEndProperties(ci, new ArrayList<>());
	multipleDefaultIntervalEndsEncountered = allDefaultIntervalEndProperties.size() > 1;
	if (allDefaultIntervalEndProperties.size() == 1) {
	    defaultIntervalEndPi = allDefaultIntervalEndProperties.get(0);
	}
	if (defaultIntervalEndPi != null && defaultIntervalEndPi.inClass() == ci
		&& defaultIntervalEndPi != this.defaultInstantPiOfCi) {

	    if (isDefaultInstant(defaultIntervalEndPi) || isDefaultIntervalStart(defaultIntervalEndPi)) {

		MessageContext mc = result.addError(msgSource, 133, ci.name(), defaultIntervalEndPi.name());
		if (mc != null) {
		    mc.addDetail(msgSource, 1, defaultIntervalEndPi.fullNameInSchema());
		}

	    } else {

		if (!multipleDefaultIntervalEndsEncountered) {
		    this.defaultIntervalEndPiOfCi = defaultIntervalEndPi;
		} else {
		    MessageContext mc = result.addError(msgSource, 111, ci.name());
		    if (mc != null) {
			mc.addDetail(msgSource, 0, ci.fullNameInSchema());
		    }
		}
	    }
	}
    }

    private void determineDefaultIntervalStartInfos() {

	PropertyInfo defaultIntervalStartPi = null;
	List<PropertyInfo> allDefaultIntervalStartProperties = defaultIntervalStartProperties(ci, new ArrayList<>());
	multipleDefaultIntervalStartsEncountered = allDefaultIntervalStartProperties.size() > 1;
	if (allDefaultIntervalStartProperties.size() == 1) {
	    defaultIntervalStartPi = allDefaultIntervalStartProperties.get(0);
	}
	if (defaultIntervalStartPi != null && defaultIntervalStartPi.inClass() == ci
		&& defaultIntervalStartPi != this.defaultInstantPiOfCi) {

	    if (isDefaultInstant(defaultIntervalStartPi) || isDefaultIntervalEnd(defaultIntervalStartPi)) {

		MessageContext mc = result.addError(msgSource, 109, ci.name(), defaultIntervalStartPi.name());
		if (mc != null) {
		    mc.addDetail(msgSource, 1, defaultIntervalStartPi.fullNameInSchema());
		}

	    } else {

		if (!multipleDefaultIntervalStartsEncountered) {
		    this.defaultIntervalStartPiOfCi = defaultIntervalStartPi;
		} else {
		    MessageContext mc = result.addError(msgSource, 110, ci.name());
		    if (mc != null) {
			mc.addDetail(msgSource, 0, ci.fullNameInSchema());
		    }
		}
	    }
	}
    }

    private void determineDefaultInstantInfos() {

	PropertyInfo defaultInstantPi = null;
	List<PropertyInfo> allDefaultInstantProperties = defaultInstantProperties(ci, new ArrayList<>());
	multipleDefaultInstantsEncountered = allDefaultInstantProperties.size() > 1;
	if (allDefaultInstantProperties.size() == 1) {
	    defaultInstantPi = allDefaultInstantProperties.get(0);
	}
	if (defaultInstantPi != null && defaultInstantPi.inClass() == ci) {

	    if (isDefaultIntervalStart(defaultInstantPi) || isDefaultIntervalEnd(defaultInstantPi)) {

		MessageContext mc = result.addError(msgSource, 106, ci.name(), defaultInstantPi.name());
		if (mc != null) {
		    mc.addDetail(msgSource, 1, defaultInstantPi.fullNameInSchema());
		}

	    } else {

		if (!multipleDefaultInstantsEncountered) {
		    this.defaultInstantPiOfCi = defaultInstantPi;
		} else {
		    MessageContext mc = result.addError(msgSource, 108, ci.name());
		    if (mc != null) {
			mc.addDetail(msgSource, 0, ci.fullNameInSchema());
		    }
		}
	    }
	}
    }

    private void determineDefaultGeometryInfos() {

	List<PropertyInfo> allDefaultGeometryProperties = defaultGeometryProperties(ci, new ArrayList<>());
	multipleDefaultGeometriesEncountered = allDefaultGeometryProperties.size() > 1;

	List<PropertyInfo> defaultGeometryPropsFromCurrentCi = allDefaultGeometryProperties.stream()
		.filter(pi -> pi.inClass() == ci).collect(Collectors.toList());

	if (multipleDefaultGeometriesEncountered) {

	    if (!defaultGeometryPropsFromCurrentCi.isEmpty()) {
		/*
		 * CurrentCi has at least one default geometry property, and in total there are
		 * multiple such properties. Log an error.
		 */
		MessageContext mc = result.addError(msgSource, 105, ci.name());
		if (mc != null) {
		    mc.addDetail(msgSource, 0, ci.fullNameInSchema());
		}
	    }

	} else {

	    if (defaultGeometryPropsFromCurrentCi.size() == 1) {
		this.defaultGeometryPiOfCi = defaultGeometryPropsFromCurrentCi.get(0);
	    }
	}
    }

    private void determineIdentifierInfos() {

	List<PropertyInfo> allIdentifierProperties = identifierProperties(ci, new ArrayList<>());

	List<PropertyInfo> identifierPropsFromCurrentCi = allIdentifierProperties.stream()
		.filter(pi -> pi.inClass() == ci).collect(Collectors.toList());

	if (Ldproxy2Target.enableFragments) {

	    List<PropertyInfo> identifierPropsFromSupertypes = allIdentifierProperties.stream()
		    .filter(pi -> pi.inClass() != ci).collect(Collectors.toList());

	    multipleIdentifierPisEncountered = (identifierPropsFromCurrentCi.size()
		    + identifierPropsFromSupertypes.size()) > 1;

	    if (ci.supertypesInCompleteHierarchy().stream().anyMatch(st -> LdpInfo.isEncoded(st)
		    && st.category() != Options.MIXIN && st.category() != Options.DATATYPE)) {
		/*
		 * currentCi has at least one non-mixin and non-datatype supertype that is
		 * encoded -> it will definitely provide an identifier property for the type
		 * definition of currentCi
		 */
	    } else if (!multipleIdentifierPisEncountered) {

		if (!identifierPropsFromSupertypes.isEmpty()) {
		    // must be an identifier of a mixin supertype
		} else if (!identifierPropsFromCurrentCi.isEmpty()) {
		    this.identifierPiOfCi = identifierPropsFromCurrentCi.get(0);
		} else {
		    /*
		     * No identifier property at all, and no non-mixin / non-datatype supertype.
		     * Encode an additional identifier property if ci itself is not a mixin and not
		     * a datatype.
		     */
		    encodeAdditionalIdentifierProp = ci.category() != Options.MIXIN
			    && ci.category() != Options.DATATYPE;
		}
	    }

	} else {

	    // ignore supertypes

	    multipleIdentifierPisEncountered = identifierPropsFromCurrentCi.size() > 1;

	    if (!multipleIdentifierPisEncountered) {

		if (!identifierPropsFromCurrentCi.isEmpty()) {
		    this.identifierPiOfCi = identifierPropsFromCurrentCi.get(0);
		} else {
		    // no identifier property at all
		    encodeAdditionalIdentifierProp = true;
		}
	    }
	}

	// log error or warning for certain cases
	if (!identifierPropsFromCurrentCi.isEmpty() && multipleIdentifierPisEncountered) {

	    /*
	     * currentCi has at least one identifier property, but in total there are
	     * multiple such properties
	     */
	    MessageContext mc = result.addError(msgSource, 107, ci.name());
	    if (mc != null) {
		mc.addDetail(msgSource, 0, ci.fullNameInSchema());
	    }

	} else if (this.identifierPiOfCi != null) {

	    if (this.identifierPiOfCi.cardinality().maxOccurs > 1) {
		// the identifier property of a type definition can only have a single value
		MessageContext mc = result.addWarning(msgSource, 104, this.identifierPiOfCi.inClass().name(),
			this.identifierPiOfCi.name());
		if (mc != null) {
		    mc.addDetail(msgSource, 1, this.identifierPiOfCi.fullNameInSchema());
		}
	    }
	}
    }

    private boolean isDefaultInstant(PropertyInfo pi) {
	return LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultInstant"));
    }

    private boolean isDefaultGeometry(PropertyInfo pi) {
	return LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultGeometry"));
    }

    private boolean isDefaultIntervalStart(PropertyInfo pi) {
	return LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalStart"));
    }

    private boolean isDefaultIntervalEnd(PropertyInfo pi) {
	return LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalEnd"));
    }

    private boolean isIdentifierProperty(PropertyInfo pi) {
	return LdpInfo.isEncoded(pi) && !target.isIgnored(pi) && pi.stereotype("identifier")
		&& pi.inClass().matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE);
    }

    private List<PropertyInfo> defaultInstantProperties(ClassInfo ci,
	    List<PropertyInfo> defaultInstantPropsFromSubtypes) {

	List<PropertyInfo> res = new ArrayList<>();

	List<PropertyInfo> defaultInstantPropsFromCi = new ArrayList<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (isDefaultInstant(pi)
		    && !(defaultInstantPropsFromSubtypes.stream().anyMatch(piSub -> piSub.name().equals(pi.name())))) {
		defaultInstantPropsFromCi.add(pi);
	    }
	}

	res.addAll(defaultInstantPropsFromCi);

	if (Ldproxy2Target.enableFragments) {
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if (LdpInfo.isEncoded(supertype)) {
		    List<PropertyInfo> newDefaultInstantPropsFromSubtypes = new ArrayList<>();
		    newDefaultInstantPropsFromSubtypes.addAll(defaultInstantPropsFromSubtypes);
		    newDefaultInstantPropsFromSubtypes.addAll(defaultInstantPropsFromCi);
		    List<PropertyInfo> defaultInstantPropsFromSupertype = defaultInstantProperties(supertype,
			    newDefaultInstantPropsFromSubtypes);
		    res.addAll(defaultInstantPropsFromSupertype);
		}
	    }
	}

	return res;

//	return ci.properties().values().stream().filter(pi -> LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
//		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultInstant"))).collect(Collectors.toList());
    }

    private List<PropertyInfo> defaultIntervalStartProperties(ClassInfo ci,
	    List<PropertyInfo> defaultIntervalStartPropsFromSubtypes) {

	List<PropertyInfo> res = new ArrayList<>();

	List<PropertyInfo> defaultIntervalStartPropsFromCi = new ArrayList<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (isDefaultIntervalStart(pi) && !(defaultIntervalStartPropsFromSubtypes.stream()
		    .anyMatch(piSub -> piSub.name().equals(pi.name())))) {
		defaultIntervalStartPropsFromCi.add(pi);
	    }
	}

	res.addAll(defaultIntervalStartPropsFromCi);

	if (Ldproxy2Target.enableFragments) {
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if (LdpInfo.isEncoded(supertype)) {
		    List<PropertyInfo> newDefaultIntervalStartPropsFromSubtypes = new ArrayList<>();
		    newDefaultIntervalStartPropsFromSubtypes.addAll(defaultIntervalStartPropsFromSubtypes);
		    newDefaultIntervalStartPropsFromSubtypes.addAll(defaultIntervalStartPropsFromCi);
		    List<PropertyInfo> defaultIntervalStartPropsFromSupertype = defaultIntervalStartProperties(
			    supertype, newDefaultIntervalStartPropsFromSubtypes);
		    res.addAll(defaultIntervalStartPropsFromSupertype);
		}
	    }
	}

	return res;

//	return ci.properties().values().stream()
//		.filter(pi -> LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
//			&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalStart")))
//		.collect(Collectors.toList());
    }

    private List<PropertyInfo> defaultIntervalEndProperties(ClassInfo ci,
	    List<PropertyInfo> defaultIntervalEndPropsFromSubtypes) {

	List<PropertyInfo> res = new ArrayList<>();

	List<PropertyInfo> defaultIntervalEndPropsFromCi = new ArrayList<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (isDefaultIntervalEnd(pi) && !(defaultIntervalEndPropsFromSubtypes.stream()
		    .anyMatch(piSub -> piSub.name().equals(pi.name())))) {
		defaultIntervalEndPropsFromCi.add(pi);
	    }
	}

	res.addAll(defaultIntervalEndPropsFromCi);

	if (Ldproxy2Target.enableFragments) {
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if (LdpInfo.isEncoded(supertype)) {
		    List<PropertyInfo> newDefaultIntervalEndPropsFromSubtypes = new ArrayList<>();
		    newDefaultIntervalEndPropsFromSubtypes.addAll(defaultIntervalEndPropsFromSubtypes);
		    newDefaultIntervalEndPropsFromSubtypes.addAll(defaultIntervalEndPropsFromCi);
		    List<PropertyInfo> defaultIntervalEndPropsFromSupertype = defaultIntervalEndProperties(supertype,
			    newDefaultIntervalEndPropsFromSubtypes);
		    res.addAll(defaultIntervalEndPropsFromSupertype);
		}
	    }
	}

	return res;

//	return ci.properties().values().stream()
//		.filter(pi -> LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
//			&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalEnd")))
//		.collect(Collectors.toList());
    }

    private List<PropertyInfo> defaultGeometryProperties(ClassInfo ci,
	    List<PropertyInfo> defaultGeometryPropsFromSubtypes) {

	List<PropertyInfo> res = new ArrayList<>();

	List<PropertyInfo> defaultGeometryPropsFromCi = new ArrayList<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (isDefaultGeometry(pi)
		    && !(defaultGeometryPropsFromSubtypes.stream().anyMatch(piSub -> piSub.name().equals(pi.name())))) {
		defaultGeometryPropsFromCi.add(pi);
	    }
	}

	res.addAll(defaultGeometryPropsFromCi);

	if (Ldproxy2Target.enableFragments) {
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if (LdpInfo.isEncoded(supertype)) {
		    List<PropertyInfo> newDefaultGeometryPropsFromSubtypes = new ArrayList<>();
		    newDefaultGeometryPropsFromSubtypes.addAll(defaultGeometryPropsFromSubtypes);
		    newDefaultGeometryPropsFromSubtypes.addAll(defaultGeometryPropsFromCi);
		    List<PropertyInfo> defaultGeometryPropsFromSupertype = defaultGeometryProperties(supertype,
			    newDefaultGeometryPropsFromSubtypes);
		    res.addAll(defaultGeometryPropsFromSupertype);
		}
	    }
	}

	return res;

//	return ci.properties().values().stream().filter(pi -> LdpInfo.isEncoded(pi) && !target.isIgnored(pi)
//		&& LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultGeometry"))).collect(Collectors.toList());
    }

    private List<PropertyInfo> identifierProperties(ClassInfo ci, List<PropertyInfo> identifierPropsFromSubtypes) {

	List<PropertyInfo> res = new ArrayList<>();

	List<PropertyInfo> identifierPropsFromCi = new ArrayList<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (isIdentifierProperty(pi)
		    && !(identifierPropsFromSubtypes.stream().anyMatch(piSub -> piSub.name().equals(pi.name())))) {
		identifierPropsFromCi.add(pi);
	    }
	}

	res.addAll(identifierPropsFromCi);

	if (Ldproxy2Target.enableFragments) {
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if (LdpInfo.isEncoded(supertype)) {
		    List<PropertyInfo> newIdentifierPropsFromSubtypes = new ArrayList<>();
		    newIdentifierPropsFromSubtypes.addAll(identifierPropsFromSubtypes);
		    newIdentifierPropsFromSubtypes.addAll(identifierPropsFromCi);
		    List<PropertyInfo> identifierPropsFromSupertype = identifierProperties(supertype,
			    newIdentifierPropsFromSubtypes);
		    res.addAll(identifierPropsFromSupertype);
		}
	    }
	}

	return res;

//	return ci.properties().values().stream()
//		.filter(pi -> LdpInfo.isEncoded(pi) && !target.isIgnored(pi) && pi.stereotype("identifier")
//			&& pi.inClass().matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE))
//		.collect(Collectors.toList());
    }

    public boolean isEncodeAdditionalIdentifierProp() {
	return this.encodeAdditionalIdentifierProp;
    }

    /**
     * @return the identifierPiOfCi
     */
    public PropertyInfo getIdentifierPiOfCi() {
	return identifierPiOfCi;
    }

    /**
     * @return the multipleIdentifierPisEncountered
     */
    public boolean isMultipleIdentifierPisEncountered() {
	return multipleIdentifierPisEncountered;
    }

    /**
     * @return the defaultGeometryPiOfCi
     */
    public PropertyInfo getDefaultGeometryPiOfCi() {
	return defaultGeometryPiOfCi;
    }

    /**
     * @return the multipleDefaultGeometriesEncountered
     */
    public boolean isMultipleDefaultGeometriesEncountered() {
	return multipleDefaultGeometriesEncountered;
    }

    /**
     * @return the defaultInstantPiOfCi
     */
    public PropertyInfo getDefaultInstantPiOfCi() {
	return defaultInstantPiOfCi;
    }

    /**
     * @return the multipleDefaultInstantsEncountered
     */
    public boolean isMultipleDefaultInstantsEncountered() {
	return multipleDefaultInstantsEncountered;
    }

    /**
     * @return the defaultIntervalStartPiOfCi
     */
    public PropertyInfo getDefaultIntervalStartPiOfCi() {
	return defaultIntervalStartPiOfCi;
    }

    /**
     * @return the multipleDefaultIntervalStartsEncountered
     */
    public boolean isMultipleDefaultIntervalStartsEncountered() {
	return multipleDefaultIntervalStartsEncountered;
    }

    /**
     * @return the defaultIntervalEndPiOfCi
     */
    public PropertyInfo getDefaultIntervalEndPiOfCi() {
	return defaultIntervalEndPiOfCi;
    }

    /**
     * @return the multipleDefaultIntervalEndsEncountered
     */
    public boolean isMultipleDefaultIntervalEndsEncountered() {
	return multipleDefaultIntervalEndsEncountered;
    }

}
