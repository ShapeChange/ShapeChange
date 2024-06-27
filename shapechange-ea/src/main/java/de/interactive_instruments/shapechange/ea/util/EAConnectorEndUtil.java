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
package de.interactive_instruments.shapechange.ea.util;

import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.RoleTag;

import de.interactive_instruments.shapechange.core.model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EAConnectorEndUtil extends AbstractEAUtil {

    /**
     * Adds the given tagged value to the given connector end, NOT checking for
     * duplicate tags.
     * <p>
     * <b>WARNING:</b> Enterprise Architect may initialize default tagged values for
     * a model element that adheres to a specific UML profile. In that case, adding
     * the same tagged values would lead to duplicates. If duplicates shall be
     * prevented, set the tagged value instead of adding it.
     * 
     * @param end the connector end to which the tagged value shall be added
     * @param tv  tagged value to add, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void addTaggedValue(ConnectorEnd end, EATaggedValue tv) throws EAException {

	Collection<RoleTag> cTV = end.GetTaggedValues();

	String name = tv.getName();
	List<String> values = tv.getValues();

	if (values != null) {

	    for (String v : values) {

		RoleTag eaTv = cTV.AddNew(name, "");
		cTV.Refresh();

		/*
		 * An EA memo-field is used to provide convenient support (via a dialog in EA)
		 * for entering a tagged value with very long text. Such fields always start
		 * with the string '<memo>' (six characters long).
		 * 
		 * If a tagged value with a memo-field has an actual textual value then the
		 * value starts with '<memo>$ea_notes=' (16 characters long). So if a tag with
		 * memo-field does not have an actual value, we will only find '<memo>', but not
		 * followed by '$ea_notes='.
		 * 
		 * Otherwise (no memo field) we can use the value as is.
		 */

		if (tv.createAsMemoField() || v.length() > 255) {

		    if (v.length() == 0) {
			eaTv.SetValue("<memo>");
		    } else {
			eaTv.SetValue("<memo>$ea_notes=" + v);
		    }
		} else {
		    eaTv.SetValue(v);
		}

		if (!eaTv.Update()) {
		    throw new EAException(createMessage(message(101), name, end.GetRole(), v, eaTv.GetLastError()));
		}
	    }
	}
    }

    /**
     * Adds the given collection of tagged values to the given connector end, NOT
     * checking for duplicate tags.
     * <p>
     * <b>WARNING:</b> Enterprise Architect may initialize default tagged values for
     * a model element that adheres to a specific UML profile. In that case, adding
     * the same tagged values would lead to duplicates. If duplicates shall be
     * prevented, set the tagged value instead of adding it.
     * 
     * @param end the connector end to which the tagged values shall be added
     * @param tvs collection of tagged values to add
     * @throws EAException tbd
     */
    public static void addTaggedValues(ConnectorEnd end, TaggedValues tvs) throws EAException {

	if (tvs == null || tvs.isEmpty()) {

	    // nothing to do

	} else {

	    Collection<RoleTag> cTV = end.GetTaggedValues();

	    for (Entry<String, List<String>> e : tvs.asMap().entrySet()) {

		String name = e.getKey();
		List<String> values = e.getValue();

		if (values != null) {

		    for (String v : values) {

			RoleTag eaTv = cTV.AddNew(name, "");
			cTV.Refresh();

			/*
			 * An EA memo-field is used to provide convenient support (via a dialog in EA)
			 * for entering a tagged value with very long text. Such fields always start
			 * with the string '<memo>' (six characters long).
			 * 
			 * If a tagged value with a memo-field has an actual textual value then the
			 * value starts with '<memo>$ea_notes=' (16 characters long). So if a tag with
			 * memo-field does not have an actual value, we will only find '<memo>', but not
			 * followed by '$ea_notes='.
			 * 
			 * Otherwise (no memo field) we can use the value as is.
			 */

			if (v.length() > 255) {
			    eaTv.SetValue("<memo>$ea_notes=" + v);
			} else {
			    eaTv.SetValue(v);
			}

			if (!eaTv.Update()) {
			    throw new EAException(
				    createMessage(message(101), name, end.GetRole(), v, eaTv.GetLastError()));
			}
		    }
		}
	    }
	}
    }

    public static void deleteTaggedValue(ConnectorEnd ce, String nameOfTVToDelete) {

	Collection<RoleTag> cTV = ce.GetTaggedValues();
	cTV.Refresh();

	for (short i = 0; i < cTV.GetCount(); i++) {
	    RoleTag tv = cTV.GetAt(i);
	    if (tv.GetTag().equalsIgnoreCase(nameOfTVToDelete)) {
		cTV.Delete(i);
	    }
	}

	cTV.Refresh();
    }

    public static void setEACardinality(ConnectorEnd conEnd, String cardinalityAsString) throws EAException {

	conEnd.SetCardinality(cardinalityAsString);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(102), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEARole(ConnectorEnd conEnd, String role) throws EAException {

	conEnd.SetRole(role);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(103), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAAlias(ConnectorEnd conEnd, String alias) throws EAException {

	conEnd.SetAlias(alias);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(106), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEARoleNote(ConnectorEnd conEnd, String note) throws EAException {

	conEnd.SetRoleNote(note);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(107), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAStereotype(ConnectorEnd conEnd, String stereotype) throws EAException {

	conEnd.SetStereotype(stereotype);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(112), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAStereotypeEx(ConnectorEnd conEnd, String stereotypeEx) throws EAException {

	conEnd.SetStereotypeEx(stereotypeEx);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(108), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAOrdering(ConnectorEnd conEnd, boolean isOrdered) throws EAException {

	conEnd.SetOrdering(isOrdered ? 1 : 0);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(109), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAOwnedByClassifier(ConnectorEnd conEnd, boolean isOwned) throws EAException {

	conEnd.SetOwnedByClassifier(isOwned);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(113), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAAllowDuplicates(ConnectorEnd conEnd, boolean allowDuplicates) throws EAException {

	conEnd.SetAllowDuplicates(allowDuplicates);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(110), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEADerived(ConnectorEnd conEnd, boolean isDerived) throws EAException {

	conEnd.SetDerived(isDerived);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(114), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAAggregation(ConnectorEnd conEnd, EAAggregation aggregation) throws EAException {

	conEnd.SetAggregation(aggregation.getEAValue());

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(111), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    public static void setEAContainment(ConnectorEnd conEnd, String containment) throws EAException {

	conEnd.SetContainment(containment);

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(105), conEnd.GetRole(), containment, conEnd.GetLastError()));
	}
    }

    /**
     * Indicates whether the connector end is navigable from the opposite classifier
     * - Navigable, Non-Navigable or Unspecified.
     * 
     * @param conEnd    tbd
     * @param navigable 'Navigable', 'Non-Navigable' or 'Unspecified'
     * @throws EAException tbd
     */
    public static void setEANavigable(ConnectorEnd conEnd, EANavigable navigable) throws EAException {

	conEnd.SetNavigable(navigable.getEAValue());

	if (!conEnd.Update()) {
	    throw new EAException(createMessage(message(104), conEnd.GetRole(), conEnd.GetLastError()));
	}
    }

    /**
     * Sets the given tagged value in the given connector end. If a tagged value
     * with the same name as the given one already exists, it will be deleted. Then
     * the tagged value will be added.
     * 
     * @param ce the connector end in which the tagged value shall be set
     * @param tv tagged value to set, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValue(ConnectorEnd ce, EATaggedValue tv) throws EAException {

	deleteTaggedValue(ce, tv.getName());
	addTaggedValue(ce, tv);
    }

    /**
     * Sets the given tagged value in the tagged values of the given connector end.
     * If tagged values with the same tag name already exist, they will be deleted.
     * Then the tagged value will be added.
     * 
     * @param end   the connector end in which the tagged value shall be set
     * @param name  name of the tagged value to set, must not be <code>null</code>
     * @param value value of the tagged value to set, can be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValue(ConnectorEnd end, String name, String value) throws EAException {

	EATaggedValue tv = new EATaggedValue(name, value);

	deleteTaggedValue(end, tv.getName());
	addTaggedValue(end, tv);
    }

    /**
     * Retrieves the first tagged value with given name of the given connector end.
     * Does not apply normalization of tags, i.e. comparison is performed using
     * string equality. With UML 2, there may be multiple values per tag. This
     * method does NOT issue a warning if more than one value exists for the tag.
     * I.e., use this method only for cases, where only one value per tag may be
     * provided.
     * 
     * @param end    connector end that contains the tagged values to search
     * @param tvName name of the tagged value to retrieve
     * @return The tagged value for the tag with given name or <code>null</code> if
     *         the tagged value was not found. If there are multiple values with the
     *         tag only the first is provided.
     */
    public static String taggedValue(ConnectorEnd end, String tvName) {

	org.sparx.Collection<org.sparx.RoleTag> tvs = end.GetTaggedValues();

	for (org.sparx.RoleTag tv : tvs) {

	    if (tvName.equals(tv.GetTag())) {

		String v = tv.GetValue();

		/*
		 * An EA memo-field is used to provide convenient support (via a dialog in EA)
		 * for entering a tagged value with very long text. Such fields always start
		 * with the string '<memo>' (six characters long).
		 * 
		 * If a tagged value with a memo-field has an actual textual value then the
		 * value starts with '<memo>$ea_notes=' (16 characters long). So if a tag with
		 * memo-field does not have an actual value, we will only find '<memo>', but not
		 * followed by '$ea_notes='.
		 * 
		 * If the tagged value does not use a memo-field, then it may still contain or
		 * start with '$ea_notes='. In that case, the part after '$ea_notes=' provides
		 * the documentation of the tag (e.g. from the MDG Technology - UnitTests showed
		 * that the documentation can be empty) and the part before provides the actual
		 * value.
		 * 
		 * Otherwise (does not start with '<memo>' and does not contain '$ea_notes=') we
		 * can use the value as is.
		 */

		if (v.startsWith("<memo>$ea_notes=")) {

		    v = v.substring(16);

		} else if (v.startsWith("<memo>")) {

		    // no actual value in the memo-field
		    v = "";

		} else if (v.contains("$ea_notes=")) {

		    // retrieve the value
		    v = v.substring(0, v.indexOf("$ea_notes="));

		} else {
		    // fine - use the value as is
		}

		return v;
	    }
	}

	return null;
    }

    /**
     * Sets the given tagged values in the given connector end. If tagged values
     * with the same name as the given ones already exist, they will be deleted.
     * Then the tagged values will be added.
     * 
     * @param ce  the connector end in which the tagged values shall be set
     * @param tvs tagged values to set, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValues(ConnectorEnd ce, TaggedValues tvs) throws EAException {

	for (String tvName : tvs.asMap().keySet()) {
	    deleteTaggedValue(ce, tvName);
	}
	addTaggedValues(ce, tvs);
    }

    /**
     * @param elmt tbd
     * @return sorted map of the tagged values (key: {name '#' fqName}; value:
     *         according EATaggedValue); can be empty but not <code>null</code>
     */
    public static SortedMap<String, EATaggedValue> getEATaggedValues(ConnectorEnd elmt) {

	/*
	 * key: {name '#' fqName}; value: according EATaggedValue
	 */
	SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

	Collection<RoleTag> tvs = elmt.GetTaggedValues();

	for (short i = 0; i < tvs.GetCount(); i++) {

	    RoleTag tv = tvs.GetAt(i);

	    String name = tv.GetTag();
	    String fqName = tv.GetFQName();
	    String value = tv.GetValue();

	    /*
	     * An EA memo-field is used to provide convenient support (via a dialog in EA)
	     * for entering a tagged value with very long text. Such fields always start
	     * with the string '<memo>' (six characters long).
	     * 
	     * If a tagged value with a memo-field has an actual textual value then the
	     * value starts with '<memo>$ea_notes=' (16 characters long). So if a tag with
	     * memo-field does not have an actual value, we will only find '<memo>', but not
	     * followed by '$ea_notes='.
	     * 
	     * If the tagged value does not use a memo-field, then it may still contain or
	     * start with '$ea_notes='. In that case, the part after '$ea_notes=' provides
	     * the documentation of the tag (e.g. from the MDG Technology - UnitTests showed
	     * that the documentation can be empty) and the part before provides the actual
	     * value.
	     * 
	     * Otherwise (does not start with '<memo>' and does not contain '$ea_notes=') we
	     * can use the value as is.
	     */

	    if (value.startsWith("<memo>$ea_notes=")) {

		value = value.substring(16);

	    } else if (value.startsWith("<memo>")) {

		// no actual value in the memo-field
		value = "";

	    } else if (value.contains("$ea_notes=")) {

		// retrieve the value
		value = value.substring(0, value.indexOf("$ea_notes="));

	    } else {
		// fine - use the value as is
	    }

	    String key = name + "#" + fqName;

	    if (result.containsKey(key)) {
		EATaggedValue eatv = result.get(key);
		eatv.addValue(value);
	    } else {
		result.put(key, new EATaggedValue(name, fqName, value));
	    }
	}

	return result;
    }

    public static String getRoleName(ConnectorEnd ce, String eaPropertyId) {
	String name = ce.GetRole();
	if (StringUtils.isBlank(name)) {
	    name = "role_" + eaPropertyId;
	}
	return name.trim();
    }

    public static boolean isNavigable(ConnectorEnd ce, Connector con) {

	boolean nav = ce.GetIsNavigable();

	/*
	 * If not explicitly set, also accept unspecified navigability, if present in
	 * both directions.
	 */
	if (!nav) {
	    nav = EAConnectorUtil.navigability(con) == 0;
	}

	// navigable only with a name, but not with a default name
	String ceName = getRoleName(ce, "" + con.GetConnectorID());
	if (ceName == null || StringUtils.startsWith(ceName, "role_")) {
	    nav = false;
	}

	return nav;
    }

    public static String message(int mnr) {

	switch (mnr) {

	case 101:
	    return "EA error encountered while updating EA tagged value '$1$' of connector end '$2$' with value '$3$'. Error message is: $4$";
	case 102:
	    return "EA error encountered while updating 'Cardinality' of EA connector end '$1$'. Error message is: $2$";
	case 103:
	    return "EA error encountered while updating 'Role' of EA connector end '$1$'. Error message is: $2$";
	case 104:
	    return "EA error encountered while updating 'Navigable' of EA connector end '$1$'. Error message is: $2$";
	case 105:
	    return "EA error encountered while updating 'Containment' of EA connector end '$1$' with value '$2$'. Error message is: $3$";
	case 106:
	    return "EA error encountered while updating 'Alias' of EA connector end '$1$'. Error message is: $2$";
	case 107:
	    return "EA error encountered while updating 'Notes' of EA connector end '$1$'. Error message is: $2$";
	case 108:
	    return "EA error encountered while updating 'StereotypeEx' of EA connector end '$1$'. Error message is: $2$";
	case 109:
	    return "EA error encountered while updating 'Ordering' of EA connector end '$1$'. Error message is: $2$";
	case 110:
	    return "EA error encountered while updating 'AllowDuplicates' of EA connector end '$1$'. Error message is: $2$";
	case 111:
	    return "EA error encountered while updating 'Aggregation' of EA connector end '$1$'. Error message is: $2$";
	case 112:
	    return "EA error encountered while updating 'Stereotype' of EA connector end '$1$'. Error message is: $2$";
	case 113:
	    return "EA error encountered while updating 'OwnedByClassifier' of EA connector end '$1$'. Error message is: $2$";
	case 114:
	    return "EA error encountered while updating 'Derived' of EA connector end '$1$'. Error message is: $2$";

	default:
	    return "(" + EAConnectorUtil.class.getName() + ") Unknown message with number: " + mnr;
	}
    }

}
