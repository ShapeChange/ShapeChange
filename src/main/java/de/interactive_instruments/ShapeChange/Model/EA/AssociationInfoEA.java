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

package de.interactive_instruments.ShapeChange.Model.EA;

import java.util.List;

import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.ConnectorTag;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.AssociationInfoImpl;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.StereotypeNormalizer;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorUtil;

public class AssociationInfoEA extends AssociationInfoImpl
		implements AssociationInfo {

	/** Document */
	EADocument document = null;

	/** EA connector handle */
	Connector eaConnector = null;

	protected String connectorId = null;

	/** Navigability 0=both, +1=source-&gt;target, -1=target-&gt;source */
	protected int navigability = 0;

	/** Relevant part of model? */
	protected boolean relevant = false;

	/** Source [0] and target [1] properties */
	protected PropertyInfoEA[] properties = { null, null };

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * association.
	 */
	protected boolean aliasAccessed = false;

	/**
	 * Flag used to prevent duplicate retrieval/computation of the documentation
	 * of this property.
	 */
	protected boolean documentationAccessed = false;

	/**
	 * Flag used to prevent duplicate retrieval/computation of the global
	 * identifier of this property.
	 */
	protected boolean globalIdentifierAccessed = false;

	/** AssociationInfoEA Ctor */
	AssociationInfoEA(EADocument doc, Connector conn, String id) {

		// Memorize parameters
		document = doc;
		eaConnector = conn;

		connectorId = id;

		// Fetch the necessary data from the connector
		name = eaConnector.GetName();

		String dirText = eaConnector.GetDirection();
		if (dirText.equals("Source -> Destination"))
			navigability = 1;
		else if (dirText.equals("Destination -> Source"))
			navigability = -1;

		// Fetch all necessary data from both roles. We need this to decide
		// if the association is part of the model we will be looking at.
		class Role {
			ConnectorEnd eaConnEnd = null;
			ClassInfoEA ci = null;
		}
		Role roles[] = new Role[2];
		Integer cid;
		ConnectorEnd ce = null;
		// Source end ...
		roles[0] = new Role();
		ce = eaConnector.GetClientEnd();
		cid = eaConnector.GetClientID();
		roles[0].ci = document.fClassById.get(cid.toString());
		roles[0].eaConnEnd = ce;
		// Target end ...
		roles[1] = new Role();
		ce = eaConnector.GetSupplierEnd();
		cid = eaConnector.GetSupplierID();
		roles[1].ci = document.fClassById.get(cid.toString());
		roles[1].eaConnEnd = ce;

		// If both association partners are classes in the model, the
		// association will be deemed a relevant one. In this case we create
		// PropertyInfo objects and register these at the end of the association
		// and as role induced properties of the opposite class.
		if (roles[0].ci != null && roles[1].ci != null) {
			// So, this is an association between known classes and will be
			// registered
			relevant = true;
			// Register both ends at the opposite ClassInfoEA ...
			for (int i = 0; i < 2; i++) {
				// Create a PropertyInfo object ...
				PropertyInfoEA pi = new PropertyInfoEA(document,
						roles[1 - i].ci, this, i == 0, roles[i].eaConnEnd,
						roles[i].ci);
				// .. and attach it to the ends of the Association.
				properties[i] = pi;
				// Now, register the PropertyInfo at the class for which it is
				// a role. This is restricted to navigability to prevent that
				// not usable roles conflicts with attributes.
				if (pi.isNavigable())
					roles[1 - i].ci.establishRoles(pi);
			}
		}

		// Write to debug trace ...
		document.result.addDebug(null, 10013, "association", id(), name());
	}

	/** Return PropertyInfo from source end */
	public PropertyInfo end1() {
		return properties[0];
	}

	/** Return PropertyInfo from source end */
	public PropertyInfo end2() {
		return properties[1];
	}

	/** Return model-unique id of association */
	public String id() {
		return connectorId;
	}

	/** Return Model object */
	public Model model() {
		return document;
	}
	
	/** Return options and configuration object. */
	public Options options() {
		return document.options;
	}

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return document.result;
	}

	/**
	 * The stereotypes added to the cache are the well-known equivalents of the
	 * stereotypes defined in the EA model, if mapped in the configuration.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Info#validateStereotypesCache()
	 */
	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			// Fetch stereotypes 'collection' ...
			String sts = eaConnector.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");
			// Allocate cache
			stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(stereotypes, this);
		}
	}

//	public int getEAConnectorId() {
//		return this.eaConnectorId;
//	}

	// Validate tagged values cache, filtering on tagged values defined within
	// ShapeChange ...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {
			// Fetch tagged values collection
			Collection<ConnectorTag> tvs = eaConnector.GetTaggedValues();

			// ensure that there are tagged values
			if (tvs != null) {

				// Allocate cache
				int ntvs = tvs.GetCount();
				taggedValuesCache = options().taggedValueFactory(ntvs);
				// Copy tag-value-pairs, leave out non-ShapeChange stuff and
				// normalize deprecated tags.
				for (ConnectorTag tv : tvs) {
					String t = tv.GetName();
					t = options().taggedValueNormalizer()
							.normalizeTaggedValue(t);
					if (t != null) {
						String v = tv.GetValue();
						if (v.equals("<memo>"))
							v = tv.GetNotes();
						taggedValuesCache.add(t, v);
					}
				}
			} else {
				taggedValuesCache = options().taggedValueFactory(0);
			}
		}
	}
	
	public ClassInfo assocClass() {
		ClassInfo assocClass = null;
		if (EAConnectorUtil.isAssociationClassConnector(eaConnector)) {
			assocClass = document.fClassById.get(eaConnector.MiscData(0));
		}
		return assocClass;
	}

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!documentationAccessed
					&& descriptor == Descriptor.DOCUMENTATION) {

				documentationAccessed = true;

				String s = eaConnector.GetNotes();

				// Handle EA formatting
				if (s != null) {
					s = document.applyEAFormatting(s);
				}

				if (s != null) {
					ls.add(new LangString(options().internalize(s)));
					this.descriptors().put(descriptor, ls);
				}

			} else if (!globalIdentifierAccessed
					&& descriptor == Descriptor.GLOBALIDENTIFIER) {

				globalIdentifierAccessed = true;

				// obtain from EA model directly
				if (model().descriptorSource(Descriptor.GLOBALIDENTIFIER)
						.equals("ea:guidtoxml")) {

					String gi = document.repository.GetProjectInterface()
							.GUIDtoXML(eaConnector.GetConnectorGUID());

					if (gi != null && !gi.isEmpty()) {
						ls.add(new LangString(options().internalize(gi)));
						this.descriptors().put(descriptor, ls);
					}
				}

			} else if (!aliasAccessed && descriptor == Descriptor.ALIAS) {

				aliasAccessed = true;

				/*
				 * obtain from EA model directly if ea:alias is identified as
				 * the source
				 */
				if (model().descriptorSource(Descriptor.ALIAS)
						.equals("ea:alias")) {

					String a = eaConnector.GetAlias();

					if (a != null && !a.isEmpty()) {
						ls.add(new LangString(options().internalize(a)));
						this.descriptors().put(descriptor, ls);
					}
				}
			}

		}

		return ls;
	}
}
