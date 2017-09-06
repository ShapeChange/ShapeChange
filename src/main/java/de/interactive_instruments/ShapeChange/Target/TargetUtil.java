package de.interactive_instruments.ShapeChange.Target;

import java.util.SortedSet;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;

public class TargetUtil {
	
	private static final String PARAM_MAIN_APP_SCHEMA = "mainAppSchema";

	private TargetUtil() {}

	public static PackageInfo findMainSchemaForSingleTargets(SortedSet<? extends PackageInfo> selectedSchemas, Options options, ShapeChangeResult result) {
		PackageInfo mainAppSchema;
		String mainAppSchemaName = options.parameter(TargetUtil.PARAM_MAIN_APP_SCHEMA);
		if (StringUtils.isBlank(mainAppSchemaName)) {
			if (selectedSchemas.size() == 1) {
				mainAppSchema = selectedSchemas.first();
			} else {
				mainAppSchema = null;
			}
		} else {
			Optional<? extends PackageInfo> tryFindMainAppSchemaResult = Iterables.tryFind(selectedSchemas, new Predicate<PackageInfo>() {
				
				@Override
				public boolean apply(PackageInfo packageInfo) {
					return mainAppSchemaName.equalsIgnoreCase(packageInfo.name());
				}
			});
			if (tryFindMainAppSchemaResult.isPresent()) {
				mainAppSchema = tryFindMainAppSchemaResult.get();
			} else {
				result.addError("Parameter " + TargetUtil.PARAM_MAIN_APP_SCHEMA + " set to " + mainAppSchemaName + " but no schema with this name was selected in the configuration");
				mainAppSchema = null;
			}
		}
		return mainAppSchema;
	}
	
	

}
