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

package de.interactive_instruments.ShapeChange.Model;

import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public interface Model {

	public Options options();

	public ShapeChangeResult result();

	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString)
			throws ShapeChangeAbortException;

	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString, String username,
			String password) throws ShapeChangeAbortException;

	public int type();

	/**
	 * Collect and return all PackageInfo objects tagged as being a schema. If a
	 * name is given, only the package with the specified name will be
	 * considered.
	 * 
	 * @param name
	 *            to restrict the search; can be <code>null</code>
	 * 
	 * @return relevant schemas; can be empty but not <code>null</code>
	 */
	public SortedSet<PackageInfo> schemas(String name);

	/**
	 * Return all schemas that are selected using the relevant parameters:
	 * appSchemaName, appSchemaNameRegex, appSchemaNamespaceRegex
	 * 
	 * @see PackageInfo#isSchema()
	 */
	public SortedSet<? extends PackageInfo> selectedSchemas();

	/**
	 * Return all ClassInfo objects contained in the given package and in sub-
	 * packages, which belong to the same targetNamespace as the given package.
	 * 
	 * @return set of classes contained in the given package and in sub-packages
	 *         of the same targetNamespace; can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<ClassInfo> classes(PackageInfo pi);

	/**
	 * Tagged values normalization. This returns the tag given or a
	 * de-deprecated tag or null.
	 * 
	 * @param tag
	 * @return
	 */
	public String normalizeTaggedValue(String tag);

	public void postprocessAfterLoadingAndValidate();

	public PackageInfo packageById(String id);

	public ClassInfo classById(String id);

	public ClassInfo classByName(String nam);

	public String characterEncoding();

	public void shutdown();

	/**
	 * 
	 * @return all {@link PackageInfo} objects contained in the model; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<PackageInfo> packages();

	/**
	 * @param schema
	 * @return all packages in the model that have the same targetNamespace as
	 *         the given package. Can be empty (if the given package does not
	 *         have a targetNamespace) but not <code>null</code>.
	 */
	public SortedSet<PackageInfo> packages(PackageInfo pkg);

	/**
	 * Load additional model information from external sources, such as
	 * constraints.
	 * 
	 * NOTE: this assumes that relevant information can be altered in the model,
	 * which works in case of direct pointers to objects and collections used in
	 * classes (which can then be set or modified), does not work for primitive
	 * types (because there are no set methods in the model interfaces) and
	 * collections that are copies of the ones used in model classes. If this is
	 * not sufficient, either change the model interfaces or create a
	 * transformation that can perform the necessary model modifications.
	 */
	public void loadInformationFromExternalSources();

	/**
	 * @param ci
	 *            Class to check
	 * @return <code>true</code> if the given ClassInfo belongs to one of the
	 *         selected schemas, otherwise <code>false</code>
	 */
	public boolean isInSelectedSchemas(ClassInfo ci);

	/**
	 * Identifies the nearest schema or application schema package in which the
	 * class is a child. A schema is identified using
	 * {@link PackageInfo#isSchema()}, while an application schema is identified
	 * using {@link PackageInfo#isAppSchema()}.
	 * 
	 * @param ci
	 *            Class to check
	 * @return The package that represents the schema or application schema of
	 *         the class. If the class is not a child of such a package, then
	 *         the result is <code>null</code> .
	 */
	public PackageInfo schemaPackage(ClassInfo ci);

	/**
	 * @return all packages that represent schemas selected for processing, or
	 *         are subpackages of these schemas and in the same target
	 *         namespace; can be empty but not <code>null</code>
	 */
	public SortedSet<PackageInfo> allPackagesFromSelectedSchemas();
}
