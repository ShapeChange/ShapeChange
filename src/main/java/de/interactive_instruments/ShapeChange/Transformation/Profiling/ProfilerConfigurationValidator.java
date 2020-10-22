package de.interactive_instruments.ShapeChange.Transformation.Profiling;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.Profiler.ConstraintHandling;

public class ProfilerConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(Profiler.PARAM_CONSTRAINTHANDLING, Profiler.PARAM_PROFILES,
		    Profiler.PARAM_RESIDUALTYPEREMOVAL_INCLUDESUBTYPESFOR).collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// Check 'profiles' transformation parameter
	if (!config.hasParameter(Profiler.PARAM_PROFILES)) {
	    isValid = false;
	    result.addError(this, 100);
	} else {
	    String profilesParameterValue = config.getParameterValue(Profiler.PARAM_PROFILES);

	    try {
		Profiles.parse(profilesParameterValue, true);
	    } catch (MalformedProfileIdentifierException e) {
		isValid = false;
		result.addError(null, 20206, Profiler.PARAM_PROFILES, e.getMessage());
	    }
	}

	// 'constraintHandling'
	String constraintHandlingValue = config.getParameterValue(Profiler.PARAM_CONSTRAINTHANDLING);

	if (constraintHandlingValue != null && constraintHandlingValue.length() > 0) {

	    boolean validConstraintHandlingParameter = false;

	    for (ConstraintHandling conHandlingEnum : ConstraintHandling.values()) {
		if (conHandlingEnum.name().equalsIgnoreCase(constraintHandlingValue)) {

		    validConstraintHandlingParameter = true;
		    break;
		}
	    }

	    if (!validConstraintHandlingParameter) {
		isValid = false;
		result.addError(null, 20221, Profiler.PARAM_CONSTRAINTHANDLING);
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {

	case 100:
	    return "Required parameter '" + Profiler.PARAM_PROFILES + "' not set.";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

}
