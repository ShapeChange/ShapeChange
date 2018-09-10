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

package de.interactive_instruments.ShapeChange.ModelDiff;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.Operation;

public class Differ {

	private boolean aaaModel = false;
	private String[] maArrRef = new String[0];
	private diff_match_patch strDiffer = new diff_match_patch();
	private HashSet<ClassInfo> processed = new HashSet<ClassInfo>();

	public Differ() {
	}

	public Differ(boolean aaa, String[] maArr) {
		aaaModel = true;
		maArrRef = maArr;
	}

	private boolean MatchingMA(Info i) {
		if (!aaaModel)
			return true;

		String malist = i.taggedValue("AAA:Modellart");

		if (malist == null)
			return true;

		malist = malist.trim();

		if (malist.length() == 0)
			return true;

		if (maArrRef.length == 0)
			return true;

		for (String ma : malist.split(",")) {
			ma = ma.trim();
			for (String max : maArrRef) {
				if (ma.equals(max.trim())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Adds the content from diff2 to diffs.
	 * 
	 * @param diffs
	 * @param diffs2
	 */
	public void merge(SortedMap<Info, SortedSet<DiffElement>> diffs,
			SortedMap<Info, SortedSet<DiffElement>> diffs2) {
		
		for (Entry<Info, SortedSet<DiffElement>> me : diffs2.entrySet()) {
			if (diffs.containsKey(me.getKey())) {
				diffs.get(me.getKey()).addAll(me.getValue());
			} else {
				diffs.put(me.getKey(), me.getValue());
			}
		}
	}

	/**
	 * @param curr
	 * @param ref
	 * @return map with set of diff elements per Info object; can be empty but
	 *         not <code>null</code>
	 */
	public SortedMap<Info, SortedSet<DiffElement>> diff(PackageInfo curr,
			PackageInfo ref) {

		SortedMap<Info, SortedSet<DiffElement>> diffs = baseDiff(curr, ref);

		Info[] infoArr, infoArrRef;
		SortedMap<Info, SortedSet<DiffElement>> diffs2;

		// diff on subpackages
		SortedSet<PackageInfo> sub = curr.containedPackages();
		infoArr = new Info[sub.size()];
		sub.toArray(infoArr);
		SortedSet<PackageInfo> subRef = (ref==null? new TreeSet<PackageInfo>() : ref.containedPackages());
		infoArrRef = new Info[subRef.size()];
		subRef.toArray(infoArrRef);

		diffs2 = infoDiffs(curr, ElementType.SUBPACKAGE, infoArr, infoArrRef);
		merge(diffs, diffs2);

		// diff on contained classes
		SortedSet<ClassInfo> temp = curr.model().classes(curr);

		SortedSet<ClassInfo> cls = new TreeSet<ClassInfo>();
		for (ClassInfo ci : temp) {
			if (ci.pkg() == curr && MatchingMA(ci))
				cls.add(ci);
		}
		infoArr = new Info[cls.size()];
		cls.toArray(infoArr);
		temp = (ref==null? new TreeSet<ClassInfo>() : ref.model().classes(ref));
		HashSet<ClassInfo> clsRef = new HashSet<ClassInfo>();
		for (ClassInfo ci : temp) {
			if (ci.pkg() == ref && MatchingMA(ci))
				clsRef.add(ci);
		}
		infoArrRef = new Info[clsRef.size()];
		clsRef.toArray(infoArrRef);
		diffs2 = infoDiffs(curr, ElementType.CLASS, infoArr, infoArrRef);
		merge(diffs, diffs2);

		return diffs;
	}

	public SortedMap<Info, SortedSet<DiffElement>> diff(ClassInfo curr,
			ClassInfo ref) {
		if (processed.contains(curr))
			return new TreeMap<Info, SortedSet<DiffElement>>();

		SortedMap<Info, SortedSet<DiffElement>> diffs = baseDiff(curr, ref);

		Info[] infoArr, infoArrRef;
		SortedMap<Info, SortedSet<DiffElement>> diffs2;

		Collection<PropertyInfo> prop = curr.properties().values();
		infoArr = new Info[prop.size()];
		prop.toArray(infoArr);
		Collection<PropertyInfo> propRef = ref.properties().values();
		infoArrRef = new Info[propRef.size()];
		propRef.toArray(infoArrRef);
		if (curr.category() == Options.ENUMERATION
				|| curr.category() == Options.CODELIST)
			diffs2 = infoDiffs(curr, ElementType.ENUM, infoArr, infoArrRef);
		else
			diffs2 = infoDiffs(curr, ElementType.PROPERTY, infoArr, infoArrRef);
		merge(diffs, diffs2);

		SortedSet<String> cls = curr.supertypes();
		infoArr = new Info[cls.size()];
		int i = 0;
		for (String id : cls) {
			infoArr[i++] = curr.model().classById(id);
		}
		SortedSet<String> clsRef = ref.supertypes();
		infoArrRef = new Info[clsRef.size()];
		i = 0;
		for (String id : clsRef) {
			infoArrRef[i++] = ref.model().classById(id);
		}
		diffs2 = infoDiffs(curr, ElementType.SUPERTYPE, infoArr, infoArrRef);
		merge(diffs, diffs2);

		processed.add(curr);

		return diffs;
	}

	public SortedMap<Info, SortedSet<DiffElement>> diff(PropertyInfo curr,
			PropertyInfo ref) {
		SortedMap<Info, SortedSet<DiffElement>> diffs = baseDiff(curr, ref);

		DiffElement diff;

		diff = stringDiff(ElementType.MULTIPLICITY,
				ref.cardinality().toString(), curr.cardinality().toString());
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		diff = stringDiff(ElementType.VALUETYPE, ref.typeInfo().name,
				curr.typeInfo().name);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		return diffs;
	}

	public SortedMap<Info, SortedSet<DiffElement>> diffEnum(PropertyInfo curr,
			PropertyInfo ref) {
		SortedMap<Info, SortedSet<DiffElement>> diffs = baseDiff(curr, ref);

		return diffs;
	}

	protected LinkedList<Diff> aaaDocumentation(String ref, String curr) {
		String[] sa = curr.split("-==-");
		String[] saRef = ref.split("-==-");
		LinkedList<Diff> strdiffs = strDiffer.diff_main(saRef[0], sa[0]);
		boolean heading = false;
		boolean first = true;
		String sRef = null;
		String head = null;
		for (String s1 : sa) {
			if (heading) {
				head = s1;
				boolean found = false;
				for (String s2 : saRef) {
					if (found) {
						sRef = s2;
						break;
					}
					if (s1.equals(s2)) {
						found = true;
					}
				}
			} else {
				if (sRef != null) {
					strdiffs.add(
							new name.fraser.neil.plaintext.diff_match_patch.Diff(
									name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL,
									"-==-" + head + "-==-"));
					strdiffs.addAll(strDiffer.diff_main(sRef, s1));
					sRef = null;
				} else if (!first && !s1.isEmpty()) {
					strdiffs.add(
							new name.fraser.neil.plaintext.diff_match_patch.Diff(
									name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL,
									"-==-" + head + "-==-"));
					strdiffs.add(
							new name.fraser.neil.plaintext.diff_match_patch.Diff(
									name.fraser.neil.plaintext.diff_match_patch.Operation.INSERT,
									s1));
				}
			}
			heading = !heading;
			first = false;
		}
		heading = false;
		first = true;
		sRef = null;
		head = null;
		for (String s2 : saRef) {
			if (heading) {
				head = s2;
				boolean found = false;
				sRef = null;
				for (String s1 : sa) {
					if (found) {
						sRef = s2;
						break;
					}
					if (s1.equals(s2)) {
						found = true;
					}
				}
			} else {
				if (!first && sRef == null && !s2.isEmpty()) {
					strdiffs.add(
							new name.fraser.neil.plaintext.diff_match_patch.Diff(
									name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL,
									"-==-" + head + "-==-"));
					strdiffs.add(
							new name.fraser.neil.plaintext.diff_match_patch.Diff(
									name.fraser.neil.plaintext.diff_match_patch.Operation.DELETE,
									s2));
				}
			}
			heading = !heading;
			first = false;
		}
		return strdiffs;
	}

	protected DiffElement stringDiff(ElementType type, String ref,
			String curr) {

		LinkedList<Diff> strdiffs;

		if (aaaModel && type == ElementType.DOCUMENTATION) {
			strdiffs = aaaDocumentation(ref, curr);
		} else {
			strdiffs = strDiffer.diff_main(ref, curr);
		}

		strDiffer.diff_cleanupEfficiency(strdiffs);

		if (strDiffer.diff_levenshtein(strdiffs) == 0)
			return null;

		DiffElement diff = new DiffElement();
		diff.change = Operation.CHANGE;
		diff.subElementType = type;
		diff.diff = strdiffs;
		return diff;
	}

	@SuppressWarnings("incomplete-switch")
	protected SortedMap<Info, SortedSet<DiffElement>> infoDiffs(Info base,
			ElementType type, Info[] currs, Info[] refs) {

		SortedMap<Info, SortedSet<DiffElement>> diffs = new TreeMap<Info, SortedSet<DiffElement>>();

		DiffElement diff;
		SortedMap<Info, SortedSet<DiffElement>> diffs2;

		for (Info i : currs) {

			if (type != ElementType.SUBPACKAGE && !MatchingMA(i))
				continue;

			boolean found = false;

			for (Info iRef : refs) {

				if (type != ElementType.SUBPACKAGE && !MatchingMA(iRef))
					continue;

				boolean eq = i.name().equalsIgnoreCase(iRef.name());

				if (type == ElementType.ENUM) {
					String s1 = ((PropertyInfo) i).initialValue();
					String s2 = ((PropertyInfo) iRef).initialValue();
					if (s1 != null && s2 != null)
						eq = s1.equalsIgnoreCase(s2);
				}

				if (eq) {
					switch (type) {
					case SUBPACKAGE:
						diffs2 = diff((PackageInfo) i, (PackageInfo) iRef);
						merge(diffs, diffs2);
						break;
					case CLASS:
						diffs2 = diff((ClassInfo) i, (ClassInfo) iRef);
						merge(diffs, diffs2);
						break;
					case PROPERTY:
						diffs2 = diff((PropertyInfo) i, (PropertyInfo) iRef);
						merge(diffs, diffs2);
						break;
					case ENUM:
						diffs2 = diffEnum((PropertyInfo) i,
								(PropertyInfo) iRef);
						merge(diffs, diffs2);
						break;
					}
					found = true;
					break;
				}
			}
			if (!found) {
				diff = new DiffElement();
				diff.change = Operation.INSERT;
				diff.subElementType = type;
				diff.subElement = i;
				if (!diffs.containsKey(base))
					diffs.put(base, new TreeSet<DiffElement>());
				diffs.get(base).add(diff);
			}
		}
		for (Info iRef : refs) {

			if (!MatchingMA(iRef))
				continue;

			boolean found = false;

			for (Info i : currs) {

				if (!MatchingMA(i))
					continue;

				boolean eq = i.name().equalsIgnoreCase(iRef.name());

				if (type == ElementType.ENUM) {
					String s1 = ((PropertyInfo) i).initialValue();
					String s2 = ((PropertyInfo) iRef).initialValue();
					if (s1 != null && s2 != null)
						eq = s1.equalsIgnoreCase(s2);
				}

				if (eq) {
					found = true;
					break;
				}
			}

			if (!found) {
				diff = new DiffElement();
				diff.change = Operation.DELETE;
				diff.subElementType = type;
				diff.subElement = iRef;
				if (!diffs.containsKey(base))
					diffs.put(base, new TreeSet<DiffElement>());
				diffs.get(base).add(diff);
			}
		}
		return diffs;
	}

	private String addConstraints(ClassInfo ci, String doc) {
		for (Constraint ocl : ci.constraints()) {

			// Ignore constraints on supertypes
			if (!ocl.contextModelElmt().id().equals(ci.id()))
				continue;

			doc += "\n\n-==- Konsistenzbedingung ";
			if (!ocl.name().equalsIgnoreCase("alle"))
				doc += ocl.name() + " ";
			doc += "-==-\n";
			String[] sa = ocl.text().split("/\\*");
			for (String sc : sa) {
				sc = sc.trim();
				if (sc.isEmpty())
					continue;
				if (sc.contains("*/")) {
					sc = sc.replaceAll("\\*/.*", "");
					sc = sc.trim();
				}
				doc += "\n" + sc;
			}
		}
		return doc;
	}

	/**
	 * @param curr
	 * @param ref
	 * @return map with set of diff elements per Info object; can be empty but
	 *         not <code>null</code>
	 */
	public SortedMap<Info, SortedSet<DiffElement>> baseDiff(Info curr, Info ref) {

		SortedMap<Info, SortedSet<DiffElement>> diffs = new TreeMap<Info, SortedSet<DiffElement>>();
		DiffElement diff;

		/*
		 * TODO change "documentation" to the specific descriptors, at least for
		 * the non-AAA case
		 * 
		 * Done for legalBasis and primaryCode; TBD: example and
		 * dataCaptureStatement
		 */
		String refs;
		if (aaaModel && curr instanceof ClassInfo) {
			String currdoc = addConstraints((ClassInfo) curr,
					curr.documentation());
			String refdoc = (ref==null ? "" : addConstraints((ClassInfo) ref, ref.documentation()));
			diff = stringDiff(ElementType.DOCUMENTATION, refdoc, currdoc);
		} else {
			refs = (ref==null ? "" : ref.documentation());
			diff = stringDiff(ElementType.DOCUMENTATION, refs, curr.documentation());
		}

		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the name
		refs = (ref==null ? "" : ref.name());
		diff = stringDiff(ElementType.NAME, refs, curr.name());
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the alias
		String s1 = (ref==null? "": ref.aliasName());
		if (s1 == null)
			s1 = "";
		String s2 = curr.aliasName();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.ALIAS, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the definition
		s1 = (ref==null ? "" : ref.definition());
		if (s1 == null)
			s1 = "";
		s2 = curr.definition();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.DEFINITION, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the description
		s1 = (ref==null ? "" : ref.description());
		if (s1 == null)
			s1 = "";
		s2 = curr.description();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.DESCRIPTION, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the legal basis
		s1 = (ref==null ? "" : ref.legalBasis());
		if (s1 == null)
			s1 = "";
		s2 = curr.legalBasis();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.LEGALBASIS, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the primary code
		s1 = (ref==null ? "" : ref.primaryCode());
		if (s1 == null)
			s1 = "";
		s2 = curr.primaryCode();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.PRIMARYCODE, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}
		
		// perform diff for the global identifier
		s1 = (ref==null ? "" : ref.globalIdentifier());
		if (s1 == null)
			s1 = "";
		s2 = curr.globalIdentifier();
		if (s2 == null)
			s2 = "";
		diff = stringDiff(ElementType.GLOBALIDENTIFIER, s1, s2);
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the stereotype
		refs = (ref==null ? "" : ref.stereotypes().toString().replace("[", "").replace("]", ""));
		diff = stringDiff(ElementType.STEREOTYPE,
				refs,
				curr.stereotypes().toString().replace("[", "").replace("]", ""));
		if (diff != null) {
			if (!diffs.containsKey(curr))
				diffs.put(curr, new TreeSet<DiffElement>());
			diffs.get(curr).add(diff);
		}

		// perform diff for the tagged values
		// TODO handle tags with multiple values
		String taglist = curr.options().parameter("representTaggedValues");
		Map<String, String> taggedValues = curr.taggedValues(taglist);
		Map<String, String> taggedValuesRef = (ref==null? null: ref.taggedValues(taglist));

		for (Map.Entry<String, String> entry : taggedValues.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			if (aaaModel & (key.equalsIgnoreCase("AAA:Modellart")
					|| key.equalsIgnoreCase("AAA:Grunddatenbestand"))) {
				String valref = null;
				if (taggedValuesRef!=null && taggedValuesRef.containsKey(key)) {
					valref = taggedValuesRef.get(key);
				}
				if (key.equalsIgnoreCase("AAA:Modellart")) {
					if (val == null || val.isEmpty())
						val = "Alle";
					if (valref == null || valref.isEmpty())
						valref = "Alle";
				} else {
					if (val == null)
						val = "";
					if (valref == null)
						valref = "";
				}
				for (String ma : val.split(",")) {
					ma = ma.trim();
					for (String max : maArrRef) {
						if (ma.equals(max.trim())) {
							boolean found = false;
							for (String ma2 : valref.split(",")) {
								ma2 = ma2.trim();
								if (ma2.equals(ma)) {
									found = true;
									break;
								}
							}
							if (!found) {
								diff = new DiffElement();
								diff.change = Operation.INSERT;
								if (key.equalsIgnoreCase("AAA:Modellart"))
									diff.subElementType = ElementType.AAAMODELLART;
								else
									diff.subElementType = ElementType.AAAGRUNDDATENBESTAND;
								diff.tag = ma;
								if (!diffs.containsKey(curr))
									diffs.put(curr, new TreeSet<DiffElement>());
								diffs.get(curr).add(diff);
							}
						}
					}
				}
			} else {
				if (taggedValuesRef!=null && taggedValuesRef.containsKey(key)) {
					String valref = taggedValuesRef.get(key);
					diff = stringDiff(ElementType.TAG, valref, val);
				} else {
					diff = stringDiff(ElementType.TAG, "", val);
				}
				if (diff != null) {
					diff.tag = key;
					if (!diffs.containsKey(curr))
						diffs.put(curr, new TreeSet<DiffElement>());
					diffs.get(curr).add(diff);
				}
			}
		}
		if (taggedValuesRef!=null) {
			for (Map.Entry<String, String> entry : taggedValuesRef.entrySet()) {
				String key = entry.getKey();
				if (aaaModel & (key.equalsIgnoreCase("AAA:Modellart")
						|| key.equalsIgnoreCase("AAA:Grunddatenbestand"))) {
					String valref = taggedValuesRef.get(key);
					String val = null;
					if (taggedValues.containsKey(key)) {
						val = taggedValues.get(key);
					}
					if (key.equalsIgnoreCase("AAA:Modellart")) {
						if (val == null || val.isEmpty())
							val = "Alle";
						if (valref == null || valref.isEmpty())
							val = "Alle";
					} else {
						if (val == null)
							val = "";
						if (valref == null)
							val = "";
					}
					for (String ma : valref.split(",")) {
						ma = ma.trim();
						for (String max : maArrRef) {
							if (ma.equals(max.trim())) {
								boolean found = false;
								for (String ma2 : val.split(",")) {
									ma2 = ma2.trim();
									if (ma2.equals(ma)) {
										found = true;
										break;
									}
								}
								if (!found) {
									diff = new DiffElement();
									diff.change = Operation.DELETE;
									if (key.equalsIgnoreCase("AAA:Modellart"))
										diff.subElementType = ElementType.AAAMODELLART;
									else
										diff.subElementType = ElementType.AAAGRUNDDATENBESTAND;
									diff.tag = ma;
									if (!diffs.containsKey(curr))
										diffs.put(curr, new TreeSet<DiffElement>());
									diffs.get(curr).add(diff);
								}
							}
						}
					}
				} else {
					if (!taggedValues.containsKey(key)) {
						diff = stringDiff(ElementType.TAG, entry.getValue(), "");
						if (diff != null) {
							diff.tag = key;
							if (!diffs.containsKey(curr))
								diffs.put(curr, new TreeSet<DiffElement>());
							diffs.get(curr).add(diff);
						}
					}
				}
			}
		}
		
		return diffs;
	}

	public String diff_toString(LinkedList<Diff> diffs) {
		StringBuilder res = new StringBuilder();
		for (Diff aDiff : diffs) {
			String text = aDiff.text;
			switch (aDiff.operation) {
			case INSERT:
				res.append("[[ins]]").append(text).append("[[/ins]]");
				break;
			case DELETE:
				res.append("[[del]]").append(text).append("[[/del]]");
				break;
			case EQUAL:
				res.append(text);
				break;
			}
		}
		return res.toString();
	}
}
