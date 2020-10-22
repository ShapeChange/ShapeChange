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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.GeoPackage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;
import mil.nga.geopackage.core.srs.SpatialReferenceSystem;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GeoPackageTemplateConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(GeoPackageConstants.PARAM_DOCUMENTATION_NOVALUE, GeoPackageConstants.PARAM_DOCUMENTATION_TEMPLATE,
		    GeoPackageConstants.PARAM_GPKGM, GeoPackageConstants.PARAM_GPKGZ,
		    GeoPackageConstants.PARAM_ID_COLUMN_NAME, GeoPackageConstants.PARAM_ORGANIZATION_COORD_SYS_ID,
		    GeoPackageConstants.PARAM_SRS_ORGANIZATION).collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = null;

    // these fields will be initialized when isValid(...) is called
    private TargetConfiguration config = null;
    private Options options = null;
    private ShapeChangeResult result = null;
    private String inputs = null;

    @Override
    public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

	this.config = (TargetConfiguration) pConfig;
	this.options = options;
	this.result = result;

	inputs = StringUtils.join(config.getInputIds(), ", ");

	boolean isValid = true;
	
	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// ensure that output directory exists
	String outputDirectory = pConfig.getParameterValue("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = ".";

	File outputDirectoryFile = new File(outputDirectory);
	boolean exi = outputDirectoryFile.exists();
	if (!exi) {
	    outputDirectoryFile.mkdirs();
	    exi = outputDirectoryFile.exists();
	}
	boolean dir = outputDirectoryFile.isDirectory();
	boolean wrt = outputDirectoryFile.canWrite();
	boolean rea = outputDirectoryFile.canRead();
	if (!exi || !dir || !wrt || !rea) {
	    isValid = false;
	    result.addError(this, 2, outputDirectory);
	}

	// check that gpkgM has value 0, 1, or 2
	if (pConfig.hasParameter(GeoPackageConstants.PARAM_GPKGM)) {
	    isValid &= checkEnumeration(GeoPackageConstants.PARAM_GPKGM,
		    pConfig.getParameterValue(GeoPackageConstants.PARAM_GPKGM), "0", "1", "2");
	}

	// now also check gpkgZ
	if (pConfig.hasParameter(GeoPackageConstants.PARAM_GPKGZ)) {
	    isValid &= checkEnumeration(GeoPackageConstants.PARAM_GPKGZ,
		    pConfig.getParameterValue(GeoPackageConstants.PARAM_GPKGZ), "0", "1", "2");
	}

	// Parse SRSs from advanced process configuration (if set)
	// NOTE: XSD validation of the configuration will also validate the SRS
	// definitions
	List<SpatialReferenceSystem> srsDefs = pConfig.getAdvancedProcessConfigurations() == null ? new ArrayList<>()
		: GeoPackageTemplate.parseGeoPackageSrsDefinitions(pConfig.getAdvancedProcessConfigurations());

	/*
	 * Check that organizationCoordSysId matches one of the srsIds of the SRS
	 * contained in the advanced process configuration.
	 */
	if (pConfig.hasParameter(GeoPackageConstants.PARAM_ORGANIZATION_COORD_SYS_ID)) {
	    try {
		int orgCoordSysId = Integer
			.parseInt(pConfig.getParameterValue(GeoPackageConstants.PARAM_ORGANIZATION_COORD_SYS_ID));

		if (!(orgCoordSysId == -1 || orgCoordSysId == 0 || orgCoordSysId == 4326
			|| srsDefs.stream().anyMatch(srs -> srs.getSrsId() == orgCoordSysId))) {
		    MessageContext mc = result.addError(this, 3, "" + orgCoordSysId);
		    mc.addDetail(this, 0, inputs);
		    isValid = false;
		}
	    } catch (NumberFormatException e) {
		MessageContext mc = result.addError(this, 4, GeoPackageConstants.PARAM_ORGANIZATION_COORD_SYS_ID,
			e.getMessage());
		mc.addDetail(this, 0, inputs);
		isValid = false;
	    }
	}

	/*
	 * Check that srsOrganization either is (ignoring case) EPSG or one of the
	 * additional SRSs
	 */
	if (pConfig.hasParameter(GeoPackageConstants.PARAM_SRS_ORGANIZATION)) {

	    String srsOrg = pConfig.getParameterValue(GeoPackageConstants.PARAM_SRS_ORGANIZATION);

	    if (!("epsg".equalsIgnoreCase(srsOrg) || "none".equalsIgnoreCase(srsOrg)
		    || srsDefs.stream().anyMatch(srs -> srs.getOrganization().equalsIgnoreCase(srsOrg)))) {
		MessageContext mc = result.addError(this, 5, "" + srsOrg);
		mc.addDetail(this, 0, inputs);
		isValid = false;
	    }
	}

	return isValid;
    }

    private boolean checkEnumeration(String parameterName, String parameterValue, String... enums) {

	String v = parameterValue.trim();
	for (String e : enums) {
	    if (v.equals(e)) {
		return true;
	    }
	}

	MessageContext mc = result.addError(this, 6, parameterName, parameterValue.trim(),
		StringUtils.join(enums, ", "));
	mc.addDetail(this, 0, inputs);

	return false;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: GeoPackageTemplate target configuration element with 'inputs'='$1$'.";
	case 1:
	    return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'.";
	case 2:
	    return "Output directory '$1$' does not exist or is not accessible.";
	case 3:
	    return "Value of target parameter 'organizationCoordSysId' is '$1$', which does not match any ID of the minimal SRSs defined for every GeoPackage (-1, 0, 4326) or of the SRSs defined via the advanced process configuration.";
	case 4:
	    return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$.";
	case 5:
	    return "Value of target parameter 'srsOrganization' is '$1$', which does not match (ignoring case) any organization of the minimal SRSs defined for every GeoPackage ('NONE', 'EPSG') or of the SRSs defined via the advanced process configuration.";
	case 6:
	    return "Value of target parameter '$1$' is '$2$', which does not match any of the allowed values: $3$";
	default:
	    return "(GeoPackageTemplateConfigurationValidator.java) Unknown message with number: " + mnr;
	}
    }

}
