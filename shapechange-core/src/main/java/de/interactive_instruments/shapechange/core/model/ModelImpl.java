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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.Type;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.fol.FolExpression;
import de.interactive_instruments.shapechange.core.sbvr.Sbvr2FolParser;
import de.interactive_instruments.shapechange.core.sbvr.SbvrConstants;
import de.interactive_instruments.shapechange.core.sbvr.SbvrRuleLoader;

public abstract class ModelImpl implements Model {

    /*
     * flags whether postprocessing/validation has been executed
     */
    protected boolean postprocessed = false;

    /*
     * temporary storage for validating the names of the XML Schema documents to be
     * created when processing the model
     */
    HashSet<String> xsdDocNames;

    /*
     * temporary storage for validating the names of classes in an application
     * schema
     */
    HashSet<String> classNames;

    @Override
    public void postprocessAfterLoadingAndValidate() {

	if (postprocessed)
	    return;

	if (options().constraintLoadingEnabled()) {
	    postprocessFolConstraints();
	}

	xsdDocNames = new HashSet<String>();
	classNames = new HashSet<String>();

	Options options = options();
	for (PackageInfo pi : schemas("")) {
	    if (options.skipSchema(pi))
		continue;
	    classNames.clear();
	    postprocessPackage(pi, true);
	}
	postprocessed = true;
    }

    private void postprocessFolConstraints() {

	/*
	 * First order logic expressions can be parsed from different sources. For those
	 * where the parser does not need to be set up per constraint, we can create
	 * them outside of the following loops.
	 */
	Sbvr2FolParser sbvrParser = new Sbvr2FolParser(this);

	for (PackageInfo pi : schemas("")) {

	    if (options().skipSchema(pi))
		continue;

	    for (ClassInfo ci : this.classes(pi)) {

		List<Constraint> cons = ci.directConstraints();

		    // sort the constraints by name
		    Collections.sort(cons, new Comparator<Constraint>() {
			@Override
			public int compare(Constraint o1, Constraint o2) {
			    return o1.name().compareTo(o2.name());
			}
		    });

		for (Constraint con : cons) {

		    if (con instanceof FolConstraint folCon) {

			if (folCon.sourceType().equals(SbvrConstants.FOL_SOURCE_TYPE)) {

			    folCon.mergeComments(new String[] { folCon.text() });

			    FolExpression folExpr = sbvrParser.parse(folCon);

			    if (folExpr != null) {
				folCon.setFolExpression(folExpr);
			    } else {
				/*
				 * the parser already logged why the expression was not created
				 */
			    }

			} else {

			    /*
			     * Apparently a new source for FOL constraints exists - add parsing it here; in
			     * the meantime, log this as an error
			     */
			    MessageContext ctx = this.result().addError(null, 38, folCon.sourceType());
			    ctx.addDetail(null, 39, folCon.name(), folCon.contextModelElmt().fullNameInSchema());
			}
		    }
		}
	    }
	}
    }

    @Override
    public SortedSet<AssociationInfo> associations() {

	SortedSet<AssociationInfo> result = new TreeSet<>();

	for (ClassInfo cls : this.classes()) {
	    for (PropertyInfo pi : cls.properties().values()) {
		if (pi.association() != null) {
		    result.add(pi.association());
		}
	    }
	}

	return result;
    }

    @Override
    public void loadInformationFromExternalSources(boolean isLoadingInputModel) {

	// do not execute this once the model has been postprocessed
	if (postprocessed)
	    return;

	Options options = options();
	ShapeChangeResult result = result();

	// ============================================================
	// If we are loading the input model, load SBVR constraint info from
	// excel file
	// NOTE: can also be done via ConstraintLoader transformation

	if (isLoadingInputModel) {

	    String sbvrFileLocation = options().parameter(Options.PARAM_CONSTRAINT_EXCEL_FILE);

	    if (sbvrFileLocation != null) {

		/*
		 * if no sbvr file is provided, the loader will simply not contain any sbvr
		 * rules
		 */
		SbvrRuleLoader sbvrLoader = new SbvrRuleLoader(sbvrFileLocation, options, result, this);

		for (PackageInfo pi : selectedSchemas()) {

		    sbvrLoader.loadSBVRRulesAsConstraints(pi);
		}
	    }
	}
    }

