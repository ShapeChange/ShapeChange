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

import java.util.SortedSet;
import java.util.TreeSet;

import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.Element;
import org.sparx.TaggedValue;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfoImpl;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

public class PackageInfoEA extends PackageInfoImpl implements PackageInfo {

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * package.
	 */
	protected boolean aliasAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the connectors of
	 * this package.
	 */
	protected boolean connectorsAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the documentation
	 * of this package.
	 */
	protected boolean documentationAccessed = false;

	/** Access to the connectors of this package in the EA model */
	protected Collection<Connector> conns = null;

	/** The Model object */
	protected EADocument document = null;

	/** The parent package object */
	protected PackageInfoEA parentPI = null;

	/** Set of child package objects */
	protected TreeSet<PackageInfoEA> childPI = new TreeSet<PackageInfoEA>();

	/** Set of child classes */
	protected TreeSet<ClassInfoEA> childCI = new TreeSet<ClassInfoEA>();

	/** The EA package object */
	protected org.sparx.Package eaPackage = null;

	/** The EA object id of the package object */
	protected int eaPackageId = 0;

	/** The EA element object possibly associated to the package */
	protected Element eaPackageElmt = null;

	/** The EA object id of the associated element object */
	protected int eaPackageElmtId = 0;

	/** Name of the Package */
	protected String eaName = null;

	/** Cache map for tagged values */
	// this map is already defined in InfoImpl

	/** Cache set for stereotypes */
	// this map is already defined in InfoImpl

	/** Inquire wrapped EA object */
	public org.sparx.Package getEaPackageObj() {
		return eaPackage;
	}

	/** Cache for the IDs of the suppliers of this class */
	protected TreeSet<String> supplierIds = null;

	/** Create new PackageInfo object. */
	public PackageInfoEA(EADocument doc, PackageInfoEA ppi,
			org.sparx.Package pack, Element packelmt) {
		// Memorize document object
		document = doc;
		// Store EA package object and inquire its id and name.
		eaPackage = pack;
		eaPackageId = eaPackage.GetPackageID();
		eaName = eaPackage.GetName().trim();

		// Store the possibly associated EA element (describing the package) and
		// its id.
		eaPackageElmt = packelmt;
		if (eaPackageElmt != null)
			eaPackageElmtId = eaPackageElmt.GetElementID();

		// Memorize parent PackageInfo and establish this package as a child
		// of its parent.
		parentPI = ppi;
		if (ppi != null)
			ppi.childPI.add(this);
	} // PackageInfoEA Ctor

	/** Return EA model object. */
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
	 * @see de.interactive_instruments.ShapeChange.Model.PackageInfo#containedPackages()
	 */
	@SuppressWarnings("unchecked")
	public SortedSet<PackageInfo> containedPackages() {
		// Return a shallow copy of the stored child relation.
		return (TreeSet<PackageInfo>) childPI.clone();
	} // containedPackages()

