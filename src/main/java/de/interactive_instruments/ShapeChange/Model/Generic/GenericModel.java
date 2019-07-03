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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Generic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Constraint.ModelElmtContextType;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ModelImpl;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.ConstraintContentHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericAssociationContentHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericClassContentHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericModelContentHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericModelErrorHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericPackageContentHandler;
import de.interactive_instruments.ShapeChange.Model.Generic.reader.GenericPropertyContentHandler;
import de.interactive_instruments.ShapeChange.SBVR.Sbvr2FolParser;
import de.interactive_instruments.ShapeChange.SBVR.SbvrConstants;

/**
 * @author echterhoff
 * 
 */
public class GenericModel extends ModelImpl implements MessageSource {

	protected static class ConstraintComparators {

		public static Comparator<Constraint> NAME = new Comparator<Constraint>() {
			@Override
			public int compare(Constraint o1, Constraint o2) {
				return o1.name().compareTo(o2.name());
			}
		};
	}

	protected Options options = null;
	protected ShapeChangeResult result = null;

	protected String characterEncoding = null;
	protected boolean loadingFromScxml = false;
	// protected Model model = null;

	protected Map<String, GenericPropertyInfo> genPropertiesById = new HashMap<String, GenericPropertyInfo>();

	protected Map<String, GenericAssociationInfo> genAssociationInfosById = new HashMap<String, GenericAssociationInfo>();
	protected Map<String, GenericClassInfo> genClassInfosById = new HashMap<String, GenericClassInfo>();
	protected Map<String, GenericClassInfo> genClassInfosByName = new HashMap<String, GenericClassInfo>();
	protected Map<String, GenericPackageInfo> genPackageInfosById = new HashMap<String, GenericPackageInfo>();

	public enum PropertyCopyPositionIndicator {
		/**
		 * Indicates that properties copied to a class shall be placed at the
		 * top of the sequence of existing properties.
		 */
		PROPERTY_COPY_TOP,
		/**
		 * Indicates that properties copied to a class shall be merged into the
		 * sequence of existing properties according to their sequence number.
		 */
		PROPERTY_COPY_INSEQUENCE,
		/**
		 * Indicates that properties copied to a class shall be placed at the
		 * bottom of the sequence of existing properties.
		 */
		PROPERTY_COPY_BOTTOM
	}

	/**
	 * Identifies different behaviors for situations in which a property is
	 * intended to be copied to a class but another property with the same name
	 * already exists in that class.
	 * 
	 * @author Johannes Echterhoff
	 * 
	 */
	public enum PropertyCopyDuplicatBehaviorIndicator {
		/**
		 * Indicates that the copy shall be ignored. The existing property with
		 * the same name is kept.
		 * 
		 * NOTE: this ignores the isRestriction setting in the existing
		 * property.
		 */
		IGNORE,
		/**
		 * Indicates that the copy shall be ignored. The existing property with
		 * the same name is kept.
		 * 
		 * NOTE: In case that the existing property is a restriction, it is set
		 * to not being a restriction.
		 */
		IGNORE_UNRESTRICT,
		/**
		 * Indicates that the copy shall be added to the content model,
		 * resulting in two properties with the same name.
		 */
		ADD,
		/**
		 * Indicates that the copy shall overwrite the existing property with
		 * the same name.
		 */
		OVERWRITE
	}

	/**
	 * Constructor used when reading the model during the initialise(...)
	 * method.
	 */
	public GenericModel() {

	}

	public GenericModel(Model model) {

		this.options = model.options();
		this.result = model.result();

		// this.model = model;

		this.characterEncoding = model.characterEncoding();

		for (PackageInfo piToCreate : model.packages()) {

			// process the schema, recursively drilling down to all child
			// packages
			this.createGenericPackageInfo(piToCreate, this);

		}

		// update references for owner and rootPackage
		for (PackageInfo pi : model.packages()) {

			GenericPackageInfo genPi = this.genPackageInfosById.get(pi.id());

			if (genPi != null) {

				if (pi.owner() != null) {
					GenericPackageInfo owner = this.genPackageInfosById
							.get(pi.owner().id());
					genPi.setOwner(owner);
				}
			}
		}

		// create and set classes for each package
		for (GenericPackageInfo gpi : genPackageInfosById.values()) {

			// look up package in original model
			PackageInfo pi = model.packageById(gpi.id());

			// create set with all classes in the package and its child packages
			SortedSet<ClassInfo> packageClassInfos = model.classes(pi);

			// remove all classes from the set that belong to one of its child
			// packages
			for (PackageInfo childPackage : pi.containedPackages()) {
				packageClassInfos.removeAll(model.classes(childPackage));
			}

			// now create GenericClassInfos for all selected schema
			// packages (and sub packages)
			SortedSet<GenericClassInfo> genericClassInfos = new TreeSet<GenericClassInfo>();
			for (ClassInfo ci : packageClassInfos) {

				GenericClassInfo genCi = new GenericClassInfo(this, ci.id(),
						ci.name(), ci.category());

				// set properties required by Info interface

				genCi.setTaggedValues(ci.taggedValuesAll(), false);
				genCi.setStereotypes(ci.stereotypes());

				genCi.setDescriptors(ci.descriptors().createCopy());
				genCi.setProfiles(ci.profiles().createCopy());

				// set properties required by ClassInfo interface
				genCi.setPkg(ci.pkg());
				genCi.setIsAbstract(ci.isAbstract());
				genCi.setIsLeaf(ci.isLeaf());
				genCi.setAssocInfo(ci.isAssocClass());
				genCi.setSupertypes(copy(ci.supertypes()));
				genCi.setSubtypes(copy(ci.subtypes()));
				genCi.setProperties(ci.properties());
				genCi.setConstraints(copy(ci.constraints()));

				genCi.setDiagrams(ci.getDiagrams());
				genCi.setLinkedDocument(ci.getLinkedDocument());

				genericClassInfos.add(genCi);

				this.register(genCi);
			}

			gpi.setClasses(genericClassInfos);
		}

		// create properties for all GenericClassInfos identified before
		for (GenericClassInfo genCi : genClassInfosById.values()) {

			SortedMap<StructuredNumber, PropertyInfo> classProperties = genCi
					.properties();

			if (classProperties == null) {
				continue;
			}

			for (PropertyInfo pi : classProperties.values()) {

				// TBD: class properties should be unique, but just in case
				if (genPropertiesById.containsKey(pi.id())) {

					GenericPropertyInfo existingGenPi = genPropertiesById
							.get(pi.id());

					result.addWarning(this, 30318, pi.id(), pi.name(),
							genCi.name(), existingGenPi.name() + " / "
									+ existingGenPi.inClass().name());

					continue;
				}

				GenericPropertyInfo genPi = new GenericPropertyInfo(this,
						pi.id(), pi.name());

				// set remaining properties required by Info interface
				genPi.setTaggedValues(pi.taggedValuesAll(), false);
				genPi.setStereotypes(pi.stereotypes());

				genPi.setDescriptors(pi.descriptors().createCopy());
				genPi.setProfiles(pi.profiles().createCopy());

				// set remaining properties required by PropertyInfo interface

				genPi.setDerived(pi.isDerived());
				genPi.setReadOnly(pi.isReadOnly());
				genPi.setAttribute(pi.isAttribute());
				genPi.copyTypeInfo(pi.typeInfo());
				genPi.setNavigable(pi.isNavigable());
				genPi.setOrdered(pi.isOrdered());
				genPi.setUnique(pi.isUnique());
				genPi.setOwned(pi.isOwned());
				genPi.setComposition(pi.isComposition());
				genPi.setAggregation(pi.isAggregation());
				Multiplicity mult = pi.cardinality();
				Multiplicity multC = new Multiplicity();
				multC.maxOccurs = mult.maxOccurs;
				multC.minOccurs = mult.minOccurs;
				genPi.setCardinality(multC);
				genPi.setInitialValue(pi.initialValue());
				genPi.setInlineOrByReference(pi.inlineOrByReference());
				genPi.setReverseProperty(pi.reverseProperty());
				genPi.setInClass(pi.inClass());
				StructuredNumber strucNum = pi.sequenceNumber();

				/*
				 * We update the "sequenceNumber" tagged value in the copy to
				 * cover the case that the tagged value of the original property
				 * does not reflect the sequence number (which can happen if the
				 * tagged value is undefined in the model and a sequence number
				 * then has automatically been assigned - without then also
				 * updating the sequence number).
				 */
				genPi.setSequenceNumber(copy(strucNum), true);
				genPi.setConstraints(copy(pi.constraints()));
				genPi.setAssociation(pi.association());
				genPi.setRestriction(pi.isRestriction());
				genPi.setNilReasonAllowed(pi.nilReasonAllowed());

				genPropertiesById.put(genPi.id, genPi);
			}
		}

		// create associations, looking for available associations in all
		// previously identified properties
		Map<String, GenericPropertyInfo> additionalPropsFromAssociationEnds = new HashMap<String, GenericPropertyInfo>();

		for (GenericPropertyInfo genPi : genPropertiesById.values()) {

			AssociationInfo ai = genPi.association();

			// association may already have been created in a previous loop
			if (ai == null || genAssociationInfosById.containsKey(ai.id()))
				continue;

			GenericAssociationInfo genAi = createCopy(ai, ai.id());

			// ensure that generic representations of association end properties
			// are created as well
			if (!genPropertiesById.containsKey(ai.end1().id())
					&& !additionalPropsFromAssociationEnds
							.containsKey(ai.end1().id())) {

				GenericPropertyInfo genEnd1Prop = this
						.createAssociationEndCopy(ai.end1());

				additionalPropsFromAssociationEnds.put(genEnd1Prop.id(),
						genEnd1Prop);
			}
			if (!genPropertiesById.containsKey(ai.end2().id())
					&& !additionalPropsFromAssociationEnds
							.containsKey(ai.end2().id())) {

				GenericPropertyInfo genEnd2Prop = this
						.createAssociationEndCopy(ai.end2());

				additionalPropsFromAssociationEnds.put(genEnd2Prop.id(),
						genEnd2Prop);
			}

			genAssociationInfosById.put(genAi.id(), genAi);
		}

		for (GenericPropertyInfo additionalProp : additionalPropsFromAssociationEnds
				.values()) {
			genPropertiesById.put(additionalProp.id(), additionalProp);
		}

		// finally, for all GenericXXX objects, fix all references to classes,
		// properties, associations etc to use the GenericXxx ones if available

		// update information in GenericPackageInfo
		for (GenericPackageInfo gpi : genPackageInfosById.values()) {

			// PackageInfo owner = gpi.owner();
			// gpi.setOwner(updatePackageInfo(owner));
			//
			// PackageInfo rootPackage = gpi.rootPackage();
			// gpi.setRootPackage(updatePackageInfo(rootPackage));

			SortedSet<GenericPackageInfo> childPi_update = new TreeSet<GenericPackageInfo>();
			for (PackageInfo genChild : gpi.containedPackages()) {
				childPi_update.add(updatePackageInfo(genChild));
			}
			gpi.setContainedPackages(childPi_update);

			SortedSet<GenericClassInfo> classes_update = new TreeSet<GenericClassInfo>();
			for (GenericClassInfo cI : gpi.getClasses()) {
				classes_update.add(updateClassInfo(cI));
			}
			gpi.setClasses(classes_update);

		}

		// update information in GenericClassInfo
		for (GenericClassInfo gci : genClassInfosById.values()) {

			PackageInfo pkg = gci.pkg();
			gci.setPkg(updatePackageInfo(pkg));

			AssociationInfo assoClass = gci.isAssocClass();
			gci.setAssocInfo(updateAssociationInfo(assoClass));

			SortedMap<StructuredNumber, PropertyInfo> properties = gci
					.properties();
			SortedMap<StructuredNumber, PropertyInfo> propertiesC = new TreeMap<StructuredNumber, PropertyInfo>();

			/*
			 * 2014-12-05 JE: the following three lines of code had a very weird
			 * behavior. origPi was null in some cases (T8_FeatureType2
			 * [property with Integer.MIN_VALUE as sequence number] of ArcGIS
			 * workspace target UnitTest model) even though during debugging one
			 * could clearly see that the properties TreeMap had two values that
			 * were not null. I couldn't find out why null was returned.
			 * Eventually, I modified the code to iterate through the list of
			 * properties directly.
			 */
			// for (StructuredNumber sn : properties.keySet()) {
			//
			// PropertyInfo origPi = properties.get(sn);
			//
			// PropertyInfo pValue = updatePropertyInfo(properties.get(sn));

			for (PropertyInfo origPi : properties.values()) {

				PropertyInfo pValue = updatePropertyInfo(origPi);

				// TBD: pValue.sequenceNumber() has been updated for
				// GenericPropertyInfos; if other classes relied on the identity
				// of the key (a StructuredNumber) in the properties TreeMap
				// then there could be side-effects when using the new
				// StructuredNumber object as key for GenericPropertyInfos

				if (pValue == null) {

					result.addError(this, 30321, origPi.name(), gci.name());

				} else if (pValue instanceof GenericPropertyInfo
						&& !pValue.inClass().id().equals(gci.id())) {

					if (options().isAIXM()) {
						/*
						 * if we are dealing with an AIXM schema we are more
						 * lenient because the AIXM core schema may have already
						 * been merged with one or more extension schemas
						 */
						propertiesC.put(pValue.sequenceNumber(), pValue);

					} else {
						result.addError(this, 30319, origPi.name(),
								origPi.inClass().name(),
								pValue.inClass().name());
					}

				} else {

					propertiesC.put(pValue.sequenceNumber(), pValue);
				}
			}
			gci.setProperties(propertiesC);

			List<Constraint> constraints = gci.constraints();
			gci.setConstraints(updateContext(constraints, gci));

			// modification of operations is not supported right now
		}

		// update information in GenericPropertyInfo
		for (GenericPropertyInfo gpi : genPropertiesById.values()) {

			PropertyInfo reverseProperty = gpi.reverseProperty();
			gpi.setReverseProperty(updatePropertyInfo(reverseProperty));

			ClassInfo inClass = gpi.inClass();
			gpi.setInClass(updateClassInfo(inClass));

			List<Constraint> constraints = gpi.constraints();
			gpi.setConstraints(updateContext(constraints, gpi));

			AssociationInfo association = gpi.association();
			gpi.setAssociation(updateAssociationInfo(association));

		}

		// update information in GenericAssociationInfo
		for (GenericAssociationInfo gai : genAssociationInfosById.values()) {

			updateAssociationInfoContent(gai);
		}
	}

