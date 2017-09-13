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

package de.interactive_instruments.ShapeChange.Model;

import java.util.List;
import java.util.SortedSet;

public interface PackageInfo extends Info {

	/**
	 * Determine the targetNamespace of the GML applications schema to be
	 * generated. The item is used from the configuration or - if not present
	 * there - from the tagged value either on this package or one of its
	 * ancestors.
	 * 
	 * @return the target namespace of the package; can be <code>null</code> if
	 *         no target namespace is defined for the package or one of its
	 *         ancestors
	 */
	public String targetNamespace();

	/**
	 * Determine the namespace abbreviation of the GML applications schema to be
	 * generated. The item is used from the configuration or - if not present
	 * there - from the tagged value either on this package or one of its
	 * ancestors.
	 * 
	 * @return namespace abbreviation defined for the package (or one of its
	 *         ancestors); can be <code>null</code> if no such abbreviation is
	 *         defined
	 */
	public String xmlns();

	/**
	 * Determine the file name of the xsd document to be generated from the
	 * package. This is either configured explicitly for the package or is
	 * otherwise obtained from the tagged value 'xsdDocument' on the package. If
	 * none of these are found and if the package is an application schema
	 * package, the file name is derived from the package name.
	 */
	public String xsdDocument();

	/**
	 * @return the value of the tag "gmlProfileSchema", or <code>null</code> in
	 *         case such a tag does not exist on the package.
	 */
	public String gmlProfileSchema();

	/**
	 * Determine the version attribute to be applied to the application schema.
	 * It is taken either from the configuration (more specifically: a
	 * PackageInfo element) or from a tagged value on this package or any of its
	 * ancestors (even outside the schema).
	 * 
	 * @return the version of the package, or <code>null</code> if no version
	 *         information is available
	 */
	public String version();

	public PackageInfo owner();

	public String schemaId();

	/**
	 * @return the package that represents a schema (isSchema() returns true),
	 *         search begins with this package and continues with its ancestors
	 *         (owners); <code>null</code> if no such package exists.
	 */
	public PackageInfo rootPackage();

	/**
	 * Determine whether the package represents an 'application schema'. The
	 * package is regarded an 'application schema', if it carries a stereotype
	 * with normalized name "application schema".
	 */
	public boolean isAppSchema();

	/**
	 * Determine whether the package represents a schema. A package is assumed
	 * to represent a schema, if it contains a tagged value defining a
	 * "targetNamespace" (NOTE: there is a subtle difference to the package
	 * 'having' a targetNamespace, because the {@link #targetNamespace()} method
	 * may retrieve the targetNamespace of a package from one of its ancestors).
	 * It is also regarded a schema, if the package is named in a PackageInfo
	 * entry of the Configuration document.
	 */
	public boolean isSchema();

	/**
	 * @return a set of directly contained (child) packages (shallow copy, NOT
	 *         deep copy). One or more of these packages may belong to a
	 *         different schema / targetNamespace.
	 */
	public SortedSet<PackageInfo> containedPackages();

	/**
	 * @return the set of classes that are directly contained in this package
	 *         (thus excluding classes from subpackages); can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<ClassInfo> containedClasses();

	/**
	 * @return the set of ids of the packages on which this package depends; may
	 *         be empty but not <code>null</code>
	 */
	public SortedSet<String> supplierIds();

	/**
	 * @return metadata about the diagrams relevant for this package,
	 *         <code>null</code> if no diagrams are available
	 */
	public List<ImageMetadata> getDiagrams();

	/**
	 * @param diagrams
	 *            metadata about the diagrams relevant for this class
	 */
	public void setDiagrams(List<ImageMetadata> diagrams);

	/**
	 * @return set of child packages (direct and indirect) that are in the same
	 *         target namespace as this package; can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<PackageInfo> containedPackagesInSameTargetNamespace();
}
