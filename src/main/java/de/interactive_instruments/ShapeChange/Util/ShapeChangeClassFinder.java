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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

/**
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ShapeChangeClassFinder {

    /**
     * @param interfaceName the fully qualified name of a ShapeChange interface
     * @return A list of all (non-abstract) classes that implement the named
     *         interface; can be empty but not <code>null</code>.
     */
    public static List<Class<?>> findClassesImplementing(String interfaceName) {

	String[] packageBlacklist = null;
	InputStream stream = ShapeChangeClassFinder.class.getResourceAsStream("/sc.properties");
	if (stream != null) {
	    Properties properties = new Properties();
	    try {
		properties.load(stream);
		packageBlacklist = properties.getProperty("sc.targetScanBlacklistPackages").split(",");
	    } catch (IOException e) {
		System.err.println(
			"Could not load sc.properties file and property sc.targetScanBlacklistPackages. Reverting to default package blacklist.");
		packageBlacklist = new String[] { "java.*", "antlr*", "com.fasterxml.*", "com.github.andrewoma.*",
			"com.github.jsonldjava.*", "com.google.*", "com.graphbuilder.*", "com.ibm.*", "com.microsoft.*",
			"com.sun.*", "com.thedeanda*", "com.topologi.*", "io.github.classgraph*", "java_cup*",
			"javax.*", "junit.*", "name.fraser*", "net.arnx*", "net.engio*", "net.sf.saxon*",
			"nonapi.io.github.classgraph.*", "org.antlr.*", "org.apache.*", "org.checkerframework.*",
			"org.codehaus.jackson*", "org.codehaus.mojo*", "org.custommonkey*", "org.docx4j*",
			"org.eclipse.*", "org.etsi.uri.*", "org.glox4j.*", "org.hamcrest*", "org.jgrapht*",
			"org.jheaps*", "org.json*", "org.junit*", "org.jvunit*", "org.jvnet", "org.merlin*",
			"org.opendope.*", "org.openxmlformats.*", "org.plutext*", "org.pptx4j*", "org.slf4j*",
			"org.sparx*", "org.w3*", "org.xlsx4j.*", "org.xml*", "schemaorg_apache_xmlbeans.*" };

	    }

	}

	long scanStart = System.currentTimeMillis();

	ScanResult scanResult = null;

	try {

	    scanResult = new ClassGraph().enableAllInfo().blacklistPackages(packageBlacklist).scan();

	    /*
	     * IMPORTANT: Keep the scanTime and following commented code for debugging
	     * purposes!
	     */
	    @SuppressWarnings("unused")
	    long scanTime = (System.currentTimeMillis() - scanStart) / 1000;

//	    System.out.println(
//			"Determination of implementations (s): " + scanTime);
//	    PackageInfoList packages = scanResult.getPackageInfo();
//	    List<String> packageNames = packages.getNames();
//	    packageNames.sort(Comparator.naturalOrder());
//	    System.out.println("Found " + packageNames.size() + " packages: " + String.join("\n", packageNames));

	    ClassInfoList targetClassInfos = scanResult.getClassesImplementing(interfaceName)
		    .filter(classInfo -> (!(classInfo.isInterface() || classInfo.isAbstract())));
	    
	    return targetClassInfos.loadClasses();

	} finally {
	    if (scanResult != null) {
		scanResult.close();
	    }
	}
    }
}
