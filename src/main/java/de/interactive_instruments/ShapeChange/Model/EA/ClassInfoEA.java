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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.sparx.Attribute;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.Method;
import org.sparx.TaggedValue;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.StereotypeNormalizer;

public class ClassInfoEA extends ClassInfoImpl implements ClassInfo {

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * class.
	 */
	protected boolean aliasAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the connectors of
	 * this class.
	 */
	protected boolean connectorsAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the documentation
	 * of this class.
	 */
	protected boolean documentationAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the
	 * globalIdentifier of this class.
	 */
	protected boolean globalIdentifierAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the association
	 * of this class.
	 */
	protected boolean isAssocClassAccessed = false;

	/**
	 * Cache for the association this class belongs to if it is an association
	 * class.
	 */
	protected AssociationInfo assoc = null;

	/** Access to the connectors of this class in the EA model */
	protected Collection<Connector> conns = null;

	/** Cache for the IDs of the suppliers of this class */
	protected TreeSet<String> supplierIds = null;

	/** Access to the document object */
	protected EADocument document;

	/** The package the class belongs to */
	protected PackageInfoEA packageInfo;

	/** Baseclasses */
	protected ClassInfoEA baseclassInfo = null;
	protected TreeSet<ClassInfoEA> baseclassInfoSet = null;

	/** Subclasses */
	protected TreeSet<ClassInfoEA> subclassInfoSet = new TreeSet<ClassInfoEA>();

	/** The EA element addressed by this ClassInfo */
	protected org.sparx.Element eaClassElement = null;

	/** The EA object id of the class element object */
	protected int eaClassId = 0;

	/** Name of the class */
	protected String eaName = null;

	/** Some class flags. */
	protected boolean isAbstract = false;
	protected boolean isLeaf = false;

	/** Roles registered as properties of the class */
	protected Vector<PropertyInfoEA> registeredRoles = new Vector<PropertyInfoEA>();

	/** Cache map for tagged values */
	// this map is already defined in InfoImpl

	/** Cache set for stereotypes */
	// this map is already defined in InfoImpl

	/** Cache (ordered) set for properties */
	protected TreeMap<StructuredNumber, PropertyInfo> propertiesCache = null;

	/** Cache (ordered) set for operations */
	protected TreeMap<Integer, OperationInfo> operationsCache = null;

	/** Cache set of constraints */
	protected Vector<Constraint> constraintsCache = null;

	private Boolean realization = null;

	/**
	 * Create new ClassInfo object
	 * 
	 * @throws ShapeChangeAbortException
	 */
	public ClassInfoEA(EADocument doc, PackageInfoEA pi, org.sparx.Element elmt)
			throws ShapeChangeAbortException {
		// Memorize document and parent package.
		document = doc;
		packageInfo = pi;
		// EA object reference. Fetch id and name.
		eaClassElement = elmt;
		eaClassId = eaClassElement.GetElementID();
		eaName = eaClassElement.GetName().trim();

		// Register as child of parent package.
		pi.childCI.add(this);

		// Determine some class flags
		isAbstract = eaClassElement.GetAbstract().equals("1");
		isLeaf = eaClassElement.GetIsLeaf();
		// Determine class category
		if (elmt.GetType().equalsIgnoreCase("enumeration"))
			category = Options.ENUMERATION;
		else
			establishCategory();

		// Cache if realisations should not be treated as generalisations
		/*
		 * 2015-04-16 JE TBD: 'realisationLikeGeneralisation' should become a
		 * general input parameter. Loading of the input model should not depend
		 * on a specific target.
		 */
		String realization = document.options.parameter(
				Options.TargetXmlSchemaClass, "realisationLikeGeneralisation");
		if (realization != null && realization.equalsIgnoreCase("false")) {
			this.realization = Boolean.FALSE;
		}
	} // ClassInfoEA Ctor

