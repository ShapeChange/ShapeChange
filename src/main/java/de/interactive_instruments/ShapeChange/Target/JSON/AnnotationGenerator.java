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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ModelElementSelectionInfo;
import de.interactive_instruments.ShapeChange.ModelElementSelectionParseException;
import de.interactive_instruments.ShapeChange.ShapeChangeParseException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.DescriptorAndTagResolver;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Target.JSON.config.AbstractJsonSchemaAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.config.MultiValueBehavior;
import de.interactive_instruments.ShapeChange.Target.JSON.config.NoValueBehavior;
import de.interactive_instruments.ShapeChange.Target.JSON.config.SimpleAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.config.TemplateAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonArray;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonBoolean;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonInteger;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonNull;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonNumber;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonString;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.GenericAnnotationKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchema;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class AnnotationGenerator implements MessageSource {

    protected ShapeChangeResult result;

    protected List<AbstractJsonSchemaAnnotationElement> annotationElements;
    protected String language;

    public AnnotationGenerator(List<AbstractJsonSchemaAnnotationElement> annotationElements, String language,
	    ShapeChangeResult result) {
	this.annotationElements = annotationElements;
	this.language = language;
	this.result = result;
    }

    /**
     * Generates JSON Schema annotations for the given Info object, based upon
     * configured annotation elements that apply to the model element. The
     * annotations are added to the given JsonSchema object.
     * 
     * @param js Annotations are added to this schema object.
     * @param i  The model element for which to generate annotations.
     */
    public void applyAnnotations(JsonSchema js, Info i) {

	for (AbstractJsonSchemaAnnotationElement annElmt : this.annotationElements) {

	    if (!appliesTo(annElmt, i)) {
		continue;
	    }

	    String annotation = annElmt.getAnnotation();

	    List<StringBuilder> builders = new ArrayList<StringBuilder>();
	    builders.add(new StringBuilder());

	    boolean noValuesForFields = true;

	    String template;
	    MultiValueBehavior multiValueBehavior;
	    String multiValueConnectorToken = " ";
	    JsonSchemaType jsonValueType = JsonSchemaType.STRING;

	    if (annElmt instanceof SimpleAnnotationElement) {
		SimpleAnnotationElement ann = (SimpleAnnotationElement) annElmt;
		template = "[[" + ann.getDescriptorOrTaggedValue() + "]]";
		multiValueBehavior = MultiValueBehavior.CREATE_MULTIPLE_ANNOTATION_VALUES;
		jsonValueType = ann.getType();
	    } else {
		TemplateAnnotationElement ann = (TemplateAnnotationElement) annElmt;
		template = ann.getValueTemplate();
		multiValueBehavior = ann.getMultiValueBehavior();
		multiValueConnectorToken = ann.getMultiValueConnectorToken();
	    }
	    String doc = template;

	    Pattern pattern = Pattern.compile("\\[\\[([^\\[].*?)\\]\\]");
	    // TV(\(.+?\))?:(.+) -> "TV(\\(.+?\\))?:(.+)"
	    Matcher matcher = pattern.matcher(template);

	    int index = 0;
	    while (matcher.find()) {

		String desc = matcher.group(1).trim();

		/*
		 * identify the descriptor or tagged value from the field and get value(s)
		 */
		List<String> values = new ArrayList<String>();
		boolean descRecognized = DescriptorAndTagResolver.resolveDescriptorOrTag(i, desc, language, values);

		// append the text from the template up until the current find
		for (StringBuilder b : builders) {
		    b.append(doc.substring(index, matcher.start()));
		}

		if (descRecognized) {

		    if (values.isEmpty()) {
			values.add(annElmt.getNoValueValue());
		    } else {
			noValuesForFields = false;
		    }

		    if (values.size() == 1) {

			for (StringBuilder b : builders) {
			    b.append(values.get(0));
			}

		    } else {

			if (multiValueBehavior == MultiValueBehavior.CONNECT_IN_SINGLE_ANNOTATION_VALUE) {

			    String connectedValues = StringUtils.join(values, multiValueConnectorToken);

			    for (StringBuilder b : builders) {
				b.append(connectedValues);
			    }

			} else {

			    // we shall split to multiple targets
			    List<StringBuilder> newBuilders = new ArrayList<StringBuilder>();

			    for (String val : values) {
				for (StringBuilder b : builders) {
				    StringBuilder newBuilder = new StringBuilder(b);
				    newBuilder.append(val);
				    newBuilders.add(newBuilder);
				}
			    }

			    builders = newBuilders;
			}
		    }

		} else {
		    // template field not recognized - put it back in
		    for (StringBuilder b : builders) {
			b.append(matcher.group(0));
		    }
		}

		index = matcher.end();
	    }

	    // append any remaining text from the template
	    for (StringBuilder b : builders) {
		b.append(doc.substring(index, doc.length()));
	    }

	    if (noValuesForFields && annElmt.getNoValueBehavior() == NoValueBehavior.IGNORE) {
		// we don't create anything from this annotation element

	    } else {

		List<String> values = new ArrayList<>();
		for (StringBuilder sb : builders) {
		    values.add(sb.toString());
		}

		List<JsonValue> jsonValues = new ArrayList<>();

		for (String v : values) {

		    if (jsonValueType == JsonSchemaType.BOOLEAN) {
			jsonValues.add(new JsonBoolean(StringUtils.equalsAnyIgnoreCase(v, "true", "1") ? true : false));
		    } else if (jsonValueType == JsonSchemaType.NUMBER) {
			try {
			    double val = Double.parseDouble(v);
			    jsonValues.add(new JsonNumber(val));
			} catch (NumberFormatException e) {
			    result.addWarning(this, 100, v, annotation, i.fullNameInSchema());
			}
		    } else if (jsonValueType == JsonSchemaType.INTEGER) {
			try {
			    int val = Integer.parseInt(v);
			    jsonValues.add(new JsonInteger(val));
			} catch (NumberFormatException e) {
			    result.addWarning(this, 101, v, annotation, i.fullNameInSchema());
			}
		    } else {
			// assume string value type
			jsonValues.add(new JsonString(v));
		    }
		}

		if (jsonValues.isEmpty() && annElmt.getNoValueBehavior() == NoValueBehavior.IGNORE) {
		    // nothing to do
		} else {

		    JsonValue vToAdd;

		    if (jsonValues.isEmpty()) {

			if (annElmt.isArrayValue()) {
			    // create empty array
			    vToAdd = new JsonArray();
			} else {
			    vToAdd = new JsonNull();
			}

		    } else if (jsonValues.size() == 1 && !annElmt.isArrayValue()) {

			vToAdd = jsonValues.get(0);

		    } else {
			// create JSON array
			JsonArray array = new JsonArray();
			array.addAll(jsonValues);
			vToAdd = array;
		    }

		    js.add(new GenericAnnotationKeyword(annotation, vToAdd));
		}
	    }
	}
    }

    protected boolean appliesTo(AbstractJsonSchemaAnnotationElement ann, Info i) {

	ModelElementSelectionInfo selectionInfo = ann.getModelElementSelectionInfo();

	return selectionInfo == null || selectionInfo.matches(i);
    }

    

    /**
     * @param advancedProcessConfigElmt the advancedProcessConfigurations element
     *                                  from the target configuration
     * @return list of JSON Schema annotation elements found in the
     *         advancedProcessConfigurations element; can be empty but not
     *         <code>null</code>
     * @throws ShapeChangeParseException If one of the annotation attributes
     *                                   contained an invalid value.
     */
    public static List<AbstractJsonSchemaAnnotationElement> parseAndValidateJsonSchemaAnnotationElements(
	    Element advancedProcessConfigElmt) throws ShapeChangeParseException {

	List<AbstractJsonSchemaAnnotationElement> result = new ArrayList<>();

	Element jsAnnElmt = XMLUtil.getFirstElement(advancedProcessConfigElmt, "JsonSchemaAnnotations");

	if (jsAnnElmt != null) {

	    Element annotations = XMLUtil.getFirstElement(jsAnnElmt, "annotations");

	    List<Element> annotationElements = XMLUtil.getElementNodes(annotations.getChildNodes());

	    List<String> compilationErrors = new ArrayList<>();

	    for (int i = 0; i < annotationElements.size(); i++) {

		Element elmt = annotationElements.get(i);

		AbstractJsonSchemaAnnotationElement ann;

		String annotationName = elmt.getAttribute("annotation");

		if ("SimpleAnnotation".equalsIgnoreCase(elmt.getLocalName())) {

		    JsonSchemaType type = JsonSchemaType.STRING;
		    if (elmt.hasAttribute("type")) {
			switch (elmt.getAttribute("type")) {
			case "integer":
			    type = JsonSchemaType.INTEGER;
			    break;
			case "boolean":
			    type = JsonSchemaType.BOOLEAN;
			    break;
			case "number":
			    type = JsonSchemaType.NUMBER;
			    break;
			default:
			    type = JsonSchemaType.STRING;
			    break;
			}
		    }

		    SimpleAnnotationElement simplAnn = new SimpleAnnotationElement(annotationName,
			    elmt.getAttribute("descriptorOrTaggedValue"), type);

		    ann = simplAnn;

		} else {

		    TemplateAnnotationElement tpltAnn = new TemplateAnnotationElement(annotationName,
			    elmt.getAttribute("valueTemplate"));

		    ann = tpltAnn;

		    if (elmt.hasAttribute("multiValueBehavior")) {
			if ("connectInSingleAnnotationValue".equals(elmt.getAttribute("multiValueBehavior"))) {
			    tpltAnn.setMultiValueBehavior(MultiValueBehavior.CONNECT_IN_SINGLE_ANNOTATION_VALUE);
			} else if ("createMultipleAnnotationValues".equals(elmt.getAttribute("multiValueBehavior"))) {
			    tpltAnn.setMultiValueBehavior(MultiValueBehavior.CREATE_MULTIPLE_ANNOTATION_VALUES);
			}
		    }

		    if (elmt.hasAttribute("multiValueConnectorToken")) {
			tpltAnn.setMultiValueConnectorToken(elmt.getAttribute("multiValueConnectorToken"));
		    }
		}

		// set common optional fields

		if (elmt.hasAttribute("arrayValue")) {
		    ann.setArrayValue((StringUtils.equalsAnyIgnoreCase(elmt.getAttribute("arrayValue"), "true", "1")));
		}

		if (elmt.hasAttribute("noValueBehavior")) {
		    if ("ignore".equals(elmt.getAttribute("noValueBehavior"))) {
			ann.setNoValueBehavior(NoValueBehavior.IGNORE);
		    } else if ("populateOnce".equals(elmt.getAttribute("noValueBehavior"))) {
			ann.setNoValueBehavior(NoValueBehavior.POPULATE_ONCE);
		    }
		}

		if (elmt.hasAttribute("noValueValue")) {
		    ann.setNoValueValue(elmt.getAttribute("noValueValue"));
		}

		ModelElementSelectionInfo selectionInfo = ModelElementSelectionInfo.parse(elmt);
		try {
		    selectionInfo.validate();
		} catch (ModelElementSelectionParseException e) {
		    compilationErrors.add(i + " JSON Schema annotation element (annotation '" + annotationName
			    + "'), model element selection attribute(s): " + e.getMessage());
		}
		ann.setModelElementSelectionInfo(selectionInfo);

		result.add(ann);
	    }

	    if (!compilationErrors.isEmpty()) {
		throw new ShapeChangeParseException(StringUtils.join(compilationErrors, ", "));
	    }
	}

	return result;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 100:
	    return "??Could not parse value '$1$' as double / number while creating annotation '$2$' for model element $3$. The value will be ignored.";
	case 101:
	    return "??Could not parse value '$1$' as integer while creating annotation '$2$' for model element $3$. The value will be ignored.";

	default:
	    return "(" + AnnotationGenerator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
