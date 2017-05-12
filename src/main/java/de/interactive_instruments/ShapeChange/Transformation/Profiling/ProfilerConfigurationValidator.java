package de.interactive_instruments.ShapeChange.Transformation.Profiling;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.Profiler.ConstraintHandling;

public class ProfilerConfigurationValidator implements ConfigurationValidator {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		// Check 'profiles' transformation parameter
		String profilesParameterValue = config
				.getParameterValue(Profiler.PROFILES_PARAMETER);

		try {
			Profiles.parse(profilesParameterValue, true);
		} catch (MalformedProfileIdentifierException e) {
			isValid = false;
			result.addError(null, 20206, Profiler.PROFILES_PARAMETER,
					e.getMessage());
		}

		// 'constraintHandling'
		String constraintHandlingValue = config
				.getParameterValue(Profiler.CONSTRAINTHANDLING_PARAMETER);

		if (constraintHandlingValue != null
				&& constraintHandlingValue.length() > 0) {

			boolean validConstraintHandlingParameter = false;

			for (ConstraintHandling conHandlingEnum : ConstraintHandling
					.values()) {
				if (conHandlingEnum.name()
						.equalsIgnoreCase(constraintHandlingValue)) {

					validConstraintHandlingParameter = true;
					break;
				}
			}

			if (!validConstraintHandlingParameter) {
				isValid = false;
				result.addError(null, 20221,
						Profiler.CONSTRAINTHANDLING_PARAMETER);
			}
		}
		
		return isValid;
	}

}