    @Override
    public SortedSet<PackageInfo> selectedSchemas() {
	SortedSet<PackageInfo> res = new TreeSet<PackageInfo>();

	Options options = options();
	for (PackageInfo pi : schemas("")) {
	    if (!options.skipSchema(pi))
		res.add(pi);
	}
	return res;
    }

    @Override
    public SortedSet<? extends ClassInfo> selectedSchemaClasses() {

	SortedSet<ClassInfo> res = new TreeSet<ClassInfo>();

	for (PackageInfo selectedSchema : selectedSchemas()) {

	    SortedSet<ClassInfo> cisOfSelectedSchema = this.classes(selectedSchema);

	    for (ClassInfo ci : cisOfSelectedSchema) {

		res.add(ci);
	    }
	}

	return res;
    }

    @Override
    public SortedSet<? extends PropertyInfo> selectedSchemaProperties() {

	SortedSet<? extends ClassInfo> selCis = this.selectedSchemaClasses();

	SortedSet<PropertyInfo> res = new TreeSet<PropertyInfo>();

	for (ClassInfo selCi : selCis) {

	    for (PropertyInfo pi : selCi.properties().values()) {

		res.add(pi);
	    }
	}

	return res;
    }
    
    @Override
    public SortedSet<? extends AssociationInfo> selectedSchemaAssociations() {

	SortedSet<? extends PropertyInfo> selPis = this.selectedSchemaProperties();

	SortedSet<AssociationInfo> res = new TreeSet<>();

	for (PropertyInfo pi : selPis) {

	    if (!pi.isAttribute()) {
		res.add(pi.association());
	    }
	}

	return res;
    }

    @Override
    public SortedSet<ClassInfo> classes() {

	SortedSet<ClassInfo> result = new TreeSet<>();

	SortedSet<PackageInfo> packages = packages();

	for (PackageInfo pkg : packages) {
	    result.addAll(pkg.containedClasses());
	}

	return result;
    }

    @Override
    public SortedSet<PropertyInfo> properties() {

	SortedSet<PropertyInfo> result = new TreeSet<>();

	SortedSet<ClassInfo> classes = classes();

	for (ClassInfo cls : classes) {
	    result.addAll(cls.properties().values());
	}

	return result;
    }

    @Override
    public PropertyInfo propertyByFullNameInSchema(String fullNameInSchema) {

	for (PropertyInfo pi : properties()) {

	    if (pi.fullNameInSchema().equals(fullNameInSchema)) {
		return pi;
	    }
	}

	return null;
    }

