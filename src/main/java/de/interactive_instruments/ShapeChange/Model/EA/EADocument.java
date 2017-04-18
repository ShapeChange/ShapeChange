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

package de.interactive_instruments.ShapeChange.Model.EA;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ModelImpl;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.sparx.*;

public class EADocument extends ModelImpl implements Model {

	public static final int STATUS_EADOCUMENT_INITSTART = 101;
	public static final int STATUS_EADOCUMENT_READMODEL = 102;
	public static final int STATUS_EADOCUMENT_ESTABLISHCLASSES = 103;
	public static final int STATUS_EADOCUMENT_READCONSTARINTS = 104;

	public Options options = null;
	public ShapeChangeResult result = null;

	/** EA repository object */
	protected Repository repository = null;

	/** Character endcoding */
	protected final String characterEncoding = "Windows-1252";

	/** Caches for all classes and packages */
	// Id to classes ...
	SortedMap<String, ClassInfoEA> fClassById = new TreeMap<String, ClassInfoEA>();
	// Name to classes ...
	SortedMap<String, ClassInfoEA> fClassByName = new TreeMap<String, ClassInfoEA>();
	// Id to packages ...
	SortedMap<String, PackageInfoEA> fPackageById = new TreeMap<String, PackageInfoEA>();
	// ElmtId to packages ...
	SortedMap<String, PackageInfoEA> fPackageByElmtId = new TreeMap<String, PackageInfoEA>();
	// AssociationId to associations ...
	SortedMap<String, AssociationInfoEA> fAssociationById = new TreeMap<String, AssociationInfoEA>();

	protected Set<String> excludedPackageNames = null;

	public EADocument() {
		super();
	}

	public EADocument(ShapeChangeResult r, Options o, String repositoryFileName)
			throws ShapeChangeAbortException {
		super();
		initialise(r, o, repositoryFileName);
	}

