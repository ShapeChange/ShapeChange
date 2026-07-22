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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.Strings;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.xtraplatform.codelists.domain.Codelist.ImportType;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableCrsVariants;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.GeometryType;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGmlBuilder;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpGidEncoder {

    private boolean liLineageFragmentCreated = false;
    private boolean ciRoleCodeCodelistConstraintCreated = false;

    /**
     * @param propertyMapForBuilder     property map for the ldproxy encoding of the
     *                                  property whose value type is LI_Lineage
     * @param sourcePathInfosForBuilder source path information for the property
     *                                  whose value type is LI_Lineage; can be
     *                                  <code>null</code>, if the encoding context
     *                                  is in fragment
     */
    public void gidLiLineageSchema(LinkedHashMap<String, FeatureSchema> propertyMapForBuilder,
	    LdpSourcePathInfos sourcePathInfosForBuilder) {

	String processStep = Ldproxy2Target.propertyIdByTaggedValue ? "prs" : "processStep";
	String processor = Ldproxy2Target.propertyIdByTaggedValue ? "pro" : "processor";
	String source = Ldproxy2Target.propertyIdByTaggedValue ? "src" : "source";
	String description = Ldproxy2Target.propertyIdByTaggedValue ? "des" : "description";
	String dateTime = Ldproxy2Target.propertyIdByTaggedValue ? "dat" : "dateTime";
	String organisationName = Ldproxy2Target.propertyIdByTaggedValue ? "org" : "organisationName";
	String individualName = Ldproxy2Target.propertyIdByTaggedValue ? "ind" : "individualName";
	String positionName = Ldproxy2Target.propertyIdByTaggedValue ? "pos" : "positionName";
	String role = Ldproxy2Target.propertyIdByTaggedValue ? "rol" : "role";

	boolean setSourcePaths = sourcePathInfosForBuilder != null
		&& !(Ldproxy2Target.enableFragments && sourcePathInfosForBuilder.getContext().isInFragment());
	boolean createObjectTypes = !Ldproxy2Target.enableFragments || sourcePathInfosForBuilder == null
		|| sourcePathInfosForBuilder.getContext().isInFragment();

	/*
	 * set labels only if property id is given by tagged value and when objectType
	 * is encoded (especially relevant for encoding with fragments)
	 */
	Optional<String> label_processStep = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("processStep")
		: Optional.empty();
	Optional<String> label_processor = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("processor")
		: Optional.empty();
	Optional<String> label_source = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("source")
		: Optional.empty();
	Optional<String> label_description = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("description")
		: Optional.empty();
	Optional<String> label_dateTime = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("dateTime")
		: Optional.empty();
	Optional<String> label_organisationName = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("organisationName")
		: Optional.empty();
	Optional<String> label_individualName = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("individualName")
		: Optional.empty();
	Optional<String> label_positionName = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes
		? Optional.of("positionName")
		: Optional.empty();
	Optional<String> label_role = Ldproxy2Target.propertyIdByTaggedValue && createObjectTypes ? Optional.of("role")
		: Optional.empty();

	/*
	 * likewise for alias: set alias only if configured and when objectType is
	 * encoded (especially relevant for encoding with fragments)
	 */
	Optional<String> alias_processStep = Ldproxy2Target.propertyAlias && createObjectTypes
		? Optional.of("processStep")
		: Optional.empty();
	Optional<String> alias_processor = Ldproxy2Target.propertyAlias && createObjectTypes ? Optional.of("processor")
		: Optional.empty();
	Optional<String> alias_source = Ldproxy2Target.propertyAlias && createObjectTypes ? Optional.of("source")
		: Optional.empty();
	Optional<String> alias_description = Ldproxy2Target.propertyAlias && createObjectTypes
		? Optional.of("description")
		: Optional.empty();
	Optional<String> alias_dateTime = Ldproxy2Target.propertyAlias && createObjectTypes ? Optional.of("dateTime")
		: Optional.empty();
	Optional<String> alias_organisationName = Ldproxy2Target.propertyAlias && createObjectTypes
		? Optional.of("organisationName")
		: Optional.empty();
	Optional<String> alias_individualName = Ldproxy2Target.propertyAlias && createObjectTypes
		? Optional.of("individualName")
		: Optional.empty();
	Optional<String> alias_positionName = Ldproxy2Target.propertyAlias && createObjectTypes
		? Optional.of("positionName")
		: Optional.empty();
	Optional<String> alias_role = Ldproxy2Target.propertyAlias && createObjectTypes ? Optional.of("role")
		: Optional.empty();

	String valueSourcePathOrColumnPrefix = null;
	PropertyInfo pi = null;

	if (setSourcePaths && sourcePathInfosForBuilder != null && !sourcePathInfosForBuilder.isEmpty()) {

	    LdpSourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().getFirst();
	    valueSourcePathOrColumnPrefix = spi.getValueSourcePath().get();
	    pi = sourcePathInfosForBuilder.getPi();
	}

	// --- processStep(s)
	LinkedHashMap<String, FeatureSchema> propertyMapForProcessStepBuilder = new LinkedHashMap<>();
	ImmutableFeatureSchema.Builder processStepBuilder = new ImmutableFeatureSchema.Builder();
	processStepBuilder.name(processStep).label(label_processStep).alias(alias_processStep);
	if (createObjectTypes) {
	    processStepBuilder.objectType("LI_ProcessStep");
	}
	/*
	 * NOTE: We always use object_array, even for cases where only a single value is
	 * stored, because we want to use a common schema for li_lineage.
	 */
	processStepBuilder.type(Type.OBJECT_ARRAY);
	if (setSourcePaths) {
	    if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		processStepBuilder.sourcePath(valueSourcePathOrColumnPrefix);
	    }
	}

	{
	    // processStep.description
	    ImmutableFeatureSchema.Builder processStepDescriptionBuilder = new ImmutableFeatureSchema.Builder();
	    processStepDescriptionBuilder.name(description).label(label_description).alias(alias_description)
		    .type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processStepDescriptionBuilder.sourcePath("des");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processStepDescriptionBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_des");
		}
	    }
	    propertyMapForProcessStepBuilder.put(description, processStepDescriptionBuilder.build());
	}

	{
	    // processStep.dateTime
	    ImmutableFeatureSchema.Builder processStepDateTimeBuilder = new ImmutableFeatureSchema.Builder();
	    processStepDateTimeBuilder.name(dateTime).label(label_dateTime).alias(alias_dateTime).type(Type.DATETIME);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processStepDateTimeBuilder.sourcePath("dat");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processStepDateTimeBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_dat");
		}
	    }
	    propertyMapForProcessStepBuilder.put(dateTime, processStepDateTimeBuilder.build());
	}

	// --- processor
	LinkedHashMap<String, FeatureSchema> propertyMapForProcessorBuilder = new LinkedHashMap<>();
	ImmutableFeatureSchema.Builder processorBuilder = new ImmutableFeatureSchema.Builder();
	processorBuilder.name(processor).label(label_processor).alias(alias_processor).type(Type.OBJECT);
	if (createObjectTypes) {
	    processorBuilder.objectType("CI_ResponsibleParty");
	}

	{
	    // processor.individualName
	    ImmutableFeatureSchema.Builder processorIndividualNameBuilder = new ImmutableFeatureSchema.Builder();
	    processorIndividualNameBuilder.name(individualName).label(label_individualName).alias(alias_individualName)
		    .type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorIndividualNameBuilder.sourcePath("pro_resp_ind");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorIndividualNameBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_ind");
		}
	    }
	    propertyMapForProcessorBuilder.put(individualName, processorIndividualNameBuilder.build());
	}

	{
	    // processor.organisationName
	    ImmutableFeatureSchema.Builder processorOrganisationNameBuilder = new ImmutableFeatureSchema.Builder();
	    processorOrganisationNameBuilder.name(organisationName).label(label_organisationName)
		    .alias(alias_organisationName).type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorOrganisationNameBuilder.sourcePath("pro_resp_org");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorOrganisationNameBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_org");
		}
	    }
	    propertyMapForProcessorBuilder.put(organisationName, processorOrganisationNameBuilder.build());
	}

	{
	    // processor.positionName
	    ImmutableFeatureSchema.Builder processorPositionNameBuilder = new ImmutableFeatureSchema.Builder();
	    processorPositionNameBuilder.name(positionName).label(label_positionName).alias(alias_positionName)
		    .type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorPositionNameBuilder.sourcePath("pro_resp_pos");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorPositionNameBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_pos");
		}
	    }
	    propertyMapForProcessorBuilder.put(positionName, processorPositionNameBuilder.build());
	}

	{
	    // processor.role
	    ImmutableFeatureSchema.Builder processorRoleBuilder = new ImmutableFeatureSchema.Builder();
	    processorRoleBuilder.name(role).label(label_role).alias(alias_role).type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorRoleBuilder.sourcePath("pro_resp_rol_cdv");
		} else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQOhneDatenerhebung",
			"AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorRoleBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_rol_cdv");
		}
	    }

	    if (Ldproxy2Target.enableFragments
		    && (sourcePathInfosForBuilder == null || sourcePathInfosForBuilder.getContext().isInFragment())) {
		ImmutableSchemaConstraints.Builder processorRoleConstraintsBuilder = new ImmutableSchemaConstraints.Builder();
		processorRoleConstraintsBuilder.codelist("CI_RoleCode");
		ciRoleCodeCodelistConstraintCreated = true;
		processorRoleConstraintsBuilder.enumValues(
			List.of("resourceProvider", "custodian", "owner", "user", "distributor", "originator",
				"pointOfContact", "principalInvestigator", "processor", "publisher", "author"));
		processorRoleBuilder.constraints(processorRoleConstraintsBuilder.build());
	    }

	    propertyMapForProcessorBuilder.put(role, processorRoleBuilder.build());
	}

	propertyMapForProcessStepBuilder.put(processor,
		processorBuilder.propertyMap(propertyMapForProcessorBuilder).build());

	{
	    // --- processStep.source
	    LinkedHashMap<String, FeatureSchema> propertyMapForSourceBuilder = new LinkedHashMap<>();
	    ImmutableFeatureSchema.Builder sourceBuilder = new ImmutableFeatureSchema.Builder();
	    sourceBuilder.name(source).label(label_source).alias(alias_source).type(Type.OBJECT);
	    if (createObjectTypes) {
		sourceBuilder.objectType("LI_Source");
	    }

	    {
		// source.description
		ImmutableFeatureSchema.Builder sourceDescriptionBuilder = new ImmutableFeatureSchema.Builder();
		sourceDescriptionBuilder.name(description).label(label_description).alias(alias_description)
			.type(Type.STRING);
		if (setSourcePaths) {
		    if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
			sourceDescriptionBuilder.sourcePath("src_des");
		    } else if (Strings.CI.equalsAny(pi.inClass().name(), "AX_DQMitDatenerhebung", "AX_DQErhebung3D",
			    "AX_DQDachhoehe")) {
			sourceDescriptionBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_src");
		    }
		}
		propertyMapForSourceBuilder.put(description, sourceDescriptionBuilder.build());
	    }

	    propertyMapForProcessStepBuilder.put(source,
		    sourceBuilder.propertyMap(propertyMapForSourceBuilder).build());
	}

	propertyMapForBuilder.put(processStep,
		processStepBuilder.propertyMap(propertyMapForProcessStepBuilder).build());
    }

    public boolean LiLineageFragmentCreated() {
	return this.liLineageFragmentCreated;
    }

    public boolean ciRoleCodeCodelistConstraintCreated() {
	return this.ciRoleCodeCodelistConstraintCreated;
    }

    public ImmutableFeatureSchema createLiLineageFragment() {

	this.liLineageFragmentCreated = true;

	LinkedHashMap<String, FeatureSchema> propertyMapForLiLineageBuilder = new LinkedHashMap<>();

	this.gidLiLineageSchema(propertyMapForLiLineageBuilder, null);

	ImmutableFeatureSchema.Builder fragmentBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
		.name(Ldproxy2Constants.LI_LINEAGE_FRAGMENT_NAME).objectType(Ldproxy2Constants.LI_LINEAGE_OBJECT_TYPE)
		.propertyMap(propertyMapForLiLineageBuilder);

	return fragmentBuilder.build();
    }

    public void addBuildingBlockGmlDetails(PropertyInfo pi, ClassInfo topLevelClass, String propertyPath,
	    LdpBuildingBlockFeaturesGmlBuilder bbGmlBuilder) {

	if ("LI_Lineage".equals(pi.typeInfo().name)) {

	    String gmdNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GMD, Ldproxy2Constants.NSABR_GMD);
	    String gcoNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GCO, Ldproxy2Constants.NSABR_GCO);

	    /*
	     * Add object type namespaces.
	     */
	    bbGmlBuilder.register("LI_Lineage", gmdNsabr);
	    bbGmlBuilder.register("LI_ProcessStep", gmdNsabr);
	    bbGmlBuilder.register("LI_Source", gmdNsabr);
	    bbGmlBuilder.register("CI_ResponsibleParty", gmdNsabr);

	    /*
	     * Define value wraps
	     */
	    SortedMap<String, List<String>> valueWraps = new TreeMap<>();

	    if (pi.inClass().name().equals("AX_DQPunktort")) {

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:description/adv:
		 * AX_LI_ProcessStep_Punktort_Description
		 */
		{
		    String path_prsDes = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.des" : ".processStep.description");
		    valueWraps.put(path_prsDes, List.of("AX_LI_ProcessStep_Punktort_Description"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:dateTime/gco:DateTime
		 */
		{
		    String path_prsDat = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.dat" : ".processStep.dateTime");
		    valueWraps.put(path_prsDat, List.of(gcoNsabr + ":DateTime"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:organisationName/gco:CharacterString
		 */
		{
		    String path_prsProOrg = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.org"
			    : ".processStep.processor.organisationName");
		    valueWraps.put(path_prsProOrg, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:individualName/gco:CharacterString
		 */
		{
		    String path_prsProInd = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.ind"
			    : ".processStep.processor.individualName");
		    valueWraps.put(path_prsProInd, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:positionName/gco:CharacterString
		 */
		{
		    String path_prsProPos = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.pos"
			    : ".processStep.processor.positionName");
		    valueWraps.put(path_prsProPos, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode
		 */
		{
		    String path_prsProRol = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.rol" : ".processStep.processor.role");
		    valueWraps.put(path_prsProRol, List.of(gmdNsabr + ":CI_RoleCode"));
		}
		// TODO What about @codeListValue and @codeList?

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:source/gmd:LI_Source/
		 * gmd:description/adv:AX_Datenerhebung_Punktort
		 */
		{
		    String path_prsSrcDes = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.src.des"
			    : ".processStep.source.description");
		    valueWraps.put(path_prsSrcDes, List.of("adv:AX_Datenerhebung_Punktort"));
		}

	    } else if (pi.inClass().name().equals("AX_DQMitDatenerhebung")
		    || pi.inClass().name().equals("AX_DQOhneDatenerhebung")
		    || pi.inClass().name().equals("AX_DQErhebung3D") || pi.inClass().name().equals("AX_DQDachhoehe")
		    || pi.inClass().name().equals("AX_DQBodenhoehe")) {
		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:source/gmd:LI_Source/
		 * gmd:description/ + processStepSourceElementName
		 */
		{
		    String path_prsSrcDes = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.src.des"
			    : ".processStep.source.description");
		    if (pi.inClass().name().equals("AX_DQMitDatenerhebung")) {
			valueWraps.put(path_prsSrcDes, List.of("AX_Datenerhebung"));
		    } else if (pi.inClass().name().equals("AX_DQErhebung3D")) {
			valueWraps.put(path_prsSrcDes, List.of("AX_Datenerhebung3D"));
		    } else if (pi.inClass().name().equals("AX_DQDachhoehe")) {
			valueWraps.put(path_prsSrcDes, List.of("AX_LI_ProcessStep_Dachhoehe_Source"));
		    }
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:description/ +
		 * processStepDescriptionElementName
		 */
		{
		    String path_prsDes = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.des" : ".processStep.description");
		    if (pi.inClass().name().equals("AX_DQOhneDatenerhebung")) {
			valueWraps.put(path_prsDes, List.of("AX_LI_ProcessStep_OhneDatenerhebung_Description"));
		    } else if (pi.inClass().name().equals("AX_DQMitDatenerhebung")) {
			valueWraps.put(path_prsDes, List.of("AX_LI_ProcessStep_MitDatenerhebung_Description"));
		    } else if (pi.inClass().name().equals("AX_DQErhebung3D")) {
			valueWraps.put(path_prsDes, List.of("AX_LI_ProcessStep3D_Description"));
		    } else if (pi.inClass().name().equals("AX_DQDachhoehe")) {
			valueWraps.put(path_prsDes, List.of("AX_BezugspunktDach"));
		    } else if (pi.inClass().name().equals("AX_DQBodenhoehe")) {
			valueWraps.put(path_prsDes, List.of("AX_LI_ProcessStep_Bodenhoehe_Description"));
		    }
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:dateTime/gco:DateTime
		 */
		{
		    String path_prsDat = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.dat" : ".processStep.dateTime");
		    valueWraps.put(path_prsDat, List.of(gcoNsabr + ":DateTime"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:organisationName/gco:CharacterString
		 */
		{
		    String path_prsProOrg = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.org"
			    : ".processStep.processor.organisationName");
		    valueWraps.put(path_prsProOrg, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:individualName/gco:CharacterString
		 */
		{
		    String path_prsProInd = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.ind"
			    : ".processStep.processor.individualName");
		    valueWraps.put(path_prsProInd, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:positionName/gco:CharacterString
		 */
		{
		    String path_prsProPos = propertyPath + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.pos"
			    : ".processStep.processor.positionName");
		    valueWraps.put(path_prsProPos, List.of(gcoNsabr + ":CharacterString"));
		}

		/*
		 * Define value wrap for
		 * /gmd:LI_Lineage/gmd:processStep/gmd:LI_ProcessStep/gmd:processor/gmd:
		 * CI_ResponsibleParty/gmd:role/gmd:CI_RoleCode
		 */
		{
		    String path_prsProRol = propertyPath
			    + (Ldproxy2Target.propertyIdByTaggedValue ? ".prs.pro.rol" : ".processStep.processor.role");
		    valueWraps.put(path_prsProRol, List.of(gmdNsabr + ":CI_RoleCode"));
		}
		// TODO What about @codeListValue and @codeList?
	    }

	    bbGmlBuilder.gmlValueWrap(topLevelClass, valueWraps);

	} else if ("DQ_RelativeInternalPositionalAccuracy".equals(pi.typeInfo().name)) {

	    String gmdNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GMD, Ldproxy2Constants.NSABR_GMD);
	    String gcoNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GCO, Ldproxy2Constants.NSABR_GCO);

	    /*
	     * Define value wraps
	     */
	    SortedMap<String, List<String>> valueWraps = new TreeMap<>();

	    /*
	     * Define value wrap for
	     * /gmd:DQ_RelativeInternalPositionalAccuracy/gmd:result/gmd:
	     * DQ_QuantitativeResult/gmd:value/gco:Record
	     */
	    valueWraps.put(propertyPath,
		    List.of(gmdNsabr + ":DQ_RelativeInternalPositionalAccuracy", gmdNsabr + ":result",
			    gmdNsabr + ":DQ_QuantitativeResult", gmdNsabr + ":valueUnit[xlink:href=urn:adv:uom:m]/",
			    gmdNsabr + ":value", gcoNsabr + ":Record[xsi:type=gml:doubleList]"));

	    bbGmlBuilder.gmlValueWrap(topLevelClass, valueWraps);

	} else if ("DQ_AbsoluteExternalPositionalAccuracy".equals(pi.typeInfo().name)) {

	    String gmdNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GMD, Ldproxy2Constants.NSABR_GMD);
	    String gcoNsabr = bbGmlBuilder.gmlNsabr(Ldproxy2Constants.NS_GCO, Ldproxy2Constants.NSABR_GCO);

	    /*
	     * Define value wraps
	     */
	    SortedMap<String, List<String>> valueWraps = new TreeMap<>();

	    /*
	     * Define value wrap for
	     * /gmd:DQ_AbsoluteExternalPositionalAccuracy/gmd:result/gmd:
	     * DQ_QuantitativeResult/gmd:value/gco:Record
	     */
	    valueWraps.put(propertyPath,
		    List.of(gmdNsabr + ":DQ_AbsoluteExternalPositionalAccuracy", gmdNsabr + ":result",
			    gmdNsabr + ":DQ_QuantitativeResult", gmdNsabr + ":valueUnit[xlink:href=urn:adv:uom:m]/",
			    gmdNsabr + ":value", gcoNsabr + ":Record[xsi:type=gml:doubleList]"));

	    bbGmlBuilder.gmlValueWrap(topLevelClass, valueWraps);
	}

    }

    public ImmutableCodelist createCiRoleCodelist(LdproxyCfgWriter cfg) {

	ImmutableCodelist ic = cfg.builder().value().codelist().label("CI_RoleCode").sourceType(ImportType.TEMPLATES)
		.description("function performed by the responsible party").build();

	SortedMap<String, String> entries = new TreeMap<>();
	List<String> roleCodes = List.of("resourceProvider", "custodian", "owner", "user", "distributor", "originator",
		"pointOfContact", "principalInvestigator", "processor", "publisher", "author");
	for (String code : roleCodes) {
	    entries.put(code, code);
	}

	ic = ic.withEntries(entries);

	return ic;
    }

    public ImmutableFeatureSchema createPunktobjektForPunktortAUFragment() {

	ImmutableFeatureSchema.Builder fragmentBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
		.name("au_punktobjekt_punktortau").objectType("AU_Punktobjekt");

	LinkedHashMap<String, FeatureSchema> propertyDefs = new LinkedHashMap<>();

	// property pos_srs
	{
	    String propName = "pos_srs";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position_srs").type(Type.STRING);
	    propBuilder.role(Role.ORIGINAL_CRS_IDENTIFIER);

	    ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    constraintsBuilder.addAllEnumValues(List.of("urn:adv:crs:DE_DHDN_3GK3_HE100",
		    "urn:adv:crs:DE_DHDN_3GK3_HE120", "urn:adv:crs:ETRS89_Lat-Lon-h", "urn:adv:crs:ETRS89_X-Y-Z",
		    "urn:adv:crs:DE_DHHN2016_NH", "urn:adv:crs:DE_DHHN92_NH", "urn:adv:crs:DE_DHHN12_NOH",
		    "urn:adv:crs:DE_DHHN85_NOH", "urn:adv:crs:ETRS89_h"));

	    propBuilder.constraints(constraintsBuilder.build());

	    propertyDefs.put(propName, propBuilder.build());
	}

	// property pos_gk3
	{
	    String propName = "pos_gk3";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position_gk3").type(Type.GEOMETRY).geometryType(GeometryType.POINT);

	    propBuilder.role(Role.ORIGINAL_GEOMETRY);

	    propBuilder.nativeCrs(5677);

	    propBuilder.addOriginalCrsIdentifiers("urn:adv:crs:DE_DHDN_3GK3_HE100", "urn:adv:crs:DE_DHDN_3GK3_HE120");

	    propBuilder.falseEastingDifference(3000000);

	    propertyDefs.put(propName, propBuilder.build());
	}

	// property pos_llh
	{
	    String propName = "pos_llh";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position_llh").type(Type.GEOMETRY).geometryType(GeometryType.POINT);

	    propBuilder.role(Role.ORIGINAL_GEOMETRY);

	    propBuilder.nativeCrs(EpsgCrs.of(4937, Force.LON_LAT));

	    propBuilder.originalCrs(4937);

	    propBuilder.addOriginalCrsIdentifiers("urn:adv:crs:ETRS89_Lat-Lon-h");

	    propertyDefs.put(propName, propBuilder.build());
	}

	// property pos_geoc
	{
	    String propName = "pos_geoc";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position_geoc").type(Type.GEOMETRY).geometryType(GeometryType.POINT);

	    propBuilder.role(Role.ORIGINAL_GEOMETRY);

	    propBuilder.nativeCrs(4936);

	    propBuilder.addOriginalCrsIdentifiers("urn:adv:crs:ETRS89_X-Y-Z");

	    propertyDefs.put(propName, propBuilder.build());
	}

	// property pos_h
	{
	    String propName = "pos_h";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position_h").type(Type.FLOAT).unit("m");

	    propBuilder.role(Role.ORIGINAL_HEIGHT);

	    propBuilder.addOriginalCrsIdentifiers("urn:adv:crs:DE_DHHN2016_NH", "urn:adv:crs:DE_DHHN92_NH",
		    "urn:adv:crs:DE_DHHN12_NOH", "urn:adv:crs:DE_DHHN85_NOH", "urn:adv:crs:ETRS89_h");

	    propertyDefs.put(propName, propBuilder.build());
	}

	// property position
	{
	    String propName = "position";
	    ImmutableFeatureSchema.Builder propBuilder = new ImmutableFeatureSchema.Builder().name(propName)
		    .sourcePath("position").type(Type.GEOMETRY).role(Role.PRIMARY_GEOMETRY)
		    .geometryType(GeometryType.POINT).label("position").alias("position")
		    .description("Raumbezug der Punktgeometrie.");

	    ImmutableCrsVariants.Builder crsVariantsBuilder = new ImmutableCrsVariants.Builder();
	    crsVariantsBuilder.crsProperty("pos_srs").verticalProperty("pos_h");
	    crsVariantsBuilder.addGeometryProperties("pos_gk3", "pos_llh", "pos_geoc");

	    propBuilder.crsVariants(crsVariantsBuilder.build());

	    propertyDefs.put(propName, propBuilder.build());
	}

	fragmentBuilder.propertyMap(propertyDefs);

	fragmentBuilder.schema("#/fragments/au_objektmitunabhaengigergeometrie");

	return fragmentBuilder.build();
    }
}
