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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import shadow.org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpGidEncoder {

    private boolean liLineageFragmentCreated = false;

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

	boolean setSourcePaths = sourcePathInfosForBuilder != null
		&& !(Ldproxy2Target.enableFragments && sourcePathInfosForBuilder.getContext().isInFragment());

	String valueSourcePathOrColumnPrefix = null;
	PropertyInfo pi = null;

	if (setSourcePaths && sourcePathInfosForBuilder != null && !sourcePathInfosForBuilder.isEmpty()) {

	    LdpSourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);
	    valueSourcePathOrColumnPrefix = spi.getValueSourcePath().get();
	    pi = sourcePathInfosForBuilder.getPi();
	}

	// --- processStep(s)
	LinkedHashMap<String, FeatureSchema> propertyMapForProcessStepBuilder = new LinkedHashMap<>();
	ImmutableFeatureSchema.Builder processStepBuilder = new ImmutableFeatureSchema.Builder();
	processStepBuilder.name("processStep");
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
	    // --- processStep.source
	    LinkedHashMap<String, FeatureSchema> propertyMapForSourceBuilder = new LinkedHashMap<>();
	    ImmutableFeatureSchema.Builder sourceBuilder = new ImmutableFeatureSchema.Builder();
	    sourceBuilder.name("source").type(Type.OBJECT);

	    {
		// source.description
		ImmutableFeatureSchema.Builder sourceDescriptionBuilder = new ImmutableFeatureSchema.Builder();
		sourceDescriptionBuilder.name("description").type(Type.STRING);
		if (setSourcePaths) {
		    if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
			sourceDescriptionBuilder.sourcePath("src_des");
		    } else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			    "AX_DQErhebung3D", "AX_DQDachhoehe")) {
			sourceDescriptionBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_src");
		    }
		}
		propertyMapForSourceBuilder.put("description", sourceDescriptionBuilder.build());
	    }

	    propertyMapForProcessStepBuilder.put("source",
		    sourceBuilder.propertyMap(propertyMapForSourceBuilder).build());
	}

	{
	    // processStep.description
	    ImmutableFeatureSchema.Builder processStepDescriptionBuilder = new ImmutableFeatureSchema.Builder();
	    processStepDescriptionBuilder.name("description").type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processStepDescriptionBuilder.sourcePath("des");
		} else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			"AX_DQOhneDatenerhebung", "AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processStepDescriptionBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_des");
		}
	    }
	    propertyMapForProcessStepBuilder.put("description", processStepDescriptionBuilder.build());
	}

	{
	    // processStep.dateTime
	    ImmutableFeatureSchema.Builder processStepDateTimeBuilder = new ImmutableFeatureSchema.Builder();
	    processStepDateTimeBuilder.name("dateTime").type(Type.DATETIME);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processStepDateTimeBuilder.sourcePath("dat");
		} else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			"AX_DQOhneDatenerhebung", "AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processStepDateTimeBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_dat");
		}
	    }
	    propertyMapForProcessStepBuilder.put("dateTime", processStepDateTimeBuilder.build());
	}

	// --- processor
	LinkedHashMap<String, FeatureSchema> propertyMapForProcessorBuilder = new LinkedHashMap<>();
	ImmutableFeatureSchema.Builder processorBuilder = new ImmutableFeatureSchema.Builder();
	processorBuilder.name("processor").type(Type.OBJECT);

	{
	    // processor.organisationName
	    ImmutableFeatureSchema.Builder processorOrganisationNameBuilder = new ImmutableFeatureSchema.Builder();
	    processorOrganisationNameBuilder.name("organisationName").type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorOrganisationNameBuilder.sourcePath("pro_resp_org");
		} else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			"AX_DQOhneDatenerhebung", "AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorOrganisationNameBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_org");
		}
	    }
	    propertyMapForProcessorBuilder.put("organisationName", processorOrganisationNameBuilder.build());
	}

	{
	    // processor.individualName
	    ImmutableFeatureSchema.Builder processorIndividualNameBuilder = new ImmutableFeatureSchema.Builder();
	    processorIndividualNameBuilder.name("individualName").type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorIndividualNameBuilder.sourcePath("pro_resp_ind");
		} else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			"AX_DQOhneDatenerhebung", "AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorIndividualNameBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_ind");
		}
	    }
	    propertyMapForProcessorBuilder.put("individualName", processorIndividualNameBuilder.build());
	}

	{
	    // processor.role
	    ImmutableFeatureSchema.Builder processorRoleBuilder = new ImmutableFeatureSchema.Builder();
	    processorRoleBuilder.name("role").type(Type.STRING);
	    if (setSourcePaths) {
		if ("AX_DQPunktort".equalsIgnoreCase(pi.inClass().name())) {
		    processorRoleBuilder.sourcePath("pro_resp_rol_cdv");
		} else if (StringUtils.equalsAnyIgnoreCase(pi.inClass().name(), "AX_DQMitDatenerhebung",
			"AX_DQOhneDatenerhebung", "AX_DQErhebung3D", "AX_DQDachhoehe", "AX_DQBodenhoehe")) {
		    processorRoleBuilder.sourcePath(valueSourcePathOrColumnPrefix + "_prs_pro_resp_rol_cdv");
		}
	    }
	    propertyMapForProcessorBuilder.put("role", processorRoleBuilder.build());
	}

	propertyMapForProcessStepBuilder.put("processor",
		processorBuilder.propertyMap(propertyMapForProcessorBuilder).build());

	propertyMapForBuilder.put("processStep",
		processStepBuilder.propertyMap(propertyMapForProcessStepBuilder).build());
    }

    public boolean LiLineageFragmentCreated() {
	return this.liLineageFragmentCreated;
    }

    public ImmutableFeatureSchema createLiLineageFragment() {

	this.liLineageFragmentCreated = true;

	LinkedHashMap<String, FeatureSchema> propertyMapForLiLineageBuilder = new LinkedHashMap<>();

	this.gidLiLineageSchema(propertyMapForLiLineageBuilder, null);

	ImmutableFeatureSchema.Builder fragmentBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
		.name(Ldproxy2Constants.LI_LINEAGE_FRAGMENT_NAME).label(Ldproxy2Constants.LI_LINEAGE_OBJECT_TYPE)
		.objectType(Ldproxy2Constants.LI_LINEAGE_OBJECT_TYPE).propertyMap(propertyMapForLiLineageBuilder);

	return fragmentBuilder.build();
    }
}