	// Validate stereotypes cache of the package. The stereotypes found are 1.
	// restricted to those defined within ShapeChange and 2. deprecated ones
	// are normalized to the lastest definitions.
	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			// Fetch stereotypes 'collection' ...
			String sts = eaPackage.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");
			// Allocate cache
			stereotypesCache = options().stereotypesFactory();
			// Copy stereotypes found in package selecting those defined in
			// ShapeChange and normalizing deprecated ones.
			for (String stereotype : stereotypes) {
				String st = document.options
						.normalizeStereotype(stereotype.trim());
				if (st != null)
					for (String s : Options.packageStereotypes) {
						if (st.toLowerCase().equals(s))
							stereotypesCache.add(s);
					}
			}
		}
	} // validateStereotypesCache()

	/**
	 * Determine whether the package represents an 'application schema'. The
	 * package is regarded an 'application schema', if it carries a stereotype
	 * with normalized name "application schema".
	 */
	@Override
	public boolean isAppSchema() {
		// Validate stereotypes cache.
		validateStereotypesCache();
		// Find out if the package has the requested stereotype.
		return stereotypesCache.contains("application schema");
	} // isAppSchema()

	/** Return the parent package if present, null otherwise. */
	public PackageInfo owner() {
		return parentPI;
	} // owner()

	/**
	 * Determine the root package. Search the package and its ancestors for one
	 * representing a schema. Return null if no such package exists.
	 */
	public PackageInfo rootPackage() {
		PackageInfoEA pi = this;
		while (pi != null && !pi.isSchema())
			pi = pi.parentPI;
		return pi;
	} // rootPackage()

	/** Return the set of ids of the packages on which this package depends. */
	public SortedSet<String> supplierIds() {

		// Only retrieve/compute supplierIds once
		// Cache the results for subsequent use
		if (supplierIds == null) {
			// Prepare set to return
			supplierIds = new TreeSet<String>();
			// Ask EA for the connectors attached to the package object and loop
			// over the connectors returned
			// Only compute them once for the whole class
			if (!connectorsAccessed) {
				conns = eaPackage.GetConnectors();
				connectorsAccessed = true;
			}
			// Ensure that there are connectors before continuing
			if (conns != null) {
				for (Connector conn : conns) {
					// Single out dependency connectors
					String type = conn.GetType();
					if (type.equals("Dependency") || type.equals("Package")) {
						// From the dependency grab the id of the supplier
						int suppId = conn.GetSupplierID();
						String suppIdS = Integer.toString(suppId);
						// Since all connectors are delivered from both objects
						// at the
						// connector ends, we have to make sure it is not
						// accidently us,
						// which we found.
						if (suppId == eaPackageElmtId)
							continue;
						// Now, this is an element id, not a package id. So we
						// have to
						// identify the package, which owns the element
						// addressed.
						PackageInfoEA suppPack = (PackageInfoEA) document.fPackageByElmtId
								.get(suppIdS);
						// From this only the id is required
						if (suppPack != null) {

							String suppPackId = suppPack.id();

							if (suppPackId != null)
								supplierIds.add(suppPackId);
						}
					}
				}
			}
		}
		return supplierIds;
	} // supplierIds()

	/**
	 * Return the documentation attached to the property object. This is fetched
	 * from tagged values and - if this is absent - from the 'notes' specific to
	 * the EA objects model.
	 */
	@Override
	public String documentation() {

		// Retrieve/compute the documentation only once
		// Cache the result for subsequent use
		if (!documentationAccessed) {

			documentationAccessed = true;

			// Fetch from tagged values
			String s = super.documentation();
			// Try EA notes, if both tagged values fail
			if ((s == null || s.length() == 0) && descriptorSource(
					Options.Descriptor.DOCUMENTATION.toString())
							.equals("ea:notes")) {
				s = eaPackage.GetNotes();
				// Fix for EA7.5 bug
				if (s != null) {
					s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
					super.documentation = options().internalize(s);
				}
			}
		}
		return super.documentation;
	} // documentation()

	/** Return model-unique id of package. */
	public String id() {
		return new Integer(eaPackageId).toString();
	} // id()

	/** Obtain the name of the package. */
	public String name() {
		if (eaName == null || eaName.equals("")) {
			eaName = id();
			MessageContext mc = document.result.addWarning(null, 100, "package",
					eaName);
			if (mc != null)
				mc.addDetail(null, 400, "Package", owner().fullName());
		}
		return eaName;
	} // name();

	/** Get alias name of the package. */
	@Override
	public String aliasName() {
		// Retrieve/compute the alias only once
		// Cache the result for subsequent use
		if (!aliasAccessed) {

			aliasAccessed = true;

			// Obtain alias name from default implementation
			String a = super.aliasName();
			// If not present, obtain from EA model directly
			if ((a == null || a.length() == 0)
					&& descriptorSource(Options.Descriptor.ALIAS.toString())
							.equals("ea:alias")) {
				a = eaPackage.GetAlias();
				super.aliasName = options().internalize(a);
			}
		}
		return super.aliasName;
	} // aliasName()

	// Validate tagged values cache, filtering on tagged values defined within
	// ShapeChange ...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {
			// Fetch tagged values collection
			Collection<TaggedValue> tvs = null;
			int ntvs = 0;
			if (eaPackageElmt != null) {
				tvs = eaPackageElmt.GetTaggedValues();
				// ensure that there are tagged values
				if (tvs != null) {
					ntvs = tvs.GetCount();
				}
			}
			// Allocate cache
			taggedValuesCache = options().taggedValueFactory(ntvs);
			// Copy tag-value-pairs, leave out non-ShapeChange stuff and
			// normalize deprecated tags.
			if (tvs != null)
				for (TaggedValue tv : tvs) {
					String t = tv.GetName();
					t = document.normalizeTaggedValue(t);
					if (t != null) {
						String v = tv.GetValue();
						if (v.equals("<memo>"))
							v = tv.GetNotes();
						taggedValuesCache.add(t, v);
					}
				}
		}
	} // loadTaggedValuesCache()

	/** Set the tagged value for the tag given. */
	public void taggedValue(String tag, String value) {
		Collection<TaggedValue> cTV = eaPackageElmt.GetTaggedValues();
		TaggedValue tv = cTV.GetByName(tag);
		if (tv == null && value != null) {
			tv = cTV.AddNew(tag, value);
			tv.Update();
		} else if (tv != null) {
			if (value == null)
				value = "";
			if (!tv.GetValue().equals(value)) {
				tv.SetValue(value);
				tv.Update();
			}
		}
		// invalidate cache
		taggedValuesCache = null;
	} // taggedValue()
}
