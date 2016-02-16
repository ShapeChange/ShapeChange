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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sparx.Attribute;
import org.sparx.AttributeTag;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.ConnectorTag;
import org.sparx.Element;
import org.sparx.Package;
import org.sparx.Repository;
import org.sparx.RoleTag;
import org.sparx.TaggedValue;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author Johannes Echterhoff
 *
 */
public class EAModelUtil {

	/**
	 * Creates an EA package for the given PackageInfo. The new EA package will
	 * be a child of the given EA parent package. Properties such as stereotype
	 * and tagged values are not set by this method and thus need to be added
	 * later on.
	 * 
	 * @param pi
	 *            package to create in EA
	 * @param eaParentPkg
	 *            EA Package element that is the parent of the EA Package to
	 *            create for pi
	 * @throws EAException
	 *             If an EA error was encountered while updating the package
	 */
	public static int createEAPackage(Repository rep, PackageInfo pi,
			int eaParentPkgId) throws EAException {

		Package eaParentPkg = rep.GetPackageByID(eaParentPkgId);

		Collection<Package> eaParentPkgs = eaParentPkg.GetPackages();

		Package eaPkg = eaParentPkgs.AddNew(pi.name(), "Package");

		if (!eaPkg.Update()) {

			throw new EAException(
					createMessage(101, pi.name(), eaPkg.GetLastError()));
		}

		eaParentPkgs.Refresh();

		return eaPkg.GetPackageID();
	}

	public static Element createEAClass(Repository rep, String className,
			int eaPkgId) throws EAException {

		Package eaPkg = rep.GetPackageByID(eaPkgId);

		Collection<Element> elements = eaPkg.GetElements();

		Element e = elements.AddNew(className, "Class");

		if (!e.Update()) {
			throw new EAException(
					createMessage(201, className, e.GetLastError()));
		}

		elements.Refresh();

		return e;
	}

	public static void setEAAlias(String aliasName, Element e)
			throws EAException {

		e.SetAlias(aliasName);

		if (!e.Update()) {
			throw new EAException(
					createMessage(201, e.GetName(), e.GetLastError()));
		}
		e.Refresh();
	}

	public static void setEANotes(String documentation, Element e)
			throws EAException {

		e.SetNotes(documentation);

		if (!e.Update()) {
			throw new EAException(
					createMessage(201, e.GetName(), e.GetLastError()));
		}
		e.Refresh();
	}

	public static void setEAStereotype(Element e, String stereotype)
			throws EAException {

		e.SetStereotypeEx(stereotype);

		if (!e.Update()) {
			throw new EAException(
					createMessage(201, e.GetName(), e.GetLastError()));
		}
		e.Refresh();
	}

	/**
	 * @param stereotypes
	 * @return comma separated list of the set of given stereotypes
	 */
	public static String stereotypesCSV(Set<String> stereotypes) {
		String res = "";
		if (stereotypes != null)
			for (String s : stereotypes) {
				res += (res.isEmpty() ? "" : ",");
				res += s;
			}
		return res;
	}

