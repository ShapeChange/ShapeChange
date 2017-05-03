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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Adding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.ClassSelector;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.PackageSelector;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyDuplicatBehaviorIndicator;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class AttributeCreator implements Transformer, MessageSource {

	class AttributeDefinition {

		private PackageSelector ps;
		private ClassSelector cs;
		private String aliasName;
		private String initialValue;
		private boolean isDerived;
		private boolean isOrdered;
		private boolean isUnique;
		private boolean isReadOnly;
		private String name;
		private Multiplicity multiplicity;
		private TaggedValues tvs;
		private Type type;
		private Stereotypes stereotypes;

		/**
		 * @return the ps
		 */
		public PackageSelector getPackageSelector() {
			return ps;
		}

		/**
		 * @return the cs
		 */
		public ClassSelector getClassSelector() {
			return cs;
		}

		/**
		 * @return the aliasName
		 */
		public String getAliasName() {
			return aliasName;
		}

		/**
		 * @return the initialValue
		 */
		public String getInitialValue() {
			return initialValue;
		}

		/**
		 * @return the isDerived
		 */
		public boolean isDerived() {
			return isDerived;
		}

		/**
		 * @return the isOrdered
		 */
		public boolean isOrdered() {
			return isOrdered;
		}

		/**
		 * @return the isUnique
		 */
		public boolean isUnique() {
			return isUnique;
		}

		/**
		 * @return the isReadOnly
		 */
		public boolean isReadOnly() {
			return isReadOnly;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the multiplicity
		 */
		public Multiplicity getMultiplicity() {
			return multiplicity;
		}

		/**
		 * @return the tvs
		 */
		public TaggedValues getTaggedValues() {
			return tvs;
		}

		/**
		 * @return the type
		 */
		public Type getType() {
			return type;
		}

		/**
		 * @param ps
		 *            the ps to set
		 */
		public void setPackageSelector(PackageSelector ps) {
			this.ps = ps;
		}

		/**
		 * @param cs
		 *            the cs to set
		 */
		public void setClassSelector(ClassSelector cs) {
			this.cs = cs;
		}

		/**
		 * @param aliasName
		 *            the aliasName to set
		 */
		public void setAliasName(String aliasName) {
			this.aliasName = aliasName;
		}

		/**
		 * @param initialValue
		 *            the initialValue to set
		 */
		public void setInitialValue(String initialValue) {
			this.initialValue = initialValue;
		}

		/**
		 * @param isDerived
		 *            the isDerived to set
		 */
		public void setDerived(boolean isDerived) {
			this.isDerived = isDerived;
		}

		/**
		 * @param isOrdered
		 *            the isOrdered to set
		 */
		public void setOrdered(boolean isOrdered) {
			this.isOrdered = isOrdered;
		}

		/**
		 * @param isUnique
		 *            the isUnique to set
		 */
		public void setUnique(boolean isUnique) {
			this.isUnique = isUnique;
		}

		/**
		 * @param isReadOnly
		 *            the isReadOnly to set
		 */
		public void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @param multiplicity
		 *            the multiplicity to set
		 */
		public void setMultiplicity(Multiplicity multiplicity) {
			this.multiplicity = multiplicity;
		}

		/**
		 * @param tvs
		 *            the tvs to set
		 */
		public void setTaggedValues(TaggedValues tvs) {
			this.tvs = tvs;
		}

		/**
		 * @param type
		 *            the type to set
		 */
		public void setType(Type type) {
			this.type = type;
		}

		/**
		 * @return the stereotypes
		 */
		public Stereotypes getStereotypes() {
			return stereotypes;
		}

		/**
		 * @param stereotypes
		 *            the stereotypes to set
		 */
		public void setStereotypes(Stereotypes stereotypes) {
			this.stereotypes = stereotypes;
		}
	}

	private ShapeChangeResult result = null;
	private Options options = null;
	private GenericModel model = null;

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		this.result = r;
		this.options = o;
		this.model = m;

		/*
		 * identify AttributeDefinitions in advancedProcessConfigurations
		 * element of transformer configuration
		 */
		if (trfConfig.getAdvancedProcessConfigurations() == null) {

			result.addWarning(this, 1);

		} else {

			List<AttributeDefinition> attDefs = parseAttributeDefinitions(
					trfConfig.getAdvancedProcessConfigurations());

			/*
			 * Create attributes
			 */
			for (AttributeDefinition attDef : attDefs) {

				PackageSelector ps = attDef.getPackageSelector();
				ClassSelector cs = attDef.getClassSelector();

				Set<PackageInfo> packageSelection = ps.selectPackages(model);
				Set<ClassInfo> classSelection = cs.selectClasses(model,
						packageSelection);

				if (classSelection.isEmpty()) {

					result.addInfo(this, 5, attDef.getName());

				} else {

					for (ClassInfo ci : classSelection) {

						GenericClassInfo genCi = (GenericClassInfo) ci;

						/*
						 * Determine if a property with the same name already
						 * exists. If so, we continue, because we do not allow
						 * overwriting an existing property (because this
						 * transformer creates attributes and the existing
						 * property could be an association role, thus
						 * destroying the model)
						 */
						SortedMap<StructuredNumber, PropertyInfo> propsInCi = ci
								.properties();
						PropertyInfo samePiInClass = null;
						for (PropertyInfo existingProp : propsInCi.values()) {
							if (existingProp.name().equals(attDef.getName())) {
								samePiInClass = existingProp;
							}
						}

						if (samePiInClass != null) {
							result.addWarning(this, 6, attDef.getName(),
									ci.name());
							continue;
						}

						String id = attDef.getName() + "_addedToClass_"
								+ genCi.id();

						GenericPropertyInfo genPi = new GenericPropertyInfo(
								model, id.toString(), attDef.getName());

//						genPi.setAliasNameAll(
//								new Descriptors(attDef.getAliasName()));
						genPi.descriptors().put(Descriptor.ALIAS, attDef.getAliasName());
						genPi.setInitialValue(attDef.getInitialValue());
						genPi.setDerived(attDef.isDerived());
						genPi.setOrdered(attDef.isOrdered());
						genPi.setUnique(attDef.isUnique());
						genPi.setReadOnly(attDef.isReadOnly());
						genPi.setCardinality(attDef.getMultiplicity());
						genPi.setTypeInfo(attDef.getType());

						/*
						 * create a copy of the tagged values from the attribute
						 * definition to use for genPi - if we don't copy then
						 * we would allow sharing of the tagged values from the
						 * attribute definition by several model properties,
						 * which can lead to unwanted side-effects
						 */
						TaggedValues tvsForPi = options
								.taggedValueFactory(attDef.getTaggedValues());

						/*
						 * Each property must have a unique sequence number. If
						 * no sequence number was provided via the
						 * configuration, determine the number with which the
						 * new attribute would be placed behind the existing
						 * properties.
						 */
						if (!tvsForPi.containsKey("sequenceNumber")) {

							if (genCi.properties() != null
									&& genCi.properties().size() > 0) {

								/*
								 * Identify the highest major component of the
								 * sequence numbers in the collection of
								 * existing properties.
								 */
								int maxMajorComponentExistingProps = Integer.MIN_VALUE;
								for (StructuredNumber snExistingProp : genCi
										.properties().keySet()) {
									if (snExistingProp.components[0] > maxMajorComponentExistingProps) {
										maxMajorComponentExistingProps = snExistingProp.components[0];
									}
								}

								tvsForPi.add("sequenceNumber", ""
										+ (maxMajorComponentExistingProps + 1));

							} else {

								tvsForPi.add("sequenceNumber", "" + genPi
										.getNextNumberForAttributeWithoutExplicitSequenceNumber());
							}

						} else {

							/*
							 * If one of the existing properties has the same
							 * sequence number, append a suffix so that the new
							 * property will be placed behind the existing one.
							 */
							String seqN = tvsForPi
									.getFirstValue("sequenceNumber");
							StructuredNumber sn = new StructuredNumber(seqN);
							for (StructuredNumber snExistingProp : genCi
									.properties().keySet()) {

								if (snExistingProp.compareTo(sn) == 0) {

									/*
									 * now search for properties with structured
									 * numbers that have the same leading
									 * components - identifying the highest
									 * value for the following component which
									 * must be added to the new structured
									 * number in order for it to be unique
									 */
									int[] components = sn.components;
									int newComponent = 1;

									loopExistingPropSns: for (StructuredNumber tmp : genCi
											.properties().keySet()) {
										if (tmp.components.length > components.length) {
											for (int i = 0; i < components.length; i++) {
												if (tmp.components[i] != components[i]) {
													// tmp structured number is
													// not what we are looking
													// for
													continue loopExistingPropSns;
												}
											}

											int compTmp = tmp.components[components.length];
											if (newComponent <= compTmp) {
												newComponent = compTmp + 1;
											}
										}
									}
									sn = sn.createCopyWithSuffix(newComponent);
									tvsForPi.put("sequenceNumber",
											sn.getString());
								}
							}

						}
						genPi.setTaggedValues(tvsForPi, true);

						/*
						 * set stereotypes - also setVoidable if voidable
						 * stereotype is contained
						 */
						genPi.setStereotypes(attDef.getStereotypes());
						if (genPi.stereotypes().contains("voidable")) {
							genPi.setVoidable(true);
						}

						/*
						 * Determine if new property would be a restriction. We
						 * already checked that ci itself does not have a
						 * property with the name of the new attribute. Thus
						 * determine if supertypes of ci have a property with
						 * the same name; if so, then the new property would be
						 * a restriction.
						 */
						PropertyInfo samePiInSupertypes = ci
								.property(genPi.name());
						if (samePiInSupertypes != null) {
							genPi.setRestriction(true);
						}

						genPi.setInClass(genCi);

						genPi.setAggregation(false);
						genPi.setAttribute(true);
						genPi.setComposition(true);
						genPi.setNavigable(true);

						/*
						 * Note: ignoring should not be necessary, as we checked
						 * in the beginning that the class does not have a
						 * property with the same name - but just in case
						 */
						model.add(genPi, genCi,
								PropertyCopyDuplicatBehaviorIndicator.IGNORE);
					}
				}
			}
		}
	}

	/**
	 * @param apcs
	 *            the advancedProcessConfigurations element
	 * @return a list of parsed AttributeDefinition infos; can be empty but not
	 *         <code>null</code>
	 */
	private List<AttributeDefinition> parseAttributeDefinitions(Element apcs) {

		List<AttributeDefinition> attDefs = new ArrayList<AttributeDefinition>();

		// identify AttributeDefinition elements
		List<Element> attDefEs = new ArrayList<Element>();

		NodeList adNl = apcs.getElementsByTagName("AttributeDefinition");

		if (adNl != null && adNl.getLength() != 0) {
			for (int k = 0; k < adNl.getLength(); k++) {
				Node n = adNl.item(k);
				if (n.getNodeType() == Node.ELEMENT_NODE) {

					attDefEs.add((Element) n);
				}
			}
		}

		for (int i = 0; i < attDefEs.size(); i++) {

			String indexForMsg = "" + (i + 1);

			Element attDefE = attDefEs.get(i);

			AttributeDefinition ad = new AttributeDefinition();

			// parse name
			Element nE = getFirstElement(attDefE, "name");
			String name = nE.getTextContent().trim();
			if (name.length() == 0) {
				result.addError(this, 3, "name", indexForMsg);
				continue;
			}
			ad.setName(name);

			// parse PackageSelector and ClassSelector
			Element selections = getFirstElement(attDefE, "classSelection");

			PackageSelector ps = new PackageSelector();
			Element psE = getFirstElement(selections, "PackageSelector");

			if (psE.hasAttribute("schemaNameRegex")) {
				String snr = psE.getAttribute("schemaNameRegex");
				try {
					Pattern snP = Pattern.compile(snr);
					ps.setSchemaNamePattern(snP);
				} catch (PatternSyntaxException e) {
					result.addError(this, 2, "schemaNameRegex",
							"PackageSelector", indexForMsg, name);
					continue;
				}
			}

			if (psE.hasAttribute("nameRegex")) {
				String nr = psE.getAttribute("nameRegex");
				try {
					Pattern nP = Pattern.compile(nr);
					ps.setNamePattern(nP);
				} catch (PatternSyntaxException e) {

					result.addError(this, 2, "nameRegex", "PackageSelector",
							indexForMsg, name);
					continue;
				}
			}

			if (psE.hasAttribute("stereotypeRegex")) {
				String sr = psE.getAttribute("stereotypeRegex");
				try {
					Pattern sP = Pattern.compile(sr);
					ps.setStereotypePattern(sP);
				} catch (PatternSyntaxException e) {
					result.addError(this, 2, "stereotypeRegex",
							"PackageSelector", indexForMsg, name);
					continue;
				}
			}

			ad.setPackageSelector(ps);

			ClassSelector cs = new ClassSelector();
			Element csE = getFirstElement(selections, "ClassSelector");

			if (csE.hasAttribute("nameRegex")) {
				String nr = csE.getAttribute("nameRegex");
				try {
					Pattern nP = Pattern.compile(nr);
					cs.setNamePattern(nP);
				} catch (PatternSyntaxException e) {
					result.addError(this, 2, "nameRegex", "ClassSelector",
							indexForMsg, name);
					continue;
				}
			}

			if (csE.hasAttribute("stereotypeRegex")) {
				String sr = csE.getAttribute("stereotypeRegex");
				try {
					Pattern sP = Pattern.compile(sr);
					cs.setStereotypePattern(sP);
				} catch (PatternSyntaxException e) {
					result.addError(this, 2, "stereotypeRegex",
							"PackageSelector", indexForMsg, name);
					continue;
				}
			}

			ad.setClassSelector(cs);

			// parse aliasName
			Element aliasE = getFirstElement(attDefE, "aliasName");
			if (aliasE != null) {
				String alias = aliasE.getTextContent().trim();
				if (alias.length() != 0) {
					ad.setAliasName(alias);
				}
			}

			// parse initialValue
			Element ivE = getFirstElement(attDefE, "initialValue");
			if (ivE != null) {
				String iv = ivE.getTextContent();
				if (iv.length() != 0) {
					ad.setInitialValue(iv);
				}
			}

			// parse isDerived
			boolean isDerived = false;
			Element isDerivedE = getFirstElement(attDefE, "isDerived");
			if (isDerivedE != null) {
				String isDerivedS = isDerivedE.getTextContent().trim();
				if (isDerivedS.equalsIgnoreCase("true")
						|| isDerivedS.equals("1")) {
					isDerived = true;
				}
			}
			ad.setDerived(isDerived);

			// parse isOrdered
			boolean isOrdered = false;
			Element isOrderedE = getFirstElement(attDefE, "isOrdered");
			if (isOrderedE != null) {
				String isOrderedS = isOrderedE.getTextContent().trim();
				if (isOrderedS.equalsIgnoreCase("true")
						|| isOrderedS.equals("1")) {
					isOrdered = true;
				}
			}
			ad.setOrdered(isOrdered);

			// parse isUnique
			boolean isUnique = true;
			Element isUniqueE = getFirstElement(attDefE, "isUnique");
			if (isUniqueE != null) {
				String isUniqueS = isUniqueE.getTextContent().trim();
				if (isUniqueS.equalsIgnoreCase("false")
						|| isUniqueS.equals("0")) {
					isUnique = false;
				}
			}
			ad.setUnique(isUnique);

			// parse isReadOnly
			boolean isReadOnly = false;
			Element isReadOnlyE = getFirstElement(attDefE, "isReadOnly");
			if (isReadOnlyE != null) {
				String isReadOnlyS = isReadOnlyE.getTextContent().trim();
				if (isReadOnlyS.equalsIgnoreCase("true")
						|| isReadOnlyS.equals("1")) {
					isReadOnly = true;
				}
			}
			ad.setReadOnly(isReadOnly);

			// parse multiplicity
			Multiplicity m = new Multiplicity();
			Element multE = getFirstElement(attDefE, "multiplicity");
			if (multE != null) {
				String mult = multE.getTextContent().trim();
				if (mult.length() != 0) {
					m = new Multiplicity(mult);
				}
			}
			ad.setMultiplicity(m);

			// parse stereotypes
			Stereotypes sts = options.stereotypesFactory();
			Element stsE = getFirstElement(attDefE, "stereotypes");

			if (stsE != null) {

				NodeList stNl = stsE.getElementsByTagName("Stereotype");

				if (stNl != null && stNl.getLength() != 0) {

					for (int k = 0; k < stNl.getLength(); k++) {

						Node n = stNl.item(k);
						if (n.getNodeType() == Node.ELEMENT_NODE) {

							Element stE = (Element) n;
							String st = stE.getTextContent().trim();

							st = options.normalizeStereotype(st);

							for (String s : Options.propertyStereotypes) {
								if (st.toLowerCase().equals(s))
									sts.add(s);
							}
						}
					}
				}
			}
			ad.setStereotypes(sts);

			// parse taggedValues
			TaggedValues tvs = options.taggedValueFactory();
			Element tvsE = getFirstElement(attDefE, "taggedValues");

			if (tvsE != null) {

				NodeList tvNl = tvsE.getElementsByTagName("TaggedValue");

				if (tvNl != null && tvNl.getLength() != 0) {

					for (int k = 0; k < tvNl.getLength(); k++) {

						Node n = tvNl.item(k);
						if (n.getNodeType() == Node.ELEMENT_NODE) {

							Element tvE = (Element) n;
							tvs.add(tvE.getAttribute("name"),
									tvE.getAttribute("value"));
						}
					}
				}
			}
			ad.setTaggedValues(tvs);

			// parse type
			Element typeE = getFirstElement(attDefE, "type");
			String type = typeE.getTextContent().trim();
			if (type.length() == 0) {
				result.addError(this, 3, "type", indexForMsg);
				continue;
			}
			ClassInfo typeCi = model.classByName(type);
			Type tInfo = new Type();
			tInfo.name = type;
			if (typeCi == null) {
				result.addWarning(this, 4, indexForMsg, type, name);
				tInfo.id = "unknown";
			} else {
				tInfo.id = typeCi.id();
			}
			ad.setType(tInfo);

			// finally, add the AttributeDefinition to the result list
			attDefs.add(ad);
		}

		return attDefs;
	}

	private Element getFirstElement(Element parent, String elementName) {

		NodeList nl = parent.getElementsByTagName(elementName);

		if (nl != null && nl.getLength() != 0) {

			for (int k = 0; k < nl.getLength(); k++) {

				Node n = nl.item(k);
				if (n.getNodeType() == Node.ELEMENT_NODE) {

					return (Element) n;
				}
			}
		}

		return null;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 0:
			return "Context: class AttributeCreator";
		case 1:
			return "No 'advancedProcessConfigurations' element present in the configuration. No attributes will be added to the model.";
		case 2:
			return "Syntax exception for regular expression value of attribute '$1$' in '$2$' of $3$ AttributeDefinition element. AttributeDefinition will be ignored.";
		case 3:
			return "'$1$' element of $2$ AttributeDefinition element is empty which is not allowed. AttributeDefinition will be ignored.";
		case 4:
			return "$1$ AttributeDefinition element states that '$2$' shall be the type of the new attribute with name '$3$', but the model does not contain a class with that name. Using 'unknown' as type id and category of value.";
		case 5:
			return "No classes were selected for definition of attribute '$1$'. AttributeDefinition will be ignored.";
		case 6:
			return "Property with name '$1$' already exists in class '$2$'. Because overwriting an existing property is not allowed the AttributeDefinition will be ignored.";

		default:
			return "(Unknown message)";
		}
	}

}
