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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
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

    private boolean aaaModel;
    private Set<String> relevanteModellarten = new HashSet<>();
    private diff_match_patch strDiffer = new diff_match_patch();
    private MapEntryParamInfos mapEntryParamInfos;
    private Pattern tagsToDiff;
    private Pattern tagsToSplitPattern;

    public Differ2(MapEntryParamInfos mapEntryParamInfos, Pattern tagsToDiff, Pattern tagsToSplitPattern) {

	this.aaaModel = false;
	this.mapEntryParamInfos = mapEntryParamInfos;
	this.tagsToDiff = tagsToDiff;
	this.tagsToSplitPattern = tagsToSplitPattern;
    }

    public Differ2(Set<String> relevanteModellarten, MapEntryParamInfos mapEntryParamInfos, Pattern tagsToDiff,
	    Pattern tagsToSplitPattern) {

	this.aaaModel = true;
	this.relevanteModellarten = relevanteModellarten;
	this.mapEntryParamInfos = mapEntryParamInfos;
	this.tagsToDiff = tagsToDiff;
	this.tagsToSplitPattern = tagsToSplitPattern;
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
	    if (targetCi != null && (matchingAaaModellart(sourceCi) || matchingAaaModellart(targetCi))) {
		matchingTargetClsBySourceCls.put(sourceCi, targetCi);
	    }
	}

	SortedSet<ClassInfo> deletedSourceClasses = new TreeSet<>(sourceClasses.stream()
		.filter(ci -> !matchingTargetClsBySourceCls.containsKey(ci) && matchingAaaModellart(ci))
		.collect(Collectors.toList()));

	SortedSet<ClassInfo> insertedTargetClasses = new TreeSet<>(targetClasses.stream()
		.filter(ci -> !matchingTargetClsBySourceCls.containsValue(ci) && matchingAaaModellart(ci))
		.collect(Collectors.toList()));

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

	boolean ignoreCase = type.isDiffIgnoringCase();

	String sourceString = StringUtils.defaultIfBlank(sourceStringIn, "");
	String targetString = StringUtils.defaultIfBlank(targetStringIn, "");

	if (ignoreCase) {
	    sourceString = sourceString.toLowerCase(Locale.ENGLISH);
	    targetString = targetString.toLowerCase(Locale.ENGLISH);
	}

	LinkedList<Diff> strdiffs;

	if (aaaModel && type == ElementChangeType.DOCUMENTATION) {
	    strdiffs = aaaDocumentation(sourceString, targetString);
	} else {
	    strdiffs = strDiffer.diff_main(sourceString, targetString);
	}

	strDiffer.diff_cleanupEfficiency(strdiffs);

	if (strDiffer.diff_levenshtein(strdiffs) != 0) {

	    // so there is a textual difference

	    if (ignoreCase) {
		/*
		 * There even was a difference when comparing the strings with case ignored. In
		 * this case, we need to compute the diff again with the original strings.
		 */
		sourceString = StringUtils.defaultIfBlank(sourceStringIn, "");
		targetString = StringUtils.defaultIfBlank(targetStringIn, "");
		strdiffs.clear();

		if (aaaModel && type == ElementChangeType.DOCUMENTATION) {
		    strdiffs = aaaDocumentation(sourceString, targetString);
		} else {
		    strdiffs = strDiffer.diff_main(sourceString, targetString);
		}

		strDiffer.diff_cleanupEfficiency(strdiffs);
	    }

	    DiffElement2 diff = new DiffElement2(Operation.CHANGE, type, strdiffs, source, target, null, null);
	    diffs.add(diff);
	}
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
	    DiffElement2 diff = new DiffElement2(Operation.DELETE, type, null, source, target, null, i);
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
	    DiffElement2 diff = new DiffElement2(Operation.INSERT, type, null, source, target, null, i);
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
	 * NOTE: Mappings have already been applied in infoDiffs to identify the
	 * corresponding info object. We can still compare the name of source and target
	 * here - in order to detect a name change.
	 */
	stringDiff(diffs, ElementChangeType.NAME, source, target, source.name(), target.name());

	stringDiff(diffs, ElementChangeType.ALIAS, source, target, source.aliasName(), target.aliasName());

	stringDiff(diffs, ElementChangeType.DEFINITION, source, target, source.definition(), target.definition());

	stringDiff(diffs, ElementChangeType.DESCRIPTION, source, target, source.description(), target.description());

	stringDiff(diffs, ElementChangeType.LEGALBASIS, source, target, source.legalBasis(), target.legalBasis());

	stringDiff(diffs, ElementChangeType.PRIMARYCODE, source, target, source.primaryCode(), target.primaryCode());

	stringDiff(diffs, ElementChangeType.GLOBALIDENTIFIER, source, target, source.globalIdentifier(),
		target.globalIdentifier());

	stringDiff(diffs, ElementChangeType.LANGUAGE, source, target, source.language(), target.language());

	stringDiff(diffs, ElementChangeType.DATACAPTURESTATEMENT, source, target, source.dataCaptureStatements(),
		target.dataCaptureStatements());

	stringDiff(diffs, ElementChangeType.EXAMPLE, source, target, source.examples(), target.examples());

	stringDiff(diffs, ElementChangeType.STEREOTYPE, source, target, source.stereotypes().asArray(),
		target.stereotypes().asArray());

	// perform diff for the tagged values

	// get all tagged values in source and target
	TaggedValues sourceTVs = source.taggedValuesAll();
	TaggedValues targetTVs = target.taggedValuesAll();

	tagsDiff(diffs, source, target, sourceTVs, targetTVs);

	return diffs;
    }

    private void tagsDiff(List<DiffElement2> diffs, Info source, Info target, TaggedValues sourceTVs,
	    TaggedValues targetTVs) {

	// identify relevant tags for source and target
	SortedSet<String> sourceTags = sourceTVs.keySet().stream().filter(tag -> tagsToDiff.matcher(tag).matches())
		.collect(Collectors.toCollection(TreeSet::new));
	SortedSet<String> targetTags = targetTVs.keySet().stream().filter(tag -> tagsToDiff.matcher(tag).matches())
		.collect(Collectors.toCollection(TreeSet::new));

	SortedSet<String> allTags = new TreeSet<>(sourceTags);
	allTags.addAll(targetTags);

	for (String tag : allTags) {
	    tagDiff(diffs, source, target, sourceTVs.get(tag), targetTVs.get(tag), tag);
	}
    }

    private void stringDiff(List<DiffElement2> diffs, ElementChangeType changeType, Info source, Info target,
	    String[] sourceValuesIn, String[] targetValuesIn) {

	String[] sourceValues = sourceValuesIn == null ? new String[0] : sourceValuesIn;
	String[] targetValues = targetValuesIn == null ? new String[0] : targetValuesIn;

	SortedSet<String> valsSource = new TreeSet<>(Arrays.asList(sourceValues));
	SortedSet<String> valsTarget = new TreeSet<>(Arrays.asList(targetValues));

	LinkedList<Diff> valueDiffs = diffsForMultipleStringValues(valsSource, valsTarget,
		changeType.isDiffIgnoringCase());

	if (valueDiffs.isEmpty()
		|| valueDiffs.stream().allMatch(d -> d.operation == diff_match_patch.Operation.EQUAL)) {
	    // there are no value changes
	    return;
	}

	DiffElement2 diff = new DiffElement2(Operation.CHANGE, changeType, valueDiffs, source, target, null, null);
	diffs.add(diff);
    }

    private LinkedList<Diff> diffsForMultipleStringValues(SortedSet<String> valsSourceIn,
	    SortedSet<String> valsTargetIn, boolean ignoreCase) {

	LinkedList<Diff> valueDiffs = new LinkedList<>();

	if (ignoreCase) {

	    SortedMap<String, String> vlasSourceInByLowerCase = new TreeMap<>();
	    for (String vSource : valsSourceIn) {
		vlasSourceInByLowerCase.put(vSource.toLowerCase(Locale.ENGLISH), vSource);
	    }

	    SortedMap<String, String> vlasTargetInByLowerCase = new TreeMap<>();
	    for (String vTarget : valsTargetIn) {
		vlasTargetInByLowerCase.put(vTarget.toLowerCase(Locale.ENGLISH), vTarget);
	    }

	    SortedSet<String> matchingVals = new TreeSet<>(vlasSourceInByLowerCase.keySet());
	    matchingVals.retainAll(vlasTargetInByLowerCase.keySet());

	    SortedSet<String> deletedSourceVals = new TreeSet<>(vlasSourceInByLowerCase.keySet());
	    deletedSourceVals.removeAll(matchingVals);

	    SortedSet<String> insertedTargetVals = new TreeSet<>(vlasTargetInByLowerCase.keySet());
	    insertedTargetVals.removeAll(matchingVals);

	    for (String v : deletedSourceVals) {
		Diff d = new Diff(diff_match_patch.Operation.DELETE, vlasSourceInByLowerCase.get(v));
		valueDiffs.add(d);
	    }
	    for (String v : insertedTargetVals) {
		Diff d = new Diff(diff_match_patch.Operation.INSERT, vlasTargetInByLowerCase.get(v));
		valueDiffs.add(d);
	    }
	    for (String v : matchingVals) {
		Diff d = new Diff(diff_match_patch.Operation.EQUAL, vlasSourceInByLowerCase.get(v));
		valueDiffs.add(d);
	    }

	} else {

	    SortedSet<String> matchingVals = new TreeSet<>(valsSourceIn);
	    matchingVals.retainAll(valsTargetIn);

	    SortedSet<String> deletedSourceVals = new TreeSet<>(valsSourceIn);
	    deletedSourceVals.removeAll(matchingVals);

	    SortedSet<String> insertedTargetVals = new TreeSet<>(valsTargetIn);
	    insertedTargetVals.removeAll(matchingVals);

	    for (String v : deletedSourceVals) {
		Diff d = new Diff(diff_match_patch.Operation.DELETE, v);
		valueDiffs.add(d);
	    }
	    for (String v : insertedTargetVals) {
		Diff d = new Diff(diff_match_patch.Operation.INSERT, v);
		valueDiffs.add(d);
	    }
	    for (String v : matchingVals) {
		Diff d = new Diff(diff_match_patch.Operation.EQUAL, v);
		valueDiffs.add(d);
	    }
	}

	valueDiffs.sort(new Comparator<Diff>() {
	    @Override
	    public int compare(Diff o1, Diff o2) {
		/*
		 * simply sort based upon text comparison; we know that there are no duplicate
		 * text values since the values are from a set
		 */
		return o1.text.compareTo(o2.text);
	    }
	});

	return valueDiffs;
    }

    private void tagDiff(List<DiffElement2> diffs, Info source, Info target, String[] sourceTagValues,
	    String[] targetTagValues, String tag) {

	SortedSet<String> valsSource = normalizeTagValues(tag, sourceTagValues);
	SortedSet<String> valsTarget = normalizeTagValues(tag, targetTagValues);

	LinkedList<Diff> valueDiffs = diffsForMultipleStringValues(valsSource, valsTarget,
		ElementChangeType.TAG.isDiffIgnoringCase());

	if (valueDiffs.isEmpty()
		|| valueDiffs.stream().allMatch(d -> d.operation == diff_match_patch.Operation.EQUAL)) {
	    // there are no value changes
	    return;
	}

	DiffElement2 diff = new DiffElement2(Operation.CHANGE, ElementChangeType.TAG, valueDiffs, source, target, tag,
		null);
	diffs.add(diff);
    }

    private SortedSet<String> normalizeTagValues(String tag, String[] values) {

	SortedSet<String> result = new TreeSet<>();

	for (String v : values) {
	    if (StringUtils.isNotBlank(v)) {

		if (tagsToSplitPattern != null && tagsToSplitPattern.matcher(tag).matches()) {
		    List<String> splitResult = splitAndTrimListValue(v);
		    result.addAll(splitResult);
		} else {
		    result.add(v.trim());
		}
	    }
	}

	return result;
    }

    private List<String> splitAndTrimListValue(String v) {
	List<String> result = new ArrayList<>();
	if (StringUtils.isNotBlank(v)) {
	    for (String s : v.split(",")) {
		if (StringUtils.isNotBlank(s)) {
		    result.add(s.trim());
		}
	    }
	}
	return result;
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
