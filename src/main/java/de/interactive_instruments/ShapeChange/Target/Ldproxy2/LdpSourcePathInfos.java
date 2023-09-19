package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.List;

import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class LdpSourcePathInfos {

    protected PropertyInfo pi;
    protected LdpPropertyEncodingContext context;

    protected List<LdpSourcePathInfo> spis = new ArrayList<>();

    public boolean isEmpty() {
	return spis.isEmpty();
    }

    public List<LdpSourcePathInfo> getSourcePathInfos() {
	return this.spis;
    }

    public boolean isSingleSourcePath() {
	return this.spis.size() == 1;
    }

    public boolean isMultipleSourcePaths() {
	return this.spis.size() > 1;
    }

    public LdpSourcePathInfo addSourcePathInfo(LdpSourcePathInfo spi) {
	this.spis.add(spi);
	return spi;
    }

    public boolean concatRequired() {
	return isMultipleSourcePaths() && pi.cardinality().maxOccurs > 1;
    }

    public boolean coalesceRequired() {
	return isMultipleSourcePaths() && pi.cardinality().maxOccurs == 1;
    }

    /**
     * @return the pi
     */
    public PropertyInfo getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(PropertyInfo pi) {
        this.pi = pi;
    }

    /**
     * @return the context
     */
    public LdpPropertyEncodingContext getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(LdpPropertyEncodingContext context) {
        this.context = context;
    }
    
    
}
