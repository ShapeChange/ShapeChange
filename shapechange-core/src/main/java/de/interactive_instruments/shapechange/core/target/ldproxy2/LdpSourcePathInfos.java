package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.interactive_instruments.shapechange.core.model.PropertyInfo;

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

    public Optional<String> commonIdSourcePath() {
	List<String> distinctIdSourcePaths = this.spis.stream().map(
		spi -> spi.getIdSourcePath().isPresent() ? spi.getIdSourcePath().get() : "ID_SOURCE_PATH_UNAVAILABLE")
		.distinct().collect(Collectors.toList());

	if (distinctIdSourcePaths.size() == 1 && !"ID_SOURCE_PATH_UNAVAILABLE".equals(distinctIdSourcePaths.getFirst())) {
	    return Optional.of(distinctIdSourcePaths.getFirst());
	} else {
	    return Optional.empty();
	}
    }

    public Optional<String> commonValueSourcePath() {
	List<String> distinctValueSourcePaths = this.spis.stream()
		.map(spi -> spi.getIdSourcePath().isPresent() ? spi.getValueSourcePath().get()
			: "VALUE_SOURCE_PATH_UNAVAILABLE")
		.distinct().collect(Collectors.toList());

	if (distinctValueSourcePaths.size() == 1
		&& !"VALUE_SOURCE_PATH_UNAVAILABLE".equals(distinctValueSourcePaths.getFirst())) {
	    return Optional.of(distinctValueSourcePaths.getFirst());
	} else {
	    return Optional.empty();
	}
    }

    private void validateSourcePaths() {

	// helper method, for debugging purposes

	for (LdpSourcePathInfo spi : spis) {
	    if (spi.getIdSourcePath().isEmpty() && spi.getValueSourcePath().isEmpty()) {
		System.out.println("Invalid source path info element for property " + pi.fullNameInSchema());
	    }
	}

    }

    public boolean allWithRefType() {
	for (LdpSourcePathInfo spi : spis) {
	    if (spi.getRefType() == null) {
		return false;
	    }
	}
	return true;
    }

}
