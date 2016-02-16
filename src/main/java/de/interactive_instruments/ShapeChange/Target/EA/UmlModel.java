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
package de.interactive_instruments.ShapeChange.Target.EA;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.CreateModelType;
import org.sparx.Element;
import org.sparx.Repository;
import org.sparx.Package;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.EAException;
import de.interactive_instruments.ShapeChange.Util.EAModelUtil;

/**
 * @author Clemens Portele
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class UmlModel implements SingleTarget, MessageSource {

	/**
	 * Optional (default is the current run directory) - The path to the folder
	 * in which the resulting UML model will be created.
	 */
	public static final String PARAM_OUTPUT_DIR = "outputDirectory";

	private static boolean initialised = false;
	private static String outputFilename = null;
	private static String documentationTemplate = null;
	private static String documentationNoValue = null;
	private static Repository rep = null;
	private static Integer pOut_EaPkgId = null;
	private static Set<AssociationInfo> associations = new HashSet<AssociationInfo>();
	private static Map<ClassInfo, Integer> elementIdByClassInfo = new HashMap<ClassInfo, Integer>();
	/**
	 * Collection of all generalization relationships between classes contained
	 * in the schemas selected for processing. key: subtype; value: supertype
	 */
	private static Map<ClassInfo, ClassInfo> generalisations = new HashMap<ClassInfo, ClassInfo>();

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private Map<String, Integer> eaPkgIdByPackageName = new HashMap<String, Integer>();

	// TODO Unit Test

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
					throws ShapeChangeAbortException {

		pi = p;
		model = m;
		options = o;
		result = r;

		if (!initialised) {
			initialised = true;

			String outputDirectory = options
					.parameter(this.getClass().getName(), PARAM_OUTPUT_DIR);
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = ".";

			outputFilename = options.parameter(this.getClass().getName(),
					"modelFilename");
			if (outputFilename == null)
				outputFilename = "ShapeChangeExport.eap";

			// change the default documentation template?
			documentationTemplate = options.parameter(this.getClass().getName(),
					"documentationTemplate");
			documentationNoValue = options.parameter(this.getClass().getName(),
					"documentationNoValue");
			
			/** Make sure repository file exists */
			java.io.File repfile = null;

			java.io.File outDir = new java.io.File(outputDirectory);
			if (!outDir.exists()) {
				try {
					FileUtils.forceMkdir(outDir);
				} catch (IOException e) {
					String errormsg = e.getMessage();
					r.addError(null, 32, errormsg, outputDirectory);
					return;
				}
			}

			repfile = new java.io.File(outDir, outputFilename);

			boolean ex = true;

			rep = new Repository();

			if (!repfile.exists()) {
				ex = false;
				if (!outputFilename.toLowerCase().endsWith(".eap")) {
					outputFilename += ".eap";
					repfile = new java.io.File(outputFilename);
					ex = repfile.exists();
				}
			}

			String absname = repfile.getAbsolutePath();

			if (!ex) {
				if (!rep.CreateModel(CreateModelType.cmEAPFromBase, absname,
						0)) {
					r.addError(null, 31, absname);
					rep = null;
					return;
				}
			}

			/** Connect to EA Repository */
			if (!rep.OpenFile(absname)) {
				String errormsg = rep.GetLastError();
				r.addError(null, 30, errormsg, outputFilename);
				rep = null;
				return;
			}

			rep.RefreshModelView(0);

			Collection<Package> c = rep.GetModels();
			Package root = c.GetAt((short) 0);

			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			df.setTimeZone(tz);

			Package pOut = root.GetPackages().AddNew(
					"ShapeChangeOutput-" + df.format(new Date()), "Class View");
			if (!pOut.Update()) {
				result.addError("EA-Fehler: " + pOut.GetLastError());
			}
			pOut_EaPkgId = pOut.GetPackageID();
		}

		if (rep == null || pOut_EaPkgId == null)
			return; // repository not initialised

		// export app schema package
		clonePackage(pi, pOut_EaPkgId);
	}

	private void clonePackage(PackageInfo pSource, Integer containerEaPkgId) {

		Package container = rep.GetPackageByID(containerEaPkgId);

		clonePackage(pSource, container);
	}

	private void clonePackage(PackageInfo pSource, Package container) {

		Package pkg = container.GetPackages().AddNew(pSource.name(), "Nothing");
		if (!pkg.Update()) {
			result.addError("EA-Fehler: " + pkg.GetLastError());
		}
		cloneStandarddItems(pkg.GetElement(), pSource);

		eaPkgIdByPackageName.put(pSource.name(), pkg.GetPackageID());

		for (PackageInfo cpi : pSource.containedPackages()) {
			clonePackage(cpi, pkg);
		}
	}

	public void process(ClassInfo ci) {
		cloneClass(ci);
	}

	private void cloneClass(ClassInfo ci) {
		Package pkg = rep
				.GetPackageByID(eaPkgIdByPackageName.get(ci.pkg().name()));
		if (pkg == null) {
			result.addError(
					"Missing package information for class " + ci.fullName());
			return;
		}
		Element e = pkg.GetElements().AddNew(ci.name(), "Class");
		if (!e.Update()) {
			result.addError("EA-Fehler: " + e.GetLastError());
		}
		elementIdByClassInfo.put(ci, e.GetElementID());
		cloneStandarddItems(e, ci);
		if (ci.isAbstract()) {
			e.SetAbstract("true");
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
		}
		for (Constraint constr : ci.constraints()) {
			org.sparx.Constraint ct = e.GetConstraints().AddNew(constr.name(),
					"OCL");
			if (!ct.Update()) {
				result.addError("EA-Fehler: " + ct.GetLastError());
			}
			ct.SetNotes(constr.text());
			ct.Update();
		}
		for (PropertyInfo propi : ci.properties().values()) {
			if (propi.isAttribute())
				cloneAttribute(e, propi);
			else
				associations.add(propi.association());
		}
		for (String tid : ci.supertypes()) {
			ClassInfo cix = model.classById(tid);
			if (cix == null) {
				result.addError(this, 101, tid, ci.name());
			} else if(!model.isInSelectedSchemas(cix)){
				result.addError(this, 102, cix.name(), ci.name());
			} else {
				generalisations.put(ci, cix);
			}
		}
		for (String tid : ci.subtypes()) {
			ClassInfo cix = model.classById(tid);
			if (cix == null) {
				result.addError(this,103,tid,ci.name());				
			} else if(!model.isInSelectedSchemas(cix)){
				result.addError(this,104,cix.name(),ci.name());
			} else {				
				generalisations.put(cix, ci);
			}
		}

		if (ci.isAssocClass() != null) {
			/*
			 * will be created when the association itself has been created, so
			 * in writeAll()
			 */
		}
	}

	private String stereotypesCSV(Stereotypes stereotypes) {
		return stereotypes == null ? "" : stereotypes.toString();
	}

	private void cloneAttribute(Element e, PropertyInfo propi) {

		try {
			org.sparx.Attribute att = e.GetAttributes().AddNew(propi.name(),
					"");
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetStyle(propi.aliasName());
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetNotes(propi.derivedDocumentation(documentationTemplate, documentationNoValue));
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetStereotype(stereotypesCSV(propi.stereotypes()));
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}

			EAModelUtil.setTaggedValues(att, propi.taggedValuesAll());

			att.SetIsDerived(propi.isDerived());
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetIsOrdered(propi.isOrdered());
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetDefault(propi.initialValue());
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetVisibility("public");
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			Multiplicity m = propi.cardinality();
			att.SetLowerBound("" + m.minOccurs);
			if (m.maxOccurs == Integer.MAX_VALUE)
				att.SetUpperBound("*");
			else
				att.SetUpperBound("" + m.maxOccurs);
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}
			att.SetType(propi.typeInfo().name);
			if (!att.Update()) {
				result.addError("EA-Fehler: " + att.GetLastError());
			}

		} catch (EAException exc) {

			result.addError(this, 10002, propi.name(), propi.inClass().name(),
					exc.getMessage());
		}
	}

	/**
	 * Clones standard items to add them to the given element (usually a class
	 * or a package).
	 * 
	 * @param e
	 * @param i
	 */
	private void cloneStandarddItems(Element e, Info i) {

		try {

			e.SetAlias(i.aliasName());
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetNotes(i.derivedDocumentation(documentationTemplate, documentationNoValue));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetStereotype(stereotypesCSV(i.stereotypes()).replace(" ", ""));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}

			EAModelUtil.setTaggedValues(e, i.taggedValuesAll());

			e.Refresh();

		} catch (EAException exc) {

			result.addError(this, 10003, i.name(), exc.getMessage());
		}
	}

	private void cloneStandardItems(Connector e, Info i) {

		try {

			e.SetAlias(i.aliasName());
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetNotes(i.derivedDocumentation(documentationTemplate, documentationNoValue));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetStereotype(stereotypesCSV(i.stereotypes()));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}

			EAModelUtil.setTaggedValues(e, i.taggedValuesAll());

		} catch (EAException exc) {

			result.addError(this, 10004,
					i.name() == null || i.name().trim().length() == 0
							? "<without_name>" : i.name(),
					exc.getMessage());
		}
	}

	private void cloneStandardItems(ConnectorEnd e, PropertyInfo i) {

		try {

			e.SetRole(i.name());
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetAlias(i.aliasName());
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			Multiplicity m = i.cardinality();
			if (m.maxOccurs == Integer.MAX_VALUE)
				e.SetCardinality(m.minOccurs + "..*");
			else
				e.SetCardinality(m.minOccurs + ".." + m.maxOccurs);
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetIsNavigable(i.isNavigable());
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetRoleNote(i.derivedDocumentation(documentationTemplate, documentationNoValue));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			e.SetStereotype(stereotypesCSV(i.stereotypes()));
			if (!e.Update()) {
				result.addError("EA-Fehler: " + e.GetLastError());
			}
			
			if(i.reverseProperty() != null) {
				if(i.reverseProperty().isAggregation()) {
					e.SetAggregation(1);
					if (!e.Update()) {
						result.addError("EA-Fehler: " + e.GetLastError());
					}
				} else if(i.reverseProperty().isComposition()) {
					e.SetAggregation(2);
					if (!e.Update()) {
						result.addError("EA-Fehler: " + e.GetLastError());
					}
				}
			}

			EAModelUtil.setTaggedValues(e, i.taggedValuesAll());

		} catch (EAException exc) {

			result.addError(this, 10004,
					i.name() == null || i.name().trim().length() == 0
							? "<without_name>" : i.name(),
					exc.getMessage());
		}
	}

	public void write() {
		// nothing to do, everything will be written in initialise() and
		// writeAll()
	}

	public int getTargetID() {
		return TargetIdentification.UML_MODEL.getId();
	}

	public void writeAll(ShapeChangeResult r) {

		result = r;

		if (rep == null)
			return; // repository not initialised

		for (AssociationInfo ai : associations) {
			PropertyInfo propi1 = ai.end1();
			PropertyInfo propi2 = ai.end2();
			ClassInfo ci1 = propi1.inClass();
			ClassInfo ci2 = propi2.inClass();

			int c1ElementId = elementIdByClassInfo.get(ci1);
			int c2ElementId = elementIdByClassInfo.get(ci2);

			Element c1 = rep.GetElementByID(c1ElementId);
			Element c2 = rep.GetElementByID(c2ElementId);

			if (c1 == null) {
				result.addWarning("Association between " + ci1.name() + " - "
						+ ci2.name()
						+ " not set as the first class is not part of target model.");
			} else if (c2 == null) {
				result.addWarning("Association between " + ci1.name() + " - "
						+ ci2.name()
						+ " not set as the second class is not part of target model.");
			} else {

				c1.Refresh();

				Collection<Connector> c1Conns = c1.GetConnectors();
				c1Conns.Refresh();

				Connector con = c1Conns.AddNew("", "Association");
				con.SetSupplierID(c2.GetElementID());
				con.SetDirection("Bi-Directional");
				if (!con.Update()) {
					result.addError("EA-Fehler: " + con.GetLastError());
				}
				c1.GetConnectors().Refresh();

				cloneStandardItems(con, ai);
				cloneStandardItems(con.GetClientEnd(), propi2);
				cloneStandardItems(con.GetSupplierEnd(), propi1);
								

				// generate association class relationship
				ClassInfo assocClassCi = ai.assocClass();
				if (assocClassCi != null) {

					int assocClassElementId = elementIdByClassInfo
							.get(assocClassCi);
					Element assocClass = rep
							.GetElementByID(assocClassElementId);

					if (assocClass == null) {
						result.addError(this, 10006, assocClassCi.name(),
								ci1.name(), ci2.name());
					} else {
						try {
							EAModelUtil.setEAAssociationClass(con, assocClass);
						} catch (EAException e) {

							result.addError(this, 10005, assocClassCi.name(),
									ci1.name(), ci2.name(), e.getMessage());
						}
					}
				}
			}
		}

		for (Entry<ClassInfo, ClassInfo> entry : generalisations.entrySet()) {
			
			ClassInfo subtype = entry.getKey();
			ClassInfo supertype = entry.getValue();
			
			Integer c1ElementId = elementIdByClassInfo.get(subtype);
			Integer c2ElementId = elementIdByClassInfo.get(supertype);
			
			// check that element IDs were found
			if(c1ElementId == null) {
				result.addWarning(this,105,subtype.name(),supertype.name(),subtype.name());
			} else if (c2ElementId == null) {
				result.addWarning(this,105,subtype.name(),supertype.name(),supertype.name());
			} else {
			
				Element c1 = rep.GetElementByID(c1ElementId);
				Element c2 = rep.GetElementByID(c2ElementId);
				
				if (c1 == null) {
					result.addWarning(this,105,subtype.name(),supertype.name(),subtype.name());
				} else if (c2 == null) {
					result.addWarning(this,105,subtype.name(),supertype.name(),supertype.name());
				} else {
					
					c1.Refresh();
					Collection<Connector> c1Conns = c1.GetConnectors();
					c1Conns.Refresh();
					Connector con = c1Conns.AddNew("", "Generalization");
					con.SetSupplierID(c2.GetElementID());
					if (!con.Update()) {
						result.addError("EA-Fehler: " + con.GetLastError());
					}
					c1.GetConnectors().Refresh();
				}
			}
		}

		// 2015-06-25 JE: compact() no longer supported in EA v12 API
		// rep.Compact();
		rep.CloseFile();
		rep.Exit();

		// TBD: release any of the static fields so that the resources don't
		// linger?
	}

	public void reset() {

		initialised = false;
		outputFilename = null;
		rep = null;
		pOut_EaPkgId = null;

		associations = new HashSet<AssociationInfo>();
		elementIdByClassInfo = new HashMap<ClassInfo, Integer>();
		generalisations = new HashMap<ClassInfo, ClassInfo>();
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		/**
		 * Number ranges defined as follows:
		 * <ul>
		 * <li>1-100: Initialization related messages</li>
		 * <li>10001-10100: EA exceptions
		 * </ul>
		 */

		switch (mnr) {

		case 0:
			return "Context: class UmlModel";

		// 1-100: Initialization related messages

		// 101-200: issues with the model
		case 101:
			return "Supertype with id '$1$' of class '$2$' was not found in the model.";
		case 102:
			return "Supertype '$1$' of class '$2$' is not part of the schemas selected for processing. The generalization relationship will not be created.";
		case 103:
			return "Subtype with id '$1$' of class '$2$' was not found in the model.";
		case 104:
			return "Subtype '$1$' of class '$2$' is not part of the schemas selected for processing. The generalization relationship will not be created.";
		case 105:
			return "Generalisation relationship between subtype '$1$' and supertype '$2$' cannot be created because '$3$' is not part of the target model.";
			
		// 10001-10100: EA exceptions
		case 10001:
			return "EA exception encountered: $1$";
		case 10002:
			return "EA exception encountered while cloning attribute '$1$' of class '$2$'. Error message: $3$";
		case 10003:
			return "EA exception encountered while cloning standard items for model element (class or package) '$1$'. Error message: $2$";
		case 10004:
			return "EA exception encountered while cloning standard items for association '$1$'. Error message: $2$";
		case 10005:
			return "EA exception encountered while establishing the association class relationship between class '$1$' and the association between classes '$2$' and '$3$'. Error message: $4$";
		case 10006:
			return "Relationship between association class '$1$' and the association between classes '$2$' and '$3$' could not be established because the association class '$1$' is not part of the target model.";

		default:
			return "(Unknown message)";
		}
	}
}
