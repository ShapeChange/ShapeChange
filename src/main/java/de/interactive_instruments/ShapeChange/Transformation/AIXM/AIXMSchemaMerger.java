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
package de.interactive_instruments.ShapeChange.Transformation.AIXM;

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

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.AIXMSchemaInfos.AIXMSchemaInfo;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericAssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyDuplicatBehaviorIndicator;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyPositionIndicator;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * Merges the AIXM core schema and the extension schemas for all such schemas
 * that are part of the selected schemas. The core schema is identified via its
 * target namespace, which can be set in the configuration.
 * <p>
 * TODO: the timeSlice type created for a subtype does not contain (or inherit)
 * the properties from its supertype. We need to create an inheritance hierarchy
 * for the time slices!
 * 
 * @author Johannes Echterhoff
 *
 */
public class AIXMSchemaMerger implements Transformer, MessageSource {

	public static final String AIXM_5_1_TNS = "http://www.aixm.aero/schema/5.1";
	public static final String PARAM_INPUT_MERGE_AIXM_CORE_TNS = "coreSchemaTargetNamespace";

	private String coreAIXMSchemaTargetNamespace = AIXM_5_1_TNS;

	private GenericModel model;
	private Options options;
	private ShapeChangeResult result;

	private GenericPackageInfo coreSchema = null;
	/**
	 * key: Info id(); value: AIXMSchemaInfo for the Info object
	 */
	private Map<String, AIXMSchemaInfo> schemaInfos;

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)

	throws ShapeChangeAbortException {

		this.model = m;
		this.options = o;
		this.result = r;

		if (trfConfig.hasParameter(PARAM_INPUT_MERGE_AIXM_CORE_TNS)) {
			this.coreAIXMSchemaTargetNamespace = trfConfig
					.getParameterValue(PARAM_INPUT_MERGE_AIXM_CORE_TNS);
		}

		/*
		 * Identify core schema based upon core AIXM schema target namespace
		 */
		for (GenericPackageInfo selectedSchema : model.selectedSchemas()) {

			if (selectedSchema.targetNamespace()
					.equals(coreAIXMSchemaTargetNamespace)) {
				coreSchema = selectedSchema;
				break;
			}
		}

		if (coreSchema == null) {

			MessageContext mc = result.addFatalError(this, 1);
			mc.addDetail(this, 0);

			throw new ShapeChangeAbortException(this.message(1));
		}

		// identify all AIXM extensions and other types contained in all
		// selected schema

		Set<GenericClassInfo> extCis = new HashSet<GenericClassInfo>();
		Map<String, GenericClassInfo> nonExtCisById = new HashMap<String, GenericClassInfo>();

		for (GenericPackageInfo selectedSchema : model.selectedSchemas()) {

			SortedSet<ClassInfo> cisOfSelectedSchema = model
					.classes(selectedSchema);

			for (ClassInfo ci : cisOfSelectedSchema) {

				/*
				 * Note on casts: should be safe because the class belongs to a
				 * selected schema
				 */
				if (ci.category() == Options.AIXMEXTENSION) {
					extCis.add((GenericClassInfo) ci);
				} else {
					nonExtCisById.put(ci.id(), (GenericClassInfo) ci);
				}
			}
		}

		// create AIXMSchemaInfos from all classes and their properties
		schemaInfos = new TreeMap<String, AIXMSchemaInfo>();

		for (ClassInfo extCi : extCis) {
			determineSchemaInfos(extCi, schemaInfos);
		}

		for (ClassInfo nonExtCi : nonExtCisById.values()) {
			determineSchemaInfos(nonExtCi, schemaInfos);
		}
		options.setAIXMSchemaInfos(schemaInfos);

		// remove contents of core schema (classes & packages)
		coreSchema.setContainedPackages(new TreeSet<GenericPackageInfo>());
		coreSchema.setClasses(new TreeSet<GenericClassInfo>());

		/*
		 * remove all packages except core schema from the model and update
		 * selectedSchemaIds to only point to core schema
		 */
		Map<String, GenericPackageInfo> pkgInfosById = new HashMap<String, GenericPackageInfo>();
		pkgInfosById.put(coreSchema.id(), coreSchema);
		model.setGenPackageInfosById(pkgInfosById);

		Set<String> selectedSchemaIds = new HashSet<String>();
		selectedSchemaIds.add(coreSchema.id());
		model.setSelectedSchemaPackageIds(selectedSchemaIds);

		/*
		 * Remove all class infos from the model
		 */
		model.setGenClassInfosById(new HashMap<String, GenericClassInfo>());
		model.setGenClassInfosByName(new HashMap<String, GenericClassInfo>());

		/*
		 * only keep references to associations in model if both ends are not
		 * AIXM extensions (one or more copies of the other associations will be
		 * created when copying extension properties)
		 */
		Map<String, GenericAssociationInfo> aisToKeepById = new HashMap<String, GenericAssociationInfo>();

		for (GenericAssociationInfo ai : model.selectedSchemaAssociations()) {

			if (ai.end1().inClass().category() != Options.AIXMEXTENSION && ai
					.end2().inClass().category() != Options.AIXMEXTENSION) {
				aisToKeepById.put(ai.id(), ai);
			}
		}

		model.setGenAssociationInfosById(aisToKeepById);

		/*
		 * reset and repopulate properties map in model, only keeping those that
		 * belong to non-extension classes
		 */
		Map<String, GenericPropertyInfo> genPropsToKeepById = new HashMap<String, GenericPropertyInfo>();

		for (GenericClassInfo nonExtCi : nonExtCisById.values()) {

			for (PropertyInfo pi : nonExtCi.properties().values()) {

				/*
				 * Note on cast: should be safe because pi is a property of a
				 * GenericClassInfo
				 */
				GenericPropertyInfo genPi = (GenericPropertyInfo) pi;
				genPropsToKeepById.put(genPi.id(), genPi);
			}
		}

		for (GenericAssociationInfo ai : aisToKeepById.values()) {
			/*
			 * Note on cast: should be safe because end1 is a property of a
			 * GenericAssociationInfo where both ends should be in selected
			 * schema
			 */
			GenericPropertyInfo end1 = (GenericPropertyInfo) ai.end1();
			genPropsToKeepById.put(end1.id(), end1);

			/*
			 * Note on cast: should be safe because end2 is a property of a
			 * GenericAssociationInfo where both ends should be in selected
			 * schema
			 */
			GenericPropertyInfo end2 = (GenericPropertyInfo) ai.end2();
			genPropsToKeepById.put(end2.id(), end2);
		}

		model.setGenPropertiesById(genPropsToKeepById);

		/*
		 * add all non AIXM extensions to the core schema package; update pkg
		 * reference to core schema; ensure that non-extension classes are
		 * referenced by the model
		 */
		for (GenericClassInfo ci : nonExtCisById.values()) {
			coreSchema.addClass(ci);
			ci.setPkg(coreSchema);
			model.register(ci);
		}

		// identify the AIXM <<feature>> types
		Map<String, GenericClassInfo> aixmFeaturesById = new HashMap<String, GenericClassInfo>();

		for (GenericClassInfo ci : nonExtCisById.values()) {

			if (ci.category() == Options.FEATURE) {

				aixmFeaturesById.put(ci.id(), ci);
			}
		}

		/*
		 * copy extension properties to appropriate <<feature>> and <<object>>
		 * types (updating inClass accordingly)
		 */

		for (GenericClassInfo ci : extCis) {

			SortedSet<String> supertypeIds = ci.supertypes();

			// check for supertype
			if (!supertypeIds.isEmpty()) {

				/*
				 * copy properties of <<extension>> classes with supertype to
				 * that specific supertype
				 */

				if (supertypeIds.size() > 1) {

					MessageContext mc = result.addError(this, 2, ci.name());
					mc.addDetail(this, 0);

				} else {

					GenericClassInfo st = nonExtCisById
							.get(supertypeIds.iterator().next());

					if (st == null) {

						MessageContext mc = result.addWarning(this, 4,
								ci.name());
						mc.addDetail(this, 0);

					} else if (st.category() != Options.FEATURE
							&& st.category() != Options.OBJECT) {

						/*
						 * <<extension>> is only allowed for <<feature>> and
						 * <<object>> types
						 */
						MessageContext mc = result.addError(this, 3, ci.name(),
								st.name());
						mc.addDetail(this, 0);

					} else {

						for (PropertyInfo pi : ci.properties().values()) {

							copyExtensionOrSupertypeProperty(pi, st);
						}

						// remove relationship between supertype and extension
						ci.removeSupertype(st.id());

						if (ci.baseClass() != null
								&& ci.baseClass().id().equals(st.id())) {
							ci.setBaseClass(null);
						}

						st.removeSubtype(ci.id());
					}
				}

			} else {

				/*
				 * copy properties of <<extension>> classes without supertype to
				 * all <<feature>> types (in all selected schema)
				 */

				for (GenericClassInfo aixmFeature : aixmFeaturesById.values()) {

					for (PropertyInfo pi : ci.properties().values()) {

						copyExtensionOrSupertypeProperty(pi, aixmFeature);
					}
				}
			}
		}

		/*
		 * Create XxxTimeSlice objects for each AIXM feature type; move the
		 * properties from the feature type to the object; only add the
		 * 'timeSlice' property to the feature, with the value type being
		 * XxxTimeSlice.
		 */

		Map<String, GenericClassInfo> timeSliceTypesByTheirFeatureTypeId = new HashMap<String, GenericClassInfo>();

		for (GenericClassInfo ft : aixmFeaturesById.values()) {

			// create and register XxxTimeSlice object type
			GenericClassInfo tsType = new GenericClassInfo(ft.model(),
					ft.id() + "TimeSlice", ft.name() + "TimeSlice",
					Options.OBJECT);

			timeSliceTypesByTheirFeatureTypeId.put(ft.id(), tsType);

			GenericPackageInfo genPkg = model.getGenPackageInfosById()
					.get(ft.pkg().id());
			tsType.setPkg(genPkg);
			genPkg.addClass(tsType);
			model.register(tsType);

			// move properties from feature to object
			SortedMap<StructuredNumber, PropertyInfo> ftProps = ft.properties();

			for (PropertyInfo pi : ftProps.values()) {

				/*
				 * Note on cast: should be safe because all AIXM classes are
				 * contained in the AIXM schema which the GenericModel
				 * represents. Thus all properties of the feature type should be
				 * GenericPropertyInfos as well.
				 */
				GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

				genPi.setInClass(tsType);
			}

			tsType.setProperties(ftProps);

			// reset properties of feature
			TreeMap<StructuredNumber, PropertyInfo> newFtProps = new TreeMap<StructuredNumber, PropertyInfo>();
			ft.setProperties(newFtProps);

			// create timeSlice property
			GenericPropertyInfo tsPi = new GenericPropertyInfo(model,
					"timeSlice_for_" + ft.id(), "timeSlice", tsType.category());

			Multiplicity mult = new Multiplicity();
			mult.minOccurs = 1;
			mult.maxOccurs = Integer.MAX_VALUE;
			tsPi.setCardinality(mult);
			tsPi.setComposition(true);
			tsPi.setConstraints(null);
			tsPi.setDefaultCodeSpace("");
			tsPi.setInClass(ft);
			tsPi.setInlineOrByReference("inline");
			tsPi.setTaggedValues(options.taggedValueFactory(), false);
			tsPi.setSequenceNumber(new StructuredNumber("1"), true);

			Type t = new Type();
			t.id = tsType.id();
			t.name = tsType.name();
			tsPi.setTypeInfo(t);


			/*
			 * register timeSlice property in model and add it to feature
			 */
			model.add(tsPi, ft);

			/*
			 * Create additional properties for XxxTimeSlice types:
			 * 
			 * - interpretation
			 */
			GenericPropertyInfo interpretationPi = createInterpretationProperty(
					tsType);
			model.add(interpretationPi, tsType,
					PropertyCopyPositionIndicator.PROPERTY_COPY_TOP,
					PropertyCopyDuplicatBehaviorIndicator.IGNORE);

			/*
			 * Update AIXM schema infos. No need to do this for the properties
			 * that have been moved from the feature to the object type.
			 * 
			 * We can basically reuse the AIXMSchemaInfo from the feature type
			 * for the new properties and the new object type.
			 */
			AIXMSchemaInfo si = this.schemaInfos.get(ft.id());

			this.schemaInfos.put(tsPi.id(), si);
			this.schemaInfos.put(interpretationPi.id(), si);

			this.schemaInfos.put(tsType.id(), si);
		}

		// TODO - create proper schema where XxxTimeSlices derive from
		// AbstractTimeSlice so that a time slice hierarchy is created into
		// which the actual feature properties can be copied; features can
		// derive from a common abstract feature type but should not inherit the
		// timeSlice property (it is added for each feature type explicitly)
		// /*
		// * copy properties of XxxTimeSlice classes for non extension classes
		// to
		// * the YyyTimeSlice classes of their subtypes
		// */
		// for (GenericClassInfo ft : aixmFeaturesById.values()) {
		//
		//
		// }

		result.addDebug("Merging complete.");
	}

	private GenericPropertyInfo createInterpretationProperty(
			GenericClassInfo inClass) {

		GenericPropertyInfo pi = new GenericPropertyInfo(model,
				"interpretation_for_" + inClass.id(), "interpretation",
				inClass.category());

		Multiplicity mult = new Multiplicity();
		mult.minOccurs = 1;
		mult.maxOccurs = 1;
		pi.setCardinality(mult);
		pi.setComposition(true);
		pi.setConstraints(null);
		pi.setDefaultCodeSpace("");
		pi.setInClass(inClass);
		pi.setInlineOrByReference("inline");
		pi.setTaggedValues(options.taggedValueFactory(), false);
		pi.setSequenceNumber(new StructuredNumber("1"),true);
		Type t = new Type();
		ClassInfo ci = model.classByName("CharacterString");
		t.id = ci != null ? ci.id() : "unknown";
		t.name = "CharacterString";
		pi.setTypeInfo(t);
		

		return pi;
	}

	private void determineSchemaInfos(ClassInfo ci,
			Map<String, AIXMSchemaInfo> schemaInfos) {

		boolean isExtension = (ci.category() == Options.AIXMEXTENSION) ? true
				: false;
		String xmlns = ci.pkg().xmlns();
		String targetNamespace = ci.pkg().targetNamespace();

		AIXMSchemaInfo si = new AIXMSchemaInfo(xmlns, targetNamespace,
				isExtension);

		if (schemaInfos.containsKey(ci.id())) {
			result.addWarning(this, 6, ci.fullNameInSchema());
			return;
		} else {
			schemaInfos.put(ci.id(), si);
		}

		for (PropertyInfo pi : ci.properties().values()) {

			if (schemaInfos.containsKey(pi.id())) {
				result.addWarning(this, 6, pi.fullNameInSchema());
				return;
			} else {
				schemaInfos.put(pi.id(), si);
			}
		}

	}

	/**
	 * Adds a copy of the given property to the given class. The inClass of the
	 * new property is updated to be the given class. NOTE: the original
	 * namespace information for the property is kept in the AIXMSchemaInfos
	 * available via Options.
	 * 
	 * If the property belongs to an association, the association is copied
	 * (including a possibly existing association class) as well as the
	 * association ends.
	 * 
	 * If the type of pi is the inClass of pi, the type will be changed to be
	 * genCi.
	 * 
	 * @param genPi
	 * @param genCi
	 */
	private void copyExtensionOrSupertypeProperty(PropertyInfo pi,
			GenericClassInfo genCi) {

		if (pi.isAttribute()) {

			GenericPropertyInfo copy = model.createCopy(pi,
					pi.id() + "_extensionPropCopyFor_" + genCi.id());

			copy.setInClass(genCi);

			if (pi.typeInfo().id.equals(pi.inClass().id())) {
				copy.typeInfo().id = genCi.id();
				copy.typeInfo().name = genCi.name();
			}

			List<GenericPropertyInfo> l = new ArrayList<GenericPropertyInfo>();
			l.add(copy);

			model.add(copy, genCi,
					PropertyCopyPositionIndicator.PROPERTY_COPY_BOTTOM,
					PropertyCopyDuplicatBehaviorIndicator.OVERWRITE);

			addSchemaInfoForCopy(pi, copy);

		} else {

			GenericPropertyInfo copyPi = model.createCopy(pi,
					pi.id() + "_extensionPropCopyFor_" + genCi.id());

			addSchemaInfoForCopy(pi, copyPi);

			copyPi.setInClass(genCi);

			if (pi.typeInfo().id.equals(pi.inClass().id())) {
				copyPi.typeInfo().id = genCi.id();
				copyPi.typeInfo().name = genCi.name();
			}

			GenericPropertyInfo copyOtherEnd = model
					.createCopy(pi.reverseProperty(), pi.reverseProperty()
							+ "_extensionPropCopyFor_" + genCi.id());

			addSchemaInfoForCopy(pi.reverseProperty(), copyOtherEnd);

			/*
			 * inClass of other end does not change but its type must now be
			 * genCi
			 */
			copyOtherEnd.typeInfo().id = genCi.id();
			copyOtherEnd.typeInfo().name = genCi.name();

			AssociationInfo ai = pi.association();

			GenericAssociationInfo genAi = model.createCopy(ai,
					ai.id() + "_extensionPropCopyFor_" + genCi.id());

			if (ai.end1() == pi) {

				genAi.setEnd1(copyPi);
				genAi.setEnd2(copyOtherEnd);

			} else {

				genAi.setEnd1(copyOtherEnd);
				genAi.setEnd2(copyPi);
			}

			model.addAssociation(genAi);

			// handle association class
			if (ai.assocClass() != null) {

				ClassInfo assocCi = ai.assocClass();

				GenericClassInfo genAssocCi = (GenericClassInfo) assocCi;

				GenericClassInfo assocCopy = new GenericClassInfo(model,
						assocCi.id() + "_extensionCopyFor_" + genCi.id(),
						assocCi.name(), assocCi.category());

				addSchemaInfoForCopy(assocCi, assocCopy);

				genAi.setAssocClass(assocCopy);

				// set properties required by Info interface

				assocCopy.setTaggedValues(assocCi.taggedValuesAll(), false);
				assocCopy.setAliasName(assocCi.aliasName());
				assocCopy.setDefinition(assocCi.definition());
				assocCopy.setDescription(assocCi.description());
				assocCopy.setPrimaryCode(assocCi.primaryCode());
				assocCopy.setGlobalIdentifier(assocCi.globalIdentifier());
				assocCopy.setLanguage(assocCi.language());
				assocCopy.setLegalBasis(assocCi.legalBasis());
				assocCopy.setDataCaptureStatements(assocCi.dataCaptureStatements());
				assocCopy.setExamples(assocCi.examples());
				assocCopy.setStereotypes(assocCi.stereotypes());
				assocCopy.setXmlSchemaType(assocCi.xmlSchemaType());
				assocCopy.setIncludePropertyType(assocCi.includePropertyType());
				assocCopy.setIncludeByValuePropertyType(
						assocCi.includeByValuePropertyType());
				assocCopy.setIsCollection(assocCi.isCollection());
				assocCopy.setAsDictionary(assocCi.asDictionary());
				assocCopy.setAsGroup(assocCi.asGroup());
				assocCopy.setAsCharacterString(assocCi.asCharacterString());
				assocCopy.setHasNilReason(assocCi.hasNilReason());
				assocCopy.setIsAbstract(assocCi.isAbstract());
				assocCopy.setIsLeaf(assocCi.isLeaf());
				assocCopy.setSupertypes(model.copy(assocCi.supertypes()));
				assocCopy.setSubtypes(model.copy(assocCi.subtypes()));
				assocCopy.setBaseClass(assocCi.baseClass());
				assocCopy.setConstraints(model.copy(assocCi.constraints()));
				assocCopy.setSuppressed(assocCi.suppressed());
				assocCopy.setAsDictionaryGml33(assocCi.asDictionaryGml33());

				// now to the interesting part

				assocCopy.setAssocInfo(genAi);

				assocCopy.setProperties(null);
				model.copyClassContent(genAssocCi, assocCopy,
						PropertyCopyPositionIndicator.PROPERTY_COPY_INSEQUENCE,
						PropertyCopyDuplicatBehaviorIndicator.ADD);

				// we let the copy reference its original package
				assocCopy.setPkg(coreSchema);

				/*
				 * TBD: do we need to let the coreSchema reference the
				 * association class if it originates from an extension schema?
				 */

				model.register(assocCopy);
			}

			model.add(copyPi, genCi,
					PropertyCopyPositionIndicator.PROPERTY_COPY_BOTTOM,
					PropertyCopyDuplicatBehaviorIndicator.OVERWRITE);

			if (pi.reverseProperty().isNavigable()) {

				MessageContext mc = result.addWarning(this, 5,
						pi.inClass().name(),
						pi.reverseProperty().inClass().name(), pi.name(),
						pi.reverseProperty().name());
				mc.addDetail(this, 0);
			}
		}
	}

	private void addSchemaInfoForCopy(Info orig, Info copy) {

		AIXMSchemaInfo si = this.schemaInfos.get(orig.id());
		this.schemaInfos.put(copy.id(), si);
	}

	public String message(int mnr) {

		switch (mnr) {

		case 0:
			return "Context: class AIXMSchemaMerger";
		case 1:
			return "Could not find the core AIXM schema. None amongst the selected schemas has a target namespace equal to '"
					+ coreAIXMSchemaTargetNamespace + "'.";
		case 2:
			return "Class '$1$' is an <<extension>> type. It has more than one supertype which is not allowed.";
		case 3:
			return "Class '$1$' is an <<extension>> type. Its supertype '$2$' is not a <<feature>> or <<object>> - this is not allowed.";
		case 4:
			return "Class '$1$' is an <<extension>> type. It has a supertype that is not part of the schemas selected for processing. '$1$' will be ignored. Ensure that all AIXM schemas - the core schema and the extension schemas - are properly selected via the input configuration.";
		case 5:
			return "The association between classes '$1$' and '$2$' (with one role being '$3$' and the other one being '$4$') is navigable in both directions. This is not allowed in AIXM.";
		case 6:
			return "The id() of Info object with full name '$1$' is already contained in AIXM schema infos. Info object '$1$' will not be added to the schema infos.";
		default:
			return "(Unknown message)";
		}
	}
}