    @Override
    public ClassInfo classByFullNameInSchema(String fullNameInSchema) {

	for (ClassInfo ci : classes()) {

	    if (ci.fullNameInSchema().equals(fullNameInSchema)) {
		return ci;
	    }
	}

	return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public SortedSet<PackageInfo> allPackagesFromSelectedSchemas() {

	SortedSet<PackageInfo> result = new TreeSet<PackageInfo>();

	for (PackageInfo selSchema : selectedSchemas()) {

	    result.add(selSchema);
	    result.addAll(selSchema.containedPackagesInSameTargetNamespace());
	}

	return result;
    }

    private void postprocessPackage(PackageInfo pi, boolean processClasses) {
	if (pi == null)
	    return;

	pi.postprocessAfterLoadingAndValidate();

	if (pi.matches("req-xsd-pkg-xsdDocument-unique")) {
	    String xsdDocName = pi.xsdDocument();
	    if (xsdDocName != null && !xsdDocName.trim().isEmpty()) {
		if (xsdDocNames.contains(xsdDocName)) {
		    MessageContext mc = result().addError(null, 162, xsdDocName);
		    if (mc != null)
			mc.addDetail(null, 400, "Package", pi.fullName());
		} else
		    xsdDocNames.add(xsdDocName);
	    }
	}

	if (processClasses) {
	    for (ClassInfo ci : classes(pi)) {
		postprocessClass(ci);
	    }
	}

	for (PackageInfo pi2 : pi.containedPackages()) {
	    if (!pi2.isSchema())
		postprocessPackage(pi2, false);
	}
    }

    private void postprocessClass(ClassInfo ci) {
	if (ci == null)
	    return;

	ci.postprocessAfterLoadingAndValidate();

	if (ci.matches("req-xsd-cls-name-unique")) {
	    String className = ci.name();
	    if (classNames.contains(className)) {
		MessageContext mc = result().addError(null, 163, className, ci.pkg().targetNamespace());
		if (mc != null)
		    mc.addDetail(null, 400, "Package", ci.fullName());
	    } else
		classNames.add(className);
	}

	for (PropertyInfo propi : ci.properties().values()) {
	    postprocessProperty(propi);
	}

	// TODO currently there is no way to get all operations of a class, so
	// we cannot validate them right now
    }

    private void postprocessProperty(PropertyInfo propi) {
	if (propi == null)
	    return;

	propi.postprocessAfterLoadingAndValidate();

	if (!propi.isAttribute()) {
	    postprocessAssociation(propi.association());
	}
    }

    private void postprocessAssociation(AssociationInfo ai) {
	if (ai == null)
	    return;

	ai.postprocessAfterLoadingAndValidate();
    }

    public void initialise(ShapeChangeResult r, Options o, String repositoryFileNameOrConnectionString, String user,
	    String pwd) throws ShapeChangeAbortException {

	throw new ShapeChangeAbortException(
		"Initialization of repository with username and password not supported for this type of input model.");
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public boolean isInSelectedSchemas(ClassInfo ci) {

	SortedSet<? extends PackageInfo> selectedSchemas = this.selectedSchemas();

	if (selectedSchemas == null || selectedSchemas.isEmpty()) {

	    return false;

	} else {

	    for (PackageInfo selectedSchema : selectedSchemas) {

		if (ci.inSchema(selectedSchema)) {
		    return true;
		}
	    }

	    // ci is not part of any of the selected schemas
	    return false;
	}
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public PackageInfo schemaPackage(ClassInfo ci) {

	PackageInfo p = ci.pkg();

	do {
	    if (p.isSchema() || p.isAppSchema()) {
		return p;
	    } else {
		p = p.owner();
	    }
	} while (p != null);

	return null;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public PackageInfo schemaPackage(PackageInfo pi) {

	PackageInfo p = pi;

	do {
	    if (p.isSchema() || p.isAppSchema()) {
		return p;
	    } else {
		p = p.owner();
	    }
	} while (p != null);

	return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public SortedSet<PackageInfo> packages(PackageInfo pkg) {

	SortedSet<PackageInfo> result = new TreeSet<PackageInfo>();

	if (pkg.targetNamespace() != null) {

	    SortedSet<PackageInfo> allPackages = this.packages();

	    for (PackageInfo pi : allPackages) {
		if (pi.targetNamespace() != null && pi.targetNamespace().equals(pkg.targetNamespace())) {
		    result.add(pi);
		}
	    }
	}

	return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: This method is intended to be "final", but not actually declared as
     * such. A depending project can thus extend the method, if absolutely
     * necessary.
     */
    @Override
    public SortedSet<PackageInfo> schemas(String name) {

	SortedSet<PackageInfo> res = new TreeSet<PackageInfo>();

	for (PackageInfo pi : packages()) {

	    if (pi.isSchema()) {
		if (name != null && !name.equals("")) {
		    if (pi.name().equals(name)) {

			res.add(pi);
		    }
		} else {
		    res.add(pi);
		}
	    }
	}
	return res;
    }

    @Override
    public ClassInfo classByIdOrName(Type typeInfo) {
	if (typeInfo == null) {
	    return null;
	} else {
	    ClassInfo result = this.classById(typeInfo.id);
	    if (result == null) {
		result = this.classByName(typeInfo.name);
	    }
	    return result;
	}
    }

    @Override
    public Type typeByName(String typeName) {

	ClassInfo ci = this.classByName(typeName);
	Type typeInfo = new Type();
	typeInfo.name = typeName;
	if (ci != null) {
	    typeInfo.id = ci.id();
	} else {
	    typeInfo.id = "UNKNOWN";
	}

	return typeInfo;
    }

    @Override
    public String descriptorSource(Descriptor descriptor) {

	String source = options().descriptorSource(descriptor.getName());

	// if nothing has been configured, use tag as default
	if (source == null) {
	    source = "tag#" + descriptor;
	}

	return source;
    }

    @Override
    public PropertyInfo lookupNonNavigableAssociationRole(SortedSet<ClassInfo> classes, String roleName) {

	for (AssociationInfo ai : this.associations()) {

	    for (ClassInfo classAtOneEnd : classes) {

		if (roleName.equals(ai.end1().name()) && classAtOneEnd == ai.end1().inClass()) {
		    return ai.end1();
		} else if (roleName.equals(ai.end2().name()) && classAtOneEnd == ai.end2().inClass()) {
		    return ai.end2();
		}
	    }
	}

	return null;
    }
}