	// Establish class derivation hierarchy. This auxiliary initializing
	// method sets the base class relation ship obtained from the model and
	// also enters this class as subclass of all its base classes.
	// Note that invocation of this method requires that all classes in
	// the model are already cached.
	// Note: ISO19107 makes use of realization instead of generalization in some
	// cases to inherit from interfaces. Therefore realization of interfaces is
	// also considered a base class relationship as a default unless overruled
	// by
	// a parameter.
	public void establishClassDerivationHierarchy() {
		// Find out about all connectors attached to the class
		// Only do this once; cache the results for subsequent use
		if (!connectorsAccessed) {
			conns = eaClassElement.GetConnectors();
			connectorsAccessed = true;
		}

		// check that this class has connectors
		if (conns != null) {

			/*
			 * Enumerate connectors selecting those where this class is the
			 * client, from these select "Generalization" and "Realisation".
			 * Retrieve the supplier class wrappers. For "Realisation" type
			 * suppliers also make sure the class is an interface. The classes
			 * found are registered as base classes. In the base classes
			 * register this class as subclass.
			 */
			int nbcl = 0;
			int clientid, bclid, cat;
			String conntype;
			boolean gen, rea;
			for (Connector conn : conns) {

				// Skip all other connector types
				conntype = conn.GetType();
				gen = conntype.equals("Generalization");
				rea = conntype.equals("Realisation");

				// this.realization is determined from configuration parameters
				// if it is not null then it is false
				if (this.realization != null
						&& !this.realization.booleanValue())
					rea = false;
				if (!gen && !rea)
					continue;

				// Make sure we are the client of this connector
				clientid = conn.GetClientID();
				if (clientid != this.eaClassId)
					continue;

				// Find out about the id of the base class (=supplier)
				bclid = conn.GetSupplierID();
				// From this determine the ClassInfo wrapper object
				ClassInfoEA baseCI = document.fClassById
						.get(String.valueOf(bclid));
				// If such an object exists establish it as base class.
				if (baseCI != null) {
					// If we know this via a Realization we additionally check
					// we are seeing an interface.
					if (rea) {
						cat = baseCI.category();
						if (cat != Options.MIXIN)
							continue;
					}
					/*
					 * Establish as base class. Since most classes indeed
					 * possess at most one base class, this case will be treated
					 * somewhat storage-optimized.
					 */
					if (++nbcl == 1) {
						baseclassInfo = baseCI;
					} else {
						if (baseclassInfoSet == null) {
							baseclassInfoSet = new TreeSet<ClassInfoEA>();
							baseclassInfoSet.add(baseclassInfo);
							baseclassInfo = null;
						}
						baseclassInfoSet.add(baseCI);
					}
					// Register with the subclasses of the base class.
					baseCI.subclassInfoSet.add(this);
				}
			}
		}
	} // establishClassDerivationHierarchy()

	/*
	 * Old version as of 2009-05-27 preserved. Required change was the
	 * additional consideration of realisations instead of generalisations.
	 * public void establishClassDerivationHierarchy() { // Find out about base
	 * classes Collection<Element> baseclasses =
	 * eaClassElement.GetBaseClasses(); short nbcl = baseclasses.GetCount(); //
	 * Enumerate base classes and retrieve their corresponding wrappers. //
	 * Register these as base classes. In the base classes register this //
	 * class as subclass. for(Element baseclass : baseclasses) { // Find out
	 * about the id of the base class int bclid = baseclass.GetElementID(); //
	 * From this determine the ClassInfo wrapper object ClassInfoEA baseCI =
	 * document.fClassById.get(new Integer(bclid).toString()); // If such an
	 * object exists establish it as base class. if(baseCI!=null) { // Establish
	 * as base class. Since most classes indeed possess // at most one base
	 * class, this case will be treated somewhat // storage-optimized.
	 * if(nbcl==1) { baseclassInfo = baseCI; } else { if(baseclassInfoSet==null)
	 * baseclassInfoSet = new HashSet<ClassInfoEA>(nbcl);
	 * baseclassInfoSet.add(baseCI); } // Register with the subclasses of the
	 * base class. baseCI.subclassInfoSet.add(this); } } } //
	 * establishClassDerivationHierarchy()
	 */

	// Establish all class associations. This is an auxiliary initializing
	// method, which retrieves all associations (EA: Connectors) known to
	// the class and creates wrapper objects for them in case they have not
	// already been encountered from the other class end. All created
	// association objects are registered. Object creation established the
	// necessary links to source and target objects and properties.
	// Note that invocation of this method requires that all classes of the
	// model are already cached.
	public void establishAssociations() {
		// Find out about associations connected to the class
		// Only do this once; cache the results for subsequent use
		if (!connectorsAccessed) {
			conns = eaClassElement.GetConnectors();
			connectorsAccessed = true;
		}

		// check that this class has connectors
		if (conns != null) {

			// Enumerate connectors
			boolean known;
			int id;
			String connid;

			for (Connector conn : conns) {

				// only process "Association" connectors
				String type = conn.GetType();
				if (!type.equalsIgnoreCase("Association")
						&& !type.equalsIgnoreCase("Aggregation")) {
					continue;
				}
				// First find out whether the association has already been
				// processed from its other end. If so, discard.
				id = conn.GetConnectorID();
				connid = Integer.valueOf(id).toString();
				known = document.fAssociationById.containsKey(connid);
				if (known)
					continue;
				// First encounter: Create AssociationInfo wrapper and
				// properties linkage.
				AssociationInfoEA ai = new AssociationInfoEA(document, conn,
						id);
				// Register with global associations map, if relevant class
				// association
				if (ai.relevant)
					document.fAssociationById.put(connid, ai);
			}
		}
	}