	/** Return options and configuration object. */
	public Options options() {
		return options;
	} // options()

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return result;
	} // result()

	/** Connect to EA Repository with security information */
	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString, String username,
			String password) throws ShapeChangeAbortException {

		options = o;
		result = r;

		StatusBoard.getStatusBoard().statusChanged(STATUS_EADOCUMENT_INITSTART);

		/** Connect to EA repository */
		repository = new Repository();

		if (!repository.OpenFile2(repositoryFileNameOrConnectionString,
				username, password)) {
			String errormsg = repository.GetLastError();
			r.addFatalError(null, 35, errormsg,
					repositoryFileNameOrConnectionString, username, password);
			throw new ShapeChangeAbortException();
		}

		executeCommonInitializationProcedure();
	}

	/** Connect to EA Repository without security information */
	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString)
			throws ShapeChangeAbortException {

		options = o;
		result = r;

		StatusBoard.getStatusBoard().statusChanged(STATUS_EADOCUMENT_INITSTART);

		String connectionString;

		/**
		 * Determine if we are dealing with a file or server based repository
		 */

		if (repositoryFileNameOrConnectionString.contains("DBType=")
				|| repositoryFileNameOrConnectionString
						.contains("Connect=Cloud")) {

			/* We are dealing with a server based repository. */

			connectionString = repositoryFileNameOrConnectionString;

		} else {

			/* We have an EAP file. Ensure that it exists */

			java.io.File repfile = new java.io.File(
					repositoryFileNameOrConnectionString);
			boolean ex = true;
			if (!repfile.exists()) {
				ex = false;
				if (!repositoryFileNameOrConnectionString.toLowerCase()
						.endsWith(".eap")) {
					repositoryFileNameOrConnectionString += ".eap";
					repfile = new java.io.File(
							repositoryFileNameOrConnectionString);
					ex = repfile.exists();
				}
			}
			if (!ex) {
				r.addFatalError(null, 31, repositoryFileNameOrConnectionString);
				throw new ShapeChangeAbortException();
			}

			connectionString = repfile.getAbsolutePath();
		}

		/** Connect to EA Repository */
		repository = new Repository();

		if (!repository.OpenFile(connectionString)) {
			String errormsg = repository.GetLastError();
			r.addFatalError(null, 30, errormsg, connectionString);
			throw new ShapeChangeAbortException();
		}

		executeCommonInitializationProcedure();
	}

	public void executeCommonInitializationProcedure()
			throws ShapeChangeAbortException {

		// determine if specific packages should not be loaded
		this.excludedPackageNames = options.getExcludedPackages();

		/** Cache classes and packages */
		// First set up initial evaluation tasks of packages consisting
		// of the models in the repository
		class EvalTask {
			PackageInfoEA fatherPI;
			org.sparx.Package eaPackage;

			EvalTask(PackageInfoEA fpi, org.sparx.Package p) {
				fatherPI = fpi;
				eaPackage = p;
			}
		}

		StatusBoard.getStatusBoard().statusChanged(STATUS_EADOCUMENT_READMODEL);

		LinkedList<EvalTask> evalp = new LinkedList<EvalTask>();
		Collection<org.sparx.Package> model = repository.GetModels();
		for (org.sparx.Package p : model) {

			// Check if this model and all its contents shall be excluded
			String name = p.GetName();
			if (excludedPackageNames != null
					&& excludedPackageNames.contains(name)) {
				// stop processing this model and continue with the next
				continue;
			}

			evalp.addLast(new EvalTask(null, p));
		}

		// Now remove tasks from the list, adding further tasks as we proceed
		// until we have no more tasks to evaluate
		while (evalp.size() > 0) {
			// Remove next evaluation task
			EvalTask et = evalp.removeFirst();
			org.sparx.Package pack = et.eaPackage;
			PackageInfoEA fpi = et.fatherPI;

			// Check if this package and all its contents shall be excluded from
			// the model
			String name = pack.GetName();
			if (excludedPackageNames != null
					&& excludedPackageNames.contains(name)) {
				// stop processing this package and continue with the next
				continue;
			}

			// Add to package cache. The PackageInfo Ctor does the necessary
			// parent/child linkage of packages
			Element packelmt = pack.GetElement();
			PackageInfoEA pi = new PackageInfoEA(this, fpi, pack, packelmt);
			fPackageById.put(pi.id(), pi);
			if (packelmt != null)
				this.fPackageByElmtId.put(
						new Integer(packelmt.GetElementID()).toString(), pi);

			// Now pick all classes and add these to their to caches.
			for (org.sparx.Element elmt : pack.GetElements()) {

				String type = elmt.GetType();

				if (!type.equals("DataType") && !type.equals("Class")
						&& !type.equals("Interface")
						&& !type.equals("Enumeration")) {
					continue;
				}

				ClassInfoEA ci = new ClassInfoEA(this, pi, elmt);

				/*
				 * prevent loading of classes that have tagged value 'status'
				 * with prohibited value
				 */
				String ciname = ci.name();
				String statusTaggedValue = ci.taggedValue("status");
				if (statusTaggedValue != null
						&& options().prohibitedStatusValuesWhenLoadingClasses()
								.contains(statusTaggedValue)) {
					continue;
				}

				fClassById.put(ci.id(), ci);
				/*
				 * TODO What's happening to identical class names? How is this
				 * supposed to be handled? Open issue. While classifier names
				 * have to be unique per app schema only, it is a legacy from
				 * Rational Rose that it is expected that classifier names are
				 * unique in the whole model. The correct solution would be to
				 * add namespace qualifiers.
				 */
				fClassByName.put(ci.name(), ci);
			}
			// Add next level packages for further evaluation
			for (org.sparx.Package pnxt : pack.GetPackages()) {
				evalp.addLast(new EvalTask(pi, pnxt));
			}
		}

		StatusBoard.getStatusBoard()
				.statusChanged(STATUS_EADOCUMENT_ESTABLISHCLASSES);

		/**
		 * Now that all classes are collected, in a second go establish class
		 * derivation hierarchy and all other associations between classes.
		 */
		for (ClassInfoEA ci : fClassById.values()) {

			// Generalization - class derivation hierarchy
			ci.establishClassDerivationHierarchy();
			// Other associations where the class is source or target
			ci.establishAssociations();
		}

		String checkingConstraints = options.parameter("checkingConstraints");
		if (checkingConstraints == null || !checkingConstraints.toLowerCase()
				.trim().equals("disabled")) {
			StatusBoard.getStatusBoard()
					.statusChanged(STATUS_EADOCUMENT_READCONSTARINTS);

			// TODO The following may be removed when constraints have been
			// tested.
			/** In a third go collect all constraints */
			for (ClassInfoEA ci : fClassById.values()) {
				ci.constraints();
				SortedMap<StructuredNumber, PropertyInfo> props = ci
						.properties();
				for (PropertyInfo pi : props.values())
					pi.constraints();
			}
		}

		/**
		 * Loop over all schemas (i.e packages with a target namespace) and
		 * store the schema location, so that it can be added in import
		 * statements
		 */
		SortedSet<PackageInfo> schemas = schemas("");
		for (Iterator<PackageInfo> i = schemas.iterator(); i.hasNext();) {
			PackageInfo pi = i.next();
			options.addSchemaLocation(pi.targetNamespace(), pi.xsdDocument());
		}

		// ==============================
		// load diagrams if so requested
		String loadDiagrams = options.parameter("loadDiagrams");

		if (loadDiagrams != null && loadDiagrams.equalsIgnoreCase("true")) {

			java.io.File tmpDir = options.imageTmpDir();

			if (tmpDir.exists()) {

				// probably content from previous run, delete the content of the
				// directory
				try {
					FileUtils.deleteDirectory(tmpDir);
				} catch (IOException e) {
					result.addWarning(null, 34, tmpDir.getAbsolutePath());
				}

				if (!tmpDir.exists()) {
					try {
						FileUtils.forceMkdir(tmpDir);
					} catch (IOException e) {
						result.addWarning(null, 32, tmpDir.getAbsolutePath());
					}
				}
			}

			AtomicInteger imgIdCounter = new AtomicInteger(0);

			SortedSet<? extends PackageInfo> selectedSchema = this
					.selectedSchemas();

			for (PackageInfo pi : selectedSchema) {

				if (pi == null) {
					continue;
				}

				// Only process schemas in a namespace and name that matches a
				// user-selected pattern
				if (options.skipSchema(null, pi))
					continue;

				saveDiagrams(imgIdCounter, "img", tmpDir,
						escapeFileName(tmpDir.getName()), pi);
			}
		}

	} // EA Document Ctor

	/**
	 * Replace all spaces with underscores and removes non-ASCII characters and
	 * removes ASCII characters that are not printable.
	 *
	 * @param filename
	 * @return
	 */
	private String escapeFileName(String filename) {

		if (filename == null) {

			return null;

		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < filename.length(); i++) {
				char aChar = filename.charAt(i);
				if (aChar == ' ') {
					sb.append('_');
				} else if (CharUtils.isAsciiPrintable(aChar)) {
					sb.append(aChar);
				} else {
					// ignore character if not ASCII printable
				}
			}
			return sb.toString();
		}
	}

	private void saveDiagrams(AtomicInteger imgIdCounter, String imgIdPrefix,
			java.io.File targetFolder, String relPathWithTargetFolder,
			PackageInfo pi) {

		if (!targetFolder.exists()) {
			targetFolder.mkdir();
		}

		java.io.File pi_folder = new java.io.File(targetFolder,
				escapeFileName(pi.name()));

		if (!pi_folder.mkdir()) {
			result.addWarning(null, 32, pi_folder.getAbsolutePath());
		}

		String newRelPathWithTargetFolder = relPathWithTargetFolder + "/"
				+ escapeFileName(pi.name());

		Project projectInterface = repository.GetProjectInterface();

		PackageInfoEA piEa = fPackageById.get(pi.id());

		List<Diagram> diagramList = getDiagramsOfPackage(piEa);

		String packageDiagramRegex = options.parameter("packageDiagramRegex");
		if (packageDiagramRegex == null) {
			packageDiagramRegex = Options.IMAGE_INCLUSION_PACKAGE_REGEX;
		}
		String classDiagramRegex = options.parameter("classDiagramRegex");
		if (classDiagramRegex == null) {
			classDiagramRegex = Options.IMAGE_INCLUSION_CLASS_REGEX;
		}

		String elementNameKeyForMatching = Options.ELEMENT_NAME_KEY_FOR_DIAGRAM_MATCHING;
		String regexForModelElement;

		for (Diagram d : diagramList) {

			String imgId = imgIdPrefix + imgIdCounter.incrementAndGet();
			String imgFileName = escapeFileName(imgId + ".jpg");
			String imgName = d.GetName();

			java.io.File img = new java.io.File(pi_folder, imgFileName);

			String relPathToFile = newRelPathWithTargetFolder + "/"
					+ imgFileName;

			String type = d.GetType();

			/*
			 * before saving the diagram, ensure that it is relevant for at
			 * least one model element
			 */
			boolean relevantDiagram = false;

			if (type.equalsIgnoreCase("Package")) {

				regexForModelElement = packageDiagramRegex
						.replaceAll(elementNameKeyForMatching, pi.name());
				if (imgName.matches(regexForModelElement)) {
					relevantDiagram = true;
				}

			} else if (type.equalsIgnoreCase("Logical")) {

				regexForModelElement = packageDiagramRegex
						.replaceAll(elementNameKeyForMatching, pi.name());
				if (imgName.matches(regexForModelElement)) {
					relevantDiagram = true;
				}

				SortedSet<ClassInfo> clTmp = this.classes(pi);

				if (clTmp == null || clTmp.isEmpty()) {

					// no classes in package, thus the logical diagram cannot be
					// relevant

				} else if (relevantDiagram) {
					// we have already established that this is a relevant
					// diagram

				} else {

					for (ClassInfo ci : clTmp) {

						// only process classes from this package
						if (ci.pkg() == pi) {

							regexForModelElement = classDiagramRegex.replaceAll(
									elementNameKeyForMatching, ci.name());
							if (imgName.matches(regexForModelElement)) {
								relevantDiagram = true;
								// we established that this is a relevant
								// diagram
								break;
							}
						}
					}
				}
			} else {
				// unsupported diagram type -> irrelevant
			}

			if (!relevantDiagram) {
				continue;
			}

			repository.OpenDiagram(d.GetDiagramID());
			projectInterface.SaveDiagramImageToFile(img.getAbsolutePath());
			repository.CloseDiagram(d.GetDiagramID());

			BufferedImage bimg;
			int width = 400;
			int height = 400;

			try {

				bimg = ImageIO.read(img);
				width = bimg.getWidth();
				height = bimg.getHeight();

			} catch (IOException e) {
				result.addError(null, 33, imgName, pi.name());
				e.printStackTrace(System.err);
				continue;
			}

			ImageMetadata imgMeta = new ImageMetadata(imgId, imgName, img,
					relPathToFile, width, height);

			if (type.equalsIgnoreCase("Package")) {

				// we already checked that the diagram is relevant for this
				// package
				addDiagramToPackage(pi, imgMeta);

			} else if (type.equalsIgnoreCase("Logical")) {

				regexForModelElement = packageDiagramRegex
						.replaceAll(elementNameKeyForMatching, pi.name());
				if (imgName.matches(regexForModelElement)) {
					addDiagramToPackage(pi, imgMeta);
				}

				SortedSet<ClassInfo> clTmp = this.classes(pi);

				if (clTmp == null || clTmp.isEmpty()) {

					continue;

				} else {

					for (ClassInfo ci : clTmp) {

						// only process classes from this package
						if (ci.pkg() == pi) {

							regexForModelElement = classDiagramRegex.replaceAll(
									elementNameKeyForMatching, ci.name());
							if (imgName.matches(regexForModelElement)) {
								addDiagramToClass(ci, imgMeta);
							}

						}
					}
				}
			} else {
				// unsupported diagram type - irrelevant
			}
		}

		SortedSet<PackageInfo> children = pi.containedPackages();

		if (children != null) {

			for (PackageInfo piChild : children) {

				if (piChild.targetNamespace().equals(pi.targetNamespace())) {
					saveDiagrams(imgIdCounter, imgIdPrefix, pi_folder,
							newRelPathWithTargetFolder, piChild);
				}
			}
		}
	}

	/**
	 * @return list of diagrams of the given package. The list is sorted by name
	 *         if parameter sortDiagramsByName is set to true (default), or by
	 *         the order in the EA model if set to false.
	 */
	private List<Diagram> getDiagramsOfPackage(PackageInfoEA piEa) {
		// note that this is an org.sparx.Collection
		Collection<Diagram> diagrams = piEa.eaPackage.GetDiagrams();

		List<Diagram> diagramList = new ArrayList<Diagram>();
		for (Diagram d : diagrams) {
			diagramList.add(d);
		}

		boolean sortDiagramsByName;
		String paramSortDiagramsByName = options
				.parameter("sortDiagramsByName");
		if (paramSortDiagramsByName == null) {
			sortDiagramsByName = true; // default
		} else {
			sortDiagramsByName = Boolean.parseBoolean(paramSortDiagramsByName);
		}
		if (sortDiagramsByName) {
			Collections.sort(diagramList, new Comparator<Diagram>() {

				@Override
				public int compare(Diagram o1, Diagram o2) {
					return o1.GetName().compareTo(o2.GetName());
				}

			});
		}
		return diagramList;
	}

	private void addDiagramToClass(ClassInfo ci, ImageMetadata imgMeta) {

		List<ImageMetadata> ciDiagrams = ci.getDiagrams();

		if (ciDiagrams == null) {
			ciDiagrams = new ArrayList<ImageMetadata>();
		}

		ciDiagrams.add(imgMeta);

		ci.setDiagrams(ciDiagrams);
	}

	private void addDiagramToPackage(PackageInfo pi, ImageMetadata imgMeta) {

		List<ImageMetadata> piDiagrams = pi.getDiagrams();

		if (piDiagrams == null) {
			piDiagrams = new ArrayList<ImageMetadata>();
		}

		piDiagrams.add(imgMeta);

		pi.setDiagrams(piDiagrams);
	}

	/** Return character encoding of repository (Windows-1252) */
	public String characterEncoding() {
		return characterEncoding;
	} // charEncoding()

	/** Return ClassInfo object given the id of a class */
	public ClassInfo classById(String id) {
		return fClassById.get(id);
	} // classById()

	/** Return ClassInfo object given the name of the class */
	// TODO To be clarified: How are classes treated, which have identical names
	// but reside in different packages? See above.
	public ClassInfo classByName(String nam) {
		return fClassByName.get(nam);
	} // classByName()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Model#classes(de.interactive_instruments.ShapeChange.Model.PackageInfo)
	 */
	public SortedSet<ClassInfo> classes(PackageInfo pi) {
		// To hold the result ...
		SortedSet<ClassInfo> res = new TreeSet<ClassInfo>();
		// Get targetNamespace. Needed to find out, when we enter other app
		// schemas.
		String tns = pi.targetNamespace();

		return addClasses((PackageInfoEA) pi, tns, res);
	} // classes()

	/**
	 * Return all ClassInfo objects contained in the given package and in sub-
	 * packages, which do not belong to an app schema different to the one of
	 * the given package.
	 */
	// 2014-04-03: clarify javadoc
	private SortedSet<ClassInfo> addClasses(PackageInfoEA pi, String tns,
			SortedSet<ClassInfo> res) {
		// Are we a different app schema? If so, skip
		String ctns = pi.targetNamespace();
		if (ctns != null && (tns == null || !tns.equals(ctns)))
			return res;
		// Same app schema, first add classes to output ...
		res.addAll(pi.childCI);
		// .. then descend to next packages
		for (PackageInfoEA cpi : pi.childPI) {
			res = addClasses(cpi, tns, res);
		}
		return res;
	} // addClasses()

	/** Return PackageInfo object given the id of a package */
	public PackageInfo packageById(String id) {
		return fPackageById.get(id);
	} // packageById()
	
	/** Return the model input type */
	public int type() {
		return Options.EA7;
	} // type()

	/** Shutdown EA model and quit EA */
	public void shutdown() {
		if (repository != null) {
			repository.CloseFile();
			repository.Exit();
			repository = null;
		}
	}

	/**
	 * This auxiliary static method replaces some SGML entities which EA7.5
	 * substitutes for a few symbols in some string fields, but does not bother
	 * to translate back in its automation interface.
	 */
	public static String removeSpuriousEA75EntitiesFromStrings(String s) {
		// Straight return if there is nothing to do
		if (!s.contains("&"))
			return s;
		// Replacement of spurious entities
		return s.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
				.replaceAll("&amp;", "&").replaceAll("&euro;", "€");
	}

	/**
	 * Return repository object (for applications using only the EA model
	 * option)
	 */
	public Repository repository() {
		return repository;
	}

	@Override
	public SortedSet<PackageInfo> packages() {
		SortedSet<PackageInfo> allPackages = new TreeSet<PackageInfo>();
		for (PackageInfo pi : fPackageById.values()) {
			allPackages.add(pi);
		}
		return allPackages;
	}

}
