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

import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorTag;
import org.sparx.Element;

import de.interactive_instruments.shapechange.core.model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EAConnectorUtil extends AbstractEAUtil {

    /**
     * Adds the given tagged value to the tagged values of the given connector, NOT
     * checking for duplicate tags.
     * <p>
     * <b>WARNING:</b> Enterprise Architect may initialize default tagged values for
     * a model element that adheres to a specific UML profile. In that case, adding
     * the same tagged values would lead to duplicates. If duplicates shall be
     * prevented, set the tagged value instead of adding it.
     * 
     * @param con the connector to which the tagged value shall be added
     * @param tv  tagged value to add
     * @throws EAException tbd
     */
    public static void addTaggedValue(Connector con, EATaggedValue tv) throws EAException {

	Collection<ConnectorTag> cTV = con.GetTaggedValues();

	String name = tv.getName();
	String type = "";

	List<String> values = tv.getValues();

	if (values != null) {

	    for (String v : values) {

		ConnectorTag eaTv = cTV.AddNew(name, type);
		cTV.Refresh();

		if (tv.createAsMemoField() || v.length() > 255) {
		    eaTv.SetValue("<memo>");
		    eaTv.SetNotes(v);
		} else {
		    eaTv.SetValue(v);
		    eaTv.SetNotes("");
		}

		if (!eaTv.Update()) {
		    throw new EAException(createMessage(message(101), name, con.GetName(), v, eaTv.GetLastError()));
		}
	    }
	}
    }

    /**
     * Adds the given collection of tagged values to the given connector, NOT
     * checking for duplicate tags.
     * <p>
     * <b>WARNING:</b> Enterprise Architect may initialize default tagged values for
     * a model element that adheres to a specific UML profile. In that case, adding
     * the same tagged values would lead to duplicates. If duplicates shall be
     * prevented, set the tagged value instead of adding it.
     * 
     * @param con the connector to which the tagged values shall be added
     * @param tvs collection of tagged values to add
     * @throws EAException tbd
     */
    public static void addTaggedValues(Connector con, List<EATaggedValue> tvs) throws EAException {

	if (tvs == null || tvs.isEmpty()) {

	    // nothing to do

	} else {

	    Collection<ConnectorTag> cTV = con.GetTaggedValues();

	    for (EATaggedValue tv : tvs) {

		String name = tv.getName();
		List<String> values = tv.getValues();

		if (values != null) {

		    for (String v : values) {

			ConnectorTag eaTv = cTV.AddNew(name, "");
			cTV.Refresh();

			if (tv.createAsMemoField() || v.length() > 255) {
			    eaTv.SetValue("<memo>");
			    eaTv.SetNotes(v);
			} else {
			    eaTv.SetValue(v);
			    eaTv.SetNotes("");
			}

			if (!eaTv.Update()) {
			    throw new EAException(
				    createMessage(message(101), name, con.GetName(), v, eaTv.GetLastError()));
			}
		    }
		}
	    }
	}
    }

    /**
     * Adds the given collection of tagged values to the given connector, NOT
     * checking for duplicate tags.
     * <p>
     * <b>WARNING:</b> Enterprise Architect may initialize default tagged values for
     * a model element that adheres to a specific UML profile. In that case, adding
     * the same tagged values would lead to duplicates. If duplicates shall be
     * prevented, set the tagged value instead of adding it.
     * 
     * @param con the connector to which the tagged values shall be added
     * @param tvs collection of tagged values to add
     * @throws EAException tbd
     */
    public static void addTaggedValues(Connector con, TaggedValues tvs) throws EAException {

	if (tvs == null || tvs.isEmpty()) {

	    // nothing to do

	} else {

	    Collection<ConnectorTag> cTV = con.GetTaggedValues();

	    for (Entry<String, List<String>> e : tvs.asMap().entrySet()) {

		String name = e.getKey();
		List<String> values = e.getValue();

		if (values != null) {

		    for (String v : values) {

			ConnectorTag eaTv = cTV.AddNew(name, "");
			cTV.Refresh();

			if (v.length() > 255) {
			    eaTv.SetValue("<memo>");
			    eaTv.SetNotes(v);
			} else {
			    eaTv.SetValue(v);
			    eaTv.SetNotes("");
			}

			if (!eaTv.Update()) {
			    throw new EAException(
				    createMessage(message(101), name, con.GetName(), v, eaTv.GetLastError()));
			}
		    }
		}
	    }
	}
    }

    public static void deleteTaggedValue(Connector con, String nameOfTVToDelete) {

	Collection<ConnectorTag> cTV = con.GetTaggedValues();
	cTV.Refresh();

	for (short i = 0; i < cTV.GetCount(); i++) {
	    ConnectorTag tv = cTV.GetAt(i);
	    if (tv.GetName().equalsIgnoreCase(nameOfTVToDelete)) {
		cTV.Delete(i);
	    }
	}

	cTV.Refresh();
    }

    /**
     * NOTE: only works with EA 12 Java API, not the previous API.
     * 
     * @param con          tbd
     * @param classElement tbd
     * @throws EAException tbd
     */
    public static void setEAAssociationClass(Connector con, Element classElement) throws EAException {

	/*
	 * 2015-06-25 JE: direct manipulation of MS Access DB to establish relationship
	 * between an AssociationClass and its connector no longer necessary with EA v12
	 * (beta had issues, but full version works). We decided to rely on the EA 12
	 * API. That means that the ArcGIS Workspace target won't work with deployments
	 * that use a previous version of EA. In any case, the direct manipulation of
	 * the DB no longer works with Java 8 and later, because the sun jdbc odbc
	 * bridge that was used to connect to the DB has been removed in Java 8. At this
	 * point in time there is no feasible alternative (tested ucanaccess and
	 * jackcess - both didn't work).
	 */

	int connectorID = con.GetConnectorID();

	classElement.CreateAssociationClass(connectorID);
    }

    public static void setEAAlias(Connector con, String aliasName) throws EAException {

	con.SetAlias(aliasName);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(107), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEANotes(Connector con, String notes) throws EAException {

	con.SetNotes(notes);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(108), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEAName(Connector con, String name) throws EAException {

	con.SetName(name);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(102), name, con.GetLastError()));
	}
    }

    public static void setEAStereotype(Connector con, String stereotype) throws EAException {

	con.SetStereotype(stereotype);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(103), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEAStereotypeEx(Connector con, String stereotypeEx) throws EAException {

	con.SetStereotypeEx(stereotypeEx);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(105), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEAStyleEx(Connector con, String styleEx) throws EAException {

	con.SetStyleEx(styleEx);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(106), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEADirection(Connector con, EADirection direction) throws EAException {

	con.SetDirection(direction.getEAValue());

	if (!con.Update()) {
	    throw new EAException(createMessage(message(104), con.GetName(), con.GetLastError()));
	}
    }

    public static void setEASupplierID(Connector con, int supplierID) throws EAException {

	con.SetSupplierID(supplierID);

	if (!con.Update()) {
	    throw new EAException(createMessage(message(109), con.GetName(), con.GetLastError()));
	}
    }

    /**
     * Sets the given tagged values in the given connector. If tagged values with
     * the same name as the given ones already exist, they will be deleted. Then the
     * tagged values will be added.
     * 
     * @param con the connector in which the tagged values shall be set
     * @param tvs tagged values to set, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValues(Connector con, List<EATaggedValue> tvs) throws EAException {

	for (EATaggedValue tv : tvs) {
	    deleteTaggedValue(con, tv.getName());
	}
	addTaggedValues(con, tvs);
    }

    /**
     * Sets the given tagged value in the tagged values of the given connector. If
     * tagged values with the same tag name already exist, they will be deleted.
     * Then the tagged value will be added.
     * 
     * @param con the connector in which the tagged value shall be set
     * @param tv  tagged value to set, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValue(Connector con, EATaggedValue tv) throws EAException {

	deleteTaggedValue(con, tv.getName());
	addTaggedValue(con, tv);
    }

    /**
     * Sets the given tagged values in the given connector. If tagged values with
     * the same name as the given ones already exist, they will be deleted. Then the
     * tagged values will be added.
     * 
     * @param con the connector in which the tagged values shall be set
     * @param tvs tagged values to set, must not be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValues(Connector con, TaggedValues tvs) throws EAException {

	for (String tvName : tvs.asMap().keySet()) {
	    deleteTaggedValue(con, tvName);
	}
	addTaggedValues(con, tvs);
    }

    /**
     * @param conn tbd
     * @return sorted map of the tagged values (key: {name '#' fqName}; value:
     *         according EATaggedValue); can be empty but not <code>null</code>
     */
    public static SortedMap<String, EATaggedValue> getEATaggedValues(Connector conn) {

	/*
	 * key: {name '#' fqName}; value: according EATaggedValue
	 */
	SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

	Collection<ConnectorTag> tvs = conn.GetTaggedValues();

	for (short i = 0; i < tvs.GetCount(); i++) {

	    ConnectorTag tv = tvs.GetAt(i);

	    String name = tv.GetName();
	    String fqName = tv.GetFQName();
	    String value;

	    String tvValue = tv.GetValue();

	    if (tvValue.equals("<memo>")) {
		value = tv.GetNotes();
	    } else {
		value = tvValue;
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

    /**
     * Updates the tagged values with given name (which can be a fully qualified
     * name) in the tagged values of the given connector. Does NOT delete those
     * tagged values. NOTE: This method is especially useful when setting tagged
     * values that are defined by an MDG / UML Profile, since these tagged values
     * cannot be created programmatically (they are created by EA - for further
     * details, see http://sparxsystems.com/forums/smf/index.php?topic=3859.0).
     * 
     * @param con               the connector in which the tagged values shall be
     *                          updated
     * @param name              (fully qualified or unqualified) name of the tagged
     *                          value to update, must not be <code>null</code>
     * @param value             value of the tagged value to update, can be
     *                          <code>null</code>
     * @param createAsMemoField If set to <code>true</code>, the values shall be
     *                          encoded using &lt;memo&gt; fields, regardless of the
     *                          actual length of each value.
     * @throws EAException If updating the connector did not succeed, this exception
     *                     contains the error message.
     */
    public static void updateTaggedValue(Connector con, String name, String value, boolean createAsMemoField)
	    throws EAException {

	boolean isQualifiedName = name.contains("::");

	Collection<ConnectorTag> cTV = con.GetTaggedValues();

	cTV.Refresh();

	for (short i = 0; i < cTV.GetCount(); i++) {

	    ConnectorTag tv = cTV.GetAt(i);

	    if ((isQualifiedName && tv.GetFQName().equalsIgnoreCase(name)) || tv.GetName().equalsIgnoreCase(name)) {

		if (createAsMemoField || value.length() > 255) {
		    tv.SetValue("<memo>");
		    tv.SetNotes(value);
		} else {
		    tv.SetValue(value);
		    tv.SetNotes("");
		}

		if (!tv.Update()) {
		    throw new EAException(createMessage(message(101), name, con.GetName(), value, tv.GetLastError()));
		}
	    }
	}

	cTV.Refresh();
    }

    /**
     * Determines if the given connector is the association of an association class.
     * That is the case if its subtype is 'class' and MiscData(0) is not empty.
     * 
     * @param con tbd
     * @return <code>true</code>, if the given connector is the association of an
     *         association class, else <code>false</code>.
     */
    public static boolean isAssociationClassConnector(Connector con) {
	return con.GetSubtype().equalsIgnoreCase("class") && !con.MiscData(0).isEmpty();
    }

    /**
     * Determine the navigability of this connector: 0 = navigable in both
     * directions, +1 = source -&gt; target, -1 = target -&gt; source
     * 
     * @param con tbd
     * @return Navigability (0=both, +1=source-&gt;target, -1=target-&gt;source)
     * 
     */
    public static int navigability(Connector con) {

	int result = 0;
	String dirText = con.GetDirection();
	if (dirText.equals("Source -> Destination")) {
	    result = 1;
	} else if (dirText.equals("Destination -> Source")) {
	    result = -1;
	}
	return result;
    }

    public static String message(int mnr) {

	switch (mnr) {

	case 101:
	    return "EA error encountered while updating EA tagged value '$1$' on connector '$2$' with value '$3$'. Error message is: $4$";
	case 102:
	    return "EA error encountered while updating 'Name' on connector '$1$'. Error message is: $2$";
	case 103:
	    return "EA error encountered while updating 'Stereotype' on connector '$1$'. Error message is: $2$";
	case 104:
	    return "EA error encountered while updating 'Direction' on connector '$1$'. Error message is: $2$";
	case 105:
	    return "EA error encountered while updating 'StereotypeEx' on connector '$1$'. Error message is: $2$";
	case 106:
	    return "EA error encountered while updating 'StyleEx' on connector '$1$'. Error message is: $2$";
	case 107:
	    return "EA error encountered while updating 'Alias' of EA connector '$1$'. Error message is: $2$";
	case 108:
	    return "EA error encountered while updating 'Notes' of EA connector '$1$'. Error message is: $2$";
	case 109:
	    return "EA error encountered while updating 'SupplierID' of EA connector '$1$'. Error message is: $2$";

	default:
	    return "(" + EAConnectorUtil.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