	public static String createMessage(int mnr, String p1, String p2, String p3,
			String p4) {
		String m = message(mnr);
		return m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3)
				.replace("$4$", p4);
	}

	public static String createMessage(int mnr, String p1, String p2,
			String p3) {
		String m = message(mnr);
		return m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3);
	}

	public static String createMessage(int mnr, String p1, String p2) {
		String m = message(mnr);
		return m.replace("$1$", p1).replace("$2$", p2);
	}

	public static String createMessage(int mnr, String p1) {
		String m = message(mnr);
		return m.replace("$1$", p1);
	}

	public static String createMessage(int mnr) {
		return message(mnr);
	}

	public static String message(int mnr) {

		switch (mnr) {

		/**
		 * Number ranges defined as follows:
		 * <ul>
		 * <li>1-100: model related messages</li>
		 * <li>101-200: package related messages</li>
		 * <li>201-300: class related messages</li>
		 * <li>301-400: property related messages</li>
		 * <li>401-500: tagged value related messages</li>
		 * <li>501-600: connector related messages</li>
		 * <li>601-700: connector end related messages</li>
		 * </ul>
		 */
		case 101:
			return "EA error encountered while updating EA package '$1$'. Error message is: $2$";
		case 201:
			return "EA error encountered while updating EA class element '$1$'. Error message is: $2$";
		case 202:
			return "EA error encountered while updating EA class element '$1$' (attempted to convert to association class for association '$2$'). Error message is: $3$";
		case 301:
			return "EA error encountered while updating EA attribute element '$1$'. Error message is: $2$";
		case 401:
			return "EA error encountered while updating EA tagged value '$1$' with value '$2$'. Error message is: $3$";
		case 501:
			return "EA error encountered while updating EA connector between classes '$1$' and '$2$'. Error message is: $3$";
		case 502:
			return "EA error encountered while updating EA connector '$1$ (attempted to assign association class '$2$'). Error message is $3$";
		case 503:
			return "Did not succeed in converting class '$1$' to an association class of connector '$2$'.";
		case 601:
			return "EA error encountered while updating EA connector end. Error message is: $1$";
		default:
			return "(Unknown message)";
		}
	}

	public static Integer getEAChildPackageByName(Repository rep,
			int eaParentPkgId, String name) {

		Package eaParentPkg = rep.GetPackageByID(eaParentPkgId);

		Collection<Package> pkgs = eaParentPkg.GetPackages();

		for (short i = 0; i < pkgs.GetCount(); i++) {

			Package p = pkgs.GetAt(i);
			if (p.GetName().equals(name)) {
				return p.GetPackageID();
			}
		}

		return null;
	}

	public static void setEAAbstract(boolean isAbstract, Element e)
			throws EAException {

		e.SetAbstract("true");

		if (!e.Update()) {
			throw new EAException(
					createMessage(201, e.GetName(), e.GetLastError()));
		}

		e.Refresh();
	}

	public static Attribute createEAAttribute(Element e, String name,
			String alias, String documentation, Set<String> stereotypes,
			List<EATaggedValue> taggedValues, boolean isDerived,
			boolean isOrdered, String initialValue, Multiplicity m, String type,
			Integer classifierID) throws EAException {

		Collection<Attribute> atts = e.GetAttributes();

		Attribute att = atts.AddNew(name, "");
		if (!att.Update()) {
			throw new EAException(createMessage(301, name, e.GetLastError()));
		}
		atts.Refresh();

		if (alias != null) {
			att.SetStyle(alias);
		}

		if (documentation != null) {
			att.SetNotes(documentation);
		}

		if (stereotypes != null) {
			att.SetStereotypeEx(stereotypesCSV(stereotypes));
		}

		if (taggedValues != null) {
			setTaggedValues(att, taggedValues);
		}

		att.SetIsDerived(isDerived);

		att.SetIsOrdered(isOrdered);

		if (initialValue != null) {
			att.SetDefault(initialValue);
		}

		att.SetVisibility("public");

		if (m != null) {
			att.SetLowerBound("" + m.minOccurs);
			if (m.maxOccurs == Integer.MAX_VALUE)
				att.SetUpperBound("*");
			else
				att.SetUpperBound("" + m.maxOccurs);
		}

		if (type != null) {
			att.SetType(type);
		}

		if (classifierID != null) {
			att.SetClassifierID(classifierID);
		}

		if (!att.Update()) {
			throw new EAException(createMessage(301, name, e.GetLastError()));
		}

		return att;
	}

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given attribute, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param att
	 *            the attribute to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(Attribute att, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();

			for (String tag : tvs.keySet()) {

				String[] values = tvs.get(tag);

				if (values != null) {

					for (String v : values) {

						AttributeTag tv = cTV.AddNew(tag, "");
						cTV.Refresh();

						if (v.length() > 255) {
							tv.SetValue("<memo>");
							tv.SetNotes(v);
						} else {
							tv.SetValue(v);
							tv.SetNotes("");
						}

						if (!tv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									tv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given tagged values in the tagged values of the given attribute.
	 * If tagged values with same tag names already exist, they will be
	 * overwritten. If fewer values existed than shall be set, new values will
	 * be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param att
	 *            the attribute
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Attribute att, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<AttributeTag>> existingTvsByName = new HashMap<String, List<AttributeTag>>();
			for (AttributeTag exTv : cTV) {

				String name = exTv.GetName();

				List<AttributeTag> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<AttributeTag>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (String tag : tvs.keySet()) {

				String[] values = tvs.get(tag);

				if (values != null) {

					List<AttributeTag> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<AttributeTag>();
					}

					if (existingTvs.size() < values.length) {

						// add new tagged values
						for (int i = existingTvs
								.size(); i < values.length; i++) {
							AttributeTag eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.length) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.length; i++) {

						String v = values[i];

						AttributeTag tv = existingTvs.get(i);

						if (v.length() > 255) {
							tv.SetValue("<memo>");
							tv.SetNotes(v);
						} else {
							tv.SetValue(v);
							tv.SetNotes("");
						}

						if (!tv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									tv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given tagged values in the tagged values of the given element.
	 * If tagged values with same tag names already exist, they will be
	 * overwritten. If fewer values existed than shall be set, new values will
	 * be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param e
	 *            the element
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Element e, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<TaggedValue> cTV = e.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<TaggedValue>> existingTvsByName = new HashMap<String, List<TaggedValue>>();
			for (TaggedValue exTv : cTV) {

				String name = exTv.GetName();

				List<TaggedValue> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<TaggedValue>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (String tag : tvs.keySet()) {

				String[] values = tvs.get(tag);

				if (values != null) {

					List<TaggedValue> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<TaggedValue>();
					}

					if (existingTvs.size() < values.length) {

						// add new tagged values
						for (int i = existingTvs
								.size(); i < values.length; i++) {
							TaggedValue eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.length) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.length; i++) {

						String v = values[i];

						TaggedValue tv = existingTvs.get(i);

						if (v.length() > 255) {
							tv.SetValue("<memo>");
							tv.SetNotes(v);
						} else {
							tv.SetValue(v);
							tv.SetNotes("");
						}

						if (!tv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									tv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given list of tagged values in the tagged values of the given
	 * element. If tagged values with same tag names already exist, they will be
	 * overwritten. If fewer values existed than shall be set, new values will
	 * be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param e
	 *            the element
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Element e, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<TaggedValue> cTV = e.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<TaggedValue>> existingTvsByName = new HashMap<String, List<TaggedValue>>();
			for (TaggedValue exTv : cTV) {

				String name = exTv.GetName();

				List<TaggedValue> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<TaggedValue>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (EATaggedValue tv : tvs) {

				String tag = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					List<TaggedValue> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<TaggedValue>();
					}

					if (existingTvs.size() < values.size()) {

						// add new tagged values
						for (int i = existingTvs.size(); i < values
								.size(); i++) {
							TaggedValue eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.size()) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.size(); i++) {

						String v = values.get(i);

						TaggedValue eaTv = existingTvs.get(i);

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds the given tagged value to the tagged values of the given element,
	 * NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param e
	 *            the element to which the tagged value shall be added
	 * @param tv
	 *            tagged value to add
	 */
	public static void addTaggedValue(Element e, EATaggedValue tv)
			throws EAException {

		Collection<TaggedValue> cTV = e.GetTaggedValues();

		String name = tv.getName();
		List<String> values = tv.getValues();

		if (values != null) {

			for (String v : values) {

				TaggedValue eaTv = cTV.AddNew(name, "");
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
							createMessage(401, name, v, eaTv.GetLastError()));
				}
			}
		}
	}

	/**
	 * Sets the given tagged value in the tagged values of the given element. If
	 * tagged values with the same tag name already exist, they will be
	 * overwritten. If fewer values existed than shall be set, new values will
	 * be added.
	 * <p>
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param e
	 *            the element in which the tagged value shall be set
	 * @param tv
	 *            tagged value to set
	 */
	public static void setTaggedValue(Element e, EATaggedValue tv)
			throws EAException {

		String name = tv.getName();
		List<String> values = tv.getValues();

		if (values != null) {

			Collection<TaggedValue> cTV = e.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values for given name
			List<TaggedValue> existingTvs = new ArrayList<TaggedValue>();
			for (TaggedValue exTv : cTV) {
				if (exTv.GetName().equalsIgnoreCase(name)) {
					existingTvs.add(exTv);
				}
			}

			if (existingTvs.size() < values.size()) {

				// add new tagged values
				for (int i = existingTvs.size(); i < values.size(); i++) {
					TaggedValue eaTv = cTV.AddNew(name, "");
					existingTvs.add(eaTv);
				}
				cTV.Refresh();

			} else if (existingTvs.size() > values.size()) {
				/*
				 * TODO remove excess tagged values (requires that we keep track
				 * of indices); shouldn't be required at the moment
				 */
			}

			// overwrite existing tagged value objects

			for (int i = 0; i < values.size(); i++) {

				String v = values.get(i);

				TaggedValue eaTv = existingTvs.get(i);

				if (tv.createAsMemoField() || v.length() > 255) {
					eaTv.SetValue("<memo>");
					eaTv.SetNotes(v);
				} else {
					eaTv.SetValue(v);
					eaTv.SetNotes("");
				}

				if (!eaTv.Update()) {
					throw new EAException(
							createMessage(401, name, v, eaTv.GetLastError()));
				}
			}
		}
	}

	/**
	 * Adds the given list of tagged values to the collection of (EA) tagged
	 * values of the given element, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param cTV
	 *            the collection of existing tagged values
	 * @param tvs
	 *            collection of tagged values to set
	 */
	public static void addTaggedValues(Element e, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<TaggedValue> cTV = e.GetTaggedValues();

			for (EATaggedValue tv : tvs) {

				String name = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					for (String v : values) {

						TaggedValue eaTv = cTV.AddNew(name, "");
						cTV.Refresh();

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given attribute, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param att
	 *            the attribute to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(Attribute att, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();

			for (EATaggedValue tv : tvs) {

				String name = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					for (String v : values) {

						AttributeTag eaTv = cTV.AddNew(name, "");
						cTV.Refresh();

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given list of tagged values in the tagged values of the given
	 * attribute. If tagged values with same tag names already exist, they will
	 * be overwritten. If fewer values existed than shall be set, new values
	 * will be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param att
	 *            the attribute
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Attribute att, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<AttributeTag>> existingTvsByName = new HashMap<String, List<AttributeTag>>();
			for (AttributeTag exTv : cTV) {

				String name = exTv.GetName();

				List<AttributeTag> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<AttributeTag>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (EATaggedValue tv : tvs) {

				String tag = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					List<AttributeTag> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<AttributeTag>();
					}

					if (existingTvs.size() < values.size()) {

						// add new tagged values
						for (int i = existingTvs.size(); i < values
								.size(); i++) {
							AttributeTag eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.size()) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.size(); i++) {

						String v = values.get(i);

						AttributeTag eaTv = existingTvs.get(i);

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	public static void createEAGeneralization(Repository rep, int c1ElementId,
			String c1Name, int c2ElementId, String c2Name) throws EAException {

		Element c1 = rep.GetElementByID(c1ElementId);

		Collection<Connector> c1Cons = c1.GetConnectors();

		Connector con = c1Cons.AddNew("", "Generalization");

		con.SetSupplierID(c2ElementId);

		if (!con.Update()) {
			throw new EAException(
					createMessage(501, c1Name, c2Name, con.GetLastError()));
		}

		c1Cons.Refresh();
	}

	public static Connector createEAAssociation(Element client,
			Element supplier) throws EAException {

		Collection<Connector> clientCons = client.GetConnectors();

		Connector con = clientCons.AddNew("", "Association");

		con.SetSupplierID(supplier.GetElementID());
		// con.SetDirection("Bi-Directional");

		if (!con.Update()) {
			throw new EAException(createMessage(501, client.GetName(),
					supplier.GetName(), con.GetLastError()));
		}

		clientCons.Refresh();

		return con;
	}

	public static void setEARole(ConnectorEnd conEnd, String role)
			throws EAException {

		conEnd.SetRole(role);

		if (!conEnd.Update()) {
			throw new EAException(createMessage(601, conEnd.GetLastError()));
		}
	}

	public static void setEACardinality(ConnectorEnd conEnd,
			String cardinalityAsString) throws EAException {

		conEnd.SetCardinality(cardinalityAsString);

		if (!conEnd.Update()) {
			throw new EAException(createMessage(601, conEnd.GetLastError()));
		}
	}

	public static void setEAName(Connector con, String name)
			throws EAException {

		con.SetName(name);

		if (!con.Update()) {
			throw new EAException(createMessage(501, con.GetLastError()));
		}
	}

	public static void setEAStereotype(Connector con, String stereotype)
			throws EAException {

		con.SetStereotype(stereotype);

		if (!con.Update()) {
			throw new EAException(createMessage(501, con.GetLastError()));
		}
	}

	/**
	 * Adds the given collection of tagged values to the given connector, NOT
	 * checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param con
	 *            the connector to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(Connector con, List<EATaggedValue> tvs)
			throws EAException {

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
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given list of tagged values in the tagged values of the given
	 * connector. If tagged values with same tag names already exist, they will
	 * be overwritten. If fewer values existed than shall be set, new values
	 * will be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param con
	 *            the connector
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Connector con, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<ConnectorTag> cTV = con.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<ConnectorTag>> existingTvsByName = new HashMap<String, List<ConnectorTag>>();
			for (ConnectorTag exTv : cTV) {

				String name = exTv.GetName();

				List<ConnectorTag> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<ConnectorTag>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (EATaggedValue tv : tvs) {

				String tag = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					List<ConnectorTag> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<ConnectorTag>();
					}

					if (existingTvs.size() < values.size()) {

						// add new tagged values
						for (int i = existingTvs.size(); i < values
								.size(); i++) {
							ConnectorTag eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.size()) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.size(); i++) {

						String v = values.get(i);

						ConnectorTag eaTv = existingTvs.get(i);

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									eaTv.GetLastError()));
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
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param con
	 *            the connector to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(Connector con, TaggedValues tvs)
			throws EAException {

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
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given list of tagged values in the tagged values of the given
	 * connector. If tagged values with same tag names already exist, they will
	 * be overwritten. If fewer values existed than shall be set, new values
	 * will be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param con
	 *            the connector
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(Connector con, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<ConnectorTag> cTV = con.GetTaggedValues();
			cTV.Refresh();

			// identify existing tagged values
			Map<String, List<ConnectorTag>> existingTvsByName = new HashMap<String, List<ConnectorTag>>();
			for (ConnectorTag exTv : cTV) {

				String name = exTv.GetName();

				List<ConnectorTag> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<ConnectorTag>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (Entry<String, List<String>> e : tvs.asMap().entrySet()) {

				String tag = e.getKey();
				List<String> values = e.getValue();

				if (values != null) {

					List<ConnectorTag> existingTvs;

					if (existingTvsByName.containsKey(tag)) {
						existingTvs = existingTvsByName.get(tag);
					} else {
						existingTvs = new ArrayList<ConnectorTag>();
					}

					if (existingTvs.size() < values.size()) {

						// add new tagged values
						for (int i = existingTvs.size(); i < values
								.size(); i++) {
							ConnectorTag eaTv = cTV.AddNew(tag, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.size()) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.size(); i++) {

						String v = values.get(i);

						ConnectorTag eaTv = existingTvs.get(i);

						if (v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, tag, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds the given collection of tagged values to the given connector end,
	 * NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param e
	 *            the connector end to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(ConnectorEnd end, TaggedValues tvs)
			throws EAException {

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
						 * 2015-11-02 JE: apparently <memo> mechanism not
						 * available for role tags
						 */
						eaTv.SetValue(v);

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets the given tagged values in the tagged values of the given connector
	 * end. If tagged values with same tag names already exist, they will be
	 * overwritten. If fewer values existed than shall be set, new values will
	 * be added.
	 * 
	 * NOTE: does currently not delete excess values (those values that already
	 * exist but that aren't needed to store the new values).
	 * 
	 * @param end
	 *            the connector end
	 * @param tvs
	 *            tagged values to set
	 */
	public static void setTaggedValues(ConnectorEnd end, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<RoleTag> cTV = end.GetTaggedValues();

			// identify existing tagged values
			Map<String, List<RoleTag>> existingTvsByName = new HashMap<String, List<RoleTag>>();
			for (RoleTag exTv : cTV) {

				String name = exTv.GetTag();

				List<RoleTag> exTvs;

				if (existingTvsByName.containsKey(name)) {
					exTvs = existingTvsByName.get(name);
				} else {
					exTvs = new ArrayList<RoleTag>();
					existingTvsByName.put(name, exTvs);
				}

				exTvs.add(exTv);
			}

			for (Entry<String, List<String>> e : tvs.asMap().entrySet()) {

				String name = e.getKey();
				List<String> values = e.getValue();

				if (values != null) {

					List<RoleTag> existingTvs;

					if (existingTvsByName.containsKey(name)) {
						existingTvs = existingTvsByName.get(name);
					} else {
						existingTvs = new ArrayList<RoleTag>();
					}

					if (existingTvs.size() < values.size()) {

						// add new tagged values
						for (int i = existingTvs.size(); i < values
								.size(); i++) {
							RoleTag eaTv = cTV.AddNew(name, "");
							existingTvs.add(eaTv);
						}
						cTV.Refresh();

					} else if (existingTvs.size() > values.size()) {
						/*
						 * TODO remove excess tagged values (requires that we
						 * keep track of indices); shouldn't be required at the
						 * moment
						 */
					}

					// overwrite existing tagged value objects

					for (int i = 0; i < values.size(); i++) {

						String v = values.get(i);

						RoleTag eaTv = existingTvs.get(i);

						/*
						 * Association ends support a <memo> mechanism - see
						 * PropertyInfoEA.validateTaggedValuesCache() for
						 * further details.
						 * 
						 * For now we simply set the value as-is.
						 */
						eaTv.SetValue(v);

						if (!eaTv.Update()) {
							throw new EAException(createMessage(401, name, v,
									eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * NOTE: only works with EA 12 Java API, not the previous API.
	 * 
	 * @param con
	 * @param classElement
	 * @throws EAException
	 */
	public static void setEAAssociationClass(Connector con,
			Element classElement) throws EAException {

		int connectorID = con.GetConnectorID();

		classElement.CreateAssociationClass(connectorID);
	}

	/*
	 * 2015-06-25 JE: direct manipulation of MS Access DB to establish
	 * relationship between an AssociationClass and its connector no longer
	 * necessary with EA v12 (beta had issues, but full version works). We
	 * decided to rely on the EA 12 API. That means that the ArcGIS Workspace
	 * target won't work with deployments that use a previous version of EA. In
	 * any case, the direct manipulation of the DB no longer works with Java 8
	 * and later, because the sun jdbc odbc bridge that was used to connect to
	 * the DB has been removed in Java 8. At this point in time there is no
	 * feasible alternative (tested ucanaccess and jackcess - both didn't work).
	 */
	// public static void setEAAssociationClass(Connector con,
	// Element classElement, String eapFileAbsolutePath)
	// throws EAException {
	//
	// /*
	// * NOTE: the EA Java API does not support creating an association class.
	// * The API prior to EA v12 does not allow to set the required fields in
	// * t_connector and t_object. The EA v12 BETA API only sets one of them.
	// *
	// * Thus we set the fields directly via JDBC.
	// */
	//
	// int connectorID = con.GetConnectorID();
	// int elementID = classElement.GetElementID();
	//
	// /*
	// * 2015-06-23 JE: JDBC-ODBC bridge apparently removed in Java 8! Need to
	// * find a new solution that works with Java 8. FIXME
	// */
	//
	// String dbJdbcDriver = "net.ucanaccess.jdbc.UcanaccessDriver"; //
	// String dbDriver = "jdbc:ucanaccess://";
	// // String dbJdbcDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
	// // String dbDriver =
	// // "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)}";
	// String dbServiceName = eapFileAbsolutePath;
	//
	// EAAccessConnection dbConn = null;
	//
	// try {
	//
	// dbConn = new EAAccessConnection(dbJdbcDriver, dbDriver,
	// dbServiceName, null, null);
	//
	// dbConn.establishAssociationClass(connectorID, elementID);
	//
	// } catch (DatabaseException e) {
	// throw new EAException(
	// "Could not establish association class due to database exception. Message
	// is: "
	// + e.getMessage());
	// } finally {
	// if (dbConn != null) {
	// dbConn.close();
	// }
	// }
	// }
}
