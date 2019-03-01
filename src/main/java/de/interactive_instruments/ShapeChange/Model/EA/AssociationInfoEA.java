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

public class AssociationInfoEA extends AssociationInfoImpl
		implements AssociationInfo {

	/** Document */
	EADocument document = null;

	/** EA connector handle */
	Connector eaConnector = null;

	/** The EA object id of the association object */
	protected int eaConnectorId = 0;

	/** Name of Association */
	protected String name = null;

	/** Navigability 0=both, +1=source->target, -1=target->source */
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

	/** Cache map for tagged values */
	// this map is already defined in InfoImpl

	/** Cache set for stereotypes */
	// this map is already defined in InfoImpl

	/** AssociationInfoEA Ctor */
	AssociationInfoEA(EADocument doc, Connector conn, int id) {

		// Memorize parameters
		document = doc;
		eaConnector = conn;
		eaConnectorId = id;

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

		// If there is no name present, we construct one from the names of the
		// classes ...
		// A default name is used if the role is unset, e.g. orphaned Note Link.
		/*
		 * 2016-07-26 JE: The association name should not always automatically
		 * be constructed. For rule-owl-prop-iso191502Aggregation we would get
		 * association names that are not in the model. I've added a new input
		 * parameter to control the behavior.
		 */
		if (name == null || name.length() == 0) {
			if (options().dontConstructAssociationNames()) {
				name = "";
			} else {
				if (roles[0].ci != null)
					name = roles[0].ci.name() + "_";
				else
					name = "roles[0]_";
				if (roles[1].ci != null)
					name = name + roles[1].ci.name();
				else
					name = name + "roles[1]";
			}
		}

		// Write to debug trace ...
		document.result.addDebug(null, 10013, "association", id(), name());
	} // AssociationInfoEA Ctor

	/** Return PropertyInfo from source end */
	public PropertyInfo end1() {
		return properties[0];
	} // end1()

	/** Return PropertyInfo from source end */
	public PropertyInfo end2() {
		return properties[1];
	} // end2()

	/** Return model-unique id of association */
	public String id() {
		return Integer.valueOf(eaConnectorId).toString();
	} // id()

	/** Return Model object */
	public Model model() {
		return document;
	} // model()

	@Override
	public String name() {
		return name;
	} // name()

	// @Override
	// public Descriptors aliasNameAll() {
	//
	// // Retrieve/compute the alias only once
	// // Cache the result for subsequent use
	// if (!aliasAccessed) {
	//
	// aliasAccessed = true;
	//
	// // Obtain alias name from default implementation
	// Descriptors ls = super.aliasNameAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty()
	// && descriptorSource(Descriptor.ALIAS).equals("ea:alias")) {
	//
	// String alias = eaConnector.GetAlias();
	//
	// if (alias != null && !alias.isEmpty()) {
	//
	// super.aliasName = new Descriptors(
	// new LangString(options().internalize(alias)));
	// } else {
	// super.aliasName = new Descriptors();
	// }
	// }
	// }
	// return super.aliasName;
	// }

	// /**
	// * Return the documentation attached to the property object. This is
	// fetched
	// * from tagged values and - if this is absent - from the 'notes' specific
	// to
	// * the EA objects model.
	// */
	// @Override
	// public Descriptors documentationAll() {
	//
	// // Retrieve/compute the documentation only once
	// // Cache the result for subsequent use
	// if (!documentationAccessed) {
	//
	// documentationAccessed = true;
	//
	// // Fetch from tagged values
	// Descriptors ls = super.documentationAll();
	//
	// // Try EA notes, if both tagged values fail and ea:notes is the
	// // source
	// if (ls.isEmpty() && descriptorSource(Descriptor.DOCUMENTATION)
	// .equals("ea:notes")) {
	//
	// String s = eaConnector.GetNotes();
	//
	// // Fix for EA7.5 bug
	// if (s != null) {
	// s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
	// }
	//
	// if (s == null) {
	// super.documentation = new Descriptors();
	// } else {
	// super.documentation = new Descriptors(
	// new LangString(options().internalize(s)));
	// }
	// }
	// }
	// return super.documentation;
	// }

	/** Return options and configuration object. */
	public Options options() {
		return document.options;
	} // options()

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return document.result;
	} // result()

	// Validate stereotypes cache of the association. The stereotypes found are
	// 1. restricted to those defined within ShapeChange and 2. deprecated ones
	// are normalized to the lastest definitions.
	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			// Fetch stereotypes 'collection' ...
			String sts = eaConnector.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");
			// Allocate cache
			stereotypesCache = options().stereotypesFactory();
			// Copy stereotypes found for connector selecting those defined in
			// ShapeChange and normalizing deprecated ones.
			for (String stereotype : stereotypes) {
				String st = document.options
						.normalizeStereotype(stereotype.trim());
				if (st != null)
					for (String s : Options.assocStereotypes) {
						if (st.toLowerCase().equals(s))
							stereotypesCache.add(s);
					}
			}
		}
	} // validateStereotypesCache()
	
	public int getEAConnectorId() {
		return this.eaConnectorId;
	}

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
					t = document.normalizeTaggedValue(t);
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
	} // validateTaggedValuesCache()

	public ClassInfo assocClass() {
		String s = eaConnector.GetSubtype();
		ClassInfo assocClass = null;
		if (s.equalsIgnoreCase("class") && !eaConnector.MiscData(0).isEmpty()) {
			assocClass = document.fClassById.get(eaConnector.MiscData(0));
		}
		return assocClass;
	}

	// @Override
	// public Descriptors globalIdentifierAll() {
	//
	// // Obtain global identifier from default implementation
	// Descriptors ls = super.globalIdentifierAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty() && descriptorSource(Descriptor.GLOBALIDENTIFIER)
	// .equals("ea:guidtoxml")) {
	//
	// String gi = document.repository.GetProjectInterface()
	// .GUIDtoXML(eaConnector.GetConnectorGUID());
	//
	// super.globalIdentifier = new Descriptors(
	// new LangString(options().internalize(gi)));
	// }
	// return super.globalIdentifier;
	// }

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!documentationAccessed
					&& descriptor == Descriptor.DOCUMENTATION) {

				documentationAccessed = true;

				String s = eaConnector.GetNotes();

				// Fix for EA7.5 bug
				if (s != null) {
					s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
				}

				if (s != null) {
					ls.add(new LangString(options().internalize(s)));
					this.descriptors().put(descriptor, ls);
				}

			} else if (!globalIdentifierAccessed
					&& descriptor == Descriptor.GLOBALIDENTIFIER) {

				globalIdentifierAccessed = true;

				// obtain from EA model directly
				if (descriptorSource(Descriptor.GLOBALIDENTIFIER)
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
				if (descriptorSource(Descriptor.ALIAS).equals("ea:alias")) {

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
