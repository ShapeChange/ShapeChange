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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Generic;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author echterhoff
 * 
 */
public class GenericPackageInfo extends PackageInfoImpl
		implements MessageSource {

	protected Options options = null;
	protected ShapeChangeResult result = null;
	protected GenericModel model = null;
	protected String id = null;
	protected String name = null;

	protected GenericPackageInfo owner = null;
	// protected boolean isSchema = false;
	protected SortedSet<GenericPackageInfo> childPi = null;
	protected SortedSet<String> supplierIds = null;

	protected SortedSet<GenericClassInfo> classes = new TreeSet<>();

	/**
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.PackageInfo#containedPackages
	 *      ()
	 */
	@Override
	public SortedSet<PackageInfo> containedPackages() {

		SortedSet<PackageInfo> children = null;

		if (childPi != null) {
			children = new TreeSet<PackageInfo>(childPi);
		}

		return children;
	}

	/**
	 * Provides the set of all packages in the package tree where this package
	 * is the head. This package is also added to the set.
	 * 
	 * @param set tbd
	 * @return tbd
	 */
	public Set<GenericPackageInfo> getAllPackages(Set<GenericPackageInfo> set) {

		if (set == null)
			return null;

		if (!childPi.isEmpty()) {

			for (PackageInfo child : childPi) {

				if (child instanceof GenericPackageInfo) {
					((GenericPackageInfo) child).getAllPackages(set);
				} else {
					// the child package is not one of the selected schema; we
					// ignore it
				}
			}
		}
		set.add(this);
		return set;
	}

	public SortedSet<GenericClassInfo> getClasses() {
		return classes;
	}

	public String id() {
		return id;
	}

	public Model model() {
		return model;
	}

	public String name() {
		return name;
	}

	public Options options() {
		return options;
	}

	public PackageInfo owner() {
		return owner;
	}

	public ShapeChangeResult result() {
		return result;
	}

	public void setClasses(SortedSet<GenericClassInfo> genericClassInfos) {
		this.classes = genericClassInfos;
	}

	public void setContainedPackages(SortedSet<GenericPackageInfo> childPi) {
		this.childPi = childPi;
	}

	public void setId(String id) {
		this.id = id;

	}

	public void setModel(GenericModel model) {
		this.model = model;

	}

	public void setName(String name) {
		this.name = name;

	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public void setOwner(GenericPackageInfo owner) {
		this.owner = owner;

	}

	public void setResult(ShapeChangeResult result) {
		this.result = result;

	}

	/** Save the (normalized) stereotypes in the cache. */
	public void validateStereotypesCache() {
		// create cache, if necessary
		if (stereotypesCache == null)
			stereotypesCache = options().stereotypesFactory();

		// do nothing else, stereotypes have to be set explicitly using
		// setStereotypes

	} // validateStereotypesCache

	public void setStereotypes(Stereotypes stereotypeSet) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotypeSet != null && !stereotypeSet.isEmpty()) {
			for (String st : stereotypeSet.asArray()) {
				stereotypesCache.add(
						options.internalize(options.normalizeStereotype(st)));
			}
		}
	}

	public void setStereotype(String stereotype) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotype != null) {
			stereotypesCache.add(options
					.internalize(options.normalizeStereotype(stereotype)));
		}
	}

	public void setSupplierIds(SortedSet<String> supplierIds) {
		if (supplierIds == null || supplierIds.isEmpty()) {
			this.supplierIds = null;
		} else {
			this.supplierIds = new TreeSet<String>();
			for (String supplierId : supplierIds) {
				this.supplierIds.add(options.internalize(supplierId));
			}
		}

	}

	public void validateTaggedValuesCache() {
		// create cache, if necessary
		if (taggedValuesCache == null)
			taggedValuesCache = options().taggedValueFactory();

		// do nothing else, tagged values have to be set explicitly using
		// setTaggedValues
	}

	/**
	 * @param taggedValues tbd
	 * @param updateFields
	 *                         true if class fields should be updated based upon
	 *                         information from given tagged values, else false
	 */
	public void setTaggedValues(TaggedValues taggedValues,
			boolean updateFields) {

		// clone tagged values
		taggedValuesCache = options().taggedValueFactory(taggedValues);

		// Now update fields, if they are affected by tagged values

		// TODO add more updates for relevant tagged values

		if (updateFields && !taggedValuesCache.isEmpty()) {

//			for (String key : taggedValuesCache.keySet()) {

				// TODO setting application) schema tagged values can have
				// additional side effects for Options.java (update information
				// stored there on (application) schema) - at the moment these
				// changes would be removed when the fields within Options.java
				// are reset when the next process (Transformer or Target) is
				// executed

				/*
				 * We don't set targetNamespace, version, xmlns, and xsdDocument
				 * explicitly. This information should be stored in tagged
				 * values.
				 */

				/*
				 * TBD: Descriptors should not be modified right away, since the
				 * descriptor source might not be the tagged value
				 */
				// else if (key.equalsIgnoreCase("alias")) {
				//
				// String[] tvs = taggedValuesCache.get(key);
				//
				// List<LangString> val;
				//
				// if (tvs.length == 0) {
				// val = new ArrayList<LangString>();
				// } else {
				// val = LangString.parse(tvs);
				// }
				// this.descriptors.put(Descriptor.ALIAS, val);
				//
				// } else if (key.equalsIgnoreCase("documentation")) {
				//
				// // we map this to the descriptor 'definition'
				// String[] tvs = taggedValuesCache.get(key);
				//
				// List<LangString> val;
				//
				// if (tvs.length == 0) {
				// val = new ArrayList<LangString>();
				// } else {
				// val = LangString.parse(tvs);
				// }
				// this.descriptors.put(Descriptor.DOCUMENTATION, val);
				// }
//			}
		}
	}

	/**
	 * Sets the target namespace for this package (by setting the according
	 * tagged value).
	 * 
	 * @param targetNamespace tbd
	 */
	public void setTargetNamespace(String targetNamespace) {

		TaggedValues tmp = this.taggedValuesAll();
		tmp.put("targetNamespace", targetNamespace);
		this.setTaggedValues(tmp, false);
	}

	/**
	 * Sets the version for this package (by setting the according
	 * tagged value).
	 * 
	 * @param version tbd
	 */
	public void setVersion(String version) {
		
		TaggedValues tmp = this.taggedValuesAll();
		tmp.put("version", version);
		this.setTaggedValues(tmp, false);
	}

	/**
	 * Sets the xmlns for this package (by setting the according
	 * tagged value).
	 * @param xmlns tbd
	 */
	public void setXmlns(String xmlns) {

		TaggedValues tmp = this.taggedValuesAll();
		tmp.put("xmlns", xmlns);
		this.setTaggedValues(tmp, false);
	}

	/**
	 * Sets the xsdDocument for this package (by setting the according
	 * tagged value).
	 * @param xsdDocument tbd
	 */
	public void setXsdDocument(String xsdDocument) {

		TaggedValues tmp = this.taggedValuesAll();
		tmp.put("xsdDocument", xsdDocument);
		this.setTaggedValues(tmp, false);
	}

	public SortedSet<String> supplierIds() {
		if (this.supplierIds == null) {
			return new TreeSet<String>();
		} else {
			return supplierIds;
		}
	}

	public void getEmptyPackages(Set<PackageInfo> containerForEmptyPackages) {

		if (childPi == null || childPi.isEmpty()) {

			if (classes == null || classes.isEmpty()) {

				// this package has no child packages and no classes
				containerForEmptyPackages.add(this);

			} else {
				// this package has classes and thus is not empty
			}

		} else {
			// this package has child packages, invoke this method recursively
			// to see if they are empty
			for (PackageInfo child : childPi) {

				if (child instanceof GenericPackageInfo) {
					((GenericPackageInfo) child)
							.getEmptyPackages(containerForEmptyPackages);

				} else {
					/*
					 * Alright, we have encountered a child which is not one of
					 * the selected schema. The way this method works, we cannot
					 * analyze such a package. We need to assume that it is not
					 * empty.
					 */
				}
			}

			if (containerForEmptyPackages.containsAll(childPi)
					&& (classes == null || classes.isEmpty())) {
				// this package has child packages but they are all empty;
				// as this package also has no classes, add it to the result set
				containerForEmptyPackages.add(this);
			} else {
				// one or more of the child packages is not empty, thus this
				// package has relevant content
			}
		}
	}
	
	public String toString(String indent) {

		StringBuffer sb = new StringBuffer();

		sb.append(indent + name + "\n");
		if (classes != null && classes.size() != 0) {
			sb.append(indent + "classes:\n");
			for (ClassInfo ci : classes) {
				sb.append(indent + indent + ci.name() + "\n");
			}
		} else {
			sb.append(indent + "<no classes>\n");
		}

		if (childPi != null && childPi.size() > 0) {
			sb.append(indent + "packages:\n");
			for (PackageInfo pi : childPi) {
				if (pi instanceof GenericPackageInfo) {
					sb.append(((GenericPackageInfo) pi)
							.toString(indent + indent));
				} else {
					sb.append(indent + indent + pi.name()
							+ "(not a GenericPackageInfo\n");
				}
			}
		} else {
			sb.append(indent + "<no child packages>\n");
		}

		return sb.toString();

	}

	public void addClass(GenericClassInfo ci) {
		if (ci != null)
			this.classes.add(ci);
	}

	/**
	 * Removes the given class info from the set of class infos for this
	 * package.
	 * 
	 * @param ciToRemove
	 */
	void remove(ClassInfo ciToRemove) {
		if (ciToRemove != null)
			this.classes.remove(ciToRemove);

	}

	/**
	 * Removes the given package info from the set of child package infos.
	 * 
	 * @param piToRemove
	 */
	void removeChild(GenericPackageInfo piToRemove) {
		if (piToRemove != null)
			this.childPi.remove(piToRemove);
	}

	/**
	 * Adds the prefix to the 'id' of this package as well as the 'schemaId' (if
	 * not <code>null</code>) and the 'supplierIds' (if not <code>null</code>).
	 * 
	 * NOTE: this method is used by the FeatureCatalogue target to ensure that
	 * IDs used in a reference model are unique to that model and do not get
	 * mixed up with the IDs of the input model.
	 * 
	 * @param prefix tbd
	 */
	public void addPrefixToModelElementIDs(String prefix) {

		this.id = prefix + id;

		if (supplierIds != null) {
			SortedSet<String> tmp_supplierIds = new TreeSet<String>();
			for (String id : supplierIds) {
				tmp_supplierIds.add(prefix + id);
			}
			this.supplierIds = tmp_supplierIds;
		}
	}

	@Override
	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {

		case 30500:
			return "(GenericPackageInfo.java) Child package '$1$' of package '$2$' is not an application schema but also not an instance of GenericPackageInfo. Cannot set the target namespace on '$3$'.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
