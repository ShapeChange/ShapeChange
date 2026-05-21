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
package de.interactive_instruments.shapechange.ea.inputtransformer.taggedvalues;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Package;

import de.interactive_instruments.shapechange.core.InputTransformerConfiguration;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.TaggedValueConfigurationEntry;
import de.interactive_instruments.shapechange.core.inputtransformation.InputTransformer;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.model.TaggedValues;
import de.interactive_instruments.shapechange.ea.model.AssociationInfoEA;
import de.interactive_instruments.shapechange.ea.model.ClassInfoEA;
import de.interactive_instruments.shapechange.ea.model.EADocument;
import de.interactive_instruments.shapechange.ea.model.PackageInfoEA;
import de.interactive_instruments.shapechange.ea.model.PropertyInfoEA;
import de.interactive_instruments.shapechange.ea.util.EAAttributeUtil;
import de.interactive_instruments.shapechange.ea.util.EAConnectorEndUtil;
import de.interactive_instruments.shapechange.ea.util.EAConnectorUtil;
import de.interactive_instruments.shapechange.ea.util.EAElementUtil;
import de.interactive_instruments.shapechange.ea.util.EAException;
import de.interactive_instruments.shapechange.ea.util.EAPackageUtil;
import de.interactive_instruments.shapechange.ea.util.EATaggedValue;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TaggedValueEaInputTransformer implements MessageSource, InputTransformer {

    private ShapeChangeResult result = null;
    private Options options = null;

    private EADocument model = null;

    private InputTransformerConfiguration trfConfig = null;

    /**
     * List of tagged values defined for the transformer; empty if none are defined.
     */
    private List<TaggedValueConfigurationEntry> taggedValues = new ArrayList<>();

    @Override
    public void initialise(Options o, InputTransformerConfiguration itrfConfig, ShapeChangeResult r,
	    String repoFileNameOrConnectionString) throws ShapeChangeAbortException {

	this.options = o;
	this.result = r;
	this.trfConfig = itrfConfig;

	/*
	 * identify tagged values in advancedProcessConfigurations element of the
	 * transformer configuration
	 */
	if (trfConfig.getAdvancedProcessConfigurations() == null) {

	    result.addWarning(this, 100);

	} else {

	    // parse tagged values, if any are defined
	    taggedValues = TaggedValueConfigurationEntry
		    .parseTaggedValues(trfConfig.getAdvancedProcessConfigurations());

	    /*
	     * NOTE: validation of all tagged value elements is done by the configuration
	     * validator
	     */

	    if (!this.taggedValues.isEmpty()) {

		model = new EADocument();

		String user = options.parameterAsString(null, "username", null, false, true);
		String pwd = options.parameterAsString(null, "password", null, false, true);

		if (StringUtils.isNotBlank(user)) {
		    model.initialise(result, options, repoFileNameOrConnectionString);
		} else {
		    model.initialise(result, options, repoFileNameOrConnectionString, user, pwd);
		}

		// Not needed for this transformation
//		model.loadInformationFromExternalSources(true);

		/*
		 * We deactivated model postprocessing because it should not be needed for
		 * tagged value transformation in the input model. Especially the validation of
		 * requirements and recommendations in the model are not of interest for an
		 * input transformation (which may fix the model for a subsequent model
		 * validation).
		 */
//		model.postprocessAfterLoadingAndValidate();
	    }
	}
    }

    @Override
    public void shutdown() {
	if (model != null) {
	    model.shutdown();
	}
    }

    @Override
    public void transform() throws ShapeChangeAbortException {

	if (model != null && !taggedValues.isEmpty()) {

	    for (PackageInfo pi : model.packages()) {

		List<EATaggedValue> taggedValuesToSet = determineTaggedValuesToSet(pi, taggedValues);

		setTaggedValues((PackageInfoEA) pi, taggedValuesToSet);
	    }

	    for (ClassInfo ci : model.classes()) {

		List<EATaggedValue> taggedValuesToSet = determineTaggedValuesToSet(ci, taggedValues);

		setTaggedValues((ClassInfoEA) ci, taggedValuesToSet);
	    }

	    for (PropertyInfo pi : model.properties()) {

		List<EATaggedValue> taggedValuesToSet = determineTaggedValuesToSet(pi, taggedValues);

		setTaggedValues((PropertyInfoEA) pi, taggedValuesToSet);
	    }

	    for (AssociationInfo ai : model.associations()) {

		List<EATaggedValue> taggedValuesToSet = determineTaggedValuesToSet(ai, taggedValues);

		setTaggedValues((AssociationInfoEA) ai, taggedValuesToSet);
	    }
	}
    }

    private void setTaggedValues(ClassInfoEA ci, List<EATaggedValue> taggedValuesToSet) {

	Element eaElmt = ci.getEaClassElement();

	for (EATaggedValue eatv : taggedValuesToSet) {

	    List<String> values = eatv.getValues();

	    if (values == null || values.isEmpty()) {

		continue;

	    } else {

		if (eatv.hasFQName()) {

		    if (values.size() > 1) {
			MessageContext mc = result.addError(this, 106, eatv.getFQName(), ci.name());
			if (mc != null) {
			    mc.addDetail(this, 2, ci.fullName());
			}
		    }

		    try {
			EAElementUtil.updateTaggedValue(eaElmt, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 104, eatv.getFQName(), ci.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 2, ci.fullName());
			}
		    }

		} else {

		    try {
			EAElementUtil.setTaggedValue(eaElmt, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 105, eatv.getName(), ci.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 2, ci.fullName());
			}
		    }
		}
	    }
	}
    }

    private void setTaggedValues(PropertyInfoEA pi, List<EATaggedValue> taggedValuesToSet) {

	if (pi.isAttribute()) {
	    setTaggedValues(pi, taggedValuesToSet, pi.getEAAttribute());
	} else {
	    setTaggedValues(pi, taggedValuesToSet, pi.getEAConnectorEnd());
	}
    }

    private void setTaggedValues(PropertyInfoEA pi, List<EATaggedValue> taggedValuesToSet,
	    ConnectorEnd eaConnectorEnd) {

	for (EATaggedValue eatv : taggedValuesToSet) {

	    List<String> values = eatv.getValues();

	    if (values == null || values.isEmpty()) {

		continue;

	    } else {

		if (eatv.hasFQName()) {

		    if (values.size() > 1) {
			MessageContext mc = result.addError(this, 109, eatv.getFQName(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 4, pi.fullName());
			}
		    }

		    try {
			EAConnectorEndUtil.updateTaggedValue(eaConnectorEnd, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 107, eatv.getFQName(), pi.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 4, pi.fullName());
			}
		    }

		} else {

		    try {
			EAConnectorEndUtil.setTaggedValue(eaConnectorEnd, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 108, eatv.getName(), pi.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 4, pi.fullName());
			}
		    }
		}
	    }
	}
    }

    private void setTaggedValues(PropertyInfoEA pi, List<EATaggedValue> taggedValuesToSet, Attribute eaAttribute) {

	for (EATaggedValue eatv : taggedValuesToSet) {

	    List<String> values = eatv.getValues();

	    if (values == null || values.isEmpty()) {

		continue;

	    } else {

		if (eatv.hasFQName()) {

		    if (values.size() > 1) {
			MessageContext mc = result.addError(this, 112, eatv.getFQName(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 3, pi.fullName());
			}
		    }

		    try {
			EAAttributeUtil.updateTaggedValue(eaAttribute, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 110, eatv.getFQName(), pi.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 3, pi.fullName());
			}
		    }

		} else {

		    try {
			EAAttributeUtil.setTaggedValue(eaAttribute, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 111, eatv.getName(), pi.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 3, pi.fullName());
			}
		    }
		}
	    }
	}
    }

    private void setTaggedValues(AssociationInfoEA ai, List<EATaggedValue> taggedValuesToSet) {

	Connector eaConnector = ai.getEAConnector();

	for (EATaggedValue eatv : taggedValuesToSet) {

	    List<String> values = eatv.getValues();

	    if (values == null || values.isEmpty()) {

		continue;

	    } else {

		if (eatv.hasFQName()) {

		    if (values.size() > 1) {
			MessageContext mc = result.addError(this, 115, eatv.getFQName(), ai.name());
			if (mc != null) {
			    mc.addDetail(this, 5, ai.end1().inClass().name(), ai.end1().name(),
				    ai.end2().inClass().name(), ai.end2().name());
			}
		    }

		    try {
			EAConnectorUtil.updateTaggedValue(eaConnector, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 113, eatv.getFQName(), ai.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 5, ai.end1().inClass().name(), ai.end1().name(),
				    ai.end2().inClass().name(), ai.end2().name());
			}
		    }

		} else {

		    try {
			EAConnectorUtil.setTaggedValue(eaConnector, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 114, eatv.getName(), ai.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 5, ai.end1().inClass().name(), ai.end1().name(),
				    ai.end2().inClass().name(), ai.end2().name());
			}
		    }
		}
	    }
	}

    }

    private void setTaggedValues(PackageInfoEA pkg, List<EATaggedValue> taggedValuesToSet) {

	Package eaPkg = pkg.getEaPackageObj();

	for (EATaggedValue eatv : taggedValuesToSet) {

	    List<String> values = eatv.getValues();

	    if (values == null || values.isEmpty()) {

		continue;

	    } else {

		if (eatv.hasFQName()) {

		    if (values.size() > 1) {
			MessageContext mc = result.addError(this, 103, eatv.getFQName(), pkg.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pkg.fullName());
			}
		    }

		    try {
			EAPackageUtil.updateTaggedValue(eaPkg, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 101, eatv.getFQName(), pkg.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 1, pkg.fullName());
			}
		    }

		} else {

		    try {
			EAPackageUtil.setTaggedValue(eaPkg, eatv);
		    } catch (EAException e) {
			MessageContext mc = result.addError(this, 102, eatv.getName(), pkg.name(), e.getMessage());
			if (mc != null) {
			    mc.addDetail(this, 1, pkg.fullName());
			}
		    }
		}
	    }
	}
    }

    /**
     * Identifies which tagged values shall be set on the given info object.
     * 
     * @param infoObject                      the model element to process
     *                                        (determine which of the tagged value
     *                                        definitions apply to it, and which
     *                                        tagged values thus to set to which
     *                                        value(s))
     * @param taggedValueConfigurationEntries tagged value definitions parsed from
     *                                        the configuration
     * @return list of the tagged values that shall be set; can be empty but not
     *         <code>null</code>
     */
    private List<EATaggedValue> determineTaggedValuesToSet(Info infoObject,
	    List<TaggedValueConfigurationEntry> taggedValueConfigurationEntries) {

	TaggedValues tvsCopy = infoObject.taggedValuesAll();

	SortedMap<String, SortedSet<String>> tvsToSet = new TreeMap<>();

	for (TaggedValueConfigurationEntry tvce : taggedValueConfigurationEntries) {

	    if (tvce.getModelElementSelectionInfo().matches(infoObject)) {

		String tvQualifiedName = tvce.getName();
		String tvName = tvQualifiedName.contains("::") ? StringUtils.substringAfterLast(tvQualifiedName, "::")
			: tvQualifiedName;
		String tvValue;

		if (tvsCopy.containsKey(tvName)) {
		    // tagged value already exists on model element

		    /*
		     * if the tagged value configuration does NOT contain an actual value, ignore it
		     * - use the existing value(s) instead
		     */
		    if (!tvce.hasValue()) {
			continue;
		    } else {
			tvValue = tvce.getValue();
		    }
		} else {
		    // tagged value does not exist on model element
		    /*
		     * if the tagged value configuration contains an actual value, use it -
		     * otherwise use the empty string
		     */
		    tvValue = tvce.hasValue() ? tvce.getValue() : "";
		}

		SortedSet<String> valueSet;

		if (tvsToSet.containsKey(tvQualifiedName)) {
		    valueSet = tvsToSet.get(tvQualifiedName);
		} else {
		    valueSet = new TreeSet<>();
		    tvsToSet.put(tvQualifiedName, valueSet);
		}
		valueSet.add(tvValue);
	    }
	}

	List<EATaggedValue> res = new ArrayList<>();

	for (Entry<String, SortedSet<String>> e : tvsToSet.entrySet()) {

	    if (e.getKey().contains(":")) {
		res.add(new EATaggedValue(StringUtils.substringAfterLast(e.getKey(), "::"), e.getKey(),
			new ArrayList<>(e.getValue())));
	    } else {
		res.add(new EATaggedValue(e.getKey(), new ArrayList<>(e.getValue())));
	    }
	}

	return res;
    }

    @Override
    public boolean isApplicableToInputModelType(String modelType) {
	return "EA7".equalsIgnoreCase(modelType);
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 1 -> "Context: package '$1$'";
	case 2 -> "Context: class '$1$'.";
	case 3 -> "Context: attribute '$1$'.";
	case 4 -> "Context: association role '$1$'.";
	case 5 ->
	    "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 100 ->
	    "No 'advancedProcessConfigurations' element present in the configuration. Thus, a tagged value transformation is not defined.";
	case 101 ->
	    "Exception occurred while updating (fully-qualified) tag '$1$' on package '$2$'. Exception message: $3$";
	case 102 -> "Exception occurred while setting tag '$1$' on package '$2$'. Exception message: $3$";
	case 103 ->
	    "Multiple values to update for (fully-qualified) tag '$1$' on package '$2$'. This InputTransformer supports updating a tag only with a single value. One of the values will be chosen.";
	case 104 ->
	    "Exception occurred while updating (fully-qualified) tag '$1$' on class '$2$'. Exception message: $3$";
	case 105 -> "Exception occurred while setting tag '$1$' on class '$2$'. Exception message: $3$";
	case 106 ->
	    "Multiple values to update for (fully-qualified) tag '$1$' on class '$2$'. This InputTransformer supports updating a tag only with a single value. One of the values will be chosen.";
	case 107 ->
	    "Exception occurred while updating (fully-qualified) tag '$1$' on association role '$2$'. Exception message: $3$";
	case 108 -> "Exception occurred while setting tag '$1$' on association role '$2$'. Exception message: $3$";
	case 109 ->
	    "Multiple values to update for (fully-qualified) tag '$1$' on association role '$2$'. This InputTransformer supports updating a tag only with a single value. One of the values will be chosen.";
	case 110 ->
	    "Exception occurred while updating (fully-qualified) tag '$1$' on attribute '$2$'. Exception message: $3$";
	case 111 -> "Exception occurred while setting tag '$1$' on attribute '$2$'. Exception message: $3$";
	case 112 ->
	    "Multiple values to update for (fully-qualified) tag '$1$' on attribute '$2$'. This InputTransformer supports updating a tag only with a single value. One of the values will be chosen.";
	case 113 ->
	    "Exception occurred while updating (fully-qualified) tag '$1$' on association '$2$'. Exception message: $3$";
	case 114 -> "Exception occurred while setting tag '$1$' on association '$2$'. Exception message: $3$";
	case 115 ->
	    "Multiple values to update for (fully-qualified) tag '$1$' on association '$2$'. This InputTransformer supports updating a tag only with a single value. One of the values will be chosen.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
