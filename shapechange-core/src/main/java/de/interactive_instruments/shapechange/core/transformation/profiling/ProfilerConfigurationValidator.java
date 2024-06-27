package de.interactive_instruments.shapechange.core.transformation.profiling;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.MalformedProfileIdentifierException;
import de.interactive_instruments.shapechange.core.profile.Profiles;
import de.interactive_instruments.shapechange.core.transformation.profiling.Profiler.ConstraintHandling;

public class ProfilerConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(Profiler.PARAM_CONSTRAINTHANDLING, Profiler.PARAM_PROFILES,
		    Profiler.PARAM_RESIDUALTYPEREMOVAL_INCLUDESUBTYPESFOR).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

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
		result.addError(this, 20206, Profiler.PARAM_PROFILES, e.getMessage());
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
		result.addError(this, 20221, Profiler.PARAM_CONSTRAINTHANDLING);
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {

	case 100:
	    return "Required parameter '" + Profiler.PARAM_PROFILES + "' not set.";
	case 20206: //x
	    return "Error parsing component of '$1$' configuration parameter: $2$";
	case 20221: //x
	    return "Value of configuration parameter '$1$' does not match one of the defined values (was: '$2$').";
	    
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

}