	private GenericPropertyInfo createAssociationEndCopy(
			PropertyInfo assocEnd) {

		GenericPropertyInfo copy = this.createCopy(assocEnd, assocEnd.id());
		// /*
		// * Ensure that tagged value "sequenceNumber" of copy reflects the
		// value
		// * from its sequenceNumber field - covering the case that there was a
		// * mismatch in the input model
		// */
		// copy.setTaggedValue("sequenceNumber",
		// copy.sequenceNumber().getString(),
		// false);

		return copy;
	}

	/**
	 * Puts the given class info into the genClassInfosById and
	 * genClassInfosByName maps.
	 * 
	 * @param genCi
	 */
	public void register(GenericClassInfo genCi) {
		this.genClassInfosById.put(genCi.id, genCi);
		this.genClassInfosByName.put(genCi.name, genCi);
	}

	/**
	 * Updates the context (model element and class (for ocl constraints) of the
	 * given constraints.
	 * 
	 * @param constraints
	 * @param owner
	 * @return
	 */
	private Vector<Constraint> updateContext(List<Constraint> constraints,
			Info owner) {

		Vector<Constraint> results = new Vector<Constraint>();

		if (constraints != null) {

			for (Constraint con : constraints) {

				if (!(con instanceof GenericFolConstraint)
						&& !(con instanceof GenericTextConstraint)
						&& !(con instanceof GenericOclConstraint)) {

					result.addError(this, 30300, con.name(),
							con.contextModelElmt().name());
					continue;
				}

				Info contextModelElement = con.contextModelElmt();

				if (contextModelElement == null) {

					StringBuffer sb = new StringBuffer();
					sb.append(
							"(GenericModel) contextModelElement for constraint named '"
									+ con.name() + "' [" + con.text()
									+ "] is null.");

					if (owner instanceof PropertyInfo) {

						PropertyInfo pi = (PropertyInfo) owner;

						sb.append(" Omitting constraint in property '"
								+ pi.name() + "' of class '"
								+ pi.inClass().name() + "'.");

					} else if (owner instanceof ClassInfo) {

						ClassInfo ci = (ClassInfo) owner;

						sb.append(" Omitting constraint in class '" + ci.name()
								+ "'.");

					} else {

						sb.append(" Omitting constraint in Info class '"
								+ owner.name() + "'.");
					}

					result.addError(this, 30301, sb.toString());
					continue;
				}

				if (con instanceof FolConstraint) {

					GenericFolConstraint genCon = (GenericFolConstraint) con;

					if (genCon.contextModelElmtType().equals(
							Constraint.ModelElmtContextType.ATTRIBUTE)) {

						GenericPropertyInfo genPi = this.genPropertiesById
								.get(contextModelElement.id());

						if (genPi == null) {
							result.addError(this, 30325, con.name(),
									contextModelElement.name());
							continue;
						}

						genCon.setContextModelElmt(genPi);
						results.add(genCon);

					} else if (genCon.contextModelElmtType().equals(

							Constraint.ModelElmtContextType.CLASS)) {

						GenericClassInfo genCi = this.genClassInfosById
								.get(contextModelElement.id());

						if (genCi == null) {
							result.addError(this, 30326, con.name(),
									contextModelElement.name());
							continue;
						}

						genCon.setContextModelElmt(genCi);
						results.add(genCon);

					} else {
						result.addWarning(this, 30304,
								genCon.contextModelElmtType().name());
					}

				} else if (con instanceof TextConstraint) {

					GenericTextConstraint genCon = (GenericTextConstraint) con;

					if (genCon.contextModelElmtType().equals(
							Constraint.ModelElmtContextType.ATTRIBUTE)) {

						GenericPropertyInfo genPi = this.genPropertiesById
								.get(contextModelElement.id());

						if (genPi == null) {
							result.addError(this, 30302, con.name(),
									contextModelElement.name());
							continue;
						}

						genCon.setContextModelElmt(genPi);
						results.add(genCon);

					} else if (genCon.contextModelElmtType().equals(

							Constraint.ModelElmtContextType.CLASS)) {

						GenericClassInfo genCi = this.genClassInfosById
								.get(contextModelElement.id());

						if (genCi == null) {
							result.addError(this, 30303, con.name(),
									contextModelElement.name());
							continue;
						}

						genCon.setContextModelElmt(genCi);
						results.add(genCon);

					} else {
						result.addWarning(this, 30304,
								genCon.contextModelElmtType().name());
					}

				} else if (con instanceof OclConstraint) {

					GenericOclConstraint genCon = (GenericOclConstraint) con;

					if (genCon.contextModelElmtType().equals(
							Constraint.ModelElmtContextType.ATTRIBUTE)) {

						GenericPropertyInfo context = this.genPropertiesById
								.get(contextModelElement.id());

						if (context == null) {

							if (genCon.contextClass() == null) {
								result.addError(this, 30305, con.name(),
										contextModelElement.name());
							} else {
								result.addError(this, 30306, con.name(),
										contextModelElement.name(),
										genCon.contextClass().name());
							}

							continue;
						}

						ClassInfo inClass = context.inClass();

						genCon.setContext(updateClassInfo(inClass), context);
						results.add(genCon);

					} else if (genCon.contextModelElmtType()
							.equals(Constraint.ModelElmtContextType.CLASS)) {

						GenericClassInfo context = this.genClassInfosById
								.get(contextModelElement.id());

						if (context == null) {

							if (genCon.contextClass() == null) {
								result.addError(this, 30307, con.name(),
										contextModelElement.name());
							} else {
								result.addError(this, 30308, con.name(),
										contextModelElement.name(),
										genCon.contextClass().name());
							}

							continue;
						}

						genCon.setContext(context, context);
						results.add(genCon);

					} else {
						result.addWarning(this, 30304,
								genCon.contextModelElmtType().name());
					}

				} else {
					result.addWarning(this, 30309, con.getClass().getName());
				}
			}
		}
		return results;
	}

	public Vector<Constraint> copy(List<Constraint> constraints) {

		Vector<Constraint> conCopies = null;

		if (constraints != null) {
			conCopies = new Vector<Constraint>();

			for (Constraint con : constraints) {

				if (con instanceof FolConstraint) {

					GenericFolConstraint genCon = new GenericFolConstraint(
							(FolConstraint) con);
					conCopies.add(genCon);

				} else if (con instanceof TextConstraint) {

					GenericTextConstraint genCon = new GenericTextConstraint(
							(TextConstraint) con);
					conCopies.add(genCon);

				} else if (con instanceof OclConstraint) {

					GenericOclConstraint genCon = new GenericOclConstraint(
							(OclConstraint) con);
					conCopies.add(genCon);

				} else {
					result.addWarning(this, 30309, con.getClass().getName());
				}

			}
		}

		return conCopies;
	}

	/**
	 * Replaces model objects (properties and classes used in the association)
	 * with generic ones if they exist. Matching generic model objects are
	 * identified via their id (if the id is the same, then the object is
	 * replaced with the generic one).
	 * 
	 * @param gai
	 */
	private void updateAssociationInfoContent(GenericAssociationInfo gai) {

		PropertyInfo end1 = gai.end1();
		gai.setEnd1(updatePropertyInfo(end1));

		PropertyInfo end2 = gai.end2();
		gai.setEnd2(updatePropertyInfo(end2));

		ClassInfo assocClass = gai.assocClass();
		gai.setAssocClass(updateClassInfo(assocClass));
	}

