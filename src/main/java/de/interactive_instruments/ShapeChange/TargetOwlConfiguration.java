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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ontology.RdfGeneralProperty;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class TargetOwlConfiguration extends TargetConfiguration {

	/**
	 * map with key: typeName#schemaName (the schemaName can be empty if it is
	 * undefined); value: the RdfTypeMapEntry
	 */
	private Map<String, RdfTypeMapEntry> rdfTypeMapEntries = new HashMap<String, RdfTypeMapEntry>();
	private Map<String, RdfPropertyMapEntry> rdfPropertyMapEntries = new HashMap<String, RdfPropertyMapEntry>();
	private SortedMap<String, List<StereotypeConversionParameter>> stereotypeConversionParameters;
	private Map<String, TypeConversionParameter> typeConversionParameters = new HashMap<String, TypeConversionParameter>();
	private Map<String, PropertyConversionParameter> propertyConversionParameters = new HashMap<String, PropertyConversionParameter>();
	private List<DescriptorTarget> descriptorTargets;
	private Map<ConstraintMapping.ConstraintType, ConstraintMapping> constraintMappings;
	private List<RdfGeneralProperty> generalProperties;

	/**
	 * Creates a TargetOwlConfiguration.
	 * 
	 * @param className
	 *                                           The fully qualified name of the
	 *                                           class implementing the target.
	 * @param processMode
	 *                                           The execution mode of the
	 *                                           target.
	 * @param parameters
	 *                                           The target parameters.
	 *                                           <code>null</code> if no
	 *                                           parameters were declared in the
	 *                                           configuration.
	 * @param ruleSets
	 *                                           The encoding rule sets declared
	 *                                           for the target.
	 *                                           <code>null</code> if no rule
	 *                                           sets were declared in the
	 *                                           configuration.
	 * @param inputIds
	 *                                           Set of identifiers referencing
	 *                                           either the input model or a
	 *                                           transformer.
	 * @param namespaces
	 *                                           List of namespaces for the
	 *                                           target. <code>null</code> if no
	 *                                           namespaces were declared in the
	 *                                           configuration.
	 * @param advancedProcessConfigurations
	 *                                           the
	 *                                           'advancedProcessConfigurations'
	 *                                           element from the configuration
	 *                                           of the process;
	 *                                           <code>null</code> if it is not
	 *                                           set there
	 * @param rdfTypeMapEntries tbd
	 * @param rdfPropertyMapEntries tbd
	 * @param stereotypeConversionParameters tbd
	 * @param typeConversionParameters tbd
	 * @param propertyConversionParameters tbd
	 * @param descriptorTargets tbd
	 * @param constraintMappings tbd
	 * @param generalProperties tbd
	 */
	public TargetOwlConfiguration(String className, ProcessMode processMode,
			Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets, SortedSet<String> inputIds,
			List<Namespace> namespaces, Element advancedProcessConfigurations,
			Map<String, List<RdfTypeMapEntry>> rdfTypeMapEntries,
			Map<String, List<RdfPropertyMapEntry>> rdfPropertyMapEntries,
			SortedMap<String, List<StereotypeConversionParameter>> stereotypeConversionParameters,
			Map<String, List<TypeConversionParameter>> typeConversionParameters,
			Map<String, List<PropertyConversionParameter>> propertyConversionParameters,
			List<DescriptorTarget> descriptorTargets,
			Map<ConstraintMapping.ConstraintType, ConstraintMapping> constraintMappings,
			List<RdfGeneralProperty> generalProperties) {

		super(className, processMode, parameters, ruleSets, null, inputIds,
				namespaces, advancedProcessConfigurations);

		for (String type : rdfTypeMapEntries.keySet()) {
			for (RdfTypeMapEntry rtme : rdfTypeMapEntries.get(type)) {
				this.rdfTypeMapEntries.put(
						type + "#" + (rtme.hasSchema() ? rtme.getSchema() : ""),
						rtme);
			}
		}

		for (String property : rdfPropertyMapEntries.keySet()) {
			for (RdfPropertyMapEntry rpme : rdfPropertyMapEntries
					.get(property)) {
				this.rdfPropertyMapEntries.put(
						property + "#"
								+ (rpme.hasSchema() ? rpme.getSchema() : ""),
						rpme);
			}
		}

		this.stereotypeConversionParameters = stereotypeConversionParameters;

		for (String type : typeConversionParameters.keySet()) {
			for (TypeConversionParameter tcp : typeConversionParameters
					.get(type)) {
				this.typeConversionParameters.put(
						type + "#" + (tcp.hasSchema() ? tcp.getSchema() : ""),
						tcp);
			}
		}

		for (String property : propertyConversionParameters.keySet()) {
			for (PropertyConversionParameter pcp : propertyConversionParameters
					.get(property)) {
				this.propertyConversionParameters.put(
						property + "#"
								+ (pcp.hasSchema() ? pcp.getSchema() : ""),
						pcp);
			}
		}

		this.descriptorTargets = descriptorTargets;
		this.constraintMappings = constraintMappings;
		this.generalProperties = generalProperties != null ? generalProperties
				: new ArrayList<>();
	}

	/**
	 * @param ci tbd
	 * @return the map entry that applies to this class; <code>null</code> if
	 *         none is applicable
	 */
	public RdfTypeMapEntry getTypeMapEntry(ClassInfo ci) {
		if (ci == null)
			return null;
		else {
			PackageInfo pkg = ci.model().schemaPackage(ci);
			if (pkg == null) {
				return getTypeMapEntry(ci.name());
			} else {
				return getTypeMapEntry(ci.name(), pkg.name());
			}
		}
	}

	public RdfTypeMapEntry getTypeMapEntry(String typeName) {
		return getTypeMapEntry(typeName, null);
	}

	/**
	 * @param pi tbd
	 * @return the map entry that applies to the type of the property;
	 *         <code>null</code> if none is applicable
	 */
	public RdfTypeMapEntry getTypeMapEntryByTypeInfo(PropertyInfo pi) {

		Model model = pi.model();
		Type t = pi.typeInfo();

		RdfTypeMapEntry rtme;

		if (t.id != null && model.classById(t.id) != null) {
			rtme = getTypeMapEntry(model.classById(t.id));
		} else {
			rtme = getTypeMapEntry(t.name);
		}

		return rtme;
	}

	/**
	 * Look up the map entry for a type, given its name and name of the schema
	 * it belongs to.
	 * 
	 * The configuration may contain multiple RdfTypeMapEntry elements (RTME)
	 * for a specific type (T). The look-up of the RdfTypeMapEntry that applies
	 * to T is performed as follows:
	 * 
	 * <ul>
	 * <li>If one RdfTypeMapEntry from RTME has a schema that matches the schema
	 * of T then that map entry is chosen (because it is specific for T).</li>
	 * <li>Otherwise, if one RdfTypeMapEntry from RTME does not define any
	 * schema, then it is chosen (because it is a generic mapping for T).</li>
	 * <li>Otherwise none of the elements in RTME applies to T.</li>
	 * </ul>
	 * 
	 * @param typeName tbd
	 * @param schemaName
	 *                       name of the schema to which the type belongs, may
	 *                       be <code>null</code> to only look for generic
	 *                       mappings
	 * @return the map entry that applies to this type; <code>null</code> if
	 *         none is applicable
	 */
	public RdfTypeMapEntry getTypeMapEntry(String typeName, String schemaName) {

		if (typeName == null) {
			return null;
		}

		if (schemaName != null
				&& rdfTypeMapEntries.containsKey(typeName + "#" + schemaName)) {
			return rdfTypeMapEntries.get(typeName + "#" + schemaName);
		}
		return rdfTypeMapEntries.get(typeName + "#");
	}

	public RdfPropertyMapEntry getPropertyMapEntry(PropertyInfo pi) {
		return getPropertyMapEntry(pi.inClass().name() + "::" + pi.name(),
				pi.model().schemaPackage(pi.inClass()).name());
	}

	/**
	 * Look up the map entry for a property, given a combination of the name of
	 * the class it belongs to and its name (example: Feature1::att4), as well
	 * as the name of the schema the class belongs to.
	 * 
	 * The configuration may contain multiple RdfPropertyMapEntry elements for a
	 * specific property (P). These elements are identified by matching the
	 * given property name (scoped to a class) against the 'property' and
	 * 'schema' of that map entry. The look-up of the RdfPropertyMapEntry that
	 * applies to P is performed as follows:
	 * 
	 * <ul>
	 * <li>If a map entry has the same combination of class name, property name,
	 * and schema then that map entry is chosen (because it is most specific for
	 * P).</li>
	 * <li>Otherwise, if a map entry has the same property name and schema, but
	 * the property name is not scoped to a specific class (example: att4) then
	 * that map entry is chosen (because it provides a generic mapping for the
	 * property that is specific to its schema).</li>
	 * <li>Otherwise, if a map entry does not define any schema, but has the
	 * same combination of class name and property name, then it is chosen
	 * (because it is a slightly more specific mapping for P compared to the
	 * generic mapping).</li>
	 * <li>Otherwise, if a map entry does not define any schema, but has the
	 * same property name and is not scoped to a specific class, then it is
	 * chosen (because it is a generic mapping for P).</li>
	 * <li>Otherwise none of map entries applies to P.</li>
	 * </ul>
	 * 
	 * @param propertyNameScopedToClass
	 *                                      name of the property to look up an
	 *                                      applicable map entry; the name has a
	 *                                      class name as prefix, separated by
	 *                                      "::" (example: Feature1::att4), may
	 *                                      NOT be <code>null</code> or empty
	 * @param schemaName
	 *                                      name of the schema to which the
	 *                                      property belongs, may NOT be
	 *                                      <code>null</code> or empty
	 * @return the map entry that applies to this property; <code>null</code> if
	 *         none is applicable
	 */
	public RdfPropertyMapEntry getPropertyMapEntry(
			String propertyNameScopedToClass, String schemaName) {

		if (propertyNameScopedToClass == null
				|| propertyNameScopedToClass.trim().isEmpty()
				|| schemaName == null || schemaName.trim().isEmpty()) {
			return null;
		}

		// do we have an ideal match?
		if (rdfPropertyMapEntries
				.containsKey(propertyNameScopedToClass + "#" + schemaName)) {
			return rdfPropertyMapEntries
					.get(propertyNameScopedToClass + "#" + schemaName);
		}

		String piName = null;

		String[] components = propertyNameScopedToClass.split("::");
		piName = components[1];

		// do we have a match upon property name and schema name?
		if (rdfPropertyMapEntries.containsKey(piName + "#" + schemaName)) {
			return rdfPropertyMapEntries.get(piName + "#" + schemaName);
		}

		// do we have a match upon property name scoped to class?
		if (rdfPropertyMapEntries
				.containsKey(propertyNameScopedToClass + "#")) {
			return rdfPropertyMapEntries.get(propertyNameScopedToClass + "#");
		}

		// finally, try looking up a map entry with the property name alone
		return rdfPropertyMapEntries.get(piName + "#");
	}

	public TypeConversionParameter getTypeConversionParameter(ClassInfo ci) {
		if (ci == null)
			return null;
		else
			return getTypeConversionParameter(ci.name(),
					ci.model().schemaPackage(ci).name());
	}

	// public TypeConversionParameter getTypeConversionParameter(String
	// typeName) {
	// return getTypeConversionParameter(typeName, null);
	// }

	/**
	 * Look up the conversion parameter for a type, given its name and name of
	 * the schema it belongs to.
	 * 
	 * The configuration may contain multiple TypeConversionParameter elements
	 * (TCP) for a specific type (T). The look-up of the TypeConversionParameter
	 * that applies to T is performed as follows:
	 * 
	 * <ul>
	 * <li>If one TypeConversionParameter from TCP has a schema that matches the
	 * schema of T then that map entry is chosen (because it is specific for T).
	 * </li>
	 * <li>Otherwise, if one TypeConversionParameter from TCP does not define
	 * any schema, then it is chosen (because it is a generic mapping for T).
	 * </li>
	 * <li>Otherwise none of the elements in TCP applies to T.</li>
	 * </ul>
	 * 
	 * @param typeName tbd
	 * @param schemaName
	 *                       name of the schema to which the type belongs, may
	 *                       be <code>null</code> to only look for generic
	 *                       conversion parameters
	 * @return the conversion parameter that applies to this type;
	 *         <code>null</code> if none is applicable
	 */
	public TypeConversionParameter getTypeConversionParameter(String typeName,
			String schemaName) {

		if (typeName == null) {
			return null;
		}

		if (schemaName != null && typeConversionParameters
				.containsKey(typeName + "#" + schemaName)) {
			return typeConversionParameters.get(typeName + "#" + schemaName);
		}
		return typeConversionParameters.get(typeName + "#");
	}

	public PropertyConversionParameter getPropertyConversionParameter(
			PropertyInfo pi) {

		return getPropertyConversionParameter(
				pi.inClass().name() + "::" + pi.name(),
				pi.model().schemaPackage(pi.inClass()).name());
	}

	/**
	 * Look up the conversion parameter for a property, given a combination of
	 * the name of the class it belongs to and its name (example:
	 * Feature1::att4), as well as the name of the schema the class belongs to.
	 * 
	 * The configuration may contain multiple PropertyConversionParameter
	 * elements for a specific property (P). These elements are identified by
	 * matching the given property name (scoped to a class) and schema against
	 * the 'property' and 'schema' of that element. The look-up of the
	 * PropertyConversionParameter that applies to P is performed as follows:
	 * 
	 * <ul>
	 * <li>If a conversion parameter has the same combination of class name,
	 * property name, and schema then that conversion parameter is chosen
	 * (because it is most specific for P).</li>
	 * <li>Otherwise, if a conversion parameter has the same property name and
	 * schema, but the property name is not scoped to a specific class (example:
	 * att4) then that conversion parameter is chosen (because it provides a
	 * generic mapping for the property that is specific to its schema).</li>
	 * <li>Otherwise, if a conversion parameter does not define any schema, but
	 * has the same combination of class name and property name, then it is
	 * chosen (because it is a slightly more specific conversion for P compared
	 * to the generic one).</li>
	 * <li>Otherwise, if a conversion parameter does not define any schema, but
	 * has the same property name and is not scoped to a specific class, then it
	 * is chosen (because it is a generic conversion for P).</li>
	 * <li>Otherwise none of conversion parameters applies to P.</li>
	 * </ul>
	 * 
	 * @param propertyNameScopedToClass
	 *                                      name of the property to look up an
	 *                                      applicable conversion parameter; the
	 *                                      name has a class name as prefix,
	 *                                      separated by "::" (example:
	 *                                      Feature1::att4), may NOT be
	 *                                      <code>null</code> or empty
	 * @param schemaName
	 *                                      name of the schema to which the
	 *                                      property belongs, may NOT be
	 *                                      <code>null</code> or empty
	 * @return the conversion parameter that applies to this property;
	 *         <code>null</code> if none is applicable
	 */
	public PropertyConversionParameter getPropertyConversionParameter(
			String propertyNameScopedToClass, String schemaName) {

		if (propertyNameScopedToClass == null
				|| propertyNameScopedToClass.trim().isEmpty()
				|| schemaName == null || schemaName.trim().isEmpty()) {
			return null;
		}

		// do we have an ideal match?
		if (propertyConversionParameters
				.containsKey(propertyNameScopedToClass + "#" + schemaName)) {
			return propertyConversionParameters
					.get(propertyNameScopedToClass + "#" + schemaName);
		}

		String piName = null;

		String[] components = propertyNameScopedToClass.split("::");
		piName = components[1];

		// do we have a match upon property name and schema name?
		if (propertyConversionParameters
				.containsKey(piName + "#" + schemaName)) {
			return propertyConversionParameters.get(piName + "#" + schemaName);
		}

		// do we have a match upon property name scoped to class?
		if (propertyConversionParameters
				.containsKey(propertyNameScopedToClass + "#")) {
			return propertyConversionParameters
					.get(propertyNameScopedToClass + "#");
		}

		// finally, try looking up a map entry with the property name alone
		return propertyConversionParameters.get(piName + "#");
	}

	/**
	 * @return map (can be empty but not <code>null</code>), with key:
	 *         identifier of wellknown stereotype, and value: list of conversion
	 *         parameters with that identifier as 'wellknown' (the list can be
	 *         empty but not <code>null</code>)
	 */
	public SortedMap<String, List<StereotypeConversionParameter>> getStereotypeConversionParameters() {
		return stereotypeConversionParameters;
	}

	/**
	 * @return list of descriptor targets (can be empty but not
	 *         <code>null</code>)
	 */
	public List<DescriptorTarget> getDescriptorTargets() {
		return descriptorTargets;
	}

	/**
	 * @param type  tbd
	 * @return mapping for the given constraint type; can be <code>null</code>
	 */
	public ConstraintMapping getConstraintMapping(
			ConstraintMapping.ConstraintType type) {
		return constraintMappings.get(type);
	}

	/**
	 * @param type tbd
	 * @return <code>true</code> if the configuration contains a mapping for the
	 *         given constraint type, else <code>false</code>
	 */
	public boolean hasConstraintMapping(ConstraintMapping.ConstraintType type) {
		return constraintMappings.containsKey(type);
	}

	/**
	 * @return the list of general property specifications defined by the
	 *         configuration; can be empty but not <code>null</code>
	 */
	public List<RdfGeneralProperty> getGeneralProperties() {
		return this.generalProperties;
	}

	/**
	 * Performs a number of tests.
	 * 
	 * @throws ShapeChangeAbortException
	 */
	void validate() throws ShapeChangeAbortException {

		List<String> messages = new ArrayList<String>();

		// ===== DescriptorTarget =====

		Pattern descriptorTargetTaggedValuePattern = Pattern
				.compile("TV(\\(.+?\\))?:(.+)");
		Pattern templatePattern = Pattern.compile("\\[\\[(.+?)\\]\\]");

		for (DescriptorTarget dt : this.descriptorTargets) {

			/* Check template */
			Matcher matcher = templatePattern.matcher(dt.getTemplate());
			while (matcher.find()) {
				String desc = matcher.group(1).trim();
				/*
				 * Check that if template starts with TV, it matches the regular
				 * expression
				 */
				if (desc.startsWith("TV")) {
					Matcher m = descriptorTargetTaggedValuePattern
							.matcher(desc);
					if (!m.matches()) {
						messages.add("Value of field [[" + desc
								+ "]] in template of DescriptorTarget configuration element for 'target' "
								+ dt.getTarget()
								+ " does not match regular expression TV(\\(.+?\\))?:(.+)");
					}
				}
			}

			/*
			 * check that 'target' is a QName with prefix matching one of the
			 * namespaces declared in the configuration
			 */
			String target = dt.getTarget();
			if (!target.contains(":") || target.startsWith(":")
					|| target.endsWith(":")
					|| !this.hasNamespaceWithAbbreviation(
							target.split(":")[0])) {
				messages.add("Value '" + target
						+ "' of attribute 'target' in the DescriptorTarget configuration element is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
			}
		}

		// ===== RdfTypeMapEntry =====

		Map<String, Set<String>> existingSchemaNameByTypeName = new HashMap<String, Set<String>>();
		SortedMap<String, SortedSet<String>> duplicateSchemaNameByTypeName = new TreeMap<String, SortedSet<String>>();

		for (RdfTypeMapEntry rtme : this.rdfTypeMapEntries.values()) {

			/*
			 * check that 'target' is a QName with prefix matching one of the
			 * namespaces declared in the configuration
			 */
			String target = rtme.getTarget();
			if (!target.contains(":") || target.startsWith(":")
					|| target.endsWith(":")
					|| !this.hasNamespaceWithAbbreviation(
							target.split(":")[0])) {
				messages.add("Value '" + target
						+ "' of attribute 'target' in the RdfTypeMapEntry configuration element (for type '"
						+ rtme.getType() + "'"
						+ (rtme.hasSchema()
								? " in schema '" + rtme.getSchema() + "'"
								: "")
						+ ") is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
			}

			/*
			 * Gather information to check that type mappings do not have the
			 * same combination of type and schema
			 */
			String schema = rtme.hasSchema() ? rtme.getSchema() : "<all>";

			if (existingSchemaNameByTypeName.containsKey(rtme.getType())) {

				Set<String> existingSchemaNames = existingSchemaNameByTypeName
						.get(rtme.getType());

				if (existingSchemaNames.contains(schema)) {

					// so we have a duplicate; log it

					if (duplicateSchemaNameByTypeName
							.containsKey(rtme.getType())) {
						duplicateSchemaNameByTypeName.get(rtme.getType())
								.add(schema);
					} else {
						SortedSet<String> duplicateSchemaNames = new TreeSet<String>();
						duplicateSchemaNames.add(schema);
						duplicateSchemaNameByTypeName.put(rtme.getType(),
								duplicateSchemaNames);
					}
				} else {
					existingSchemaNames.add(schema);
				}

			} else {

				Set<String> existingSchemaNames = new HashSet<String>();
				existingSchemaNames.add(schema);
				existingSchemaNameByTypeName.put(rtme.getType(),
						existingSchemaNames);
			}
		}

		/*
		 * Evaluate information to check that type mappings do not have the same
		 * combination of type and schema
		 */
		for (String type : duplicateSchemaNameByTypeName.keySet()) {
			for (String schema : duplicateSchemaNameByTypeName.get(type)) {
				messages.add("Multiple RdfTypeMapEntry elements with type '"
						+ type + "' and schema '" + schema
						+ "' encountered. The combination of type and schema must be unique.");
			}
		}

		// ===== RdfPropertyMapEntry =====

		Map<String, Set<String>> existingSchemaNameByPropertyName = new HashMap<String, Set<String>>();
		SortedMap<String, SortedSet<String>> duplicateSchemaNameByPropertyName = new TreeMap<String, SortedSet<String>>();

		for (RdfPropertyMapEntry rpme : this.rdfPropertyMapEntries.values()) {

			/*
			 * Check that 'property' has non-empty content on both sides of "::"
			 * if it has that separator.
			 */
			String property = rpme.getProperty();
			if (property.contains("::")) {
				String[] parts = property.split("::");
				if (parts.length != 2 || parts[0].trim().length() == 0
						|| parts[1].trim().length() == 0) {
					messages.add("Value '" + property
							+ "' of attribute 'property' in the RdfPropertyMapEntry configuration element (for property '"
							+ property + "'"
							+ (rpme.hasSchema()
									? " in schema '" + rpme.getSchema() + "'"
									: "")
							+ ") is not well-formed.");
				}
			}

			/*
			 * check that 'target' is a QName with prefix matching one of the
			 * namespaces declared in the configuration
			 */
			if (rpme.hasTarget()) {

				String target = rpme.getTarget();

				if (!target.contains(":") || target.startsWith(":")
						|| target.endsWith(":")
						|| !this.hasNamespaceWithAbbreviation(
								target.split(":")[0])) {
					messages.add("Value '" + target
							+ "' of attribute 'target' in the RdfPropertyMapEntry configuration element (for property '"
							+ property + "'"
							+ (rpme.hasSchema()
									? " in schema '" + rpme.getSchema() + "'"
									: "")
							+ ") is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
				}
			}

			/*
			 * check that 'range' - if set - is a QName with prefix matching one
			 * of the namespaces declared in the configuration
			 */
			if (rpme.hasRange()) {
				String range = rpme.getRange();
				if (!range.contains(":") || range.startsWith(":")
						|| range.endsWith(":")
						|| !this.hasNamespaceWithAbbreviation(
								range.split(":")[0])) {
					messages.add("Value '" + range
							+ "' of attribute 'range' in the RdfPropertyMapEntry configuration element (for property '"
							+ property + "'"
							+ (rpme.hasSchema()
									? " in schema '" + rpme.getSchema() + "'"
									: "")
							+ ") is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
				}
			}

			/*
			 * Gather information to check that property mappings do not have
			 * the same combination of type and schema
			 */
			String schema = rpme.hasSchema() ? rpme.getSchema() : "<all>";

			if (existingSchemaNameByPropertyName
					.containsKey(rpme.getProperty())) {

				Set<String> existingSchemaNames = existingSchemaNameByPropertyName
						.get(rpme.getProperty());

				if (existingSchemaNames.contains(schema)) {

					// so we have a duplicate; log it

					if (duplicateSchemaNameByPropertyName
							.containsKey(rpme.getProperty())) {
						existingSchemaNameByPropertyName.get(rpme.getProperty())
								.add(schema);
					} else {
						SortedSet<String> duplicateSchemaNames = new TreeSet<String>();
						duplicateSchemaNames.add(schema);
						existingSchemaNameByPropertyName.put(rpme.getProperty(),
								duplicateSchemaNames);
					}
				} else {
					existingSchemaNames.add(schema);
				}

			} else {

				Set<String> existingSchemaNames = new HashSet<String>();
				existingSchemaNames.add(schema);
				existingSchemaNameByPropertyName.put(rpme.getProperty(),
						existingSchemaNames);
			}
		}

		/*
		 * Evaluate information to check that property mappings do not have the
		 * same combination of property and schema
		 */
		for (String property : duplicateSchemaNameByPropertyName.keySet()) {
			for (String schema : duplicateSchemaNameByPropertyName
					.get(property)) {
				messages.add(
						"Multiple RdfPropertyMapEntry elements with property '"
								+ property + "' and schema '" + schema
								+ "' encountered. The combination of type and schema must be unique.");
			}
		}

		// ===== StereotypeConversionParameter =====

		/*
		 * check that 'subClassOf' is a QName with prefix matching one of the
		 * namespaces declared in the configuration
		 */
		for (String wellknownStereotype : this.stereotypeConversionParameters
				.keySet()) {
			for (StereotypeConversionParameter scp : this.stereotypeConversionParameters
					.get(wellknownStereotype)) {
				Set<String> subClassOf = scp.getSubClassOf();
				for (String sco : subClassOf) {
					if (!sco.contains(":") || sco.startsWith(":")
							|| sco.endsWith(":")
							|| !this.hasNamespaceWithAbbreviation(
									sco.split(":")[0])) {
						messages.add("Value '" + sco
								+ "' of attribute 'subClassOf' in the StereotypeConversionParameter configuration element for the wellknown stereotype '"
								+ scp.getWellknown()
								+ "' is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
					}
				}
			}
		}

		// ===== TypeConversionParameter =====

		for (TypeConversionParameter tcp : this.typeConversionParameters
				.values()) {

			/*
			 * check that 'subClassOf' is a QName with prefix matching one of
			 * the namespaces declared in the configuration
			 */
			Set<String> subClassOf = tcp.getSubClassOf();
			for (String sco : subClassOf) {
				if (!sco.contains(":") || sco.startsWith(":")
						|| sco.endsWith(":")
						|| !this.hasNamespaceWithAbbreviation(
								sco.split(":")[0])) {
					messages.add("Value '" + sco
							+ "' of attribute 'subClassOf' in the TypeConversionParameter configuration element (for type '"
							+ tcp.getType() + "'"
							+ (tcp.hasSchema()
									? " in schema '" + tcp.getSchema() + "'"
									: "")
							+ " )' is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
				}
			}
		}

		// ===== PropertyConversionParameter =====

		for (PropertyConversionParameter pcp : this.propertyConversionParameters
				.values()) {

			/*
			 * Check that 'property' has non-empty content on both sides of "::"
			 * if it has that separator.
			 */
			String property = pcp.getProperty();
			if (property.contains("::")) {
				String[] parts = property.split("::");
				if (parts.length != 2 || parts[0].trim().length() == 0
						|| parts[1].trim().length() == 0) {
					messages.add("Value '" + property
							+ "' of attribute 'property' in the PropertyConversionParameter configuration element (for property '"
							+ property + "'"
							+ (pcp.hasSchema()
									? " in schema '" + pcp.getSchema() + "'"
									: "")
							+ ") is not well-formed.");
				}
			}

			if (pcp.hasTarget()) {

				/*
				 * Check that 'target' is not empty and does include "::".
				 */
				String target = pcp.getTarget();
				if (target.isEmpty() || !target.contains("::")) {
					messages.add("Value '" + target
							+ "' of attribute 'target' in the PropertyConversionParameter configuration element (for property '"
							+ property + "'"
							+ (pcp.hasSchema()
									? " in schema '" + pcp.getSchema() + "'"
									: "")
							+ ") is not well-formed. It must not be empty and must include '::' (because it shall identify a global property, scoped to a specific class, and from a specific schema).");
				}

				/*
				 * Check that 'targetSchema' is also set.
				 */
				if (!pcp.hasTargetSchema()) {
					messages.add(
							"The PropertyConversionParameter configuration element (for property '"
									+ property + "'"
									+ (pcp.hasSchema()
											? " in schema '" + pcp.getSchema()
													+ "'"
											: "")
									+ ") has a 'target' but does not have a 'targetSchema' - both must be set or none of them.");
				}
			}

			/*
			 * check that 'subPropertyOf' is a QName with prefix matching one of
			 * the namespaces declared in the configuration
			 */
			if (pcp.hasSubPropertyOf()) {
				Set<String> subPropertyOf = pcp.getSubPropertyOf();
				for (String spo : subPropertyOf) {
					if (!spo.contains(":") || spo.startsWith(":")
							|| spo.endsWith(":")
							|| !this.hasNamespaceWithAbbreviation(
									spo.split(":")[0])) {
						messages.add("Value '" + spo
								+ "' of attribute 'subPropertyOf' in the PropertyConversionParameter configuration element (for property '"
								+ pcp.getProperty() + "'"
								+ (pcp.hasSchema()
										? " in schema '" + pcp.getSchema() + "'"
										: "")
								+ ")' is not well-formed. The prefix must be equal to the namespace abbreviation of a namespace that is contained in the configuration of the target.");
					}
				}
			}

			/*
			 * check that if global is true then the property is a property name
			 * scoped to a class from a specific schema.
			 */
			if (pcp.isGlobal()
					&& (!pcp.hasSchema() || pcp.getSchema().trim().isEmpty()
							|| !pcp.getProperty().contains("::"))) {
				messages.add("The PropertyConversionParameter (for property '"
						+ pcp.getProperty() + "'"
						+ (pcp.hasSchema()
								? " in schema '" + pcp.getSchema() + "'"
								: "")
						+ ")' with 'global' set to true is not well-formed. The 'property' must be scoped to a class and a schema must be given so that the parameter identifies a single property from the model.");
			}
		}

		if (!messages.isEmpty())
			throw new ShapeChangeAbortException(
					StringUtils.join(messages, "\r\n"));
	}
}
