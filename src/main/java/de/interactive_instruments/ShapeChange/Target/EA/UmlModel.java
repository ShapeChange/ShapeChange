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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.CreateModelType;
import org.sparx.Element;
import org.sparx.Package;
import org.sparx.Repository;

import com.google.common.collect.HashMultimap;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.ea.EAAggregation;
import de.interactive_instruments.ShapeChange.Util.ea.EAAttributeUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorEndUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EADirection;
import de.interactive_instruments.ShapeChange.Util.ea.EAElementUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAException;
import de.interactive_instruments.ShapeChange.Util.ea.EANavigable;
import de.interactive_instruments.ShapeChange.Util.ea.EARepositoryUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EATaggedValue;

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
	public static final String PARAM_MODEL_FILENAME = "modelFilename";
	public static final String PARAM_OMIT_OUTPUT_PACKAGE_DATETIME = "omitOutputPackageDateTime";
	public static final String PARAM_EAP_TEMPLATE = "eapTemplate";

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
	private static HashMultimap<ClassInfo, ClassInfo> generalisations = HashMultimap
			.create();
	private static List<ClassInfo> classesToProcess = new ArrayList<ClassInfo>();
	private static Map<PackageInfo, Integer> eaPkgIdByPackageInfo = new HashMap<PackageInfo, Integer>();
	private static Map<String, String> stereotypeMappings = new HashMap<>();

	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		PackageInfo pi = p;
		model = m;
		options = o;
		result = r;

		if (!initialised) {

			String outputDirectory = options
					.parameter(this.getClass().getName(), PARAM_OUTPUT_DIR);
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = ".";

			outputFilename = options.parameterAsString(
					this.getClass().getName(), PARAM_MODEL_FILENAME,
					"ShapeChangeExport.eap", false, true);

			// change the default documentation template?
			documentationTemplate = options.parameter(this.getClass().getName(),
					"documentationTemplate");
			documentationNoValue = options.parameter(this.getClass().getName(),
					"documentationNoValue");

			/*
			 * Make sure repository file exists
			 */
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

				/*
				 * Either copy EAP template, or create new repository.
				 */

				String eapTemplateFilePath = options.parameter(
						this.getClass().getName(), PARAM_EAP_TEMPLATE);

				if (eapTemplateFilePath != null) {

					// copy template file either from remote or local URI
					if (eapTemplateFilePath.toLowerCase().startsWith("http")) {
						try {
							URL templateUrl = new URL(eapTemplateFilePath);
							FileUtils.copyURLToFile(templateUrl, repfile);
						} catch (MalformedURLException e1) {
							result.addFatalError(this, 51, eapTemplateFilePath,
									e1.getMessage());
							throw new ShapeChangeAbortException();
						} catch (IOException e2) {
							result.addFatalError(this, 53, e2.getMessage());
							throw new ShapeChangeAbortException();
						}
					} else {
						File eaptemplate = new File(eapTemplateFilePath);
						if (eaptemplate.exists()) {
							try {
								FileUtils.copyFile(eaptemplate, repfile);
							} catch (IOException e) {
								result.addFatalError(this, 53, e.getMessage());
								throw new ShapeChangeAbortException();
							}
						} else {
							result.addFatalError(this, 52,
									eaptemplate.getAbsolutePath());
							throw new ShapeChangeAbortException();
						}
					}

				} else {

					if (!rep.CreateModel(CreateModelType.cmEAPFromBase, absname,
							0)) {
						r.addError(null, 31, absname);
						rep = null;
						return;
					}
				}
			}

			/** Connect to EA Repository */
			if (!rep.OpenFile(absname)) {
				String errormsg = rep.GetLastError();
				r.addError(null, 30, errormsg, outputFilename);
				rep = null;
				return;
			}

			EARepositoryUtil.setEABatchAppend(rep, true);
			EARepositoryUtil.setEAEnableUIUpdates(rep, false);

			rep.RefreshModelView(0);

			Collection<Package> c = rep.GetModels();
			Package root = c.GetAt((short) 0);

			String outputPackageName = "ShapeChangeOutput";

			boolean omitOutputPackageDateTime = options.parameterAsBoolean(
					this.getClass().getName(),
					PARAM_OMIT_OUTPUT_PACKAGE_DATETIME, false);

			if (!omitOutputPackageDateTime) {
				TimeZone tz = TimeZone.getTimeZone("UTC");
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
				df.setTimeZone(tz);
				outputPackageName += "-" + df.format(new Date());
			}

			Package pOut = root.GetPackages().AddNew(outputPackageName,
					"Class View");
			if (!pOut.Update()) {
				result.addError("EA-Fehler: " + pOut.GetLastError());
			}
			pOut_EaPkgId = pOut.GetPackageID();

			// load stereotype mappings
			List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig()
					.getMapEntries();
			for (ProcessMapEntry me : mapEntries) {
				if ("stereotype".equalsIgnoreCase(me.getParam())) {
					/*
					 * NOTE: We map the value of @type, i.e. the well-known
					 * stereotype, to lower case, since all well-known
					 * stereotypes are in lower case. This avoids potential
					 * configuration issues.
					 */
					stereotypeMappings.put(
							me.getType().toLowerCase(Locale.ENGLISH),
							me.getTargetType());
				}
			}

			initialised = true;
		}

		if (rep == null || pOut_EaPkgId == null || !initialised)
			return; // repository not initialised

		// export app schema package
		try {
			clonePackage(pi, pOut_EaPkgId);
		} catch (EAException e) {
			result.addError(this, 100007, pi.name(), e.getMessage());
		}
	}

	private void clonePackage(PackageInfo pSource, Integer containerEaPkgId)
			throws EAException {

		int newPkgId = EARepositoryUtil.createEAPackage(rep, pSource,
				containerEaPkgId);

		Package pkg = rep.GetPackageByID(newPkgId);

		cloneStandarddItems(pkg.GetElement(), pSource);

		eaPkgIdByPackageInfo.put(pSource, newPkgId);

		for (PackageInfo cpi : pSource.containedPackages()) {
			clonePackage(cpi, pkg.GetPackageID());
		}
	}

	public void process(ClassInfo ci) {
		classesToProcess.add(ci);
	}

	private void cloneClass(ClassInfo ci) throws EAException {

		Integer pkgID = eaPkgIdByPackageInfo.get(ci.pkg());
		if (pkgID == null) {
			result.addError(
					"Missing package information for class " + ci.fullName());
			return;
		}

		Package pkg = rep.GetPackageByID(pkgID);
		if (pkg == null) {
			result.addError(
					"Missing package information for class " + ci.fullName());
			return;
		}

		Element e = EARepositoryUtil.createEAClass(rep, ci.name(), pkgID);

		elementIdByClassInfo.put(ci, e.GetElementID());

		cloneStandarddItems(e, ci);

		if (ci.getLinkedDocument() != null) {
			EAElementUtil.loadLinkedDocument(e,
					ci.getLinkedDocument().getAbsolutePath());
		}

		if (ci.isAbstract()) {
			EAElementUtil.setEAAbstract(e, true);
		}
		for (Constraint constr : ci.constraints()) {
			String type = determineConstraintType(constr);
			EAElementUtil.addConstraint(e, constr.name(), type, constr.text());
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
			} else if (!model.isInSelectedSchemas(cix)) {
				result.addError(this, 102, cix.name(), ci.name());
			} else {
				generalisations.put(ci, cix);
			}
		}
		for (String tid : ci.subtypes()) {
			ClassInfo cix = model.classById(tid);
			if (cix == null) {
				result.addError(this, 103, tid, ci.name());
			} else if (!model.isInSelectedSchemas(cix)) {
				result.addError(this, 104, cix.name(), ci.name());
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

	/**
	 * @param constr
	 * @return the type of the constraint; can be empty but not
	 *         <code>null</code>
	 */
	private String determineConstraintType(Constraint constr) {

		String type = "OCL";
		if (constr instanceof TextConstraint) {
			type = ((TextConstraint) constr).type();
			if (StringUtils.isBlank(type)) {
				type = "Text";
			}
		}
		return type;
	}

	private void cloneAttribute(Element e, PropertyInfo propi) {

		try {

			/*
			 * Create tagged value info suitable for method. Note that this kind
			 * of generic conversion currently does not take into account a
			 * potentially relevant fully qualified name for a tag, and if the
			 * values of the tag are stored in memo fields.
			 */
			List<EATaggedValue> taggedValues = EATaggedValue
					.fromTaggedValues(propi.taggedValuesAll());

			org.sparx.Attribute att = EAElementUtil.createEAAttribute(e,
					propi.name(), propi.aliasName(),
					propi.derivedDocumentation(documentationTemplate,
							documentationNoValue),
					mapStereotypes(propi.stereotypes()).asSet(), taggedValues,
					propi.isDerived(), propi.isOrdered(), !propi.isUnique(),
					propi.initialValue(), propi.cardinality(),
					propi.typeInfo().name, null);

			for (Constraint constr : propi.constraints()) {

				String type = determineConstraintType(constr);
				EAAttributeUtil.addConstraint(att, constr.name(), type,
						constr.text());
			}

		} catch (EAException exc) {

			result.addError(this, 10002, propi.name(), propi.inClass().name(),
					exc.getMessage());
		}
	}

	/**
	 * Maps the given stereotypes according to map entries (with param attribute
	 * 'stereotype') defined in the target configuration.
	 * 
	 * @param stereotypes
	 * @return the given Stereotypes, if no stereotype mappings are defined by
	 *         the configuration, or a new Stereotypes object with mapped
	 *         stereotypes
	 */
	private Stereotypes mapStereotypes(Stereotypes stereotypes) {

		if (stereotypeMappings.isEmpty()) {
			return stereotypes;
		} else {
			Stereotypes result = options.stereotypesFactory();
			for (String stereotype : stereotypes.asSet()) {
				if (stereotypeMappings.containsKey(stereotype)) {
					result.add(stereotypeMappings.get(stereotype));
				} else {
					result.add(stereotype);
				}
			}
			return result;
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

			EAElementUtil.setEAAlias(e, i.aliasName());

			EAElementUtil.setEANotes(e, i.derivedDocumentation(
					documentationTemplate, documentationNoValue));

			EAElementUtil.setEAStereotype(e,
					mapStereotypes(i.stereotypes()).toString());

			EAElementUtil.setTaggedValues(e, i.taggedValuesAll());

			e.Refresh();

		} catch (EAException exc) {

			result.addError(this, 10003, i.name(), exc.getMessage());
		}
	}

	private void cloneStandardItems(Connector con, Info i) {

		try {

			EAConnectorUtil.setEAAlias(con, i.aliasName());

			EAConnectorUtil.setEANotes(con, i.derivedDocumentation(
					documentationTemplate, documentationNoValue));

			EAConnectorUtil.setEAStereotype(con,
					mapStereotypes(i.stereotypes()).toString());

			EAConnectorUtil.setTaggedValues(con, i.taggedValuesAll());

		} catch (EAException exc) {

			result.addError(this, 10004,
					i.name() == null || i.name().trim().length() == 0
							? "<without_name>" : i.name(),
					exc.getMessage());
		}
	}

	private void cloneStandardItems(ConnectorEnd ce, PropertyInfo i) {

		try {

			EAConnectorEndUtil.setEARole(ce, i.name());
			EAConnectorEndUtil.setEAAlias(ce, i.aliasName());

			Multiplicity m = i.cardinality();
			if (m.minOccurs == 1 && m.maxOccurs == 1) {
				EAConnectorEndUtil.setEACardinality(ce, "");
			} else if (m.maxOccurs == Integer.MAX_VALUE) {
				EAConnectorEndUtil.setEACardinality(ce, m.minOccurs + "..*");
			} else {
				EAConnectorEndUtil.setEACardinality(ce,
						m.minOccurs + ".." + m.maxOccurs);
			}

			EAConnectorEndUtil.setEARole(ce, i.name());

			if (i.isNavigable()) {
				EAConnectorEndUtil.setEANavigable(ce, EANavigable.NAVIGABLE);
			} else {
				EAConnectorEndUtil.setEANavigable(ce, EANavigable.NONNAVIGABLE);
			}

			EAConnectorEndUtil.setEARoleNote(ce, i.derivedDocumentation(
					documentationTemplate, documentationNoValue));

			EAConnectorEndUtil.setEAStereotype(ce,
					mapStereotypes(i.stereotypes()).toString());

			EAConnectorEndUtil.setEAOrdering(ce, i.isOrdered());

			EAConnectorEndUtil.setEAAllowDuplicates(ce, !i.isUnique());

			if (i.reverseProperty() != null) {
				if (i.reverseProperty().isAggregation()) {
					EAConnectorEndUtil.setEAAggregation(ce,
							EAAggregation.SHARED);

				} else if (i.reverseProperty().isComposition()) {
					EAConnectorEndUtil.setEAAggregation(ce,
							EAAggregation.COMPOSITE);
				}
			}

			EAConnectorEndUtil.setTaggedValues(ce, i.taggedValuesAll());

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

	@Override
	public String getTargetName() {
		return "UML Model";
	}

	public void writeAll(ShapeChangeResult r) {

		result = r;
		options = r.options();

		if (rep == null || !initialised) {
			result.addError(this, 50);
			return;
		}

		for (ClassInfo ci : classesToProcess) {

			model = ci.model();

			try {
				cloneClass(ci);
			} catch (EAException e) {
				MessageContext mc = result.addError(this, 10008, ci.name(),
						e.getMessage());
				if (mc != null) {
					mc.addDetail(this, 1, ci.fullNameInSchema());
				}
			}
		}

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

				Connector con = c1Conns.AddNew(
						StringUtils.isBlank(ai.name()) ? "" : ai.name(),
						"Association");

				try {
					EAConnectorUtil.setEASupplierID(con, c2.GetElementID());
					EAConnectorUtil.setEADirection(con,
							EADirection.BIDIRECTIONAL);
				} catch (EAException e1) {
					result.addError(this, 10009,
							StringUtils.isBlank(ai.name()) ? "<unnamed>"
									: ai.name(),
							ci1.name(), ci2.name(), e1.getMessage());
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
							EAConnectorUtil.setEAAssociationClass(con,
									assocClass);
						} catch (EAException e) {

							result.addError(this, 10005, assocClassCi.name(),
									ci1.name(), ci2.name(), e.getMessage());
						}
					}
				}
			}
		}

		for (ClassInfo subtype : generalisations.keySet()) {
			for (ClassInfo supertype : generalisations.get(subtype)) {

				Integer c1ElementId = elementIdByClassInfo.get(subtype);
				Integer c2ElementId = elementIdByClassInfo.get(supertype);

				// check that element IDs were found
				if (c1ElementId == null) {
					result.addWarning(this, 105, subtype.name(),
							supertype.name(), subtype.name());
				} else if (c2ElementId == null) {
					result.addWarning(this, 105, subtype.name(),
							supertype.name(), supertype.name());
				} else {

					Element c1 = rep.GetElementByID(c1ElementId);
					Element c2 = rep.GetElementByID(c2ElementId);

					if (c1 == null) {
						result.addWarning(this, 105, subtype.name(),
								supertype.name(), subtype.name());
					} else if (c2 == null) {
						result.addWarning(this, 105, subtype.name(),
								supertype.name(), supertype.name());
					} else {

						c1.Refresh();
						Collection<Connector> c1Conns = c1.GetConnectors();
						c1Conns.Refresh();
						Connector con = c1Conns.AddNew("", "Generalization");

						try {
							EAConnectorUtil.setEASupplierID(con,
									c2.GetElementID());
						} catch (EAException e) {
							result.addError(this, 10010, subtype.name(),
									supertype.name(), e.getMessage());
						}
						c1.GetConnectors().Refresh();
					}
				}
			}
		}

		EARepositoryUtil.closeRepository(rep);

		// release resources from static fields
		rep = null;
	}

	public void reset() {

		initialised = false;
		outputFilename = null;
		rep = null;
		pOut_EaPkgId = null;

		associations = new HashSet<>();
		elementIdByClassInfo = new HashMap<>();
		eaPkgIdByPackageInfo = new HashMap<>();
		classesToProcess = new ArrayList<>();
		generalisations = HashMultimap.create();
		stereotypeMappings = new HashMap<>();
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
		case 1:
			return "Context: class $1$";

		// 50-100: Initialization related messages
		case 50:
			return "Could not write the model, because the target has not been initialized properly.";
		case 51:
			return "URL '$1$' provided for configuration parameter "
					+ PARAM_EAP_TEMPLATE
					+ " is malformed. Execution will be aborted. Exception message is: '$2$'.";
		case 52:
			return "EAP template at '$1$' does not exist or cannot be read. Check the value of the configuration parameter '"
					+ PARAM_EAP_TEMPLATE
					+ "' and ensure that: a) it contains the path to the template file and b) the file can be read by ShapeChange.";
		case 53:
			return "Exception encountered when copying EAP template file to output destination. Message is: $1$.";

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
		case 10007:
			return "EA exception encountered while cloning package '$1$'. Error message: $2$";
		case 10008:
			return "EA exception encountered while cloning class '$1$'. Error message: $2$";
		case 10009:
			return "EA exception encountered while updating association '$1$' between classes '$2$' and '$3$'. Exception message is: $4$";
		case 10010:
			return "EA exception encountered while updating generalisation relationship between classes '$1$' (subtype) and '$2$' (supertype). Exception message is: $4$";

		default:
			return "(" + UmlModel.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
