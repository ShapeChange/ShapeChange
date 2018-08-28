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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util.ea;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.AttributeConstraint;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorConstraint;
import org.sparx.ConnectorEnd;
import org.sparx.Constraint;
import org.sparx.Element;
import org.sparx.Method;
import org.sparx.ObjectType;
import org.sparx.Package;
import org.sparx.Parameter;
import org.sparx.Repository;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class EAModelDiff {

	class EAConstraintInfo {

		private String name;
		private String type;
		private String status;
		private String notes;

		/**
		 * @param name
		 * @param type
		 * @param status
		 * @param notes
		 */
		public EAConstraintInfo(String name, String type, String status,
				String notes) {
			super();
			this.name = name;
			this.type = type;
			this.status = status;
			this.notes = notes;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}

		/**
		 * @return the status
		 */
		public String getStatus() {
			return status;
		}

		/**
		 * @return the notes
		 */
		public String getNotes() {
			return notes;
		}
	}

	class EAModelElementInfo {

		private String name;
		private Integer id;
		private String modelPath;

		/**
		 * @param name
		 * @param id
		 * @param modelPath
		 */
		public EAModelElementInfo(String name, Integer id, String modelPath) {
			super();
			this.name = name;
			this.id = id;
			this.modelPath = modelPath;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the id
		 */
		public Integer getId() {
			return id;
		}

		/**
		 * @return the modelPath
		 */
		public String getModelPath() {
			return modelPath;
		}
	}

	class EAPackageInfo extends EAModelElementInfo {

		private EAModelElementInfo element;

		/**
		 * @param name
		 * @param id
		 * @param modelPath
		 * @param element
		 *            - can be <code>null</code> (e.g. in case of a model
		 *            package)
		 */
		public EAPackageInfo(String name, Integer id, String modelPath,
				EAModelElementInfo element) {
			super(name, id, modelPath);
			this.element = element;
		}

		/**
		 * @return the element, can be <code>null</code> (e.g. in case of a
		 *         model package)
		 */
		public EAModelElementInfo getElement() {
			return element;
		}
	}

	class EAConnectorInfo extends EAModelElementInfo {

		private String key;

		/**
		 * @param name
		 * @param id
		 * @param modelPath
		 * @param key
		 */
		public EAConnectorInfo(String name, Integer id, String modelPath,
				String key) {
			super(name, id, modelPath);
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	}

	private StringJoiner sj = new StringJoiner("\n");
	private Repository rep = null;
	private Repository refRep = null;

	private Set<String> allowedElementTypes = new HashSet<String>();
	private Set<String> allowedConnectorTypes = new HashSet<String>();

	private Set<Integer> idsOfCheckedConnectors = new HashSet<Integer>();
	private Set<Integer> refIdsOfCheckedConnectors = new HashSet<Integer>();

	private Map<Integer, EAPackageInfo> pkgInfosByID = new HashMap<Integer, EAPackageInfo>();
	private Map<Integer, EAPackageInfo> refPkgInfosByID = new HashMap<Integer, EAPackageInfo>();

	/**
	 * Compares the two EA repositories represented by the given File objects.
	 * NOTE: Not each and every details is checked.
	 * 
	 * @param file
	 * @param refFile
	 * @return
	 */
	public boolean similar(File file, File refFile) {

		sj = new StringJoiner("\n");
		idsOfCheckedConnectors = new HashSet<Integer>();
		refIdsOfCheckedConnectors = new HashSet<Integer>();

		allowedElementTypes.add("Class");
		allowedElementTypes.add("Interface");
		allowedElementTypes.add("Package");

		allowedConnectorTypes.add("Aggregation");
		allowedConnectorTypes.add("Association");
		allowedConnectorTypes.add("Generalization");
		allowedConnectorTypes.add("Generalisation");
		allowedConnectorTypes.add("Realization");
		allowedConnectorTypes.add("Realisation");

		try {
			rep = EARepositoryUtil.openRepository(file, true);
			rep.SetEnableCache(true);
			rep.SetEnableUIUpdates(false);

		} catch (EAException e) {

			sj.add("Could not open .eap file " + file.getAbsolutePath());

			EARepositoryUtil.closeRepository(rep);
			rep = null;

			return false;
		}

		try {
			refRep = EARepositoryUtil.openRepository(refFile, true);
			refRep.SetEnableCache(true);
			refRep.SetEnableUIUpdates(false);

		} catch (EAException e) {

			sj.add("Could not open reference .eap file "
					+ refFile.getAbsolutePath());

			EARepositoryUtil.closeRepository(rep);
			rep = null;

			EARepositoryUtil.closeRepository(refRep);
			refRep = null;

			return false;
		}

		try {

			return diffPackages(null, rep.GetModels(), refRep.GetModels());

		} catch (Exception e) {

			sj.add("Exception while comparing EAP '" + file + "' to reference '"
					+ refFile + "'. Exception message is: " + e.getMessage());

			e.printStackTrace();
			return false;

		} finally {

			EARepositoryUtil.closeRepository(rep);
			rep = null;

			EARepositoryUtil.closeRepository(refRep);
			refRep = null;
		}
	}

	private boolean diffPackages(EAPackageInfo parentPkg,
			Collection<Package> eaPkgs, Collection<Package> refEaPkgs) {

		ListMultimap<String, EAPackageInfo> pkgsByName = parseEAPackages(eaPkgs,
				this.pkgInfosByID);
		ListMultimap<String, EAPackageInfo> refPkgsByName = parseEAPackages(
				refEaPkgs, this.refPkgInfosByID);

		if (pkgsByName.size() > refPkgsByName.size()) {

			sj.add((parentPkg == null ? ""
					: "Package " + parentPkg.getModelPath() + " - ")
					+ "Reference model has less child packages.");
			return false;

		} else if (pkgsByName.size() < refPkgsByName.size()) {

			sj.add((parentPkg == null ? ""
					: "Package " + parentPkg.getModelPath() + " - ")
					+ "Reference model has more child packages.");
			return false;

		} else {

			boolean result = true;

			for (String refPkgName : refPkgsByName.keySet()) {

				List<EAPackageInfo> refPkgsForName = refPkgsByName
						.get(refPkgName);
				List<EAPackageInfo> pkgsForName = pkgsByName
						.removeAll(refPkgName);

				if (pkgsForName.size() > refPkgsForName.size()) {

					System.out
							.println(
									(parentPkg == null ? "Model"
											: "Package "
													+ parentPkg.getModelPath())
											+ " - Reference has less child packages with name '"
											+ refPkgName + "'.");
					result = false;

				} else if (pkgsForName.size() < refPkgsForName.size()) {

					System.out
							.println(
									(parentPkg == null ? "Model"
											: "Package "
													+ parentPkg.getModelPath())
											+ " - Reference has more child packages with name '"
											+ refPkgName + "'.");
					result = false;

				} else {

					for (int i = 0; i < refPkgsForName.size(); i++) {

						EAPackageInfo refPkg = refPkgsForName.get(i);
						EAPackageInfo pkg = pkgsForName.get(i);

						result &= diffPackage(pkg, refPkg);
					}
				}
			}

			// check if any packages remain, which would be new
			for (EAPackageInfo pkg : pkgsByName.values()) {

				sj.add((parentPkg == null ? "Model"
						: "Package " + parentPkg.getModelPath())
						+ " - Found unexpected package with name '"
						+ pkg.getName() + "'.");

				result = false;
			}

			return result;
		}
	}

	private boolean diffPackage(EAPackageInfo pkgInfo,
			EAPackageInfo refPkgInfo) {

		boolean result = true;

		Package pkg = rep.GetPackageByID(pkgInfo.getId());
		Package refPkg = refRep.GetPackageByID(refPkgInfo.getId());

		if (pkgInfo.getElement() != null && refPkgInfo.getElement() != null) {
			// diff package details
			result &= diffElement(pkgInfo.getElement(),
					refPkgInfo.getElement());
		}

		// diff contained elements
		result &= diffElements(pkgInfo, pkg.GetElements(),
				refPkg.GetElements());

		// diff child packages
		result &= diffPackages(pkgInfo, pkg.GetPackages(),
				refPkg.GetPackages());

		return result;
	}

	private boolean diffElements(EAPackageInfo pkg, Collection<Element> eaElmts,
			Collection<Element> refEaElmts) {

		ListMultimap<String, EAModelElementInfo> elmtsByName = parseEAElements(
				pkg.getModelPath(), eaElmts);
		ListMultimap<String, EAModelElementInfo> refElmtsByName = parseEAElements(
				pkg.getModelPath(), refEaElmts);

		if (elmtsByName.size() > refElmtsByName.size()) {

			sj.add("Package " + pkg.getModelPath()
					+ " - Reference package has less elements.");
			return false;

		} else if (elmtsByName.size() < refElmtsByName.size()) {

			sj.add("Package " + pkg.getModelPath()
					+ " - Reference package has more elements.");
			return false;

		} else {

			boolean result = true;

			for (String refElmtName : refElmtsByName.keySet()) {

				List<EAModelElementInfo> refElmtsForName = refElmtsByName
						.get(refElmtName);
				List<EAModelElementInfo> elmtsForName = elmtsByName
						.removeAll(refElmtName);

				if (elmtsForName.size() > refElmtsForName.size()) {

					sj.add("Package " + pkg.getModelPath()
							+ " - Reference package has less elements with name '"
							+ refElmtName + "'.");

					result = false;

				} else if (elmtsForName.size() < refElmtsForName.size()) {

					sj.add("Package " + pkg.getModelPath()
							+ " - Reference package has more elements with name '"
							+ refElmtName + "'.");

					result = false;

				} else {

					for (int i = 0; i < refElmtsForName.size(); i++) {

						EAModelElementInfo refElmt = refElmtsForName.get(i);
						EAModelElementInfo elmt = elmtsForName.get(i);

						result &= diffElement(elmt, refElmt);
					}
				}
			}

			// check if any elements remain, which would be new
			for (EAModelElementInfo elmt : elmtsByName.values()) {

				sj.add("Package " + pkg.getModelPath()
						+ " - Found unexpected element with name '"
						+ elmt.getName() + "'.");

				result = false;
			}

			return result;
		}
	}

	private boolean diffConnectors(EAModelElementInfo elmt,
			Collection<Connector> eaConns, Collection<Connector> refEaConns) {

		ListMultimap<String, EAConnectorInfo> connsByKey = parseEAConnectors(
				elmt.getModelPath(), eaConns, rep);
		ListMultimap<String, EAConnectorInfo> refConnsByKey = parseEAConnectors(
				elmt.getModelPath(), refEaConns, refRep);

		if (connsByKey.size() > refConnsByKey.size()) {

			sj.add("Element " + elmt.getModelPath()
					+ " - Reference has less connectors.");
			return false;

		} else if (connsByKey.size() < refConnsByKey.size()) {

			sj.add("Element " + elmt.getModelPath()
					+ " - Reference has more connectors.");
			return false;

		} else {

			boolean result = true;

			for (String refConnKey : refConnsByKey.keySet()) {

				List<EAConnectorInfo> refConnsForKey = refConnsByKey
						.get(refConnKey);
				List<EAConnectorInfo> connsForKey = connsByKey
						.removeAll(refConnKey);

				if (connsForKey.size() > refConnsForKey.size()) {

					sj.add("Element " + elmt.getModelPath()
							+ " - Reference has less connectors with key: "
							+ refConnKey);

					result = false;

				} else if (connsForKey.size() < refConnsForKey.size()) {

					sj.add("Element " + elmt.getModelPath()
							+ " - Reference has more connectors with key: "
							+ refConnKey);

					result = false;

				} else {

					for (int i = 0; i < refConnsForKey.size(); i++) {

						EAConnectorInfo refConn = refConnsForKey.get(i);
						EAConnectorInfo conn = connsForKey.get(i);

						result &= diffConnector(elmt.getModelPath(),
								rep.GetConnectorByID(conn.getId()),
								refRep.GetConnectorByID(refConn.getId()));
					}
				}
			}

			// check if any connectors remain, which would be new
			for (EAConnectorInfo conn : connsByKey.values()) {

				sj.add("Element " + elmt.getModelPath()
						+ " - Found unexpected connector with key: "
						+ conn.getKey());

				result = false;
			}

			return result;
		}
	}

	private boolean diffMethods(EAModelElementInfo elmt,
			Collection<Method> eaMethods, Collection<Method> refEaMethods) {

		ListMultimap<String, EAModelElementInfo> methodsByName = parseEAMethods(
				elmt.getModelPath(), eaMethods);
		ListMultimap<String, EAModelElementInfo> refMethodsByName = parseEAMethods(
				elmt.getModelPath(), refEaMethods);

		if (methodsByName.size() > refMethodsByName.size()) {

			sj.add("Element " + elmt.getModelPath()
					+ " - Reference has less methods.");
			return false;

		} else if (methodsByName.size() < refMethodsByName.size()) {

			sj.add("Element " + elmt.getModelPath()
					+ " - Reference has more methods.");
			return false;

		} else {

			boolean result = true;

			for (String refMethodName : refMethodsByName.keySet()) {

				List<EAModelElementInfo> refMethodsForName = refMethodsByName
						.get(refMethodName);
				List<EAModelElementInfo> methodsForName = methodsByName
						.removeAll(refMethodName);

				if (methodsForName.size() > refMethodsForName.size()) {

					sj.add("Element " + elmt.getModelPath()
							+ " - Reference has less methods with name '"
							+ refMethodName + "'.");

					result = false;

				} else if (methodsForName.size() < refMethodsForName.size()) {

					sj.add("Element " + elmt.getModelPath()
							+ " - Reference has more methods with name '"
							+ refMethodName + "'.");

					result = false;

				} else {

					for (int i = 0; i < refMethodsForName.size(); i++) {

						EAModelElementInfo refMethod = refMethodsForName.get(i);
						EAModelElementInfo method = methodsForName.get(i);

						result &= diffMethod(method, refMethod);
					}
				}
			}

			// check if any methods remain, which would be new
			for (EAModelElementInfo method : methodsByName.values()) {

				sj.add("Element " + elmt.getModelPath()
						+ " - Found unexpected method with name '"
						+ method.getName() + "'.");

				result = false;
			}

			return result;
		}
	}

	private boolean diffMethod(EAModelElementInfo method,
			EAModelElementInfo refMethod) {

		Method meth = rep.GetMethodByID(method.getId());
		Method refMeth = refRep.GetMethodByID(refMethod.getId());

		boolean result = true;

		result &= similar(method.getModelPath(), "Abstract", meth.GetAbstract(),
				refMeth.GetAbstract());

		// Ignore Behavior, ClassifierID

		result &= similar(method.getModelPath(), "Code", meth.GetCode(),
				refMeth.GetCode());

		result &= similar(method.getModelPath(), "Concurrency",
				meth.GetConcurrency(), refMeth.GetConcurrency());

		result &= similarIgnoreCase(method.getModelPath(), "FQStereotype",
				meth.GetFQStereotype(), refMeth.GetFQStereotype());

		result &= similar(method.getModelPath(), "IsConst", meth.GetIsConst(),
				refMeth.GetIsConst());

		result &= similar(method.getModelPath(), "IsLeaf", meth.GetIsLeaf(),
				refMeth.GetIsLeaf());

		result &= similar(method.getModelPath(), "IsPure", meth.GetIsPure(),
				refMeth.GetIsPure());

		result &= similar(method.getModelPath(), "IsQuery", meth.GetIsQuery(),
				refMeth.GetIsQuery());

		result &= similar(method.getModelPath(), "IsRoot", meth.GetIsRoot(),
				refMeth.GetIsRoot());

		result &= similar(method.getModelPath(), "IsStatic", meth.GetIsStatic(),
				refMeth.GetIsStatic());

		result &= similar(method.getModelPath(), "IsSynchronized",
				meth.GetIsSynchronized(), refMeth.GetIsSynchronized());

		// Ignore Name (was used to identify the methods to compare)

		result &= similar(method.getModelPath(), "Notes", meth.GetNotes(),
				refMeth.GetNotes());

		result &= similar(method.getModelPath(), "ObjectType",
				meth.GetObjectType(), refMeth.GetObjectType());

		// Parameters
		result &= diffParameters(method.getModelPath(), meth.GetParameters(),
				refMeth.GetParameters());

		// Ignore ParentID (parent element name was used to identify the methods
		// to compare)

		// Ignore Pos (methods to compare are identified by their name; order
		// should not matter)

		// Ignore PostConditions, PreConditions

		result &= similar(method.getModelPath(), "ReturnIsArray",
				meth.GetReturnIsArray(), refMeth.GetReturnIsArray());

		result &= similar(method.getModelPath(), "ReturnType",
				meth.GetReturnType(), refMeth.GetReturnType());

		// Ignore StateFlags, Stereotype (contained in StereotypeEx)

		result &= similarIgnoreCase(method.getModelPath(), "StereotypeEx",
				meth.GetStereotypeEx(), refMeth.GetStereotypeEx());

		result &= similar(method.getModelPath(), "Style", meth.GetStyle(),
				refMeth.GetStyle());

		result &= similar(method.getModelPath(), "StyleEx", meth.GetStyleEx(),
				refMeth.GetStyleEx());

		// TaggedValues
		result &= similar(method.getModelPath(),
				EAMethodUtil.getEATaggedValues(meth),
				EAMethodUtil.getEATaggedValues(refMeth));

		result &= similar(method.getModelPath(), "Throws", meth.GetThrows(),
				refMeth.GetThrows());

		result &= similar(method.getModelPath(), "Visibility",
				meth.GetVisibility(), refMeth.GetVisibility());

		return result;
	}

	private boolean diffParameters(String modelPath,
			Collection<Parameter> eaParams, Collection<Parameter> refEaParams) {

		short countParams = eaParams.GetCount();
		short countRefParams = refEaParams.GetCount();

		if (countParams > countRefParams) {

			sj.add("Method " + modelPath + " - Reference has less parameters.");
			return false;

		} else if (countParams < countRefParams) {

			sj.add("Method " + modelPath + " - Reference has more parameters.");
			return false;

		} else {

			/*
			 * Here we actually check the order of the parameters as well.
			 */

			boolean result = true;

			for (short i = 0; i < countRefParams; i++) {

				Parameter refParam = refEaParams.GetAt(i);
				Parameter param = eaParams.GetAt(i);

				result &= diffParameter(
						modelPath + ", parameter [" + (i + 1) + "]", param,
						refParam);
			}

			return result;
		}
	}

	private boolean diffAttributes(String elmtModelPath,
			Collection<Attribute> eaAtts, Collection<Attribute> refEaAtts) {

		ListMultimap<String, EAModelElementInfo> attsByName = parseEAAttributes(
				elmtModelPath, eaAtts);
		ListMultimap<String, EAModelElementInfo> refAttsByName = parseEAAttributes(
				elmtModelPath, refEaAtts);

		if (attsByName.size() > refAttsByName.size()) {

			sj.add("Element " + elmtModelPath
					+ " - Reference element has less attributes.");
			return false;

		} else if (attsByName.size() < refAttsByName.size()) {

			sj.add("Element" + elmtModelPath
					+ " - Reference element has more attributes.");
			return false;

		} else {

			boolean result = true;

			for (String refAttName : refAttsByName.keySet()) {

				List<EAModelElementInfo> refAttsForName = refAttsByName
						.get(refAttName);
				List<EAModelElementInfo> attsForName = attsByName
						.removeAll(refAttName);

				if (attsForName.size() > refAttsForName.size()) {

					sj.add("Element " + elmtModelPath
							+ " - Reference element has less attributes with name '"
							+ refAttName + "'.");

					result = false;

				} else if (attsForName.size() < refAttsForName.size()) {

					sj.add("Element " + elmtModelPath
							+ " - Reference element has more attributes with name '"
							+ refAttName + "'.");

					result = false;

				} else {

					for (int i = 0; i < refAttsForName.size(); i++) {

						EAModelElementInfo refAtt = refAttsForName.get(i);
						EAModelElementInfo att = attsForName.get(i);

						result &= diffAttribute(att, refAtt);
					}
				}
			}

			// check if any attributes remain, which would be new
			for (EAModelElementInfo att : attsByName.values()) {

				sj.add("Element " + elmtModelPath
						+ " - Found unexpected attribute with name '"
						+ att.getName() + "'.");

				result = false;
			}

			return result;
		}
	}

	/**
	 * @param pkgModelPath
	 * @param elmts
	 * @return multimap of element infos, with element names as sorted keys, and
	 *         elements stored in an array list
	 */
	private ListMultimap<String, EAModelElementInfo> parseEAElements(
			String pkgModelPath, Collection<Element> elmts) {

		ListMultimap<String, EAModelElementInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < elmts.GetCount(); i++) {

			Element elmt = elmts.GetAt(i);

			String elmtType = elmt.GetType();

			if (ignoreElementType(elmtType)) {
				continue;
			}

			int id = elmt.GetElementID();
			String name = elmt.GetName();
			String path = pkgModelPath + "::" + name;

			if (result.containsKey(name)) {
				int index = result.get(name).size() + 1;
				path = path + "[" + index + "]";
			}

			EAModelElementInfo elmtInfo = new EAModelElementInfo(name, id,
					path);

			result.put(name, elmtInfo);
		}

		return result;
	}

	/**
	 * @param elmtModelPath
	 * @param conns
	 * @param repOfConns
	 *            the repository to which the connectors belong
	 * @return multimap of connectors infos, with connector keys as defined by
	 *         {@link #getKey(Connector, ConnectorEnd, ConnectorEnd)} as sorted
	 *         keys, and connectors stored in an array list
	 */
	private ListMultimap<String, EAConnectorInfo> parseEAConnectors(
			String elmtModelPath, Collection<Connector> conns,
			Repository repOfConns) {

		ListMultimap<String, EAConnectorInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < conns.GetCount(); i++) {

			Connector conn = conns.GetAt(i);

			String connType = conn.GetType();

			if (ignoreConnectorType(connType)) {
				continue;
			}

			int id = conn.GetConnectorID();
			String name = conn.GetName();

			String key = getKey(conn, conn.GetClientEnd(),
					conn.GetSupplierEnd(), repOfConns);
			String path = elmtModelPath + ", connector" + key;

			if (result.containsKey(name)) {
				int index = result.get(name).size() + 1;
				path = path + "[" + index + "]";
			}

			EAConnectorInfo elmtInfo = new EAConnectorInfo(name, id, path, key);

			result.put(key, elmtInfo);
		}

		return result;
	}

	/**
	 * @param elmtModelPath
	 * @param methods
	 * @return multimap of method infos, with method names as sorted keys, and
	 *         methods stored in an array list
	 */
	private ListMultimap<String, EAModelElementInfo> parseEAMethods(
			String elmtModelPath, Collection<Method> methods) {

		ListMultimap<String, EAModelElementInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < methods.GetCount(); i++) {

			Method method = methods.GetAt(i);

			int id = method.GetMethodID();
			String name = method.GetName();
			String path = elmtModelPath + ", method " + name;

			if (result.containsKey(name)) {
				int index = result.get(name).size() + 1;
				path = path + "[" + index + "]";
			}

			EAModelElementInfo elmtInfo = new EAModelElementInfo(name, id,
					path);

			result.put(name, elmtInfo);
		}

		return result;
	}

	/**
	 * @param elmtModelPath
	 * @param atts
	 * @return multimap of attribute infos, with attribute names as sorted keys,
	 *         and attributes stored in an array list
	 */
	private ListMultimap<String, EAModelElementInfo> parseEAAttributes(
			String elmtModelPath, Collection<Attribute> atts) {

		ListMultimap<String, EAModelElementInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < atts.GetCount(); i++) {

			Attribute att = atts.GetAt(i);

			int id = att.GetAttributeID();
			String name = att.GetName();
			String path = elmtModelPath + "." + name;

			if (result.containsKey(name)) {
				int index = result.get(name).size() + 1;
				path = path + "[" + index + "]";
			}

			EAModelElementInfo attInfo = new EAModelElementInfo(name, id, path);

			result.put(name, attInfo);
		}

		return result;
	}

	private boolean diffElement(EAModelElementInfo elmtInfo,
			EAModelElementInfo refElmtInfo) {

		Element elmt = rep.GetElementByID(elmtInfo.getId());
		Element refElmt = refRep.GetElementByID(refElmtInfo.getId());

		String elmtType = elmt.GetType();
		String refElmtType = refElmt.GetType();

		if (ignoreElementType(elmtType)) {
			sj.add(elmtInfo.getModelPath() + " - Ignored because its type is "
					+ elmtType);
			return true;
		}

		boolean result = true;

		result &= similar(elmtInfo.getModelPath(), "Abstract",
				elmt.GetAbstract(), refElmt.GetAbstract());

		// Ignore ActionFlags

		result &= similar(elmtInfo.getModelPath(), "Alias", elmt.GetAlias(),
				refElmt.GetAlias());

		// association class
		result &= similar(elmtInfo.getModelPath(), "IsAssociationClass",
				elmt.IsAssociationClass(), refElmt.IsAssociationClass());
		if (elmt.IsAssociationClass() && refElmt.IsAssociationClass()) {
			result &= diffConnector(elmtInfo.getModelPath(),
					rep.GetConnectorByID(elmt.GetAssociationClassConnectorID()),
					refRep.GetConnectorByID(
							refElmt.GetAssociationClassConnectorID()));
		}

		// Ignore Author, BaseClasses (should be checked via connectors),
		// ClassifierID, ClassifierName, ClassifierType, Complexity,
		// CompositeDiagram

		// constraints
		ListMultimap<String, EAConstraintInfo> consByName = parseEAConstraints(
				elmt.GetConstraints());
		ListMultimap<String, EAConstraintInfo> refConsByName = parseEAConstraints(
				refElmt.GetConstraints());
		result &= diffConstraints(elmtInfo.getModelPath(), consByName,
				refConsByName);

		// Ignore Created, CustomProperties, Diagrams, Difficulty, Efforts,
		// Elements, EmbeddedElements, EventFlags, ExtensionPoints, Files

		result &= similar(elmtInfo.getModelPath(), "FQName", elmt.GetFQName(),
				refElmt.GetFQName());

		result &= similarIgnoreCase(elmtInfo.getModelPath(), "FQStereotype",
				elmt.GetFQStereotype(), refElmt.GetFQStereotype());

		// Ignore GenFile, Genlinks

		result &= similar(elmtInfo.getModelPath(), "GenType", elmt.GetGentype(),
				refElmt.GetGentype());

		// Ignore Header1, Header2

		result &= similar(elmtInfo.getModelPath(), "IsActive",
				elmt.GetIsActive(), refElmt.GetIsActive());

		result &= similar(elmtInfo.getModelPath(), "IsComposite",
				elmt.GetIsComposite(), refElmt.GetIsComposite());

		result &= similar(elmtInfo.getModelPath(), "IsLeaf", elmt.GetIsLeaf(),
				refElmt.GetIsLeaf());

		result &= similar(elmtInfo.getModelPath(), "IsNew", elmt.GetIsNew(),
				refElmt.GetIsNew());

		result &= similar(elmtInfo.getModelPath(), "IsRoot", elmt.GetIsRoot(),
				refElmt.GetIsRoot());

		result &= similar(elmtInfo.getModelPath(), "IsSpec", elmt.GetIsSpec(),
				refElmt.GetIsSpec());

		result &= similar(elmtInfo.getModelPath(), "LinkedDocument",
				elmt.GetLinkedDocument(), refElmt.GetLinkedDocument());

		// Ignore Issues, Locked, MetaType

		// Methods
		result &= diffMethods(elmtInfo, elmt.GetMethods(),
				refElmt.GetMethods());

		// Ignore Metrics, MiscData, Modified

		result &= similar(elmtInfo.getModelPath(), "Multiplicity",
				elmt.GetMultiplicity(), refElmt.GetMultiplicity());

		/*
		 * Ignore Name - it should be the same due to the way we identify
		 * elements to compare.
		 */

		result &= similar(elmtInfo.getModelPath(), "Notes", elmt.GetNotes(),
				refElmt.GetNotes());

		result &= similar(elmtInfo.getModelPath(), "ObjectType",
				elmt.GetObjectType(), refElmt.GetObjectType());

		// Ignore PackageID, ParentID, Partitions, Persistence, Phase, Priority,
		// Properties, PropertyType, PropertyTypeName, Realizes,
		// Requirements(Ex), Resources, Risks, RunState, Scenarios,
		// StateTransitions

		result &= similar(elmtInfo.getModelPath(), "Status", elmt.GetStatus(),
				refElmt.GetStatus());

		// Ignore Stereotype (contained in StereotypeEx)

		result &= similarIgnoreCase(elmtInfo.getModelPath(), "StereotypeEx",
				elmt.GetStereotypeEx(), refElmt.GetStereotypeEx());

		result &= similar(elmtInfo.getModelPath(), "StyleEx", elmt.GetStyleEx(),
				refElmt.GetStyleEx());

		// Ignore Subtype

		result &= similar(elmtInfo.getModelPath(), "Tablespace",
				elmt.GetTablespace(), refElmt.GetTablespace());

		result &= similar(elmtInfo.getModelPath(), "Tag", elmt.GetTag(),
				refElmt.GetTag());

		// TaggedValues
		result &= similar(elmtInfo.getModelPath(),
				EAElementUtil.getEATaggedValues(elmt),
				EAElementUtil.getEATaggedValues(refElmt));

		// Ignore TemplateParameters, Tests, TreePos

		result &= similar(elmtInfo.getModelPath(), "Type", elmtType,
				refElmtType);

		// Ignore Version

		result &= similar(elmtInfo.getModelPath(), "Visibility",
				elmt.GetVisibility(), refElmt.GetVisibility());

		// attributes
		result &= diffAttributes(elmtInfo.getModelPath(), elmt.GetAttributes(),
				refElmt.GetAttributes());

		// Connectors
		result &= diffConnectors(elmtInfo, elmt.GetConnectors(),
				refElmt.GetConnectors());

		return result;
	}

	private boolean diffAttribute(EAModelElementInfo attInfo,
			EAModelElementInfo refAttInfo) {

		Attribute att = rep.GetAttributeByID(attInfo.getId());
		Attribute refAtt = refRep.GetAttributeByID(refAttInfo.getId());

		boolean result = true;

		result &= similar(attInfo.getModelPath(), "Alias", att.GetAlias(),
				refAtt.GetAlias());

		result &= similar(attInfo.getModelPath(),
				"AllowDuplicates (DB: Not Null)", att.GetAllowDuplicates(),
				refAtt.GetAllowDuplicates());

		result &= similarReferencedElementName(attInfo.getModelPath(),
				"ClassifierID", att.GetClassifierID(),
				refAtt.GetClassifierID());

		result &= similar(attInfo.getModelPath(), "Container",
				att.GetContainer(), refAtt.GetContainer());

		result &= similar(attInfo.getModelPath(), "Containment",
				att.GetContainment(), refAtt.GetContainment());

		ListMultimap<String, EAConstraintInfo> consByName = parseEAAttributeConstraints(
				att.GetConstraints());
		ListMultimap<String, EAConstraintInfo> refConsByName = parseEAAttributeConstraints(
				refAtt.GetConstraints());
		result &= diffConstraints(attInfo.getModelPath(), consByName,
				refConsByName);

		result &= similar(attInfo.getModelPath(), "Default", att.GetDefault(),
				refAtt.GetDefault());

		result &= similarIgnoreCase(attInfo.getModelPath(), "FQStereotype",
				att.GetFQStereotype(), refAtt.GetFQStereotype());

		result &= similar(attInfo.getModelPath(),
				"IsCollection (DB: is foreign key)", att.GetIsCollection(),
				refAtt.GetIsCollection());

		result &= similar(attInfo.getModelPath(), "IsConst", att.GetIsConst(),
				refAtt.GetIsConst());

		result &= similar(attInfo.getModelPath(), "IsDerived",
				att.GetIsDerived(), refAtt.GetIsDerived());

		result &= similar(attInfo.getModelPath(), "IsID", att.GetIsID(),
				refAtt.GetIsID());

		result &= similar(attInfo.getModelPath(),
				"IsOrdered (DB: is primary key)", att.GetIsOrdered(),
				refAtt.GetIsOrdered());

		result &= similar(attInfo.getModelPath(), "IsStatic (DB: is unique)",
				att.GetIsStatic(), refAtt.GetIsStatic());

		result &= similar(attInfo.getModelPath(), "Length", att.GetLength(),
				refAtt.GetLength());

		result &= similar(attInfo.getModelPath(), "LowerBound",
				att.GetLowerBound(), refAtt.GetLowerBound());

		/*
		 * Name is automatically equal, since that's how we found the attributes
		 * to compare.
		 */

		result &= similar(attInfo.getModelPath(), "Notes", att.GetNotes(),
				refAtt.GetNotes());

		result &= similar(attInfo.getModelPath(), "ObjectType",
				att.GetObjectType(), refAtt.GetObjectType());

		/*
		 * Comparison of ParentID - more specifically: the name of the
		 * referenced parent element - should be irrelevant, since we found the
		 * attributes to compare through elements that have the same name.
		 */

		// We ignore the order of the attributes, and thus their 'Pos'

		result &= similar(attInfo.getModelPath(), "Precision",
				att.GetPrecision(), refAtt.GetPrecision());

		// Ignore RedefinedProperty

		result &= similar(attInfo.getModelPath(), "Scale", att.GetScale(),
				refAtt.GetScale());

		// Ignore Stereotype (StereotypeEx contains it)

		result &= similarIgnoreCase(attInfo.getModelPath(), "StereotypeEx",
				att.GetStereotypeEx(), refAtt.GetStereotypeEx());

		result &= similar(attInfo.getModelPath(), "Style", att.GetStyle(),
				refAtt.GetStyle());

		result &= similar(attInfo.getModelPath(), "StyleEx", att.GetStyleEx(),
				refAtt.GetStyleEx());

		// Ignore SubsettedProperty

		// TaggedValues
		result &= similar(attInfo.getModelPath(),
				EAAttributeUtil.getEATaggedValuesWithCombinedKeys(att),
				EAAttributeUtil.getEATaggedValuesWithCombinedKeys(refAtt));

		result &= similar(attInfo.getModelPath(), "Type", att.GetType(),
				refAtt.GetType());

		result &= similar(attInfo.getModelPath(), "UpperBound",
				att.GetUpperBound(), refAtt.GetUpperBound());

		result &= similar(attInfo.getModelPath(), "Visibility",
				att.GetVisibility(), refAtt.GetVisibility());

		return result;
	}

	private boolean diffParameter(String modelPath, Parameter param,
			Parameter refParam) {

		boolean result = true;

		result &= similar(modelPath, "Alias", param.GetAlias(),
				refParam.GetAlias());

		// Ignore ClassifierID

		result &= similar(modelPath, "Default", param.GetDefault(),
				refParam.GetDefault());

		result &= similar(modelPath, "IsConst", param.GetIsConst(),
				refParam.GetIsConst());

		result &= similar(modelPath, "Kind", param.GetKind(),
				refParam.GetKind());

		result &= similar(modelPath, "Name", param.GetName(),
				refParam.GetName());

		result &= similar(modelPath, "Notes", param.GetNotes(),
				refParam.GetNotes());

		result &= similar(modelPath, "ObjectType", param.GetObjectType(),
				refParam.GetObjectType());

		// Ignore OperationID (method name was used to identify parameters to
		// compare)

		// Ignore Position - parameters are checked in the order in which
		// they occur in the parameter collection of the method

		// Ignore Stereotype (contained in StereotypeEx)

		result &= similarIgnoreCase(modelPath, "StereotypeEx",
				param.GetStereotypeEx(), refParam.GetStereotypeEx());

		result &= similar(modelPath, "Style", param.GetStyle(),
				refParam.GetStyle());

		result &= similar(modelPath, "StyleEx", param.GetStyleEx(),
				refParam.GetStyleEx());

		result &= similar(modelPath, "Type", param.GetType(),
				refParam.GetType());

		// TaggedValues
		result &= similar(modelPath, EAParameterUtil.getEATaggedValues(param),
				EAParameterUtil.getEATaggedValues(refParam));

		return result;
	}

	/**
	 * @param modelPath
	 * @param tvs
	 *            key: {name '#' fqName}; value: according EATaggedValue
	 * @param refTvs
	 *            key: {name '#' fqName}; value: according EATaggedValue
	 * @return
	 */
	private boolean similar(String modelPath,
			SortedMap<String, EATaggedValue> tvs,
			SortedMap<String, EATaggedValue> refTvs) {

		if (tvs.size() > refTvs.size()) {

			sj.add(modelPath + " - Reference has less tagged values.");
			return false;

		} else if (tvs.size() < refTvs.size()) {

			sj.add(modelPath + " - Reference has more tagged values.");
			return false;

		} else {

			boolean result = true;

			for (String refTvKey : refTvs.keySet()) {

				EATaggedValue refTv = refTvs.get(refTvKey);
				EATaggedValue tv = tvs.remove(refTvKey);

				String refTVModelPath = modelPath + ", tagged value '"
						+ refTv.getName() + "' (FQName '" + refTv.getFQName()
						+ "')";

				if (tv == null) {

					sj.add(modelPath + " - Missing tagged value with name '"
							+ refTv.getName() + "', FQName '"
							+ refTv.getFQName() + "' and values: "
							+ StringUtils.join(refTv.getValues(), ", ") + ".");

					result = false;

				} else if (tv.getValues().size() > refTv.getValues().size()) {

					sj.add(refTVModelPath + " - Reference has less values.");

					result = false;

				} else if (tv.getValues().size() < refTv.getValues().size()) {

					sj.add(refTVModelPath + " - Reference has more values.");

					result = false;

				} else {

					// compare values
					List<String> tvValues = tv.getValues();
					Collections.sort(tvValues);

					List<String> refTvValues = refTv.getValues();
					Collections.sort(refTvValues);

					/*
					 * Apply specific comparison for tags that reference model
					 * elements
					 */
					if (tv.getName().equalsIgnoreCase("OIDFieldName")
							|| tv.getName().equalsIgnoreCase("LengthFieldName")
							|| tv.getName().equalsIgnoreCase("ShapeFieldName")
							|| tv.getName()
									.equalsIgnoreCase("DestinationForeignKey")
							|| tv.getName()
									.equalsIgnoreCase("DestinationPrimaryKey")
							|| tv.getName().equalsIgnoreCase("OriginForeignKey")
							|| tv.getName().equalsIgnoreCase("OriginPrimaryKey")
							|| tv.getName().equalsIgnoreCase("AreaFieldName")
							|| (StringUtils.isNotBlank(tv.getFQName())
									&& tv.getFQName().equalsIgnoreCase(
											"ArcGIS::AttributeIndex::Fields"))) {

						result &= similarAttributesReferencedByTaggedValue(
								tv.getName(), refTVModelPath, tvValues,
								refTvValues);

					} else {

						String tvValuesString = Joiner.on(", ").skipNulls()
								.join(tvValues);
						String refTvValuesString = Joiner.on(", ").skipNulls()
								.join(refTvValues);

						if (!tvValuesString.equals(refTvValuesString)) {

							sj.add(refTVModelPath + " - "
									+ "Different values. EXPECTED '"
									+ refTvValuesString + "' FOUND '"
									+ tvValuesString + "'.");
							result = false;
						}
					}
				}
			}

			// check if any tagged values remain, which would be new
			for (EATaggedValue tv : tvs.values()) {

				sj.add(modelPath + " - Found unexpected tagged value '"
						+ tv.getName() + "', FQName '" + tv.getFQName()
						+ "' and values: "
						+ StringUtils.join(tv.getValues(), ", ") + ".");

				result = false;
			}

			return result;
		}
	}

	private boolean similarAttributesReferencedByTaggedValue(String tvName,
			String tvModelPath, List<String> tvValues,
			List<String> refTvValues) {

		boolean result = true;

		// compare attribute and element name
		for (int i = 0; i < tvValues.size(); i++) {

			String val = tvValues.get(i);
			String refVal = refTvValues.get(i);

			if (StringUtils.isBlank(val) && StringUtils.isBlank(refVal)) {

				// fine - no actual reference set for the tag
				continue;

			} else if (StringUtils.isBlank(val)
					|| StringUtils.isBlank(refVal)) {

				sj.add(tvModelPath + " - " + "Different value for tag '"
						+ tvName + "' at index '" + i + "'. EXPECTED '"
						+ StringUtils.stripToEmpty(refVal) + "' FOUND '"
						+ StringUtils.stripToEmpty(val) + "'.");
				result = false;

			} else {

				Attribute att = rep.GetAttributeByGuid(val);
				Attribute refAtt = refRep.GetAttributeByGuid(refVal);

				Element elmt = rep.GetElementByID(att.GetParentID());
				Element refElmt = refRep.GetElementByID(refAtt.GetParentID());

				String refTvValueString = "attribute '" + refAtt.GetName()
						+ "' of element '" + refElmt.GetName();
				String tvValueString = "attribute '" + att.GetName()
						+ "' of element '" + elmt.GetName();

				if (!tvValueString.equals(refTvValueString)) {
					sj.add(tvModelPath + " - "
							+ "Different referenced attribute for tag '"
							+ tvName + "' (value at index '" + i
							+ "'). EXPECTED '" + refTvValueString + "' FOUND '"
							+ tvValueString + "'.");
					result = false;
				}
			}
		}

		return result;
	}

	private boolean similarReferencedElementName(String modelPath,
			String attributeToCompare, int elmtId, int refElmtId) {

		if (elmtId <= 0 && refElmtId <= 0) {

			return true;

		} else if (elmtId <= 0 && refElmtId > 0) {

			sj.add(modelPath + " - " + "Reference has " + attributeToCompare
					+ ", while compared model element has not.");
			return false;

		} else if (elmtId > 0 && refElmtId <= 0) {

			sj.add(modelPath + " - " + "Compared model element has "
					+ attributeToCompare + ", while reference has not.");
			return false;

		} else {

			// elmtId > 0 && refElmtId > 0

			Element elmt = rep.GetElementByID(elmtId);
			Element refElmt = refRep.GetElementByID(refElmtId);

			String elmtName = elmt.GetName();
			String refElmtName = refElmt.GetName();

			if (!elmtName.equals(refElmtName)) {

				sj.add(modelPath + " - " + "Different " + attributeToCompare
						+ ". EXPECTED element with name '" + refElmtName
						+ "' FOUND element with name '" + elmtName + ".");
				return false;

			} else {

				return true;
			}
		}

	}

	private boolean diffConstraints(String elmtModelPath,
			ListMultimap<String, EAConstraintInfo> consByName,
			ListMultimap<String, EAConstraintInfo> refConsByName) {

		if (consByName.size() > refConsByName.size()) {

			sj.add("Element " + elmtModelPath
					+ " - Reference element has less constraints.");
			return false;

		} else if (consByName.size() < refConsByName.size()) {

			sj.add("Element " + elmtModelPath
					+ " - Reference element has more constraints.");
			return false;

		} else {

			boolean result = true;

			for (String refConName : refConsByName.keySet()) {

				List<EAConstraintInfo> refConsForName = refConsByName
						.get(refConName);
				List<EAConstraintInfo> consForName = consByName
						.removeAll(refConName);

				if (consForName.size() > refConsForName.size()) {

					sj.add("Element " + elmtModelPath
							+ " - Reference element has less constraints with name '"
							+ refConName + "'.");
					result = false;

				} else if (consForName.size() < refConsForName.size()) {

					sj.add("Element " + elmtModelPath
							+ " - Reference element has more constraints with name '"
							+ refConName + "'.");
					result = false;

				} else {

					for (int i = 0; i < refConsForName.size(); i++) {

						String conModelPath = elmtModelPath + ", " + (i + 1)
								+ ". constraint '" + refConName + "'";

						EAConstraintInfo refCon = refConsForName.get(i);
						EAConstraintInfo con = consForName.get(i);

						result &= similar(conModelPath, "Status",
								con.getStatus(), refCon.getStatus());

						result &= similar(conModelPath, "Notes", con.getNotes(),
								refCon.getNotes());

						result &= similar(conModelPath, "Type", con.getType(),
								refCon.getType());
					}
				}
			}

			// check if any constraints remain, which would be new
			for (EAConstraintInfo con : consByName.values()) {
				sj.add("Element " + elmtModelPath
						+ " - Found unexpected constraint with name '"
						+ con.getName() + "', type '" + con.getType()
						+ "', status '" + con.getStatus() + "', and notes '"
						+ con.getNotes() + "'.");
				result = false;
			}

			return result;
		}
	}

	/**
	 * @param cons
	 * @return multimap of constraint infos, with constraint names as sorted
	 *         keys, and constraints stored in an array list
	 */
	private ListMultimap<String, EAConstraintInfo> parseEAConstraints(
			Collection<Constraint> cons) {

		ListMultimap<String, EAConstraintInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < cons.GetCount(); i++) {

			Constraint con = cons.GetAt(i);
			result.put(con.GetName(), new EAConstraintInfo(con.GetName(),
					con.GetType(), con.GetStatus(), con.GetNotes()));
		}

		return result;
	}

	/**
	 * @param cons
	 * @return multimap of constraint infos, with constraint names as sorted
	 *         keys, and constraints stored in an array list
	 */
	private ListMultimap<String, EAConstraintInfo> parseEAAttributeConstraints(
			Collection<AttributeConstraint> cons) {

		ListMultimap<String, EAConstraintInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < cons.GetCount(); i++) {

			AttributeConstraint con = cons.GetAt(i);
			result.put(con.GetName(), new EAConstraintInfo(con.GetName(),
					con.GetType(), "", con.GetNotes()));
		}

		return result;
	}

	/**
	 * @param cons
	 * @return multimap of constraint infos, with constraint names as sorted
	 *         keys, and constraints stored in an array list
	 */
	private ListMultimap<String, EAConstraintInfo> parseEAConnectorConstraints(
			Collection<ConnectorConstraint> cons) {

		ListMultimap<String, EAConstraintInfo> result = MultimapBuilder
				.treeKeys().arrayListValues().build();

		for (short i = 0; i < cons.GetCount(); i++) {

			ConnectorConstraint con = cons.GetAt(i);
			result.put(con.GetName(), new EAConstraintInfo(con.GetName(),
					con.GetType(), "", con.GetNotes()));
		}

		return result;
	}

	private boolean ignoreElementType(String elmtType) {

		return !allowedElementTypes.contains(elmtType);
	}

	private boolean ignoreConnectorType(String connType) {

		return !allowedConnectorTypes.contains(connType);
	}

	private boolean diffConnector(String modelPath, Connector conn,
			Connector refConn) {

		int connId = conn.GetConnectorID();
		int refConnId = refConn.GetConnectorID();

		if (idsOfCheckedConnectors.contains(connId)
				&& refIdsOfCheckedConnectors.contains(refConnId)) {
			// skip the check - it has already been performed
			return true;
		}

		idsOfCheckedConnectors.add(connId);
		refIdsOfCheckedConnectors.add(refConnId);

		boolean result = true;

		ConnectorEnd client = conn.GetClientEnd();
		ConnectorEnd supplier = conn.GetSupplierEnd();

		String connKey = getKey(conn, client, supplier, rep);

		String connModelPath = modelPath + ", connector " + connKey;

		result &= similar(connModelPath, "Alias", conn.GetAlias(),
				refConn.GetAlias());

		Element connAssoc = conn.GetAssociationClass();
		Element refConnAssoc = refConn.GetAssociationClass();
		result &= similar(connModelPath, "has association class",
				connAssoc != null, refConnAssoc != null);
		if (connAssoc != null && refConnAssoc != null) {
			result &= similar(connModelPath, "association class name",
					connAssoc.GetName(), refConnAssoc.GetName());
		}

		// Ignore Color

		ListMultimap<String, EAConstraintInfo> consByName = parseEAConnectorConstraints(
				conn.GetConstraints());
		ListMultimap<String, EAConstraintInfo> refConsByName = parseEAConnectorConstraints(
				refConn.GetConstraints());
		result &= diffConstraints(connModelPath, consByName, refConsByName);

		// Ignore ConveyedItems, CustomProperties, DiagramID

		result &= similar(connModelPath, "Direction", conn.GetDirection(),
				refConn.GetDirection());

		// Ignore EndPointX, EndPointY, EventFlags

		result &= similarIgnoreCase(connModelPath, "FQStereotype",
				conn.GetFQStereotype(), refConn.GetFQStereotype());

		result &= similar(connModelPath, "ForeignKeyInformation",
				conn.GetForeignKeyInformation(),
				refConn.GetForeignKeyInformation());

		result &= similar(connModelPath, "IsLeaf", conn.GetIsLeaf(),
				refConn.GetIsLeaf());

		result &= similar(connModelPath, "IsRoot", conn.GetIsRoot(),
				refConn.GetIsRoot());

		result &= similar(connModelPath, "IsSpec", conn.GetIsSpec(),
				refConn.GetIsSpec());

		// Ignore MessageArguments, MetaType, MisData

		/*
		 * Name may actually be important, when comparing the associations that
		 * belong to association classes, since these connectors are retrieved
		 * by ID, not by name. Their names could thus be different.
		 */
		result &= similar(connModelPath, "Name", conn.GetName(),
				refConn.GetName());

		result &= similar(connModelPath, "Notes", conn.GetNotes(),
				refConn.GetNotes());

		result &= similar(connModelPath, "ObjectType", conn.GetObjectType(),
				refConn.GetObjectType());

		// Ignore Properties, ReturnValueAlias, RouteStyle, SequenceNo,
		// StartPointX, StartPointY, StateFlags, Stereotype (since StereotypeEx
		// contains all applied stereotypes)

		result &= similarIgnoreCase(connModelPath, "StereotypeEx",
				conn.GetStereotypeEx(), refConn.GetStereotypeEx());

		result &= similar(connModelPath, "StyleEx", conn.GetStyleEx(),
				refConn.GetStyleEx());

		result &= similar(connModelPath, "Subtype", conn.GetSubtype(),
				refConn.GetSubtype());

		result &= similar(connModelPath, "Type", conn.GetType(),
				refConn.GetType());

		// tagged values
		result &= similar(connModelPath,
				EAConnectorUtil.getEATaggedValues(conn),
				EAConnectorUtil.getEATaggedValues(refConn));

		// Ignore TemplateBindings, TransitionAction, TransitionEvent,
		// TransitionGuard, VirtualInheritance, Width

		/*
		 * Check that the names of the elements to which the two ends belong are
		 * equal.
		 */
		boolean clientEndsSimilar = similarReferencedElementName(connModelPath,
				"name of element that owns the ClientEnd", conn.GetClientID(),
				refConn.GetClientID());
		result &= clientEndsSimilar;

		boolean supplierEndsSimilar = similarReferencedElementName(
				connModelPath, "name of element that owns the SupplierEnd",
				conn.GetSupplierID(), refConn.GetSupplierID());
		result &= supplierEndsSimilar;

		/*
		 * Only perform a detailed comparison of the connector ends if the names
		 * of elements they belong to are similar. That is typically not the
		 * case if the connector has been modelled starting with a different
		 * element (from element B to A instead of from A to B).
		 */
		if (clientEndsSimilar && supplierEndsSimilar) {
			result &= diffConnectorEnd(connModelPath + ", client (source) end",
					conn.GetClientEnd(), refConn.GetClientEnd());

			result &= diffConnectorEnd(
					connModelPath + ", supplier (target) end",
					conn.GetSupplierEnd(), refConn.GetSupplierEnd());
		}

		return result;
	}

	private boolean diffConnectorEnd(String modelPath, ConnectorEnd connEnd,
			ConnectorEnd refConnEnd) {

		boolean result = true;

		result &= similar(modelPath, "Aggregation", connEnd.GetAggregation(),
				refConnEnd.GetAggregation());

		result &= similar(modelPath, "Alias", connEnd.GetAlias(),
				refConnEnd.GetAlias());

		result &= similar(modelPath, "AllowDuplicates",
				connEnd.GetAllowDuplicates(), refConnEnd.GetAllowDuplicates());

		result &= similar(modelPath, "Cardinality", connEnd.GetCardinality(),
				refConnEnd.GetCardinality());

		result &= similar(modelPath, "Constraint", connEnd.GetConstraint(),
				refConnEnd.GetConstraint());

		result &= similar(modelPath, "Containment", connEnd.GetContainment(),
				refConnEnd.GetContainment());

		result &= similar(modelPath, "Derived", connEnd.GetDerived(),
				refConnEnd.GetDerived());

		result &= similar(modelPath, "DerivedUnion", connEnd.GetDerivedUnion(),
				refConnEnd.GetDerivedUnion());

		result &= similar(modelPath, "End", connEnd.GetEnd(),
				refConnEnd.GetEnd());

		result &= similar(modelPath, "IsChangeable", connEnd.GetIsChangeable(),
				refConnEnd.GetIsChangeable());

		result &= similar(modelPath, "Navigable", connEnd.GetNavigable(),
				refConnEnd.GetNavigable());

		result &= similar(modelPath, "ObjectType", connEnd.GetObjectType(),
				refConnEnd.GetObjectType());

		result &= similar(modelPath, "Ordering", connEnd.GetOrdering(),
				refConnEnd.GetOrdering());

		// Ignore OwnedByClassifier

		result &= similar(modelPath, "Qualifier", connEnd.GetQualifier(),
				refConnEnd.GetQualifier());

		result &= similar(modelPath, "Role", connEnd.GetRole(),
				refConnEnd.GetRole());

		result &= similar(modelPath, "RoleNote", connEnd.GetRoleNote(),
				refConnEnd.GetRoleNote());

		result &= similar(modelPath, "RoleType", connEnd.GetRoleType(),
				refConnEnd.GetRoleType());

		// Ignore Stereotype

		result &= similarIgnoreCase(modelPath, "StereotypeEx",
				connEnd.GetStereotypeEx(), refConnEnd.GetStereotypeEx());

		result &= similar(modelPath, "Visibility", connEnd.GetVisibility(),
				refConnEnd.GetVisibility());

		// tagged values
		result &= similar(modelPath,
				EAConnectorEndUtil.getEATaggedValues(connEnd),
				EAConnectorEndUtil.getEATaggedValues(refConnEnd));

		return result;
	}

	private String getKey(Connector conn, ConnectorEnd client,
			ConnectorEnd supplier, Repository repOfConn) {

		Element clientElmt = repOfConn.GetElementByID(conn.GetClientID());
		Element supplierElmt = repOfConn.GetElementByID(conn.GetSupplierID());

		String connKey = conn.GetType() + " Source/Client '" + client.GetRole()
				+ "' (at element '" + clientElmt.GetName() + "') > '"
				+ conn.GetName() + "' > Target/Supplier '" + supplier.GetRole()
				+ "' (at element '" + supplierElmt.GetName() + "')";

		return connKey;
	}

	private boolean similar(String modelPath, String attributeToCompare,
			boolean attValue, boolean refAttValue) {

		if (attValue != refAttValue) {

			sj.add(modelPath + " - " + "Different " + attributeToCompare
					+ ". EXPECTED '" + refAttValue + "' FOUND '" + attValue
					+ "'.");
			return false;
		} else {
			return true;
		}
	}

	private boolean similar(String modelPath, String attributeToCompare,
			ObjectType attValue, ObjectType refAttValue) {

		if (attValue != refAttValue) {

			sj.add(modelPath + " - " + "Different " + attributeToCompare
					+ ". EXPECTED '" + refAttValue.name() + "' FOUND '"
					+ attValue.name() + "'.");
			return false;
		} else {
			return true;
		}
	}

	private boolean similar(String modelPath, String attributeToCompare,
			String attValue, String refAttValue) {

		if (!attValue.equals(refAttValue)) {

			sj.add(modelPath + " - " + "Different " + attributeToCompare
					+ ". EXPECTED '" + refAttValue + "' FOUND '" + attValue
					+ "'.");
			return false;
		} else {
			return true;
		}
	}

	private boolean similarIgnoreCase(String modelPath,
			String attributeToCompare, String attValue, String refAttValue) {

		if (!attValue.equalsIgnoreCase(refAttValue)) {

			sj.add(modelPath + " - " + "Different " + attributeToCompare
					+ ". EXPECTED '" + refAttValue + "' FOUND '" + attValue
					+ "'. Case is ignored.");
			return false;
		} else {
			return true;
		}
	}

	private boolean similar(String modelPath, String attributeToCompare,
			long attValue, long refAttValue) {

		if (attValue != refAttValue) {

			sj.add(modelPath + " - " + "Different " + attributeToCompare
					+ ". EXPECTED '" + refAttValue + "' FOUND '" + attValue
					+ "'.");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Parses infos from the given EA packages.
	 * 
	 * @param pkgs
	 * @param pkgsById
	 * @return multimap of package infos, with package names as sorted keys, and
	 *         packages stored in an array list
	 */
	private ListMultimap<String, EAPackageInfo> parseEAPackages(
			Collection<Package> pkgs, Map<Integer, EAPackageInfo> pkgsById) {

		ListMultimap<String, EAPackageInfo> result = MultimapBuilder.treeKeys()
				.arrayListValues().build();

		for (short i = 0; i < pkgs.GetCount(); i++) {

			Package pkg = pkgs.GetAt(i);

			int id = pkg.GetPackageID();
			String name = pkg.GetName();
			int parentId = pkg.GetParentID();
			String path;
			if (parentId == 0) {
				path = name;
			} else {
				path = pkgsById.get(parentId).getModelPath() + "::" + name;
			}

			if (result.containsKey(name)) {
				int index = result.get(name).size() + 1;
				path = path + "[" + index + "]";
			}

			EAModelElementInfo pkgElmtInfo = null;
			Element pkgElmt = pkg.GetElement();

			if (pkgElmt != null) {

				String pkgElmtName = pkgElmt.GetName();
				pkgElmtInfo = new EAModelElementInfo(pkgElmtName,
						pkgElmt.GetElementID(), path);
			}

			EAPackageInfo pkgInfo = new EAPackageInfo(name, id, path,
					pkgElmtInfo);

			result.put(pkgInfo.getName(), pkgInfo);

			pkgsById.put(id, pkgInfo);
		}

		return result;
	}

	public String getDiffDetails() {
		return sj.toString();
	}

}