	// Establish the roles attached to the class. This auxiliary initializing
	// method is called from the AssociationInfoEA Ctor to register a list of
	// roles as properties of the class.
	public void establishRoles(PropertyInfoEA pi) {
		registeredRoles.add(pi);
	}

	/** Inquire wrapped EA object */
	public org.sparx.Element getEaClassElement() {
		return eaClassElement;
	}

	public int getEaElementId() {
		return eaClassId;
	}

	/** Return EA model object. */
	public Model model() {
		return document;
	} // model()

	/** Return options and configuration object. */
	public Options options() {
		return document.options;
	} // options()

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return document.result;
	} // result()

	@Override
	public ClassInfo baseClass() {
		// Initialize
		int stsize = 0; // # of proper base candidates
		ClassInfo cir = null; // the base result
		int cat = category(); // category of this class
		// Check if the class has one of the acknowledged categories. If not
		// no bases classes will be reported.
		if (cat == Options.FEATURE || cat == Options.OBJECT
				|| cat == Options.DATATYPE || cat == Options.MIXIN
				|| cat == Options.UNION) {
			// Get hold of the available base classes
			TreeSet<ClassInfoEA> baseCIs = null;
			if (baseclassInfoSet != null)
				baseCIs = baseclassInfoSet;
			else if (baseclassInfo != null) {
				baseCIs = new TreeSet<ClassInfoEA>();
				baseCIs.add(baseclassInfo);
			}
			// Loop over base classes and select the GML-relevant one
			if (baseCIs != null) {
				for (ClassInfoEA baseCI : baseCIs) {
					// Get base class category
					int bcat = baseCI.category();
					// Needs to compatible and not a mixin. If not so,
					// we have an error
					if ((cat == bcat || bcat == Options.UNKNOWN)
							&& bcat != Options.MIXIN) {
						// Compatible select and count
						stsize++;
						cir = baseCI;
					} else if (bcat != Options.MIXIN) {

						// Ignore, if we accept supertypes that are not mixins
						// and we are a mixin
						if (cat == Options.MIXIN && matches(
								"rule-xsd-cls-mixin-classes-non-mixin-supertypes")) {

							// do nothing and ignore

							/*
							 * FIXME 2017-09-12 JE: Method baseClass() should be
							 * in ClassInfoImpl(). However, that we are matching
							 * on a specific target rule here is an issue.
							 * Everything in the XxxImpl classes should not
							 * depend on specific target rules, since
							 * transformations can produce models for multiple
							 * targets. Rules such as the one above should be
							 * general rules that apply for the whole processing
							 * chain.
							 */

						} else if (this.model().isInSelectedSchemas(this)) {
							// Not compatible and not mixin: An error
							MessageContext mc = document.result.addError(null,
									108, name());
							if (mc != null)
								mc.addDetail(null, 400, "Package",
										pkg().fullName());
							document.result.addDebug(null, 10003, name(),
									"" + cat, "!FALSE");
							document.result.addDebug(null, 10003, name(),
									"" + bcat, "!TRUE");
						} else {
							/*
							 * 2015-07-17 JE: So this is a class that violates
							 * multiple inheritance rules. However, it is
							 * outside the selected schemas. We could log a
							 * debug, info, or even warning message. However, we
							 * should not raise an error because creation of a
							 * complete GenericModel that also copies ISO
							 * classes would raise an error which would cause a
							 * unit test to fail.
							 */
						}
					}
				}
			}
			// Did we find more than one suitable base class? Which is
			// an error.
			if (stsize > 1) {

				if (this.model().isInSelectedSchemas(this)) {
					MessageContext mc = document.result.addError(null, 109,
							name());
					if (mc != null)
						mc.addDetail(null, 400, "Package", pkg().fullName());
				} else {
					/*
					 * 2015-07-17 JE: So this is a class that violates multiple
					 * inheritance rules. However, it is outside the selected
					 * schemas. We could log a debug, info, or even warning
					 * message. However, we should not raise an error because
					 * creation of a complete GenericModel that also copies ISO
					 * classes would raise an error which would cause a unit
					 * test to fail.
					 */
				}
			}
		}
		// Return, what we found
		return cir;
	} // baseClass()

	/**
	 * This is supposed to find out, whether the given category 'cat' applied in
	 * 'this' class complies to the categories of all its base classes. If at
	 * least one base class does not comply, 'false' is returned. Overloaded
	 * from supertype, because we here have more comfortable data structures
	 * available.
	 */
	@Override
	public boolean checkSupertypes(int cat) {
		// Prepare set of base classes
		TreeSet<ClassInfoEA> bcis = new TreeSet<ClassInfoEA>();
		if (baseclassInfo != null)
			bcis.add(baseclassInfo);
		else if (baseclassInfoSet != null)
			bcis = baseclassInfoSet;
		// Consider all baseclasses in turn, break as soon as a first non-
		// compliancy is detected.
		boolean res = true;
		for (ClassInfoEA bci : bcis) {
			// Get category of base class
			int bcicat = bci.category();
			// Find out about category compliance
			if (bcicat == Options.UNKNOWN) {
				// If base class category unknown, obtain from its base classes
				res = bci.checkSupertypes(cat);
			} else if (bcicat == Options.MIXIN) {
				// Ignore mixin base class
				continue;
			} else if (bcicat != cat) {
				// Not compliant: Reject
				res = false;
			}
			// We no longer need to look, if the result is non-compliant ...
			if (!res)
				break;
		}
		// Trace for debugging
		if (res)
			document.result.addDebug(null, 10003, name(), "" + cat, "TRUE");
		else
			document.result.addDebug(null, 10003, name(), "" + cat, "FALSE");
		// Return, what we found out
		return res;
	} // checkSupertypes()

	@Override
	public PackageInfo pkg() {
		return packageInfo;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#properties()
	 */
	@SuppressWarnings("unchecked")
	public SortedMap<StructuredNumber, PropertyInfo> properties() {
		validatePropertiesCache();
		return (TreeMap<StructuredNumber, PropertyInfo>) propertiesCache
				.clone();
	} // properties()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#property(java.lang.String)
	 */
	public PropertyInfo property(String name) {
		// Search in own properties
		validatePropertiesCache();
		for (PropertyInfo pi : propertiesCache.values()) {
			if (pi.name().equals(name))
				return pi;
		}
		// Go and search in base classes
		TreeSet<ClassInfoEA> bcis = new TreeSet<ClassInfoEA>();
		if (baseclassInfo != null)
			bcis.add(baseclassInfo);
		else if (baseclassInfoSet != null)
			bcis = baseclassInfoSet;
		for (ClassInfoEA bci : bcis) {
			PropertyInfo pi = bci.property(name);
			if (pi != null)
				return pi;
		}
		return null;
	} // property()

	// Validate stereotypes cache of the class. The stereotypes found are 1.
	// restricted to those defined within ShapeChange and 2. deprecated ones
	// are normalized to the lastest definitions.
	public void validateStereotypesCache() {

		if (stereotypesCache == null) {

			// Fetch stereotypes 'collection' ...
			String sts = eaClassElement.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");

			// Allocate cache
			stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(stereotypes, this);

			/*
			 * 2017-03-23 JE: Apparently when calling
			 * eaClassElement.GetStereotypeEx() the EA API does not return the
			 * stereotype of a class that has been created in EA as an element
			 * with type enumeration (which is different than a normal class).
			 * We explicitly add the stereotype "enumeration" for such elements.
			 * That will also help in case that the UML profile of an
			 * application schema did not define a stereotype for enumerations
			 * but still used EA elements of type enumeration (with the intent
			 * to treat them as enumerations). The "enumeration" stereotype is
			 * necessary when exporting a model without also exporting the
			 * category of a class, since then the class category must be
			 * established based upon the stereotype (and potentially existing
			 * XML Schema conversion rules) when importing an SCXML model.
			 */
			if (!stereotypesCache.contains("enumeration") && eaClassElement
					.GetType().equalsIgnoreCase("enumeration")) {
				stereotypesCache.add("enumeration");
			}
		}
	} // validateStereotypesCache()

	/** Provide the ids of all subclasses of this class. */
	public SortedSet<String> subtypes() {
		// Convert subclass object set to subclass id set.
		SortedSet<String> subids = new TreeSet<String>();
		for (ClassInfoEA sci : subclassInfoSet)
			subids.add(sci.id());
		return subids;
	} // subtypes()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#supertypes()
	 */
	public SortedSet<String> supertypes() {
		// Convert base class object set to base class id set.
		SortedSet<String> baseids = new TreeSet<String>();
		if (baseclassInfo != null)
			baseids.add(baseclassInfo.id());
		else if (baseclassInfoSet != null)
			for (ClassInfoEA bci : baseclassInfoSet)
				baseids.add(bci.id());
		return baseids;
	} // supertypes()

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!documentationAccessed
					&& descriptor == Descriptor.DOCUMENTATION) {

				documentationAccessed = true;

				String s = null;

				// Try EA notes if ea:notes is the source
				if (model().descriptorSource(Descriptor.DOCUMENTATION)
						.equals("ea:notes")) {
					s = eaClassElement.GetNotes();
					// Fix for EA7.5 bug
					if (s != null) {
						s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
					}
				}

				/*
				 * If result is empty, check if we can get the documentation
				 * from a dependency
				 */
				if (s == null || s.isEmpty()) {

					for (String cid : this.supplierIds()) {

						ClassInfoEA cix = document.fClassById.get(cid);

						if (cix != null) {
							if (cix.name().equalsIgnoreCase(this.name())
									&& cix.stereotype("featureconcept")) {
								s = cix.documentation();
								break;
							}
						}
					}
				}

				// If result is empty, check if we can get the documentation
				// from a
				// supertype with the same name (added for ELF/INSPIRE)
				if (s == null || s.isEmpty()) {

					HashSet<ClassInfoEA> sts = supertypesAsClassInfoEA();

					if (sts != null) {
						for (ClassInfoEA stci : sts) {
							if (stci.name().equals(this.name())) {
								s = stci.documentation();
								break;
							}
						}
					}
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
							.GUIDtoXML(eaClassElement.GetElementGUID());

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

					String a = eaClassElement.GetAlias();

					if (a != null && !a.isEmpty()) {
						ls.add(new LangString(options().internalize(a)));
						this.descriptors().put(descriptor, ls);
					}
				}
			}

		}

		return ls;
	}

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
	// // Try default first
	// Descriptors ls = super.documentationAll();
	//
	// if (ls.isEmpty()) {
	//
	// String s = null;
	//
	// // Try EA notes if ea:notes is the source
	// if (descriptorSource(Descriptor.DOCUMENTATION)
	// .equals("ea:notes")) {
	// s = eaClassElement.GetNotes();
	// // Fix for EA7.5 bug
	// if (s != null) {
	// s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
	// }
	// }
	//
	// // If result is empty, check if we can get the documentation
	// // from a
	// // dependency
	// if (s == null || s.isEmpty()) {
	//
	// for (Iterator<String> i = this.supplierIds().iterator(); i
	// .hasNext();) {
	//
	// String cid = i.next();
	//
	// ClassInfoEA cix = document.fClassById.get(cid);
	//
	// if (cix != null) {
	// if (cix.name().equalsIgnoreCase(this.name())
	// && cix.stereotype("featureconcept")) {
	// s = cix.documentation();
	// break;
	// }
	// }
	// }
	// }
	//
	// // If result is empty, check if we can get the documentation
	// // from a
	// // supertype with the same name (added for ELF/INSPIRE)
	// if (s == null || s.isEmpty()) {
	//
	// HashSet<ClassInfoEA> sts = supertypesAsClassInfoEA();
	//
	// if (sts != null) {
	// for (ClassInfoEA stci : sts) {
	// if (stci.name().equals(this.name())) {
	// s = stci.documentation();
	// break;
	// }
	// }
	// }
	// }
	//
	// // Assign what we got or "" ...
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

	/** Return model-unique id of class. */
	public String id() {
		return Integer.valueOf(eaClassId).toString();
	} // id()

	/** Obtain the name of the class. */
	public String name() {
		// Get the name obtained from the model
		if (eaName == null || eaName.equals("")) {
			// TBD: this can also be checked in the ctor
			eaName = id();
			MessageContext mc = document.result.addWarning(null, 100, "class",
					eaName);
			if (mc != null)
				mc.addDetail(null, 400, "Package", pkg().fullName());
		}
		return eaName;
	} // name()

	// @Override
	// public Descriptors aliasNameAll() {
	//
	// // Only retrieve/compute the alias once
	// // Cache the result for subsequent use
	// if (!aliasAccessed) {
	//
	// aliasAccessed = true;
	//
	// // Obtain alias name from default implementation
	// Descriptors ls = super.aliasNameAll();
	//
	// // If not present, obtain from EA model directly, if ea:alias is
	// // identified as the source
	// if (ls.isEmpty()
	// && descriptorSource(Descriptor.ALIAS).equals("ea:alias")) {
	//
	// String a = eaClassElement.GetAlias();
	//
	// if (a != null && !a.isEmpty()) {
	//
	// super.aliasName = new Descriptors(
	// new LangString(options().internalize(a)));
	// } else {
	// super.aliasName = new Descriptors();
	// }
	// }
	// }
	// return super.aliasName;
	// }

	// Validate tagged values cache, filtering on tagged values defined within
	// ShapeChange ...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {
			// Fetch tagged values collection
			Collection<TaggedValue> tvs = eaClassElement.GetTaggedValues();

			// ensure that there are tagged values
			if (tvs != null) {
				// Allocate cache
				int ntvs = tvs.GetCount();
				taggedValuesCache = options().taggedValueFactory(ntvs);
				// Copy tag-value-pairs, leave out non-ShapeChange stuff and
				// normalize deprecated tags.
				for (TaggedValue tv : tvs) {
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
	} // validateTaggedValuesCache()

	/** Set the tagged value for the tag given. */
	public void taggedValue(String tag, String value) {
		Collection<TaggedValue> cTV = eaClassElement.GetTaggedValues();
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

	/** Determine whether the class is tagged as being an abstract class */
	public boolean isAbstract() {
		return isAbstract;
	} // isAbstract()

	@Override
	public boolean isLeaf() {
		return isLeaf;
	} // isLeaf()

	// Validate constraints cache. This makes sure the constraints cache
	// contains all constraints ordered by their appearance in the class.
	// If constraints are disabled the cache is empty.
	private void validateConstraintsCache() {
		if (constraintsCache == null) {
			// Allocate cache
			constraintsCache = new Vector<Constraint>();
			// Constraints disabled?
			String check = document.options.parameter("checkingConstraints");
			if (check != null && check.equalsIgnoreCase("disabled"))
				return;

			// Constraints for this class category irrelevant?
			if (!document.options.isClassTypeToCreateConstraintsFor(category()))
				return;

			// Constraints from selected schemas only?
			if (document.options.isLoadConstraintsForSelectedSchemasOnly()
					&& !document.isInSelectedSchemas(this)) {
				return;
			}

			// Filter map for inheritance and overriding by name
			HashMap<String, Constraint> namefilter = new HashMap<String, Constraint>();
			// Access EA constraints data
			Collection<org.sparx.Constraint> constrs = eaClassElement
					.GetConstraints();
			// Determine constraint types to be parsed as OCL
			String oclTypes = document.options
					.parameter("oclConstraintTypeRegex");
			// Determine constraint types to be parsed as FOL
			String folTypes = document.options
					.parameter("folConstraintTypeRegex");
			// Enumerate all constraints found
			// Ensure that there are constraints before continuing
			if (constrs != null) {
				for (org.sparx.Constraint constr : constrs) {
					// Wrap into constraint object
					String type = constr.GetType();
					Constraint oc;
					if (oclTypes.length() > 0 && type.matches(oclTypes)) {
						// 100422/re removed: &&
						// !encodingRule("xsd").equals(Options.ISO19136_2007_INSPIRE)
						OclConstraintEA ocl = new OclConstraintEA(document,
								this, constr);
						if (ocl.syntaxTree() == null)
							// Text constraint is a fallback in case of parsing
							// issues
							oc = new TextConstraintEA(document, this, constr);
						else
							oc = ocl;

					} else if (folTypes != null && folTypes.length() > 0
							&& type.matches(folTypes)) {

						/*
						 * only sets up the textual information; parsing is done
						 * during model postprocessing - see
						 * ModelImpl.postprocessFolConstraints()
						 */
						oc = new FolConstraintEA(document, this, constr);

					} else {
						oc = new TextConstraintEA(document, this, constr);
					}
					// Collect in cache
					constraintsCache.add(oc);
					// If the constraint has a name, add it to the filter which
					// blocks inheritance of constraints
					String conam = oc.name();
					if (conam != null && conam.length() > 0)
						namefilter.put(conam, oc);
				}
			}

			/*
			 * Fetch constraints from super-classes. Override by name.
			 */

			/*
			 * JE: replaced this code with code (see below) that directly
			 * accesses the supertype objects, instead of first getting all
			 * their IDs and then looking the objects up in the model.
			 */

			// HashSet<String> stids = supertypes();
			// if (stids != null) {
			// for (String stid : stids) {
			// ClassInfo stci = model().classById(stid);
			// Vector<Constraint> stcos = null;
			// if (stci != null)
			// stcos = stci.constraints();
			// if (stcos != null) {
			// for (Constraint stco : stcos) {
			// String nam = stco == null ? null : stco.name();
			// if(nam!=null && nam.length()>0 && namefilter.containsKey(nam))
			// continue;
			// constraintsCache.add(stco);
			// }
			// }
			// }
			// }
			HashSet<ClassInfoEA> sts = supertypesAsClassInfoEA();
			if (sts != null) {
				for (ClassInfoEA stci : sts) {
					Vector<Constraint> stcos = null;
					if (stci != null)
						stcos = stci.constraints();
					if (stcos != null) {
						for (Constraint stco : stcos) {
							String nam = stco == null ? null : stco.name();
							if (nam != null && nam.length() > 0
									&& namefilter.containsKey(nam))
								continue;

							// Is the context of stco still the supertype, or
							// should it not be this (ClassInfoEA)?
							constraintsCache.add(stco);
						}
					}
				}
			}
		}
	}

	private HashSet<ClassInfoEA> supertypesAsClassInfoEA() {
		// Create base class object set
		HashSet<ClassInfoEA> baseClasses = new HashSet<ClassInfoEA>(1);
		if (baseclassInfo != null)
			baseClasses.add(baseclassInfo);
		else if (baseclassInfoSet != null)
			for (ClassInfoEA bci : baseclassInfoSet)
				baseClasses.add(bci);
		return baseClasses;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#constraints()
	 */
	public Vector<Constraint> constraints() {
		validateConstraintsCache();
		return constraintsCache;
	}

	// Validate properties cache. This makes sure the property cache contains
	// all properties ordered by their appearance in the class.
	private void validatePropertiesCache() {

		if (propertiesCache == null) {

			// Allocate the cache
			propertiesCache = new TreeMap<StructuredNumber, PropertyInfo>();

			// Load attributes ...
			Collection<Attribute> attrs = eaClassElement.GetAttributes();

			// Ensure that there are attributes before continuing
			if (attrs != null) {
				for (Attribute attr : attrs) {
					// Pick public attributes if this has been requested
					if (document.options.parameter("publicOnly")
							.equals("true")) {
						String vis = attr.GetVisibility();
						if (!vis.equalsIgnoreCase("Public")) {
							continue;
						}
					}
					// Create the property object.
					PropertyInfoEA pi = new PropertyInfoEA(document, this,
							attr);
					// Check sequence number on duplicates
					PropertyInfo piTemp = propertiesCache
							.get(pi.sequenceNumber());
					if (piTemp != null) {
						int cat = category();
						if (cat != Options.ENUMERATION
								&& cat != Options.CODELIST
								&& !pi.sequenceNumber
										.equals(new StructuredNumber(
												Integer.MIN_VALUE))) {
							MessageContext mc = document.result.addError(null,
									107, pi.name(), name(), piTemp.name());
							if (mc != null)
								mc.addDetail(null, 400, "Package",
										pkg().fullName());
						}
					}
					// Add to properties cache
					propertiesCache.put(pi.sequenceNumber(), pi);
				}
			}

			// Load roles ...
			for (PropertyInfoEA pi : registeredRoles) {
				// Check sequence number on duplicates
				PropertyInfo piTemp = propertiesCache.get(pi.sequenceNumber());
				if (piTemp != null) {
					MessageContext mc = document.result.addError(null, 107,
							pi.name(), name(), piTemp.name());
					if (mc != null)
						mc.addDetail(null, 400, "Package", pkg().fullName());
				}
				// Add to properties cache
				propertiesCache.put(pi.sequenceNumber(), pi);
			}
		}
	} // validatePropertiesCache()

	// Validate operations cache. This makes sure the operations cache contains
	// all Operations ordered by their appearance in the class.
	private void validateOperationsCache() {
		if (operationsCache == null) {
			// Allocate the cache
			operationsCache = new TreeMap<Integer, OperationInfo>();
			// Load methods ...
			Collection<Method> meths = eaClassElement.GetMethods();
			int i = 0;
			// Ensure that there are methods before continuing
			if (meths != null) {
				for (Method meth : meths) {
					// Create the operation object.
					OperationInfoEA oi = new OperationInfoEA(document, this,
							meth);
					// Drop in cache
					// operationsCache.put(oi.eaMethod.GetPos(), oi); <-- does
					// not work!
					operationsCache.put(++i, oi);
				}
			}
		}
	} // validateOperationsCache()

	/**
	 * Find the operation identified by its name and the types of its parameters
	 * in this class or (if not present there) recursively in its base classes.
	 */
	public OperationInfo operation(String name, String[] types) {
		// Make sure operations are loaded
		validateOperationsCache();
		// Search in the class itself
		search_operation: for (OperationInfo oi : operationsCache.values()) {
			if (oi.name().equals(name)) {
				// Success if types shall not be compared
				if (types == null)
					return oi;
				// Check number of parameters
				if (types.length != oi.parameterCount())
					continue;
				// Check type strings
				SortedMap<Integer, String> ptypes = oi.parameterTypes();
				for (int i = 0; i < types.length; i++) {
					if (types[i].equals("*"))
						continue;
					if (!ptypes.get(i + 1).equals(types[i]))
						continue search_operation;
				}
				// All types found matching
				return oi;
			}
		}
		// Go and search in base classes
		TreeSet<ClassInfoEA> bcis = new TreeSet<ClassInfoEA>();
		if (baseclassInfo != null)
			bcis.add(baseclassInfo);
		else if (baseclassInfoSet != null)
			bcis = baseclassInfoSet;
		for (ClassInfoEA bci : bcis) {
			OperationInfo oi = bci.operation(name, types);
			if (oi != null)
				return oi;
		}
		return null;
	}

	public AssociationInfo isAssocClass() {
		// Only retrieve/compute the association once
		// Cache the result for subsequent use
		if (!isAssocClassAccessed) {

			isAssocClassAccessed = true;

			if (eaClassElement.GetSubtype() == 17
					&& !eaClassElement.MiscData(3).isEmpty()) {
				assoc = document.fAssociationById
						.get(eaClassElement.MiscData(3));
			}
		}
		return assoc;
	}

	/**
	 * @return set of ids of classes this class depends on; can be empty but not
	 *         <code>null</code>
	 */
	protected TreeSet<String> supplierIds() {

		// Only retrieve/compute supplierIds once
		// Cache the results for subsequent use
		if (supplierIds == null) {

			// Prepare set to return
			supplierIds = new TreeSet<String>();
			// Ask EA for the connectors attached to the package object and loop
			// over the connectors returned
			// Only compute them once for the whole class
			if (!connectorsAccessed) {
				conns = eaClassElement.GetConnectors();
				connectorsAccessed = true;
			}
			// Ensure that there are connectors before continuing
			if (conns != null) {
				for (Connector conn : conns) {
					// Single out dependency connectors
					String type = conn.GetType();
					if (type.equals("Dependency")) {
						// From the dependency grab the id of the supplier
						int suppId = conn.GetSupplierID();
						String suppIdS = Integer.toString(suppId);
						/*
						 * Since all connectors are delivered from both objects
						 * at the connector ends, we have to make sure it is not
						 * accidently us, which we found.
						 */
						if (suppId == eaClassId)
							continue;
						/*
						 * Now, this is an element id, not a package id. So we
						 * have to identify the package, which owns the element
						 * addressed.
						 */
						ClassInfoEA suppClass = (ClassInfoEA) document.fClassById
								.get(suppIdS);

						if (suppClass != null) {
							// From this only the id is required
							String suppPackId = suppClass.id();
							if (suppPackId != null)
								supplierIds.add(suppPackId);

						} else {
							// package of supplier class has not been loaded,
							// dismiss the supplier
						}
					}
				}
			}
		}
		return supplierIds;
	} // supplierIds()
		//
		// @Override
		// public Descriptors descriptors() {
		//
		// // Obtain descriptors from default implementation
		// Descriptors ls = super.descriptors();
		// // If not present, obtain from EA model directly
		// if (ls.isEmpty() && descriptorSource(Descriptor.GLOBALIDENTIFIER)
		// .equals("ea:guidtoxml")) {
		//
		// String gi = document.repository.GetProjectInterface()
		// .GUIDtoXML(eaClassElement.GetElementGUID());
		//
		// super.globalIdentifier = new Descriptors(
		// new LangString(options().internalize(gi)));
		// }
		// return super.globalIdentifier;
		// }
}
