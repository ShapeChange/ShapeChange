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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement2.ElementChangeType;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement2.Operation;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

/**
 * Computes a diff between two given schemas, typically provided in different
 * models, representing different versions of the same schema.
 * 
 * NOTE: This class is a revision of the {@link Differ} class. It was created in
 * support of the
 * {@link de.interactive_instruments.ShapeChange.Target.Diff.DiffTarget}. The
 * way in which model differences are reported is different to how the old
 * Differ reports them.
 * 
 * Each model element change is reported explicitly, as a diff-object
 * ({@link DiffElement2}) which contains references to relevant elements from
 * the source and/or the target schema - depending upon the actual model change.
 * That also includes reports for Info-objects (packages, classes, properties)
 * that have been deleted (exist in the source schema but not in the target
 * schema) or inserted (exist in the target schema but not in the source schema)
 * - making it easier to evaluate for a given Info object what happened to it.
 * 
 * The computation supports mappings for packages (that may have been moved or
 * renamed within the schema), classes (renamed), and properties (renamed, also
 * taking into account mapping of the owning class).
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Differ2 {

    private boolean aaaModel = false;
    private Set<String> relevanteModellarten = new HashSet<>();
    private diff_match_patch strDiffer = new diff_match_patch();
    private MapEntryParamInfos mapEntryParamInfos;
    private Pattern tagsToDiff;

    public Differ2(MapEntryParamInfos mapEntryParamInfos, Pattern tagsToDiff) {
	this.mapEntryParamInfos = mapEntryParamInfos;
	this.tagsToDiff = tagsToDiff;
    }

    public Differ2(boolean aaa, Set<String> relevanteModellarten, MapEntryParamInfos mapEntryParamInfos,
	    Pattern tagsToDiff) {
	this.aaaModel = aaa;
	this.relevanteModellarten = relevanteModellarten;
	this.mapEntryParamInfos = mapEntryParamInfos;
	this.tagsToDiff = tagsToDiff;
    }

    private boolean matchingAaaModellart(Info i) {

	if (!aaaModel)
	    return true;

	String malist = i.taggedValue("AAA:Modellart");

	if (relevanteModellarten.isEmpty() || StringUtils.isBlank(malist)) {
	    return true;
	}

	for (String ma : malist.trim().split(",")) {
	    if (relevanteModellarten.contains(ma.trim())) {
		return true;
	    }
	}

	return false;
    }

    /**
     * @param source tbd, not <code>null</code>
     * @param target tbd, not <code>null</code>
     * @return map with set of diff elements per Info object; can be empty but not
     *         <code>null</code>
     */
    public List<DiffElement2> diff(PackageInfo source, PackageInfo target) {

	List<DiffElement2> diffs = baseDiff(source, target);

	infoDiffs(diffs, source, target, ElementChangeType.SUBPACKAGE, source.containedPackages(),
		target.containedPackages());

	infoDiffs(diffs, source, target, ElementChangeType.CLASS, source.containedClasses(), target.containedClasses());

	return diffs;
    }

    public List<DiffElement2> diff(ClassInfo source, ClassInfo target) {

	List<DiffElement2> diffs = baseDiff(source, target);

	if (target.category() == Options.ENUMERATION || target.category() == Options.CODELIST) {
	    infoDiffs(diffs, source, target, ElementChangeType.ENUM, source.properties().values(),
		    target.properties().values());
	} else {
	    infoDiffs(diffs, source, target, ElementChangeType.PROPERTY, source.properties().values(),
		    target.properties().values());
	}

	infoDiffs(diffs, source, target, ElementChangeType.SUPERTYPE, source.supertypeClasses(),
		target.supertypeClasses());

	return diffs;
    }

    public List<DiffElement2> diff(PropertyInfo source, PropertyInfo target) {

	List<DiffElement2> diffs = baseDiff(source, target);

	stringDiff(diffs, ElementChangeType.MULTIPLICITY, source, target, source.cardinality().toString(),
		target.cardinality().toString());

	// apply class mappings on source value type before comparing value types
	String sourceValueType = mapTypeName(source.typeInfo().name, source.encodingRule("modeldiff"));

	stringDiff(diffs, ElementChangeType.VALUETYPE, source, target, sourceValueType, target.typeInfo().name);

	stringDiff(diffs, ElementChangeType.INITIALVALUE, source, target, source.initialValue(), target.initialValue());

	return diffs;
    }

    public List<DiffElement2> diffEnum(PropertyInfo source, PropertyInfo target) {

	List<DiffElement2> diffs = baseDiff(source, target);
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
		    strdiffs.add(new name.fraser.neil.plaintext.diff_match_patch.Diff(
			    name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL, "-==-" + head + "-==-"));
		    strdiffs.addAll(strDiffer.diff_main(sRef, s1));
		    sRef = null;
		} else if (!first && !s1.isEmpty()) {
		    strdiffs.add(new name.fraser.neil.plaintext.diff_match_patch.Diff(
			    name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL, "-==-" + head + "-==-"));
		    strdiffs.add(new name.fraser.neil.plaintext.diff_match_patch.Diff(
			    name.fraser.neil.plaintext.diff_match_patch.Operation.INSERT, s1));
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
		    strdiffs.add(new name.fraser.neil.plaintext.diff_match_patch.Diff(
			    name.fraser.neil.plaintext.diff_match_patch.Operation.EQUAL, "-==-" + head + "-==-"));
		    strdiffs.add(new name.fraser.neil.plaintext.diff_match_patch.Diff(
			    name.fraser.neil.plaintext.diff_match_patch.Operation.DELETE, s2));
		}
	    }
	    heading = !heading;
	    first = false;
	}
	return strdiffs;
    }

    public List<DiffElement2> diffSchemas(PackageInfo sourceSchema, PackageInfo targetSchema) {

	List<DiffElement2> diffs = new ArrayList<>();

	/*
	 * find packages in source schema that have matching package in target schema
	 * (match = same full name in schema, potentially modified by map entries); all
	 * other packages from source schema are considered to have been deleted
	 * 
	 * Then compute the packages in the target schema which have no equivalence in
	 * the source schema, and consider them as having been inserted
	 * 
	 * do a detailed comparison of matching packages
	 * 
	 * create diff elements for deleted and inserted packages
	 * 
	 * repeat the process for schema classes (and their properties, but the direct
	 * properties of matching classes that are being diffed)
	 */

	// get all packages in source schema
	SortedSet<PackageInfo> sourcePackages = sourceSchema.containedPackagesInSameTargetNamespace();
	sourcePackages.add(sourceSchema);

	// get all packages in target schema
	SortedSet<PackageInfo> targetPackages = targetSchema.containedPackagesInSameTargetNamespace();
	targetPackages.add(targetSchema);
	Map<String, PackageInfo> targetPackagesByFullNameInSchema = new HashMap<>();
	for (PackageInfo pkg : targetPackages) {
	    targetPackagesByFullNameInSchema.put(pkg.fullNameInSchema(), pkg);
	}

	Map<PackageInfo, PackageInfo> matchingTargetPkgBySourcePkg = new HashMap<>();
	for (PackageInfo sourcePi : sourcePackages) {
	    PackageInfo targetPi = findMatchingTargetPackage(sourcePi, targetPackagesByFullNameInSchema);
	    if (targetPi != null) {
		matchingTargetPkgBySourcePkg.put(sourcePi, targetPi);
	    }
	}

	SortedSet<PackageInfo> deletedSourcePackages = new TreeSet<>(sourcePackages);
	deletedSourcePackages.removeAll(matchingTargetPkgBySourcePkg.keySet());

	SortedSet<PackageInfo> insertedTargetPackages = new TreeSet<>(targetPackages);
	insertedTargetPackages.removeAll(matchingTargetPkgBySourcePkg.values());

	for (PackageInfo pi : deletedSourcePackages) {
	    DiffElement2 diff = new DiffElement2(Operation.DELETE, ElementChangeType.SELF, null, pi, null, null, null);
	    diffs.add(diff);
	}

	for (PackageInfo pi : insertedTargetPackages) {
	    DiffElement2 diff = new DiffElement2(Operation.INSERT, ElementChangeType.SELF, null, null, pi, null, null);
	    diffs.add(diff);
	}

	for (Entry<PackageInfo, PackageInfo> entry : matchingTargetPkgBySourcePkg.entrySet()) {

	    PackageInfo sourcePi = entry.getKey();
	    PackageInfo targetPi = entry.getValue();

	    diffs.addAll(diff(sourcePi, targetPi));
	}

	// DIFF SCHEMA CLASSES

	SortedSet<ClassInfo> sourceClasses = sourceSchema.model().classes(sourceSchema);
	SortedSet<ClassInfo> targetClasses = targetSchema.model().classes(targetSchema);

	Map<ClassInfo, ClassInfo> matchingTargetClsBySourceCls = new HashMap<>();
	for (ClassInfo sourceCi : sourceClasses) {
	    ClassInfo targetCi = findMatchingTargetClass(sourceCi, targetClasses);
	    if (targetCi != null) {
		matchingTargetClsBySourceCls.put(sourceCi, targetCi);
	    }
	}

	SortedSet<ClassInfo> deletedSourceClasses = new TreeSet<>(sourceClasses);
	deletedSourceClasses.removeAll(matchingTargetClsBySourceCls.keySet());

	SortedSet<ClassInfo> insertedTargetClasses = new TreeSet<>(targetClasses);
	insertedTargetClasses.removeAll(matchingTargetClsBySourceCls.values());

	for (ClassInfo ci : deletedSourceClasses) {
	    DiffElement2 diff = new DiffElement2(Operation.DELETE, ElementChangeType.SELF, null, ci, null, null, null);
	    diffs.add(diff);

	    for (PropertyInfo pi : ci.properties().values()) {
		DiffElement2 diffPi = new DiffElement2(Operation.DELETE, ElementChangeType.SELF, null, pi, null, null,
			null);
		diffs.add(diffPi);
	    }
	}

	for (ClassInfo ci : insertedTargetClasses) {
	    DiffElement2 diff = new DiffElement2(Operation.INSERT, ElementChangeType.SELF, null, null, ci, null, null);
	    diffs.add(diff);

	    for (PropertyInfo pi : ci.properties().values()) {
		DiffElement2 diffPi = new DiffElement2(Operation.INSERT, ElementChangeType.SELF, null, null, pi, null,
			null);
		diffs.add(diffPi);
	    }
	}

	for (Entry<ClassInfo, ClassInfo> entry : matchingTargetClsBySourceCls.entrySet()) {

	    ClassInfo sourceCi = entry.getKey();
	    ClassInfo targetCi = entry.getValue();

	    diffs.addAll(diff(sourceCi, targetCi));
	}

	return diffs;
    }

    private Info findMatchingTargetSubElement(Info sourceI, Collection<? extends Info> targetSubElements) {

	if (sourceI instanceof ClassInfo) {

	    ClassInfo sourceCi = (ClassInfo) sourceI;
	    SortedSet<ClassInfo> targetClasses = new TreeSet<>();
	    for (Info tse : targetSubElements) {
		targetClasses.add((ClassInfo) tse);
	    }
	    return findMatchingTargetClass(sourceCi, targetClasses);

	} else if (sourceI instanceof PackageInfo) {

	    PackageInfo sourcePi = (PackageInfo) sourceI;
	    Map<String, PackageInfo> targetPackagesByFullNameInSchema = new HashMap<>();
	    for (Info tse : targetSubElements) {
		PackageInfo targetPackage = (PackageInfo) tse;
		targetPackagesByFullNameInSchema.put(targetPackage.fullNameInSchema(), targetPackage);
	    }
	    return findMatchingTargetPackage(sourcePi, targetPackagesByFullNameInSchema);

	} else if (sourceI instanceof PropertyInfo) {

	    PropertyInfo sourcePi = (PropertyInfo) sourceI;
	    SortedSet<PropertyInfo> targetProperties = new TreeSet<>();
	    for (Info tse : targetSubElements) {
		targetProperties.add((PropertyInfo) tse);
	    }

	    return findMatchingTargetProperty(sourcePi, targetProperties);
	}

	return null;
    }

    private Info findMatchingTargetProperty(PropertyInfo sourcePi, SortedSet<PropertyInfo> targetProperties) {

	String sourcePiClassQualifiedName = sourcePi.inClass().name() + "::" + sourcePi.name();

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(sourcePiClassQualifiedName,
		sourcePi.encodingRule("modeldiff"));

	String nameForSearch = (pme != null) ? pme.getTargetType() : sourcePiClassQualifiedName;

	for (PropertyInfo targetPi : targetProperties) {
	    String targetPiClassQualifiedName = targetPi.inClass().name() + "::" + targetPi.name();
	    if (nameForSearch.equals(targetPiClassQualifiedName)) {
		return targetPi;
	    }
	}

	return null;
    }

    private ClassInfo findMatchingTargetClass(ClassInfo sourceCi, SortedSet<ClassInfo> targetClasses) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(sourceCi.name(), sourceCi.encodingRule("modeldiff"));

	String nameForSearch = (pme != null) ? pme.getTargetType() : sourceCi.name();

	return targetClasses.stream().filter(targetCi -> targetCi.name().equals(nameForSearch)).findFirst()
		.orElse(null);
    }

    private String mapTypeName(String typeNameIn, String encodingRule) {

	String typeName = StringUtils.defaultIfBlank(typeNameIn, "");

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	String result = (pme != null) ? pme.getTargetType() : typeName;

	return result;
    }

    private PackageInfo findMatchingTargetPackage(PackageInfo sourcePi,
	    Map<String, PackageInfo> targetPackagesByFullNameInSchema) {

	ProcessMapEntry pkgMapEntry = packageMapEntry(sourcePi);

	String fullNameForSearch = sourcePi.fullNameInSchema();
	if (pkgMapEntry != null) {
	    String sourceNamePrefix = pkgMapEntry.getType();
	    String targetNamePrefix = pkgMapEntry.getTargetType();
	    fullNameForSearch = targetNamePrefix + StringUtils.removeStart(fullNameForSearch, sourceNamePrefix);
	}

	return targetPackagesByFullNameInSchema.get(fullNameForSearch);
    }

    private String mapPackageName(PackageInfo pi) {

	ProcessMapEntry pkgMapEntry = packageMapEntry(pi);

	String fullNameInSchema = pi.fullNameInSchema();
	if (pkgMapEntry != null) {
	    String sourceNamePrefix = pkgMapEntry.getType();
	    String targetNamePrefix = pkgMapEntry.getTargetType();
	    fullNameInSchema = targetNamePrefix + StringUtils.removeStart(fullNameInSchema, sourceNamePrefix);
	}

	return fullNameInSchema.contains("::") ? StringUtils.substringAfterLast(fullNameInSchema, "::")
		: fullNameInSchema;
    }

    private ProcessMapEntry packageMapEntry(PackageInfo sourcePi) {

	PackageInfo pi = sourcePi;
	ProcessMapEntry pkgMapEntry = mapEntryParamInfos.getMapEntry(pi.fullNameInSchema(),
		pi.encodingRule("modeldiff"));
	while (pkgMapEntry == null && pi.owner() != null && pi.targetNamespace().equals(pi.owner().targetNamespace())) {
	    pi = pi.owner();
	    pkgMapEntry = mapEntryParamInfos.getMapEntry(pi.fullNameInSchema(), pi.encodingRule("modeldiff"));
	}

	return pkgMapEntry;
    }

    protected void stringDiff(List<DiffElement2> diffs, ElementChangeType type, Info source, Info target,
	    String sourceStringIn, String targetStringIn) {

	String sourceString = StringUtils.defaultIfBlank(sourceStringIn, "");
	String targetString = StringUtils.defaultIfBlank(targetStringIn, "");

	LinkedList<Diff> strdiffs;

	if (aaaModel && type == ElementChangeType.DOCUMENTATION) {
	    strdiffs = aaaDocumentation(sourceString, targetString);
	} else {
	    strdiffs = strDiffer.diff_main(sourceString, targetString);
	}

	strDiffer.diff_cleanupEfficiency(strdiffs);

	if (strDiffer.diff_levenshtein(strdiffs) == 0)
	    return;

	DiffElement2 diff = new DiffElement2(Operation.CHANGE, type, strdiffs, source, target, null, null);
	diffs.add(diff);
    }

    protected void tagDiff(List<DiffElement2> diffs, ElementChangeType type, Info source, Info target,
	    String sourceStringIn, String targetStringIn, String tagName) {

	String sourceString = StringUtils.defaultIfBlank(sourceStringIn, "");
	String targetString = StringUtils.defaultIfBlank(targetStringIn, "");

	LinkedList<Diff> strdiffs;

	if (aaaModel && type == ElementChangeType.DOCUMENTATION) {
	    strdiffs = aaaDocumentation(sourceString, targetString);
	} else {
	    strdiffs = strDiffer.diff_main(sourceString, targetString);
	}

	strDiffer.diff_cleanupEfficiency(strdiffs);

	if (strDiffer.diff_levenshtein(strdiffs) == 0)
	    return;

	DiffElement2 diff = new DiffElement2(Operation.CHANGE, type, strdiffs, source, target, tagName, null);
	diffs.add(diff);
    }

    protected void infoDiffs(List<DiffElement2> diffs, Info source, Info target, ElementChangeType type,
	    Collection<? extends Info> sourceSubElementsIn, Collection<? extends Info> targetSubElementsIn) {

	List<Info> sourceSubElements = new ArrayList<>();
	for (Info si : sourceSubElementsIn) {
	    if (type == ElementChangeType.SUBPACKAGE || matchingAaaModellart(si)) {
		sourceSubElements.add(si);
	    }
	}
	List<Info> targetSubElements = new ArrayList<>();
	for (Info ti : targetSubElementsIn) {
	    if (type == ElementChangeType.SUBPACKAGE || matchingAaaModellart(ti)) {
		targetSubElements.add(ti);
	    }
	}

	Map<Info, Info> matchingTargetSubElementBySourceSubElement = new HashMap<>();
	for (Info sourceI : sourceSubElements) {
	    Info targetI = findMatchingTargetSubElement(sourceI, targetSubElements);
	    if (targetI != null) {
		matchingTargetSubElementBySourceSubElement.put(sourceI, targetI);
	    }
	}

	SortedSet<Info> deletedSourceSubElements = new TreeSet<>(sourceSubElements);
	deletedSourceSubElements.removeAll(matchingTargetSubElementBySourceSubElement.keySet());

	SortedSet<Info> insertedTargetSubElements = new TreeSet<>(targetSubElements);
	insertedTargetSubElements.removeAll(matchingTargetSubElementBySourceSubElement.values());

	for (Info i : deletedSourceSubElements) {
	    DiffElement2 diff = new DiffElement2(Operation.DELETE, type, null, source, null, null, i);
	    diffs.add(diff);

	    /*
	     * Also take note of any deleted property here with elementChangeType SELF
	     * (better to do it here because mappings have been applied)
	     */
	    if (type == ElementChangeType.PROPERTY || type == ElementChangeType.ENUM) {
		DiffElement2 diff2 = new DiffElement2(Operation.DELETE, ElementChangeType.SELF, null, i, null, null,
			null);
		diffs.add(diff2);
	    }
	}

	for (Info i : insertedTargetSubElements) {
	    DiffElement2 diff = new DiffElement2(Operation.INSERT, type, null, null, target, null, i);
	    diffs.add(diff);

	    /*
	     * Also take note of any inserted property here with elementChangeType SELF
	     * (better to do it here because mappings have been applied)
	     */
	    if (type == ElementChangeType.PROPERTY || type == ElementChangeType.ENUM) {
		DiffElement2 diff2 = new DiffElement2(Operation.INSERT, ElementChangeType.SELF, null, null, i, null,
			null);
		diffs.add(diff2);
	    }
	}

	/*
	 * Since we have matching source and target as well as matching sub elements
	 * now, this is a good place to diff properties and enums.
	 */
	if (type == ElementChangeType.PROPERTY || type == ElementChangeType.ENUM) {
	    for (Entry<Info, Info> entry : matchingTargetSubElementBySourceSubElement.entrySet()) {

		PropertyInfo sourcePi = (PropertyInfo) entry.getKey();
		PropertyInfo targetPi = (PropertyInfo) entry.getValue();

		if (type == ElementChangeType.ENUM) {
		    diffs.addAll(diffEnum(sourcePi, targetPi));
		} else {
		    diffs.addAll(diff(sourcePi, targetPi));
		}
	    }
	}

//	for (Info i : targetSubElements) {
//
//	    if (type != ElementChangeType.SUBPACKAGE && !MatchingMA(i))
//		continue;
//
//	    boolean found = false;
//
//	    for (Info iRef : sourceSubElements) {
//
//		if (type != ElementChangeType.SUBPACKAGE && !MatchingMA(iRef))
//		    continue;
//
//		boolean eq = i.name().equalsIgnoreCase(iRef.name());
//
//		/*
//		 * Compare enums based on their initial value, if both have one
//		 */
//		if (type == ElementChangeType.ENUM) {
//		    String s1 = ((PropertyInfo) i).initialValue();
//		    String s2 = ((PropertyInfo) iRef).initialValue();
//		    if (StringUtils.isNotBlank(s1) && StringUtils.isNotBlank(s2))
//			eq = s1.equalsIgnoreCase(s2);
//		}
//
//		if (eq) {
//		    found = true;
//		    break;
//		}
//	    }
//	    if (!found) {
//		DiffElement2 diff = new DiffElement2(Operation.INSERT, type, null, source, target, null, i);
//		diffs.add(diff);
//	    }
//	}
//	for (Info iRef : sourceSubElements) {
//
//	    if (!MatchingMA(iRef))
//		continue;
//
//	    boolean found = false;
//
//	    for (Info i : targetSubElements) {
//
//		if (!MatchingMA(i))
//		    continue;
//
//		boolean eq = i.name().equalsIgnoreCase(iRef.name());
//
//		/*
//		 * Compare enums based on their initial value, if both have one
//		 */
//		if (type == ElementChangeType.ENUM) {
//		    String s1 = ((PropertyInfo) i).initialValue();
//		    String s2 = ((PropertyInfo) iRef).initialValue();
//		    if (StringUtils.isNotBlank(s1) && StringUtils.isNotBlank(s2))
//			eq = s1.equalsIgnoreCase(s2);
//		}
//
//		if (eq) {
//		    found = true;
//		    break;
//		}
//	    }
//
//	    if (!found) {
//		DiffElement2 diff = new DiffElement2(Operation.DELETE, type, null, source, target, null, iRef);
//		diffs.add(diff);
//	    }
//	}
    }

    private String addConstraints(ClassInfo ci, String doc) {

	for (Constraint ocl : ci.directConstraints()) {

	    doc += "\n\n-==- Konsistenzbedingung ";
	    if (!ocl.name().equalsIgnoreCase("alle"))
		doc += ocl.name() + " ";
	    doc += "-==-\n";
	    String txt = ocl.text();
	    String[] sa = Objects.nonNull(txt) ? txt.split("/\\*") : new String[0];
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
     * @param source tbd, not <code>null</code>
     * @param target tbd, not <code>null</code>
     * @return map with set of diff elements per Info object; can be empty but not
     *         <code>null</code>
     */
    public List<DiffElement2> baseDiff(Info source, Info target) {

	List<DiffElement2> diffs = new ArrayList<>();

	/*
	 * TODO: example and dataCaptureStatement, i.e. tags with potentially multiple
	 * values
	 */

	if (aaaModel && target instanceof ClassInfo) {
	    // TBD - why do we need this AAA specific code here?
	    String targetDoc = addConstraints((ClassInfo) target, target.documentation());
	    String sourceDoc = addConstraints((ClassInfo) source, source.documentation());
	    stringDiff(diffs, ElementChangeType.DOCUMENTATION, source, target, sourceDoc, targetDoc);
	} else {
	    stringDiff(diffs, ElementChangeType.DOCUMENTATION, source, target, source.documentation(),
		    target.documentation());
	}

	/*
	 * Apply mappings for properties, classes, and packages when comparing their
	 * name.
	 * 
	 * UPDATE: mappings have already be applied in infoDiffs to identify the
	 * corresponding info object, thus we can compare the name of source and target
	 * here in order to detect any name changes
	 */
//	if (source instanceof ClassInfo) {
//
//	    String sourceName = mapTypeName(source.name(), source.encodingRule("modeldiff"));
//	    stringDiff(diffs, ElementChangeType.NAME, source, target, sourceName, target.name());
//
//	} else if (source instanceof PropertyInfo) {
//
//	    String sourceName = mapPropertyName((PropertyInfo) source);
//	    stringDiff(diffs, ElementChangeType.NAME, source, target, sourceName, target.name());
//
//	} else if (source instanceof PackageInfo) {
//
//	    String sourceName = mapPackageName((PackageInfo) source);
//	    stringDiff(diffs, ElementChangeType.NAME, source, target, sourceName, target.name());
//	}

	stringDiff(diffs, ElementChangeType.NAME, source, target, source.name(), target.name());

	stringDiff(diffs, ElementChangeType.ALIAS, source, target, source.aliasName(), target.aliasName());

	stringDiff(diffs, ElementChangeType.DEFINITION, source, target, source.definition(), target.definition());

	stringDiff(diffs, ElementChangeType.DESCRIPTION, source, target, source.description(), target.description());

	stringDiff(diffs, ElementChangeType.LEGALBASIS, source, target, source.legalBasis(), target.legalBasis());

	stringDiff(diffs, ElementChangeType.PRIMARYCODE, source, target, source.primaryCode(), target.primaryCode());

	stringDiff(diffs, ElementChangeType.GLOBALIDENTIFIER, source, target, source.globalIdentifier(),
		target.globalIdentifier());

	stringDiff(diffs, ElementChangeType.STEREOTYPE, source, target,
		source.stereotypes().toString().replace("[", "").replace("]", ""),
		target.stereotypes().toString().replace("[", "").replace("]", ""));

	// diff for retired in AAA
	if (aaaModel) {
	    boolean retiredTarget = target.stereotypes().contains("retired");
	    boolean retiredSource = source.stereotypes().contains("retired");
	    if (!retiredSource && retiredTarget) {
		diffs.add(new DiffElement2(Operation.INSERT, ElementChangeType.AAARETIRED, null, source, target, null,
			null));
	    } else if (retiredSource && !retiredTarget) {
		diffs.add(new DiffElement2(Operation.DELETE, ElementChangeType.AAARETIRED, null, source, target, null,
			null));
	    }
	}

	// perform diff for the tagged values

	// TODO handle tags with multiple values (using, for example, method
	// taggedValuesForTagList(taglist))

	Map<String, String> taggedValuesTarget = target.taggedValues();
	Map<String, String> taggedValuesSource = source.taggedValues();

	for (Map.Entry<String, String> entry : taggedValuesTarget.entrySet()) {

	    String key = entry.getKey();
	    String valTarget = entry.getValue();

	    if (!tagsToDiff.matcher(key).matches()) {
		continue;
	    }

	    String valSource = taggedValuesSource.get(key);

	    if (aaaModel && (key.equalsIgnoreCase("AAA:Modellart") || key.equalsIgnoreCase("AAA:Grunddatenbestand"))) {

		if (key.equalsIgnoreCase("AAA:Modellart")) {
		    valTarget = StringUtils.defaultIfBlank(valTarget, "Alle");
		    valSource = StringUtils.defaultIfBlank(valSource, "Alle");
		} else {
		    valTarget = StringUtils.defaultIfBlank(valTarget, "");
		    valSource = StringUtils.defaultIfBlank(valSource, "");
		}
		for (String ma : valTarget.split(",")) {
		    ma = ma.trim();
		    if (relevanteModellarten.contains(ma)) {
			boolean found = false;
			for (String ma2 : valSource.split(",")) {
			    ma2 = ma2.trim();
			    if (ma2.equals(ma)) {
				found = true;
				break;
			    }
			}
			if (!found) {
			    diffs.add(
				    new DiffElement2(Operation.INSERT,
					    (key.equalsIgnoreCase("AAA:Modellart")) ? ElementChangeType.AAAMODELLART
						    : ElementChangeType.AAAGRUNDDATENBESTAND,
					    null, source, target, ma, null));
			}
		    }
		}

	    } else if (aaaModel && key.equalsIgnoreCase("AAA:Landnutzung")) {

		// normalize values
		if (!"true".equalsIgnoreCase(valTarget))
		    valTarget = null;
		if (!"true".equalsIgnoreCase(valSource))
		    valSource = null;
		if (valSource == null && valTarget != null) {
		    diffs.add(new DiffElement2(Operation.INSERT, ElementChangeType.AAALANDNUTZUNG, null, source, target,
			    valTarget, null));
		} else if (valTarget == null && valSource != null) {
		    diffs.add(new DiffElement2(Operation.DELETE, ElementChangeType.AAALANDNUTZUNG, null, source, target,
			    valSource, null));
		}

	    } else if (aaaModel && key.equalsIgnoreCase("AAA:GueltigBis")) {

		stringDiff(diffs, ElementChangeType.AAAGUELTIGBIS, source, target, valSource, valTarget);

	    } else {

		tagDiff(diffs, ElementChangeType.TAG, source, target, valSource, valTarget, key);
	    }
	}

	for (Map.Entry<String, String> entry : taggedValuesSource.entrySet()) {

	    String key = entry.getKey();
	    String valSource = entry.getValue();

	    if (!tagsToDiff.matcher(key).matches()) {
		continue;
	    }

	    String valTarget = taggedValuesTarget.get(key);

	    if (aaaModel && (key.equalsIgnoreCase("AAA:Modellart") || key.equalsIgnoreCase("AAA:Grunddatenbestand"))) {

		if (key.equalsIgnoreCase("AAA:Modellart")) {
		    valTarget = StringUtils.defaultIfBlank(valTarget, "Alle");
		    valSource = StringUtils.defaultIfBlank(valSource, "Alle");
		} else {
		    valTarget = StringUtils.defaultIfBlank(valTarget, "");
		    valSource = StringUtils.defaultIfBlank(valSource, "");
		}
		for (String ma : valSource.split(",")) {
		    ma = ma.trim();
		    if (relevanteModellarten.contains(ma)) {
			boolean found = false;
			for (String ma2 : valTarget.split(",")) {
			    ma2 = ma2.trim();
			    if (ma2.equals(ma)) {
				found = true;
				break;
			    }
			}
			if (!found) {
			    diffs.add(
				    new DiffElement2(Operation.DELETE,
					    (key.equalsIgnoreCase("AAA:Modellart")) ? ElementChangeType.AAAMODELLART
						    : ElementChangeType.AAAGRUNDDATENBESTAND,
					    null, source, target, ma, null));
			}
		    }
		}
	    } else if (aaaModel && key.equalsIgnoreCase("AAA:Landnutzung")) {

		// normalize values
		if (!"true".equalsIgnoreCase(valTarget))
		    valTarget = null;
		if (!"true".equalsIgnoreCase(valSource))
		    valSource = null;

		if (valSource == null && valTarget != null) {
		    diffs.add(new DiffElement2(Operation.INSERT, ElementChangeType.AAALANDNUTZUNG, null, source, target,
			    valTarget, null));
		} else if (valTarget == null && valSource != null) {
		    diffs.add(new DiffElement2(Operation.DELETE, ElementChangeType.AAALANDNUTZUNG, null, source, target,
			    valSource, null));
		}

	    } else if (!taggedValuesTarget.containsKey(key)) {

		if (aaaModel && key.equalsIgnoreCase("AAA:GueltigBis")) {

		    stringDiff(diffs, ElementChangeType.AAAGUELTIGBIS, source, target, valSource, valTarget);

		} else {

		    tagDiff(diffs, ElementChangeType.TAG, source, target, valSource, valTarget, key);
		}
	    }
	}

	return diffs;
    }

    private String mapPropertyName(PropertyInfo pi) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(pi.inClass().name() + "::" + pi.name(),
		pi.encodingRule("modeldiff"));

	String result = (pme != null) ? StringUtils.substringAfterLast(pme.getTargetType(), "::") : pi.name();

	return result;
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
