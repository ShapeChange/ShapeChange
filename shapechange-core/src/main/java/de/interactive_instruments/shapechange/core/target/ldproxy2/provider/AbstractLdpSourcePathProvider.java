package de.interactive_instruments.shapechange.core.target.ldproxy2.provider;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Constants;

public abstract class AbstractLdpSourcePathProvider implements LdpSourcePathProvider {

    protected boolean isImplementedAsFeatureReference(PropertyInfo pi) {

	return LdpInfo.isTypeWithIdentityValueType(pi)
		&& pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF);
    }
    
    public String sourcePathForDataTypeMemberOfGenericValueType() {
	return "datatype";
    }
    
    public String sourcePathForValueMemberOfGenericValueType(String valuePropertyName, String suffix) {
	return valuePropertyName + "_" + suffix;
    }
    
    public List<String> parseLdpSourcePathsTaggedValue(PropertyInfo pi) {
	List<String> result = new ArrayList<>();	
	String ldpSourcePaths = pi.taggedValue("ldpSourcePaths");
	if(StringUtils.isNotBlank(ldpSourcePaths)) {
	    for(String s : ldpSourcePaths.split("\\|\\|\\|")) {
		if(StringUtils.isNotBlank(s)) {
		    result.add(s.trim()); 
		}
	    }
	}	
	return result;
    }
}
