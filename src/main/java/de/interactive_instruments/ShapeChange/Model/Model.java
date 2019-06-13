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
import de.interactive_instruments.ShapeChange.Type;

public interface Model {

	public Options options();

	public ShapeChangeResult result();

	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString)
			throws ShapeChangeAbortException;

	public void initialise(ShapeChangeResult r, Options o,
			String repositoryFileNameOrConnectionString, String username,
			String password) throws ShapeChangeAbortException;

	/**
	 * Collect and return all PackageInfo objects tagged as being a schema. If a
	 * name is given, only the package with the specified name will be
	 * considered.
	 * 
	 * @param name
	 *                 to restrict the search; can be <code>null</code>
	 * 
	 * @return relevant schemas; can be empty but not <code>null</code>
	 */
	public SortedSet<PackageInfo> schemas(String name);

	/**
	 * Return all schemas that are selected using the relevant parameters:
	 * appSchemaName, appSchemaNameRegex, appSchemaNamespaceRegex
	 * <p>
	 * NOTE: Transformations may change the set of selected schemas
	 * 
	 * @see PackageInfo#isSchema()
	 */
	public SortedSet<? extends PackageInfo> selectedSchemas();

	/**
	 * @return a set with all classes that belong to selected schemas; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<? extends ClassInfo> selectedSchemaClasses();

	/**
	 * @return a set with the navigable properties of all classes that belong to
	 *         selected schemas; can be empty but not <code>null</code>.
	 */
	public SortedSet<? extends PropertyInfo> selectedSchemaProperties();

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
	 * Execute postprocessing and validation checks after the model has been
	 * loaded.
	 */
	public void postprocessAfterLoadingAndValidate();

	/**
	 * @return the PackageInfo object with the given id, or <code>null</code> if
	 *         such a class was not found
	 */
	public PackageInfo packageById(String id);

	/**
	 * @return the ClassInfo object with the given id, or <code>null</code> if
	 *         such a class was not found
	 */
	public ClassInfo classById(String id);

	/**
	 * @return the ClassInfo object with the given name, or <code>null</code> if
	 *         such a class was not found
	 */
	public ClassInfo classByName(String nam);

	/**
	 * @return the ClassInfo object with the id from the given type info, or
	 *         name (if lookup by id was not successful), or <code>null</code>
	 *         if such a class was not found
	 */
	public ClassInfo classByIdOrName(Type typeInfo);

	public String characterEncoding();

	public void shutdown();

	/**
	 * 
	 * @return all {@link PackageInfo} objects contained in the model; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<PackageInfo> packages();

	/**
	 * 
	 * @return all {@link ClassInfo} objects contained in the model; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<ClassInfo> classes();

	/**
	 * 
	 * @return all {@link PropertyInfo} objects contained in the model; can be
	 *         empty but not <code>null</code>.
	 */
	public SortedSet<PropertyInfo> properties();

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
	 * 
	 * @param isLoadingInputModel
	 *                                <code>true</code> If the method is called
	 *                                during the input loading phase,
	 *                                <code>false</code> if it is called while
	 *                                executing a transformer or target.
	 */
	public void loadInformationFromExternalSources(boolean isLoadingInputModel);

	/**
	 * @param ci
	 *               Class to check
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
	 *               Class to check
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

	/**
	 * @param fullNameInSchema
	 * @return the property that has the fully qualified name (omitting packages
	 *         that are outside of the schema the property belongs to); can be
	 *         <code>null</code> if no such property was found
	 */
	public PropertyInfo propertyByFullNameInSchema(String fullNameInSchema);

	/**
	 * Create a {@link Type} that can be used as value type of a
	 * {@link PropertyInfo}.
	 * 
	 * @param typeName
	 *                     Name of a class
	 * @return a {@link Type} with the given name, and id as defined for a class
	 *         with that name, if it exists in the model, otherwise the id is
	 *         set to 'UNKNOWN'
	 */
	public Type typeByName(String typeName);

	/**
	 * Provides the source for a given descriptor. Sources are typically
	 * configured in the ShapeChange configuration, and should thus be checked
	 * first. If no explicit configuration for the source is available, model
	 * type specific sources may be used as defaults (e.g. ea:alias for the
	 * alias within an EA model). tag#{descriptor} can be used as ultimate
	 * fallback. In some situations, even a case based source resolution may be
	 * necessary.
	 * 
	 * @param descriptor
	 *                       the descriptor for which to identify the source
	 * @return the source identifier (e.g. sc:extract#PROLOG, ea:alias,
	 *         ea:notes, tag#{descriptor}, sc:internal, none,
	 *         tag#globalIdentifier).
	 */
	public String descriptorSource(Descriptor descriptor);

}