	/**
	 * @param strucNum
	 * @return
	 */
	private StructuredNumber copy(StructuredNumber strucNum) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strucNum.components.length - 1; i++) {
			sb.append(strucNum.components[i] + ".");
		}
		sb.append(strucNum.components[strucNum.components.length - 1]);
		StructuredNumber res = new StructuredNumber(sb.toString());
		return res;
	}

	/**
	 * Updates/fixes references to PackageInfos.
	 * 
	 * @param pIn
	 *                PackageInfo for which to search an update; may be
	 *                <code>null</code>
	 * @return a GenericPackageInfo with the same id as the given PackageInfo,
	 *         if it exists, otherwise the given PackageInfo
	 */
	private GenericPackageInfo updatePackageInfo(PackageInfo pIn) {
		if (pIn == null)
			return null;
		else
			return genPackageInfosById.get(pIn.id());

	}

	/**
	 * Updates/fixes references to ClassInfos.
	 * 
	 * @param cIn
	 *                ClassInfo for which to search an update; may be
	 *                <code>null</code>
	 * @return a GenericClassInfo with the same id as the given ClassInfo, if it
	 *         exists, otherwise the given ClassInfo
	 */
	private GenericClassInfo updateClassInfo(ClassInfo cIn) {
		if (cIn == null)
			return null;
		else
			return genClassInfosById.get(cIn.id());

	}

	/**
	 * Updates/fixes references to PropertyInfos.
	 * 
	 * @param pIn
	 *                PropertyInfo for which to search an update; may be
	 *                <code>null</code>
	 * @return a GenericPropertyInfo with the same id as the given PropertyInfo,
	 *         if it exists, otherwise the given PropertyInfo
	 */
	private PropertyInfo updatePropertyInfo(PropertyInfo pIn) {
		if (pIn == null)
			return null;
		else if (genPropertiesById.containsKey(pIn.id())) {
			return genPropertiesById.get(pIn.id());
		} else
			return pIn;
	}

	public void addClass(GenericClassInfo genCi) {

		this.genClassInfosById.put(genCi.id(), genCi);

		this.genClassInfosByName.put(genCi.name(), genCi);

		if (!(genCi.pkg() instanceof GenericPackageInfo)) {
			result.addError(this, 30310, genCi.pkg().name(), genCi.name());
			return;
		}

		// TODO package should already exist
		/*
		 * NOTE for cast: the cast should be safe, because genCi is a
		 * GenericClassInfo and thus the package it belongs to is a
		 * GenericPackageInfo - this is true after the GenericModel has been
		 * constructed.
		 */
		this.genPackageInfosById.put(genCi.pkg().id(),
				(GenericPackageInfo) genCi.pkg());

		((GenericPackageInfo) genCi.pkg()).addClass(genCi);

		if (genCi.isAssocClass() != null) {
			/*
			 * NOTE for cast: the cast should be safe, because genCi is a
			 * GenericClassInfo and would not have been established as
			 * association class if it did not belong to a
			 * GenericAssociationInfo belongs to a GenericClassInfo
			 */
			this.genAssociationInfosById.put(genCi.isAssocClass().id(),
					(GenericAssociationInfo) genCi.isAssocClass());
		}

		if (genCi.properties() != null && genCi.properties().size() > 0) {

			for (PropertyInfo pi : genCi.properties().values()) {

				/*
				 * NOTE for cast: the cast should be safe, because pi belongs to
				 * a GenericClassInfo
				 */
				this.genPropertiesById.put(pi.id(), (GenericPropertyInfo) pi);
			}
		}
	}

	/**
	 * Updates/fixes references to AssociationInfos.
	 * 
	 * @param aIn
	 *                AssociationInfo for which to search an update; may be
	 *                <code>null</code>
	 * @return a GenericAssociationInfo with the same id as the given
	 *         AssociationInfo, if it exists, otherwise the given
	 *         AssociationInfo
	 */
	private AssociationInfo updateAssociationInfo(AssociationInfo aIn) {
		if (aIn == null)
			return null;
		else if (genAssociationInfosById.containsKey(aIn.id())) {
			return genAssociationInfosById.get(aIn.id());
		} else
			return aIn;
	}

	/**
	 * @param model
	 * @param appSchemaPackage
	 * @return
	 */
	private GenericPackageInfo createGenericPackageInfo(PackageInfo pi,
			GenericModel model) {

		// check if generic representation of given PackageInfo already exists
		// if so, return it
		if (genPackageInfosById.containsKey(pi.id())) {
			return genPackageInfosById.get(pi.id());

		} else {

			// otherwise create new GenericPackageInfo
			GenericPackageInfo genPi = new GenericPackageInfo();

			// set properties required by Info interface
			genPi.setOptions(pi.options());
			genPi.setResult(pi.result());
			genPi.setModel(model);

			genPi.setTaggedValues(pi.taggedValuesAll(), false);
			genPi.setStereotypes(pi.stereotypes());

			genPi.setId(pi.id());
			genPi.setName(pi.name());

			genPi.setDescriptors(pi.descriptors().createCopy());

			genPi.setDiagrams(pi.getDiagrams());

			SortedSet<GenericPackageInfo> genChildPi = new TreeSet<GenericPackageInfo>();
			for (PackageInfo childPi : pi.containedPackages()) {
				genChildPi.add(this.createGenericPackageInfo(childPi, this));
			}
			genPi.setContainedPackages(genChildPi);

			SortedSet<String> genSupIds = new TreeSet<String>();
			for (String supId : pi.supplierIds()) {
				genSupIds.add(supId);
			}
			genPi.setSupplierIds(genSupIds);

			// creation is complete, add the object to map field for later
			// reference
			this.genPackageInfosById.put(genPi.id(), genPi);

			return genPi;
		}
	}

	public boolean isInAppSchema(PackageInfo pi) {
		if (selectedSchemas().contains(pi)) {
			return true;
		} else {
			if (pi.owner() != null)
				return isInAppSchema(pi.owner());
		}

		return false;
	}

	public boolean isInAppSchema(ClassInfo ci) {
		return this.isInAppSchema(ci.pkg());
	}

	/**
	 * Determines if ci1 is kind of ci2, by searching the complete inheritance
	 * tree of ci2 created by its subtypes.
	 * 
	 * @param childCi
	 *                     - the potential child class
	 * @param parentCi
	 *                     - the potential parent class
	 * @return <code>true</code> if ci1 is kind of ci2 (includes that ci1 and
	 *         ci2 are of the same type)
	 */
	public boolean isKindOf(ClassInfo childCi, ClassInfo parentCi) {

		if (childCi.id().equals(parentCi.id())) {

			return true;

		} else if (parentCi.subtypes() == null
				|| parentCi.subtypes().isEmpty()) {

			return false;

		} else {

			SortedSet<String> parentCiSubtypeIds = parentCi.subtypes();

			for (String parentCiSubtypeId : parentCiSubtypeIds) {

				ClassInfo parentCiSubtype = this.classById(parentCiSubtypeId);

				if (isKindOf(childCi, parentCiSubtype)) {
					return true;
				}
			}

			return false;
		}
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Model#options()
	 */
	public Options options() {
		return options;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Model#result()
	 */
	public ShapeChangeResult result() {
		return result;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Model#initialise(de.
	 *      interactive_instruments.ShapeChange.ShapeChangeResult,
	 *      de.interactive_instruments.ShapeChange.Options, java.lang.String)
	 */
	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileName) throws ShapeChangeAbortException {

		this.result = r;
		this.options = o;

		// TODO check existence of repository file in applicable configuration
		// validators
		File modelfile = new File(repositoryFileName);
		boolean ex = true;
		if (!modelfile.exists()) {
			ex = false;
			if (!(repositoryFileName.toLowerCase().endsWith(".xml")
					|| repositoryFileName.toLowerCase().endsWith(".zip"))) {
				repositoryFileName += ".xml";
				modelfile = new File(repositoryFileName);
				ex = modelfile.exists();
			}
		}
		if (!ex) {
			r.addFatalError(this, 25, repositoryFileName);
			throw new ShapeChangeAbortException();
		}

		boolean isZip = repositoryFileName.toLowerCase().endsWith(".zip");

		ZipFile zip = null;

		try {

			/*
			 * Validate SCXML if a schema was provided
			 */
			String scxmlXsdLocation = options.parameterAsString(null,
					"scxmlXsdLocation", null, false, true);
			if (scxmlXsdLocation != null) {

				InputStream inputStreamForValidation;

				if (isZip) {

					zip = new ZipFile(modelfile);
					Enumeration<? extends ZipEntry> entries = zip.entries();

					if (entries.hasMoreElements()) {
						ZipEntry zentry = entries.nextElement();
						inputStreamForValidation = zip.getInputStream(zentry);
						if (entries.hasMoreElements()) {
							result.addWarning(this, 30327, repositoryFileName,
									zentry.getName());
						}
					} else {
						inputStreamForValidation = null;
						result.addError(this, 30328, repositoryFileName);
					}

				} else {
					inputStreamForValidation = new FileInputStream(modelfile);
				}

				if (inputStreamForValidation != null) {

					Source instanceDocument = new StreamSource(
							inputStreamForValidation);

					System.setProperty(
							"javax.xml.validation.SchemaFactory:http://www.w3.org/XML/XMLSchema/v1.1",
							"org.apache.xerces.jaxp.validation.XMLSchema11Factory");
					SchemaFactory sf = SchemaFactory.newInstance(
							"http://www.w3.org/XML/XMLSchema/v1.1");

					Schema s = null;

					try {
						if (scxmlXsdLocation.startsWith("http")) {
							s = sf.newSchema(new URL(scxmlXsdLocation));
						} else {
							File schemaFile = new File(scxmlXsdLocation);
							s = sf.newSchema(schemaFile);
						}
					} catch (SAXException e) {
						result.addError(this, 30507, scxmlXsdLocation,
								e.getMessage());
					} catch (MalformedURLException e) {
						result.addError(this, 30506, scxmlXsdLocation,
								e.getMessage());
					}

					if (s != null) {
						Validator v = s.newValidator();

						v.setFeature(
								"http://apache.org/xml/features/validation/schema",
								true);
						v.setFeature(
								"http://apache.org/xml/features/validation/schema-full-checking",
								true);

						GenericModelErrorHandler errorHandler = new GenericModelErrorHandler();
						v.setErrorHandler(errorHandler);

						v.validate(instanceDocument);

						// check validation results
						if (errorHandler.hasWarnings()
								|| errorHandler.hasErrors()) {
							result.addInfo(this, 30500);

							if (errorHandler.hasWarnings()) {
								List<String> warnings = errorHandler.warnings();
								for (String w : warnings) {
									result.addInfo(this, 30502, w);
								}
							}
							if (errorHandler.hasErrors()) {
								List<String> errors = errorHandler.errors();
								for (String e : errors) {
									result.addInfo(this, 30503, e);
								}
							}

							result.addInfo(this, 30501);

							if (errorHandler.hasErrors()) {
								result.addFatalError(this, 30504);
								throw new ShapeChangeAbortException();
							}

						} else {
							result.addInfo(this, 30505);
						}
					}
				}
			}

			/*
			 * Using pure input stream as argument for InputSource so that the
			 * encoding is determined from the byte stream; for further details,
			 * see http://stackoverflow.com/questions/3482494/howto-let-the-sax-
			 * parser-determine-the-encoding-from-the-xml-declaration
			 */
			InputStream inputStream;

			if (isZip) {

				zip = new ZipFile(modelfile);
				Enumeration<? extends ZipEntry> entries = zip.entries();

				if (entries.hasMoreElements()) {
					ZipEntry zentry = entries.nextElement();
					inputStream = zip.getInputStream(zentry);
					if (entries.hasMoreElements()) {
						result.addWarning(this, 30327, repositoryFileName,
								zentry.getName());
					}
				} else {
					inputStream = null;
					result.addError(this, 30328, repositoryFileName);
				}

			} else {
				inputStream = new FileInputStream(modelfile);
			}

			if (inputStream != null) {

				InputSource is = new InputSource(inputStream);

				System.setProperty("javax.xml.parsers.SAXParserFactory",
						"org.apache.xerces.jaxp.SAXParserFactoryImpl");

				SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setNamespaceAware(true);

				SAXParser saxParser = spf.newSAXParser();

				// === Create XML Pipeline
				XMLReader xmlReader = saxParser.getXMLReader();

				GenericModelContentHandler modelHandler = new GenericModelContentHandler(
						result, options, xmlReader);

				xmlReader.setContentHandler(modelHandler);

				xmlReader.parse(is);

				this.characterEncoding = modelHandler.getEncoding();
				this.loadingFromScxml = true;

				if (options.parameterAsBoolean(null,
						"applyDescriptorSourcesWhenLoadingScxml", false)) {
					result.addDebug(this, 30401);
				}

				Map<String, GenericPackageContentHandler> packageHandlers = modelHandler
						.getAllPackageContentHandlers();
				Map<String, GenericClassContentHandler> classHandlers = new HashMap<String, GenericClassContentHandler>();
				Map<String, GenericAssociationContentHandler> associationHandlers = new HashMap<String, GenericAssociationContentHandler>();
				Map<String, GenericPropertyContentHandler> propertyHandlers = new HashMap<String, GenericPropertyContentHandler>();

				for (GenericPackageContentHandler pkgHandler : packageHandlers
						.values()) {

					GenericPackageInfo genPkg = pkgHandler.getGenericPackage();
					genPkg.setModel(this);
					/*
					 * Ensure that descriptors are loaded from descriptor
					 * sources, if so configured.
					 */
					genPkg.descriptors();

					this.genPackageInfosById.put(genPkg.id(),
							pkgHandler.getGenericPackage());

					for (GenericClassContentHandler clsHandler : pkgHandler
							.getClassContentHandlers()) {

						GenericClassInfo genCi = clsHandler.getGenericClass();
						genCi.setModel(this);
						/*
						 * Ensure that descriptors are loaded from descriptor
						 * sources, if so configured.
						 */
						genCi.descriptors();

						classHandlers.put(genCi.id(), clsHandler);
						this.genClassInfosById.put(genCi.id(), genCi);
						this.genClassInfosByName.put(genCi.name(), genCi);

						for (GenericPropertyContentHandler propHandler : clsHandler
								.getPropertyContentHandlers()) {

							GenericPropertyInfo genProp = propHandler
									.getGenericProperty();
							genProp.setModel(this);
							/*
							 * Ensure that descriptors are loaded from
							 * descriptor sources, if so configured.
							 */
							genProp.descriptors();

							propertyHandlers.put(genProp.id(), propHandler);
							this.genPropertiesById.put(genProp.id(), genProp);
						}
					}
				}

				for (GenericAssociationContentHandler assocHandler : modelHandler
						.getAssociationContentHandlers()) {

					GenericAssociationInfo genAi = assocHandler
							.getGenericAssociationInfo();
					genAi.setModel(this);
					/*
					 * Ensure that descriptors are loaded from descriptor
					 * sources, if so configured.
					 */
					genAi.descriptors();

					associationHandlers.put(genAi.id(), assocHandler);
					this.genAssociationInfosById.put(genAi.id(), genAi);

					GenericPropertyContentHandler end1Handler = assocHandler
							.getEnd1PropertyContentHandler();
					GenericPropertyContentHandler end2Handler = assocHandler
							.getEnd2PropertyContentHandler();

					if (end1Handler != null) {
						GenericPropertyInfo genPropEnd1 = end1Handler
								.getGenericProperty();
						genPropEnd1.setModel(this);
						/*
						 * Ensure that descriptors are loaded from descriptor
						 * sources, if so configured.
						 */
						genPropEnd1.descriptors();
						propertyHandlers.put(genPropEnd1.id(), end1Handler);
						this.genPropertiesById.put(genPropEnd1.id(),
								genPropEnd1);
					}
					if (end2Handler != null) {
						GenericPropertyInfo genPropEnd2 = end2Handler
								.getGenericProperty();
						genPropEnd2.setModel(this);
						/*
						 * Ensure that descriptors are loaded from descriptor
						 * sources, if so configured.
						 */
						genPropEnd2.descriptors();
						propertyHandlers.put(genPropEnd2.id(), end2Handler);
						this.genPropertiesById.put(genPropEnd2.id(),
								genPropEnd2);
					}
				}

				// update references (packages, classes, properties)
				for (GenericPackageContentHandler pkgHandler : packageHandlers
						.values()) {

					GenericPackageInfo genPkg = pkgHandler.getGenericPackage();

					for (GenericPackageContentHandler childPkgHandler : pkgHandler
							.getChildPackageContentHandlers()) {

						GenericPackageInfo childPkg = childPkgHandler
								.getGenericPackage();
						childPkg.setOwner(genPkg);
					}

					// if (pkgHandler.getOwnerId() != null) {
					// genPkg.setOwner(this.genPackageInfosById
					// .get(pkgHandler.getOwnerId()));
					// }

					for (GenericClassContentHandler clsHandler : pkgHandler
							.getClassContentHandlers()) {
						GenericClassInfo genCi = clsHandler.getGenericClass();
						genCi.setPkg(genPkg);
					}
				}

				for (GenericClassContentHandler clsHandler : classHandlers
						.values()) {

					GenericClassInfo genCi = clsHandler.getGenericClass();

					if (clsHandler.getAssociationId() != null) {
						genCi.setAssocInfo(this.genAssociationInfosById
								.get(clsHandler.getAssociationId()));
					}

					if (StringUtils
							.isNotBlank(clsHandler.getLinkedDocument())) {

						File ldFile = new File(options.linkedDocumentsTmpDir(),
								clsHandler.getLinkedDocument());

						if (!ldFile.exists()) {

							result.addWarning(this, 30400, genCi.id(),
									StringUtils.stripToEmpty(genCi.name()),
									clsHandler.getLinkedDocument());

						} else {

							genCi.setLinkedDocument(ldFile);
						}
					}

					/*
					 * Set inClass for navigable properties (does not cover
					 * non-navigable association roles).
					 */
					for (GenericPropertyContentHandler propHandler : clsHandler
							.getPropertyContentHandlers()) {
						GenericPropertyInfo genPi = propHandler
								.getGenericProperty();
						genPi.setInClass(genCi);
					}

					for (ConstraintContentHandler conHandler : clsHandler
							.getConstraintContentHandlers()) {

						updateConstraintContext(conHandler);
					}
				}

				for (GenericAssociationContentHandler assocHandler : associationHandlers
						.values()) {

					GenericAssociationInfo genAi = assocHandler
							.getGenericAssociationInfo();

					if (assocHandler.getAssocClassId() != null) {
						genAi.setAssocClass(this.genClassInfosById
								.get(assocHandler.getAssocClassId()));
					}

					GenericPropertyInfo end1;
					GenericPropertyInfo end2;

					if (assocHandler.getEnd1Id() != null) {
						end1 = this.genPropertiesById
								.get(assocHandler.getEnd1Id());
					} else {
						end1 = assocHandler.getEnd1PropertyContentHandler()
								.getGenericProperty();
					}

					if (assocHandler.getEnd2Id() != null) {
						end2 = this.genPropertiesById
								.get(assocHandler.getEnd2Id());
					} else {
						end2 = assocHandler.getEnd2PropertyContentHandler()
								.getGenericProperty();
					}

					genAi.setEnd1(end1);
					genAi.setEnd2(end2);
				}

				for (GenericPropertyContentHandler propHandler : propertyHandlers
						.values()) {

					GenericPropertyInfo genPi = propHandler
							.getGenericProperty();

					if (propHandler.getAssociationId() != null) {
						genPi.setAssociation(this.genAssociationInfosById
								.get(propHandler.getAssociationId()));
					}

					if (propHandler.getReversePropertyId() != null) {
						genPi.setReverseProperty(this.genPropertiesById
								.get(propHandler.getReversePropertyId()));
					}

					/*
					 * set inClass if the information is provided by the handler
					 * - typically only for non-navigable association roles
					 */
					if (propHandler.getInClassId() != null) {
						genPi.setInClass(this.genClassInfosById
								.get(propHandler.getInClassId()));
					}

					for (ConstraintContentHandler conHandler : propHandler
							.getConstraintContentHandlers()) {

						updateConstraintContext(conHandler);
					}
				}

				/*
				 * establish categories for all classes (since the SCXML does
				 * not contain that information)
				 */
				for (GenericClassInfo genCi : this.genClassInfosById.values()) {

					genCi.establishCategory();
				}
			}
		} catch (SAXException e) {
			result.addFatalError(null, 30803, e.getMessage());
			throw new ShapeChangeAbortException();
		} catch (FileNotFoundException e) {
			result.addFatalError(null, 30803, e.getMessage());
			throw new ShapeChangeAbortException();
		} catch (UnsupportedEncodingException e) {
			result.addFatalError(null, 30803, e.getMessage());
			throw new ShapeChangeAbortException();
		} catch (ParserConfigurationException e) {
			result.addFatalError(null, 30803, e.getMessage());
			throw new ShapeChangeAbortException();
		} catch (IOException e) {
			result.addFatalError(null, 30803, e.getMessage());
			throw new ShapeChangeAbortException();
		}
		// catch (ClassNotFoundException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {
					result.addFatalError(null, 30803, e.getMessage());
					throw new ShapeChangeAbortException();
				}
			}
			this.loadingFromScxml = false;
		}
	}

	private void updateConstraintContext(ConstraintContentHandler conHandler) {

		Constraint con = conHandler.getConstraint();

		Info contextModelElement;
		if (con.contextModelElmtType() == ModelElmtContextType.ATTRIBUTE) {
			contextModelElement = this.genPropertiesById
					.get(conHandler.getContextModelElementId());
		} else {
			contextModelElement = this.genClassInfosById
					.get(conHandler.getContextModelElementId());
		}

		if (con instanceof GenericFolConstraint) {
			((GenericFolConstraint) con)
					.setContextModelElmt(contextModelElement);
		} else if (con instanceof GenericOclConstraint) {
			((GenericOclConstraint) con)
					.setContextModelElmt(contextModelElement);
		} else {
			// assume GenericTextConstraint
			((GenericTextConstraint) con)
					.setContextModelElmt(contextModelElement);
		}
	}

	/**
	 * Return all ClassInfo objects contained in the given package and in sub-
	 * packages, which do not belong to an app schema different to the one of
	 * the given package. The target namespace of classes is not checked, only
	 * of packages.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Model#classes(de.
	 *      interactive_instruments.ShapeChange.Model.PackageInfo)
	 */
	@Override
	public SortedSet<ClassInfo> classes(PackageInfo pi) {

		// if (this.genPackageInfosById.containsKey(pi.id())) {
		// To hold the result ...
		SortedSet<ClassInfo> res = new TreeSet<ClassInfo>();
		// Get targetNamespace. Needed to find out, when we enter other app
		// schemas.
		String tns = pi.targetNamespace();

		return addClasses(genPackageInfosById.get(pi.id()), tns, res);
		// } else {
		// return model.classes(pi);
		// }
	}

	/**
	 * Return all ClassInfo objects contained in the given package and in sub-
	 * packages, which do not belong to an app schema different to the one of
	 * the given package.
	 */
	private SortedSet<ClassInfo> addClasses(PackageInfo pi, String tns,
			SortedSet<ClassInfo> res) {

		// Are we a different app schema? If so, skip
		String ctns = pi.targetNamespace();

		if (ctns != null && (tns == null || !tns.equals(ctns)))
			return res;

		// Same app schema, first add classes (already converted to generic
		// classes because it is the same app schema) to output ...

		if (pi instanceof GenericPackageInfo) {

			res.addAll(((GenericPackageInfo) pi).getClasses());

			// .. then descend to next packages
			for (PackageInfo cpi : pi.containedPackages()) {
				res = addClasses(cpi, tns, res);
			}
		} else {
			// well, then we are definitely no longer within one of the selected
			// schema
		}

		return res;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Model#
	 *      postprocessAfterLoadingAndValidate()
	 */
	public void postprocessAfterLoadingAndValidate() {

		/*
		 * Take into account input parameter
		 * loadConstraintsForSelectedSchemasOnly (cannot be done in
		 * GenericClass/PropertyContentHandler because model structure is not
		 * fully established while parsing the SCXML).
		 */
		if (options.isLoadConstraintsForSelectedSchemasOnly()) {
			for (GenericClassInfo genCi : this.genClassInfosById.values()) {
				removeConstraintsOfClassAndItsProperties(genCi);
			}
		}

		/*
		 * Take into account input parameter excluded packages.
		 */
		Set<PackageInfo> genPkgsToRemove = new HashSet<>();
		Set<String> excludedPkgNames = options.getExcludedPackages();
		for (GenericPackageInfo genPi : this.getGenPackages().values()) {
			if (excludedPkgNames.contains(genPi.name())) {
				genPkgsToRemove.add(genPi);
			}
		}
		this.remove(genPkgsToRemove);

		/*
		 * Postprocessing in ModelImpl.java also fixes class categories!
		 */
		super.postprocessAfterLoadingAndValidate();

		/*
		 * Take into account input parameter classTypesToCreateConstraintsFor
		 * (must be performed after class categories have fully been established
		 * (by super.postprocessAfterLoadingAndValidate())
		 */
		for (GenericClassInfo genCi : this.genClassInfosById.values()) {
			if (!options.isClassTypeToCreateConstraintsFor(genCi.category())) {
				removeConstraintsOfClassAndItsProperties(genCi);
			}
		}

		/*
		 * validate the constraints (includes parsing, taking into account the
		 * current model)
		 */
		this.validateConstraints();

		/*
		 * Fix some settings for properties that represent codes/enums.
		 */
		for (GenericPropertyInfo genPi : this.getGenProperties().values()) {

			if (genPi.inClass().category() == Options.ENUMERATION
					|| genPi.inClass().category() == Options.CODELIST) {

				if (genPi.isOrdered()) {
					genPi.setOrdered(false);
					MessageContext mc = result.addDebug(this, 30330,
							"isOrdered", genPi.name(), "true");
					if (mc != null) {
						mc.addDetail(this, 1, genPi.fullNameInSchema());
					}
				}

				if (!genPi.isUnique()) {
					genPi.setUnique(true);
					MessageContext mc = result.addDebug(this, 30330, "isUnique",
							genPi.name(), "false");
					if (mc != null) {
						mc.addDetail(this, 1, genPi.fullNameInSchema());
					}
				}

				if (genPi.isOwned()) {
					genPi.setOwned(false);
					MessageContext mc = result.addDebug(this, 30330, "isOwned",
							genPi.name(), "true");
					if (mc != null) {
						mc.addDetail(this, 1, genPi.fullNameInSchema());
					}
				}

				if (genPi.isComposition()) {
					genPi.setComposition(false);
					MessageContext mc = result.addDebug(this, 30330,
							"isComposition", genPi.name(), "true");
					if (mc != null) {
						mc.addDetail(this, 1, genPi.fullNameInSchema());
					}
				}

				if (genPi.isAggregation()) {
					genPi.setAggregation(false);
					MessageContext mc = result.addDebug(this, 30330,
							"isAggregation", genPi.name(), "true");
					if (mc != null) {
						mc.addDetail(this, 1, genPi.fullNameInSchema());
					}
				}
			}
		}
	}

	private void removeConstraintsOfClassAndItsProperties(
			GenericClassInfo genCi) {

		if (genCi.hasConstraints()) {
			genCi.setConstraints(null);
		}

		for (PropertyInfo pi : genCi.properties().values()) {
			GenericPropertyInfo genPi = (GenericPropertyInfo) pi;
			if (genPi.hasConstraints()) {
				genPi.setConstraints(null);
			}
		}
	}

	@Override
	public PackageInfo packageById(String id) {
		return genPackageInfosById.get(id);
	}

	@Override
	public ClassInfo classById(String id) {
		return genClassInfosById.get(id);
	}

	@Override
	public ClassInfo classByName(String name) {
		return genClassInfosByName.get(name);
	}

	/**
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Model#shutdown()
	 */
	public void shutdown() {
		// nothing to do here
	}

	/**
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.Model#characterEncoding()
	 */
	public String characterEncoding() {
		return characterEncoding;
	}

	public TreeSet<String> copy(SortedSet<String> hs) {

		if (hs == null)
			return null;

		TreeSet<String> res = new TreeSet<String>();
		for (String s : hs) {
			res.add(s);
		}
		return res;
	}

	public String printToString(String indent) {

		StringBuffer sb = new StringBuffer();

		for (PackageInfo selectedSchema : this.selectedSchemas()) {
			sb.append("Selected schema: " + selectedSchema.name() + "\r\n");
		}

		if (genAssociationInfosById != null) {
			for (String assocInfoId : genAssociationInfosById.keySet()) {
				sb.append("Association Id: " + assocInfoId + "\r\n");
			}
		}
		if (genPackageInfosById != null) {
			for (String packageInfoId : genPackageInfosById.keySet()) {
				sb.append("Package full name: "
						+ genPackageInfosById.get(packageInfoId).fullName()
						+ "\r\n");
			}
		}
		if (genClassInfosById != null) {
			for (String classInfoId : genClassInfosById.keySet()) {
				sb.append("Class full name: "
						+ genClassInfosById.get(classInfoId).fullName()
						+ "\r\n");
			}
		}

		return sb.toString();

	}

	/**
	 * Adds the property to the model map and also the given class.
	 * 
	 * WARNING: if the new property has the same name as an already existing one
	 * it will simply be added, not ignored or overwriting the existing one. Use
	 * the method
	 * {@link #add(GenericPropertyInfo, ClassInfo, PropertyCopyDuplicatBehaviorIndicator)}
	 * to control this behavior.
	 * 
	 * @param newProperty
	 * @param classToAddProperty
	 */
	public void add(GenericPropertyInfo newProperty,
			ClassInfo classToAddProperty) {

		this.add(newProperty, classToAddProperty,
				PropertyCopyDuplicatBehaviorIndicator.ADD);

	}

	/**
	 * Adds the property to the model map and also the given class.
	 * 
	 * The behavior how to handle the case that the new property has the same
	 * name as an already existing one is controlled by the behavior parameter.
	 * 
	 * @param newProperty
	 * @param classToAddProperty
	 * @param behavior
	 */
	public void add(GenericPropertyInfo newProperty,
			ClassInfo classToAddProperty,
			PropertyCopyDuplicatBehaviorIndicator behavior) {

		((GenericClassInfo) classToAddProperty).addProperty(newProperty,
				behavior);
	}

	/**
	 * Adds the given properties to their inClasses
	 * 
	 * @param newProperty
	 * @param behavior
	 */
	public void add(List<GenericPropertyInfo> newProperties,
			PropertyCopyDuplicatBehaviorIndicator behavior) {

		if (newProperties == null || newProperties.isEmpty()) {
			return;
		}

		Map<GenericClassInfo, List<GenericPropertyInfo>> newPropsByInClass = new HashMap<GenericClassInfo, List<GenericPropertyInfo>>();

		for (GenericPropertyInfo newProp : newProperties) {

			GenericClassInfo inClass = (GenericClassInfo) newProp.inClass();

			List<GenericPropertyInfo> propsForInClass;

			if (newPropsByInClass.containsKey(inClass)) {
				propsForInClass = newPropsByInClass.get(inClass);
			} else {
				propsForInClass = new ArrayList<GenericPropertyInfo>();
				newPropsByInClass.put(inClass, propsForInClass);
			}

			propsForInClass.add(newProp);
		}

		for (GenericClassInfo inClass : newPropsByInClass.keySet()) {

			inClass.addPropertiesInSequence(newPropsByInClass.get(inClass),
					behavior);
		}
	}

	/**
	 * Adds the property to the model map and also the given class.
	 * 
	 * The behavior how to handle the case that the new property has the same
	 * name as an already existing one is controlled by the behavior parameter.
	 * Where to add the property (top, bottom, or in-sequence of existing
	 * properties) is also determined via parameter.
	 * 
	 * @param newProperty
	 * @param classToAddProperty
	 * @param behavior
	 */
	public void add(GenericPropertyInfo newProperty,
			GenericClassInfo classToAddProperty,
			PropertyCopyPositionIndicator copyPositionIndicator,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		// // now add the property copy to the property map in the
		// // model
		// this.genPropertiesById.put(newProperty.id(), newProperty);

		List<GenericPropertyInfo> l = new ArrayList<GenericPropertyInfo>();
		l.add(newProperty);

		switch (copyPositionIndicator) {
		case PROPERTY_COPY_TOP:
			classToAddProperty.addPropertiesAtTop(l, duplicateHandling);
			break;
		case PROPERTY_COPY_INSEQUENCE:
			classToAddProperty.addPropertiesInSequence(l, duplicateHandling);
			break;
		case PROPERTY_COPY_BOTTOM:
			classToAddProperty.addPropertiesAtBottom(l, duplicateHandling);
			break;
		}
	}

	/**
	 * Removes the given class from the model. The model is updated to reflect
	 * this change, which involves:
	 * 
	 * <ul>
	 * <li>Removing all properties of the class.</li>
	 * <li>Removing all properties whose type is the class.</li>
	 * <li>Removing all associations with at least one of these properties.</li>
	 * <li>Removing all association classes of these associations.</li>
	 * <li>Removing the class and all identified association classes from their
	 * packages.</li>
	 * <li>Removing the relationships to super- and subtypes of the class.</li>
	 * </ul>
	 * 
	 * @param ciToRemove
	 */
	public void remove(GenericClassInfo ciToRemove) {
		remove(ciToRemove, false);
	}

	/**
	 * @param ciToRemove
	 * @param keepAssociationProperties
	 *                                      true if navigable properties of the
	 *                                      association that the given class may
	 *                                      be an association class for shall
	 *                                      not be removed from the model, else
	 *                                      false
	 */
	private void remove(GenericClassInfo ciToRemove,
			boolean keepAssociationProperties) {

		/*
		 * Ensure that the class is only removed if it is still registered in
		 * the model (in genClassInfosById). The check shall prevent removing a
		 * class twice, once because it is an association class which is removed
		 * together with an association, and then later on again - for example
		 * because the class does not belong to a profile and is therefore
		 * explicitly removed by the Profiler.
		 */
		if (ciToRemove == null || !this.isInAppSchema(ciToRemove)
				|| !this.genClassInfosById.containsKey(ciToRemove.id()))
			return;

		// Identify all properties to remove
		Set<PropertyInfo> propsToRemove = new HashSet<PropertyInfo>();
		Set<AssociationInfo> associationsToRemove = new HashSet<AssociationInfo>();

		// First get all properties from the class itself.
		for (PropertyInfo piToRemove : ciToRemove.properties().values()) {

			propsToRemove.add(piToRemove);

			AssociationInfo association = piToRemove.association();
			if (association != null) {

				if (association.end1() != null) {
					propsToRemove.add(association.end1());
				}
				if (association.end2() != null) {
					propsToRemove.add(association.end2());
				}
				associationsToRemove.add(association);
				// Note: a potentially existing association class will
				// automatically be removed in the loop where all associations
				// from the associationsToRemove are removed.
			}
		}

		// Then get all app schema properties whose type is the class to remove.
		for (PropertyInfo piToRemove : this.genPropertiesById.values()) {

			if (piToRemove.typeInfo().id.equals(ciToRemove.id())) {

				propsToRemove.add(piToRemove);

				// also check for associations where the class is the type of a
				// non-navigable association end
				AssociationInfo association = piToRemove.association();
				if (association != null) {
					if (association.end1() != null) {
						propsToRemove.add(association.end1());
					}
					if (association.end2() != null) {
						propsToRemove.add(association.end2());
					}
					associationsToRemove.add(association);
				}
			}
		}

		// Now get the association that this class may be an associationclass
		// for
		AssociationInfo association = ciToRemove.isAssocClass();
		if (association != null) {
			if (association.end1() != null && !(keepAssociationProperties
					&& association.end1().isNavigable())) {
				propsToRemove.add(association.end1());
			}
			if (association.end2() != null && !(keepAssociationProperties
					&& association.end2().isNavigable())) {
				propsToRemove.add(association.end2());
			}
			associationsToRemove.add(association);
		}

		// Remove identified properties from the model and their classes
		for (PropertyInfo piToRemove : propsToRemove) {

			if (piToRemove instanceof GenericPropertyInfo) {

				GenericPropertyInfo propToRemove = (GenericPropertyInfo) piToRemove;

				this.genPropertiesById.remove(propToRemove.id());

				/*
				 * This works also in case that the property is non-navigable
				 * and belongs to an association but is not contained in the
				 * property list of its inClass - then there simply is nothing
				 * to remove.
				 */
				if (propToRemove.inClass() instanceof GenericClassInfo) {
					((GenericClassInfo) propToRemove.inClass())
							.removePropertyById(propToRemove.id());
				} else {

					result.addError(this, 30313, propToRemove.inClass().name(),
							propToRemove.name);
				}

			} else {
				result.addWarning(this, 30312, piToRemove.inClass().name(),
						piToRemove.name());
			}
		}

		// Remove identified associations in model association map, including
		// any association class (unless it is the given class); the association
		// properties have already been removed.
		for (AssociationInfo assoToRemove : associationsToRemove) {

			/*
			 * NOTE for cast: the cast should be safe, because each assoToRemove
			 * has a GenericPropertyInfo; the GenericModel construction process
			 * ensures that association roles belonging to classes in the
			 * selected schema are converted to GenericPropertyInfos including
			 * the associations they belong to
			 */
			this.genAssociationInfosById
					.remove(((GenericAssociationInfo) assoToRemove).id());

			/*
			 * If the association has an association class, remove it, unless it
			 * is the class to remove in this operation.
			 */
			ClassInfo assocClass = assoToRemove.assocClass();
			if (assocClass != null && assocClass != ciToRemove) {

				if (assocClass instanceof GenericClassInfo) {

					this.remove((GenericClassInfo) assoToRemove.assocClass());

				} else {

					// if the association that the association class belongs to
					// is navigable into both directions, or navigable from the
					// selected schema, this is problematic
					result.addError(this, 30322, assocClass.name(),
							assocClass.id());
				}
			}
		}

		// get Ids of all subtypes from ciToRemove and remove the supertype
		// relationship with ciToRemove there
		SortedSet<String> ciToRemoveSubtypeIds = ciToRemove.subtypes();
		if (ciToRemoveSubtypeIds != null && ciToRemoveSubtypeIds.size() > 0) {

			for (String ciToRemoveSubtypeId : ciToRemoveSubtypeIds) {

				ClassInfo ciToRemoveSubtype = this
						.classById(ciToRemoveSubtypeId);

				if (ciToRemoveSubtype == null) {

					result.addDebug(this, 30324, ciToRemove.name(),
							ciToRemoveSubtypeId);

				} else if (!(ciToRemoveSubtype instanceof GenericClassInfo)) {

					result.addWarning(this, 30323, ciToRemove.name(),
							ciToRemoveSubtype.name());

				} else {

					GenericClassInfo genCiToRemoveSubtype = (GenericClassInfo) ciToRemoveSubtype;

					genCiToRemoveSubtype.removeSupertype(ciToRemove.id());
				}
			}
		}

		/*
		 * Get Ids of all supertypes from ciToRemove and remove the subtype
		 * relationship with ciToRemove there.
		 */
		SortedSet<String> ciToRemoveSupertypeIds = ciToRemove.supertypes();
		if (ciToRemoveSupertypeIds != null
				&& ciToRemoveSupertypeIds.size() > 0) {

			for (String ciToRemoveSupertypeId : ciToRemoveSupertypeIds) {

				GenericClassInfo ciBase = genClassInfosById
						.get(ciToRemoveSupertypeId);
				if (ciBase != null)
					ciBase.removeSubtype(ciToRemove.id());
				else {
					result.addError(this, 30314, ciToRemoveSupertypeId,
							ciToRemove.name);
				}
			}
		}

		/*
		 * Constraints from ciToRemove are automatically removed because they
		 * belong to it.
		 */

		/*
		 * baseClass relationship from ciToRemove is automatically removed
		 * because it belongs to it.
		 */

		/*
		 * Operation from ciToRemove are automatically removed because they
		 * belong to it.
		 */

		// remove ciToRemove from its package
		/*
		 * NOTE for cast: the cast should be safe, because ciToRemove is a
		 * GenericClassInfo and its package therefore is a GenericPackageInfo -
		 * this is true after the GenericModel has been constructed
		 */
		((GenericPackageInfo) ciToRemove.pkg()).remove(ciToRemove);

		// remove references to ciToRemove in model maps
		genClassInfosById.remove(ciToRemove.id());
		genClassInfosByName.remove(ciToRemove.name());
	}

	/**
	 * Removes the given classes from the model. Internally calls the
	 * remove(GenericClassInfo) method.
	 * 
	 * @param cisToRemove
	 */
	public void remove(Collection<GenericClassInfo> cisToRemove) {

		if (cisToRemove == null || cisToRemove.size() == 0)
			return;

		for (GenericClassInfo ciToRemove : cisToRemove) {

			remove(ciToRemove);
		}
	}

	/**
	 * @return map of all generic associations within the application schema
	 *         (key: association id, value: association)
	 */
	public Map<String, GenericAssociationInfo> getGenAssociations() {
		return genAssociationInfosById;
	}

	/**
	 * @return map of all generic properties within the model (key: property id,
	 *         value: property)
	 */
	public Map<String, GenericPropertyInfo> getGenProperties() {
		return genPropertiesById;
	}

	/**
	 * @return map of all generic packages within the model (key: package id,
	 *         value: package)
	 */
	public Map<String, GenericPackageInfo> getGenPackages() {
		return genPackageInfosById;
	}

	/**
	 * @return map of all generic classes within the model (key: class id,
	 *         value: class)
	 */
	public Map<String, GenericClassInfo> getGenClasses() {
		return genClassInfosById;
	}

	/**
	 * Removes the property from the model.
	 * 
	 * If the property is not part of an association, it is removed from both
	 * the model and its class.
	 * 
	 * If the property is part of an association, then the behavior depends upon
	 * the boolean parameter tryKeepAssociation: if it is false, then the
	 * association is removed with both its properties and any association
	 * class. Otherwise the given property is set to be non-navigable (also
	 * removing that property from its inClass). If both association ends then
	 * are no longer navigable, the association is removed with both its
	 * properties and any association class.
	 * 
	 * @param genPi
	 *                               the property that potentially is removed
	 *                               from the model and its class (depends on
	 *                               the tryKeepAssociation parameter and
	 *                               whether the property is navigable or not)
	 * @param tryKeepAssociation
	 *                               true if the algorithm should only delete
	 *                               the property if the association it
	 *                               potentially belongs to is no longer
	 *                               navigable (in both directions) after the
	 *                               property has been set to be non-navigable,
	 *                               false if any association the property
	 *                               belongs to shall be removed outright.
	 */
	public void remove(GenericPropertyInfo genPi, boolean tryKeepAssociation) {

		if (genPi != null) {

			AssociationInfo association = genPi.association();

			if (association == null) {

				// remove the property in its class and also in the model's
				// property map
				if (genPi.inClass() instanceof GenericClassInfo) {
					((GenericClassInfo) genPi.inClass())
							.removePropertyById(genPi.id());
					this.genPropertiesById.remove(genPi.id());
				} else {
					result.addError(this, 30313, genPi.inClass().name(),
							genPi.name);
				}
			} else {

				PropertyInfo end1 = association.end1();
				PropertyInfo end2 = association.end2();

				PropertyInfo other = null;

				if (end1.id().equals(genPi.id())) {
					other = end2;
				} else {
					other = end1;
				}

				boolean removeAssociation = false;

				if (tryKeepAssociation) {

					if (!other.isNavigable()) {
						removeAssociation = true;
					} else {
						/*
						 * fine, we keep the association, but we set genPi to be
						 * non-navigable and thus also need to remove it from
						 * the property list of its inClass
						 */
						genPi.setNavigable(false);
						if (genPi.inClass() instanceof GenericClassInfo) {
							((GenericClassInfo) genPi.inClass())
									.removePropertyById(genPi.id());
						} else {
							result.addError(this, 30313, genPi.inClass().name(),
									genPi.name);
						}
					}

				} else {
					removeAssociation = true;
				}

				if (removeAssociation) {
					remove(association);
				}
			}
		}
	}

	public void remove(AssociationInfo ai) {

		// remove the association, both properties and any
		// association class

		GenericClassInfo associationClass = null;
		ClassInfo assocClass = ai.assocClass();

		if (assocClass != null) {
			/*
			 * NOTE: cast should be safe because generic model is now complete
			 * copy of the model
			 */
			associationClass = (GenericClassInfo) ai.assocClass();
		}

		if (associationClass != null) {

			this.remove(associationClass);
			// this also takes care of the association itself and
			// its properties

		} else {

			// remove both properties

			PropertyInfo end1 = ai.end1();
			PropertyInfo end2 = ai.end2();

			/*
			 * NOTE: casts should be safe because generic model is now complete
			 * copy of the model
			 */

			((GenericClassInfo) end1.inClass()).removePropertyById(end1.id());
			this.genPropertiesById.remove(end1.id());

			((GenericClassInfo) end2.inClass()).removePropertyById(end2.id());
			this.genPropertiesById.remove(end2.id());

			// remove the association
			this.genAssociationInfosById.remove(ai.id());
		}
	}

	public void removeByClassCategory(int classCategory) {

		for (GenericClassInfo genCi : genClassInfosById.values().toArray(
				new GenericClassInfo[genClassInfosById.values().size()])) {

			if (genCi.category() == classCategory) {
				remove(genCi);
			}
		}

	}

	/**
	 * Copies the content of fromClass to toClass, including attributes, but
	 * NOT:
	 * 
	 * <ul>
	 * <li>associations</li>
	 * <li>constraints (because the latter are automatically retrieved and
	 * potentially overridden by each class while the input model is loaded)
	 * </li>
	 * <li>operations</li>
	 * <li>dependencies</li>
	 * </ul>
	 * 
	 * @param fromClass
	 * @param toClass
	 * @param copyPositionIndicator
	 * @param duplicateHandling
	 */
	public void copyClassContent(GenericClassInfo fromClass,
			GenericClassInfo toClass,
			PropertyCopyPositionIndicator copyPositionIndicator,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		// add property copies to toClass
		SortedMap<StructuredNumber, PropertyInfo> properties = fromClass
				.properties();

		// check that the fromClass has properties before copying them
		if (properties != null && !properties.isEmpty()) {

			List<GenericPropertyInfo> copiedProps = new ArrayList<GenericPropertyInfo>();

			// keep note of properties that belong to self associations so that
			// they are not processed twice

			// key: id of original property (from fromClass), value: property
			// copy (with modified id!)
			Map<String, GenericPropertyInfo> propsToSkipById = new HashMap<String, GenericPropertyInfo>();

			for (PropertyInfo propI : properties.values()) {

				// determine if property has already been processed because it
				// belongs to a circular association
				if (propsToSkipById.containsKey(propI.id()))
					continue;

				if (!propI.isAttribute())
					continue;

				// create property copy
				/*
				 * NOTE for cast: the cast should be safe, because propI belongs
				 * to fromClass, which is a GenericClassInfo
				 */
				GenericPropertyInfo genProp = (GenericPropertyInfo) propI;

				String copyId = genProp.id() + "_copyFrom" + fromClass.name()
						+ "To" + toClass.name();

				GenericPropertyInfo copy = genProp.createCopy(copyId);

				copy.setInClass(toClass);

				copiedProps.add(copy);

				/*
				 * Property copy is NOT added to the property map in the model
				 * because whether or not the copy is really added to the class
				 * depends on duplicate handling behavior (if a duplicate is
				 * ignored, the copy should not be registered in the model).
				 */
			}

			switch (copyPositionIndicator) {
			case PROPERTY_COPY_TOP:
				toClass.addPropertiesAtTop(copiedProps, duplicateHandling);
				break;
			case PROPERTY_COPY_INSEQUENCE:
				toClass.addPropertiesInSequence(copiedProps, duplicateHandling);
				break;
			case PROPERTY_COPY_BOTTOM:
				toClass.addPropertiesAtBottom(copiedProps, duplicateHandling);
				break;
			}

		}

		/*
		 * No need to copy constraints because they are already retrieved from
		 * supertypes while the input model is loaded
		 */

		// TODO: add dependencies to toClass; a parameter could control this
		// behavior

		// TODO: copy operations

		/*
		 * TODO: maybe copy tagged values of fromClass to toClass; the TVs could
		 * be specified via configuration, or be all of them - and do not
		 * overwrite ones already explicitly defined in subtypes [may need to be
		 * configurable])
		 */
	}

	/**
	 * Creates a copy of the given association. Only updates the id and copies
	 * tagged values, but does not update the objects (e.g. association ends or
	 * class; they need to be set separately).
	 * 
	 * @param ai
	 * @param copyId
	 * @return
	 */
	public GenericAssociationInfo createCopy(AssociationInfo ai,
			String copyId) {

		GenericAssociationInfo genAi = new GenericAssociationInfo();

		// set properties required by Info interface
		genAi.setOptions(ai.options());
		genAi.setResult(ai.result());
		genAi.setModel(this);

		genAi.setId(copyId);
		genAi.setName(ai.name());

		genAi.setDescriptors(ai.descriptors().createCopy());

		genAi.setStereotypes(ai.stereotypes());
		genAi.setTaggedValues(ai.taggedValuesAll(), false);

		genAi.setEnd1(ai.end1());
		genAi.setEnd2(ai.end2());
		genAi.setAssocClass(ai.assocClass());

		return genAi;
	}

	/**
	 * Creates a copy of the given class. Updates the id, creates independent
	 * copies of tagged values, as well as the subtype and supertype ID sets.
	 * Creates a shallow copy of the constraint vector. All other properties are
	 * used as-is. References may need to be updated, especially for: pkg,
	 * associationInfo, baseClass, properties (and potentially constraints).
	 * 
	 * Does not register the class copy in the model.
	 * 
	 * @param ci
	 * @param copyId
	 * @return
	 */
	public GenericClassInfo createCopy(ClassInfo ci, String copyId) {

		GenericClassInfo genCi = new GenericClassInfo(this, copyId, ci.name(),
				ci.category());

		// set properties required by Info interface

		genCi.setTaggedValues(ci.taggedValuesAll(), false);
		genCi.setStereotypes(ci.stereotypes());

		genCi.setDescriptors(ci.descriptors().createCopy());
		genCi.setProfiles(ci.profiles().createCopy());

		// set properties required by ClassInfo interface

		genCi.setPkg(ci.pkg());
		genCi.setIsAbstract(ci.isAbstract());
		genCi.setIsLeaf(ci.isLeaf());

		genCi.setAssocInfo(ci.isAssocClass());

		genCi.setSupertypes(copy(ci.supertypes()));
		genCi.setSubtypes(copy(ci.subtypes()));

		genCi.setProperties(ci.properties());

		genCi.setConstraints(copy(ci.constraints()));

		genCi.setDiagrams(ci.getDiagrams());
		genCi.setLinkedDocument(ci.getLinkedDocument());

		return genCi;
	}

	/**
	 * Creates a copy of the given property. Updates the id, creates independent
	 * copies of tagged values, the sequence number, type info, cardinality, as
	 * well as all attributes of primitive types. Does not update the objects
	 * (e.g. inClass or model) - they are used as-is and in order to be changed
	 * they need to be set separately.
	 * 
	 * @param pi
	 * @param copyId
	 * @return
	 */
	public GenericPropertyInfo createCopy(PropertyInfo pi, String copyId) {

		GenericPropertyInfo copy = new GenericPropertyInfo(this, copyId,
				pi.name());

		copy.setDescriptors(pi.descriptors().createCopy());
		copy.setProfiles(pi.profiles().createCopy());

		copy.setStereotypes(pi.stereotypes());
		copy.setTaggedValues(pi.taggedValuesAll(), false);
		copy.setDerived(pi.isDerived());
		copy.setReadOnly(pi.isReadOnly());
		copy.setAttribute(pi.isAttribute());
		copy.copyTypeInfo(pi.typeInfo());
		copy.setNavigable(pi.isNavigable());
		copy.setOrdered(pi.isOrdered());
		copy.setUnique(pi.isUnique());
		copy.setOwned(pi.isOwned());
		copy.setComposition(pi.isComposition());
		copy.setAggregation(pi.isAggregation());
		copy.setCardinality(new Multiplicity(pi.cardinality().toString()));
		copy.setInitialValue(pi.initialValue());
		copy.setInlineOrByReference(pi.inlineOrByReference());
		copy.setReverseProperty(pi.reverseProperty());
		copy.setInClass(pi.inClass());

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pi.sequenceNumber().components.length - 1; i++) {
			sb.append(pi.sequenceNumber().components[i] + ".");
		}
		sb.append(pi.sequenceNumber().components[pi
				.sequenceNumber().components.length - 1]);
		StructuredNumber res = new StructuredNumber(sb.toString());

		/*
		 * Updating the "sequenceNumber" tagged value is not necessary because
		 * we didn't change it here.
		 */
		copy.setSequenceNumber(res, false);
		copy.setConstraints(pi.constraints());
		copy.setAssociation(pi.association());
		copy.setRestriction(pi.isRestriction());
		copy.setNilReasonAllowed(pi.nilReasonAllowed());

		return copy;
	}

	/**
	 * Removes all of the packages in the set from the model, including all
	 * classes contained in these packages. Also recursively removes all child
	 * packages of the given packages.
	 * 
	 * @param packagesToRemove
	 */
	public void remove(Set<PackageInfo> packagesToRemove) {

		for (PackageInfo pi : packagesToRemove) {

			if (this.genPackageInfosById.containsKey(pi.id())) {

				GenericPackageInfo genPi = genPackageInfosById.get(pi.id());

				// remove any classes this package may contain
				SortedSet<GenericClassInfo> genPiClasses = genPi.getClasses();

				if (genPiClasses != null && !genPiClasses.isEmpty()) {
					for (GenericClassInfo ci : genPiClasses) {

						/*
						 * NOTE for cast: the cast should be safe, because ci
						 * belongs to a GenericPackageInfo
						 */
						this.remove(ci);
					}
				}

				// remove any child packages this package may contain
				SortedSet<PackageInfo> genPiChildren = genPi
						.containedPackages();

				if (genPiChildren != null && !genPiChildren.isEmpty()) {
					this.remove(genPiChildren);
				}

				// remove this package from its parent/owner (if it has one)
				PackageInfo owner = genPi.owner();
				if (owner != null) {
					/*
					 * NOTE for cast: the cast should be safe, because owner is
					 * the package of a GenericPropertyInfo, and thus should be
					 * a GenericPackageInfo - this is true after the
					 * GenericModel has been constructed
					 */
					((GenericPackageInfo) owner).removeChild(genPi);
				}

				// finally, remove this package from the packages map
				this.genPackageInfosById.remove(pi.id());
			}
		}
	}

	/**
	 * Basically turns the properties representing the two ends of an
	 * association into attributes of the classes they are in (unless the
	 * association end is not navigable).
	 * 
	 * Removes the link between the properties that represent the ends of a
	 * given association. Each navigable property is kept in the model. A
	 * non-navigable property is removed. A reverse property reference as well
	 * as the reference from an end property to its association is (re-)set to
	 * <code>null</code>. Each property that is kept in the model is set to be
	 * an attribute, a composition, but no aggregation. Finally, the reference
	 * to the association is removed from the model.
	 * 
	 * WARNING: a possibly existing association class is also removed from the
	 * model, with all its properties and links to other model elements!
	 * 
	 * @param genAI
	 */
	public void dissolveAssociation(GenericAssociationInfo genAI) {

		ArrayList<GenericPropertyInfo> associationends = new ArrayList<GenericPropertyInfo>();
		associationends.add((GenericPropertyInfo) genAI.end1());
		associationends.add((GenericPropertyInfo) genAI.end2());

		for (int i = 0; i < associationends.size(); i++) {

			GenericPropertyInfo genPi = associationends.get(i);

			if (!genPi.isNavigable()) {

				this.genPropertiesById.remove(genPi.id());
				((GenericClassInfo) genPi.inClass())
						.removePropertyById(genPi.id());

			} else {

				// turn this property into an attribute one
				genPi.setReverseProperty(null);
				genPi.setAssociation(null);

				genPi.setAttribute(true);
				genPi.setAggregation(false);
				genPi.setComposition(true);
			}
		}

		// remove the association class, but keep navigable association ends
		if (genAI.assocClass() != null) {

			if (genAI.assocClass() instanceof GenericClassInfo) {

				this.remove((GenericClassInfo) genAI.assocClass(), true);

			} else {

				result.addError(this, 30315, genAI.assocClass().name());
			}
		}

		// now remove the association
		this.genAssociationInfosById.remove(genAI.id());
	}

	/**
	 * Replaces the current name of the class with the given name, applying the
	 * update in the model as well (superclasses, subtypes, model, and property
	 * type info).
	 * 
	 * @param newName
	 */
	public void updateClassName(GenericClassInfo genCi, String newName) {

		String oldName = genCi.name();

		// no need to update the supertype/subtype infos because the id of the
		// class has not changed

		if (genClassInfosByName.keySet().contains(oldName)) {
			GenericClassInfo tmp = genClassInfosByName.get(oldName);
			genClassInfosByName.remove(oldName);
			genClassInfosByName.put(newName, tmp);
		}

		// update in property type info
		for (GenericPropertyInfo genPi : getGenProperties().values()) {
			if (genPi.typeInfo().id.equals(genCi.id())) {
				genPi.typeInfo().name = newName;
			}
		}

		// finally, udpate the name in the class itself
		genCi.setName(newName);
	}

	@Override
	public SortedSet<PackageInfo> packages() {
		SortedSet<PackageInfo> allPackages = new TreeSet<PackageInfo>();
		for (PackageInfo pi : genPackageInfosById.values()) {
			allPackages.add(pi);
		}
		return allPackages;
	}

	public void removeByClassId(String id) {

		if (genClassInfosById.containsKey(id)) {

			GenericClassInfo genCi = genClassInfosById.get(id);
			remove(genCi);
		}
	}

	/**
	 * At the moment simply puts the association info into the map with all
	 * GenericAssociationInfos
	 * 
	 * @param ai
	 */
	public void addAssociation(GenericAssociationInfo ai) {
		this.genAssociationInfosById.put(ai.id(), ai);
	}

	/**
	 * @return a set with all classes that belong to selected schemas; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<GenericClassInfo> selectedSchemaClasses() {

		SortedSet<GenericClassInfo> res = new TreeSet<GenericClassInfo>();

		for (PackageInfo selectedSchema : selectedSchemas()) {

			SortedSet<ClassInfo> cisOfSelectedSchema = this
					.classes(selectedSchema);

			for (ClassInfo ci : cisOfSelectedSchema) {
				/*
				 * note on cast: should be safe because all classes in a generic
				 * model are GenericClassInfos
				 */
				res.add((GenericClassInfo) ci);
			}
		}

		return res;
	}

	/**
	 * @return a set with all navigable properties that belong to the classes
	 *         from selected schemas; can be empty but not <code>null</code>.
	 */
	public SortedSet<GenericPropertyInfo> selectedSchemaProperties() {

		SortedSet<GenericClassInfo> selCis = this.selectedSchemaClasses();

		SortedSet<GenericPropertyInfo> res = new TreeSet<GenericPropertyInfo>();

		for (GenericClassInfo selCi : selCis) {

			for (PropertyInfo pi : selCi.properties.values()) {

				if (!pi.isNavigable()) {
					continue;
				}

				/*
				 * note on cast: safe because all properties in a generic model
				 * are GenericPropertyInfos
				 */
				GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

				res.add(genPi);
			}
		}

		return res;
	}

	/**
	 * @return a set with all associations that connect at least one class from
	 *         the selected schemas; can be empty but not <code>null</code>.
	 */
	public SortedSet<GenericAssociationInfo> selectedSchemaAssociations() {

		SortedSet<GenericPropertyInfo> selGenPis = this
				.selectedSchemaProperties();

		SortedSet<GenericAssociationInfo> res = new TreeSet<GenericAssociationInfo>();

		for (GenericPropertyInfo genPi : selGenPis) {

			if (!genPi.isAttribute) {

				/*
				 * Note on cast: should be safe because the association of a
				 * GenericPropertyInfo is a GenericAssociationInfo (that's how
				 * the GenericModel is created).
				 */
				res.add((GenericAssociationInfo) genPi.association());
			}
		}

		return res;
	}

	/**
	 * @return the genPackageInfosById
	 */
	public Map<String, GenericPackageInfo> getGenPackageInfosById() {
		return genPackageInfosById;
	}

	/**
	 * @param genPackageInfosById
	 *                                the genPackageInfosById to set
	 */
	public void setGenPackageInfosById(
			Map<String, GenericPackageInfo> genPackageInfosById) {
		this.genPackageInfosById = genPackageInfosById;
	}

	/**
	 * @param genClassInfosById
	 *                              the genClassInfosById to set
	 */
	public void setGenClassInfosById(
			Map<String, GenericClassInfo> genClassInfosById) {
		this.genClassInfosById = genClassInfosById;
	}

	/**
	 * @param genClassInfosByName
	 *                                the genClassInfosByName to set
	 */
	public void setGenClassInfosByName(
			Map<String, GenericClassInfo> genClassInfosByName) {
		this.genClassInfosByName = genClassInfosByName;
	}

	/**
	 * @param genAssociationInfosById
	 *                                    the genAssociationInfosById to set
	 */
	public void setGenAssociationInfosById(
			Map<String, GenericAssociationInfo> genAssociationInfosById) {
		this.genAssociationInfosById = genAssociationInfosById;
	}

	/**
	 * @param genPropertiesById
	 *                              the genPropertiesById to set
	 */
	public void setGenPropertiesById(
			Map<String, GenericPropertyInfo> genPropertiesById) {
		this.genPropertiesById = genPropertiesById;
	}

	public void register(GenericPropertyInfo genPi) {
		if (genPi != null) {
			this.genPropertiesById.put(genPi.id(), genPi);
		}
	}

	/**
	 * Adds the given prefix to the IDs of all model elements and updates all
	 * fields where the ID is relevant.
	 * 
	 * NOTE: this method is used by the FeatureCatalogue target to ensure that
	 * IDs used in a reference model are unique to that model and do not get
	 * mixed up with the IDs of the input model.
	 * 
	 * @param prefix
	 */
	public void addPrefixToModelElementIDs(String prefix) {

		if (prefix == null) {
			return;
		}

		// update genPropertiesById
		Map<String, GenericPropertyInfo> tmp_genPropertiesById = new HashMap<String, GenericPropertyInfo>();
		for (Entry<String, GenericPropertyInfo> e : genPropertiesById
				.entrySet()) {
			String newId = prefix + e.getKey();
			e.getValue().addPrefixToModelElementIDs(prefix);
			tmp_genPropertiesById.put(newId, e.getValue());
		}
		genPropertiesById = tmp_genPropertiesById;

		// update genAssociationInfosById
		Map<String, GenericAssociationInfo> tmp_genAssociationInfosById = new HashMap<String, GenericAssociationInfo>();
		for (Entry<String, GenericAssociationInfo> e : genAssociationInfosById
				.entrySet()) {
			String newId = prefix + e.getKey();
			e.getValue().addPrefixToModelElementIDs(prefix);
			tmp_genAssociationInfosById.put(newId, e.getValue());
		}
		genAssociationInfosById = tmp_genAssociationInfosById;

		// update genClassInfosById
		Map<String, GenericClassInfo> tmp_genClassInfosById = new HashMap<String, GenericClassInfo>();
		for (Entry<String, GenericClassInfo> e : genClassInfosById.entrySet()) {
			String newId = prefix + e.getKey();
			e.getValue().addPrefixToModelElementIDs(prefix);
			tmp_genClassInfosById.put(newId, e.getValue());
		}
		genClassInfosById = tmp_genClassInfosById;

		// update genPackageInfosById
		Map<String, GenericPackageInfo> tmp_genPackageInfosById = new HashMap<String, GenericPackageInfo>();
		for (Entry<String, GenericPackageInfo> e : genPackageInfosById
				.entrySet()) {
			String newId = prefix + e.getKey();
			e.getValue().addPrefixToModelElementIDs(prefix);
			tmp_genPackageInfosById.put(newId, e.getValue());
		}
		genPackageInfosById = tmp_genPackageInfosById;
	}

	public void validateConstraints() {

		boolean invalidConstraintsEncountered = false;

		Sbvr2FolParser sbvrParser = new Sbvr2FolParser(this, true);

		/*
		 * Handle actual constraints
		 */
		for (PackageInfo pkg : selectedSchemas()) {

			for (ClassInfo tmp : this.classes(pkg)) {

				/*
				 * Cast should be safe, because all classes of 'pkg' are
				 * GenericClassInfos.
				 */
				GenericClassInfo genCi = (GenericClassInfo) tmp;

				/*
				 * Ignore constraints on AIXM <<extension>> types
				 */
				if (genCi.category() == Options.AIXMEXTENSION) {
					continue;
				}

				List<Constraint> ciCons = genCi.constraints();

				if (ciCons != null) {

					// sort the constraints by name
					Collections.sort(ciCons, ConstraintComparators.NAME);

					Vector<Constraint> newConstraints = new Vector<Constraint>();

					for (Constraint con : ciCons) {

						if (con instanceof OclConstraint) {

							OclConstraint oclCon = (OclConstraint) con;

							Constraint parsedConstraint = parse(oclCon, genCi);

							if (parsedConstraint instanceof TextConstraint) {
								invalidConstraintsEncountered = true;
							}

							newConstraints.add(parsedConstraint);

						} else if (con instanceof FolConstraint) {

							FolConstraint folCon = (FolConstraint) con;

							Constraint parsedConstraint = parse(folCon,
									sbvrParser, genCi);

							if (parsedConstraint instanceof TextConstraint) {
								invalidConstraintsEncountered = true;
							}

							newConstraints.add(parsedConstraint);

						} else if (con instanceof TextConstraint) {

							/*
							 * this can be ignored, because TextConstraint is
							 * not validated
							 */
							newConstraints.add(con);

						} else {

							/*
							 * for all other cases, simply add the constraint
							 */

							result.addInfo(this, 20111, con.name(),
									genCi.fullNameInSchema());

							newConstraints.add(con);
						}
					}

					genCi.setConstraints(newConstraints);

				}

				// check constraints on properties
				if (genCi.properties() != null) {

					for (PropertyInfo pi : genCi.properties().values()) {

						/*
						 * Cast should be safe, because all properties of
						 * 'genCi' are GenericPropertyInfos.
						 */
						GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

						List<Constraint> piCons = genPi.constraints();

						if (piCons != null) {

							// sort the constraints by name
							Collections.sort(piCons,
									ConstraintComparators.NAME);

							Vector<Constraint> newConstraints = new Vector<Constraint>();

							for (Constraint con : piCons) {

								if (con instanceof OclConstraint) {

									OclConstraint oclCon = (OclConstraint) con;

									Constraint parsedConstraint = parse(oclCon,
											genPi);

									if (parsedConstraint instanceof TextConstraint) {
										invalidConstraintsEncountered = true;
									}

									newConstraints.add(parsedConstraint);

								} else if (con instanceof TextConstraint) {

									/*
									 * this can be ignored, because
									 * TextConstraint is not validated
									 */
									newConstraints.add(con);

								} else {

									/*
									 * For all other cases, simply add the
									 * constraint.
									 * 
									 * 2016-07-12 JE: at the moment,
									 * FolConstraints are only created with
									 * classes as context element. Therefore
									 * there is no need to handle FolConstraints
									 * here.
									 */

									result.addInfo(this, 20111, con.name(),
											genPi.fullNameInSchema());

									newConstraints.add(con);
								}
							}

							genPi.setConstraints(newConstraints);
						}
					}
				}
			}
		}

		if (invalidConstraintsEncountered) {
			result.addWarning(this, 30329);
		}
	}

	protected Constraint parse(FolConstraint con, Sbvr2FolParser parser,
			GenericClassInfo genCi) {

		if (con.sourceType().equals(SbvrConstants.FOL_SOURCE_TYPE)) {

			con.mergeComments(new String[] { con.text() });

			FolExpression folExpr = parser.parse(con);

			if (folExpr != null) {

				con.setFolExpression(folExpr);
				return con;

			} else {
				/*
				 * The parser already logged why the expression was not created;
				 * use a text constraint as fallback.
				 */

				result.addInfo(this, 20110, con.name(),
						genCi.fullNameInSchema());

				return new GenericTextConstraint(genCi, con);
			}

		} else {

			/*
			 * Apparently a new source for FOL constraints exists - add parsing
			 * it here; in the meantime, log this as an error and create a text
			 * constraint as fallback.
			 */
			MessageContext ctx = result.addError(null, 38, con.sourceType());
			ctx.addDetail(null, 39, con.name(),
					con.contextModelElmt().fullNameInSchema());

			return new GenericTextConstraint(genCi, con);
		}
	}

	/**
	 * @param con
	 *                  constraint to validate
	 * 
	 * @param genCi
	 *                  context of the constraint
	 * 
	 */
	protected Constraint parse(OclConstraint con, GenericClassInfo genCi) {

		GenericOclConstraint validated = new GenericOclConstraint(genCi, con);

		if (validated.syntaxTree() != null) {
			/*
			 * Parsing succeeded
			 */
			return validated;

		} else {

			/*
			 * The reason why parsing the constraint failed has already been
			 * logged; use a text constraint as fallback.
			 */

			result.addInfo(this, 20110, con.name(), genCi.fullNameInSchema());

			GenericTextConstraint fallback = new GenericTextConstraint(genCi,
					con);
			return fallback;
		}
	}

	protected Constraint parse(OclConstraint con, GenericPropertyInfo genPi) {

		GenericOclConstraint validated = new GenericOclConstraint(genPi, con);

		if (validated.syntaxTree() != null) {
			/*
			 * Parsing succeeded
			 */
			return validated;

		} else {

			/*
			 * The reason why parsing the constraint failed has already been
			 * logged; use a text constraint as fallback.
			 */

			result.addInfo(this, 20110, con.name(), genPi.fullNameInSchema());

			GenericTextConstraint fallback = new GenericTextConstraint(genPi,
					con);
			return fallback;
		}
	}

	@Override
	public String descriptorSource(Descriptor descriptor) {

		String source = null;

		if (options().parameterAsBoolean(null,
				"applyDescriptorSourcesWhenLoadingScxml", false)
				&& loadingFromScxml) {

			source = options().descriptorSource(descriptor.getName());

			// if nothing has been configured, use tag as default
			if (source == null) {
				source = "tag#" + descriptor;
			}

		} else {

			source = "sc:internal";
		}

		return source;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Context: property '$1$'";
		case 2:
			return "Context: class '$1$'";

		case 25:
			return "Model repository file named '$1$' not found";

		case 20110:
			return "The constraint '$1$' on '$2$' will be converted into a simple TextConstraint.";
		case 20111:
			return "The constraint '$1$' on '$2$' was not recognized as a constraint to be validated.";

		case 30300:
			return "(GenericModel.java) Constraint '$1$' in Class '$2$' not of type 'GenericText/OclConstraint'.";
		case 30301:
			return "(GenericModel.java) $1$";
		case 30302:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericTextConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30303:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericTextConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30304:
			return "(GenericModel.java) Unrecognized constraint context model element type: '$1$'";
		case 30305:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30306:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'. - Context class name is '$3$'.";
		case 30307:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30308:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'. - Context class name is '$3$'.";
		case 30309:
			return "(GenericModel.java) Unrecognized constraint type: '$1$'.";
		case 30310:
			return "(GenericModel.java) Package '$1$' is not of type 'GenericPackageInfo'. Cannot add class '$2$'";
		case 30311:
			return "(GenericModel.java) Class '$1$' is not of type 'GenericClassInfo' (was trying to add new property '$2$').";
		case 30312:
			return "(GenericModel.java) Property $1$.$2$ is not of type 'GenericPropertyInfo' (most likely because the property belongs to a class that is not part of the selected schema). Cannot remove the property.";
		case 30313:
			return "(GenericModel.java) Class '$1$' is not of type 'GenericClassInfo' (was trying to remove property '$2$')";
		case 30314:
			return "(GenericModel.java) Class with id '$1$' not found. Cannot remove subtype '$2$'";
		case 30315:
			return "(GenericModel.java) Association class '$1$' not of type GenericClassInfo. Cannot remove it.";
		case 30316:
			return "(GenericModel.java) Class with name '$1$' and id '$2$' is not of type 'GenericClassInfo'.";
		case 30317:
			return "(GenericModel.java) Property with name '$1$' and id '$2$' is not of type 'GenericPropertyInfo'.";
		case 30318:
			return "(GenericModel.java) Property with id '$1$' (name: '$2$', in class: '$3$') already exists in the generic model (details about that property [property name / in class name]: $4$). The property will be ignored (not added to its class).";
		case 30319:
			return "(GenericModel.java) GenericPropertyInfo that should be used to represent the property '$1$' is not in the same class (property in class is '$2$', generic property in class is '$3$'). The property will not be added to class '$2$'.";
		case 30320:
			return "(GenericModel.java) PropertyInfo with sequenceNumber '$1$' in class '$2$' is null. The property will be ignored.";
		case 30321:
			return "(GenericModel.java) No GenericPropertyInfo found that represents property '$1$' in class '$2$'. The property will be ignored.";
		case 30322:
			return "(GenericModel.java) Class with name '$1$' and id '$2$' is not of type 'GenericClassInfo'. Cannot remove it from the model.";
		case 30323:
			return "(GenericModel.java) Subtype of '$1$' with name '$2$' found in the model but it is not a GenericClassInfo (likely because it does not belong to a schema selected for processing). Cannot remove the supertype relationship to '$1$' in the subtype '$2$'.";
		case 30324:
			return "(GenericModel.java) Subtype of '$1$' with id '$2$' not found in the model. Cannot remove the supertype relationship to '$1$' in the subtype.";
		case 30325:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericFolConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30326:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericFolConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30327:
			return "(Generic model) The zip file at '$1$' contains more than one entry. Only the entry '$2$' will be loaded. Other entries will be ignored.";
		case 30328:
			return "(Generic model) The zip file at '$1$' does not contain any entry. The model will be empty.";
		case 30329:
			return "One or more OclConstraints or FolConstraints were invalid and have been transformed into TextConstraints. For further details, consult the validation messages that were logged on INFO level before this message. This is not an issue if these constraints are not processed by subsequent transformations or targets.";
		case 30330:
			return "Field '$1$' of code/enum '$2$' is $3$, which is not the applicable default. Using default for this property.";

		case 30400:
			return "While parsing content of Class element with id '$1$' and name '$2$', linked document does not exist at '$3$'.";
		case 30401:
			return "Input parameter 'applyDescriptorSourcesWhenLoadingScxml' is set to 'true'. Descriptors elements in SCXML will be ignored. Descriptors of model elements loaded from SCXML will be determined based upon configured descriptor sources.";

		case 30500:
			return "--- SCXML VALIDATION RESULTS - START ---";
		case 30501:
			return "--- SCXML VALIDATION RESULTS - END ---";
		case 30502:
			return "Warning: $1$";
		case 30503:
			return "Error: $1$";
		case 30504:
			return "SCXML is invalid. Execution will stop now.";
		case 30505:
			return "--- SCXML IS VALID ---";
		case 30506:
			return "SCXML XSD location URL '$1$' (defined via input parameter 'scxmlXsdLocation') is malformed. Validation of SCXML will be skipped. Message from Java MalformedURLException is: $2$.";
		case 30507:
			return "Schema could not be created from SCXML XSD location '$1$' (defined via input parameter 'scxmlXsdLocation'). Validation of SCXML will be skipped. Message from Java SAXException is: $2$.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
