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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.json.jsonschema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import de.interactive_instruments.shapechange.core.target.json.json.JsonBoolean;
import de.interactive_instruments.shapechange.core.target.json.json.JsonObject;
import de.interactive_instruments.shapechange.core.target.json.json.JsonValue;

import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchema extends ArrayList<JsonSchemaKeyword> implements JsonSerializable {

    /**
     * 
     */
    private static final long serialVersionUID = 6435126599869464953L;
    public static final JsonSchema TRUE = new JsonSchema(true);
    public static final JsonSchema FALSE = new JsonSchema(false);

    protected Boolean inherentValue = null;

    protected JsonObject otherData = new JsonObject();

    /**
     * Creates a new JSON Schema.
     */
    public JsonSchema() {

    }

    /**
     * Creates a new JSON Schema with boolean value.
     * 
     * @param value - the boolean value that the schema shall represent
     */
    public JsonSchema(boolean value) {
	inherentValue = value;
    }

    public void addOtherData(String jsonKey, JsonValue jsonValue) {
	otherData.put(jsonKey, jsonValue);
    }

    public JsonSchema additionalProperties(JsonSchema otherSchema) {
	this.add(new AdditionalPropertiesKeyword(otherSchema));
	return this;
    }

    public JsonSchema allOf(JsonSchema... definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof AllOfKeyword)
		.findFirst();

	AllOfKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new AllOfKeyword();
	    this.add(keyword);
	} else {
	    keyword = (AllOfKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(definition));

	return this;
    }

    public JsonSchema anyOf(JsonSchema... definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof AnyOfKeyword)
		.findFirst();

	AnyOfKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new AnyOfKeyword();
	    this.add(keyword);
	} else {
	    keyword = (AnyOfKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(definition));

	return this;
    }

    public JsonSchema comment(String comment) {
	this.add(new CommentKeyword(comment));
	return this;
    }

    public JsonSchema anchor(String anchor) {
	this.add(new AnchorKeyword(anchor));
	return this;
    }

    public JsonSchema const_(JsonValue value) {
	this.add(new ConstKeyword(value));
	return this;
    }

    public JsonSchema default_(JsonValue value) {
	this.add(new DefaultKeyword(value));
	return this;
    }

    public JsonSchema definition(String name, JsonSchema definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof DefinitionsKeyword)
		.findFirst();

	DefinitionsKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new DefinitionsKeyword();
	    this.add(keyword);
	} else {
	    keyword = (DefinitionsKeyword) lookup.get();
	}

	keyword.put(name, definition);

	return this;
    }

    public JsonSchema def(String name, JsonSchema definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof DefsKeyword)
		.findFirst();

	DefsKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new DefsKeyword();
	    this.add(keyword);
	} else {
	    keyword = (DefsKeyword) lookup.get();
	}

	keyword.put(name, definition);

	return this;
    }

    public JsonSchema description(String description) {
	this.add(new DescriptionKeyword(description));
	return this;
    }

    public JsonSchema else_(JsonSchema elseSchema) {
	this.add(new ElseKeyword(elseSchema));
	return this;
    }

    public JsonSchema enum_(JsonValue... value) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof EnumKeyword)
		.findFirst();

	EnumKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new EnumKeyword();
	    this.add(keyword);
	} else {
	    keyword = (EnumKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(value));

	return this;
    }

    public JsonSchema enumDescription(String name, JsonSchema enumSchema) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof EnumDescriptionKeyword)
		.findFirst();

	EnumDescriptionKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new EnumDescriptionKeyword();
	    this.add(keyword);
	} else {
	    keyword = (EnumDescriptionKeyword) lookup.get();
	}

	keyword.put(name, enumSchema);

	return this;
    }

    public JsonSchema examples(JsonValue... value) {
	this.add(new ExamplesKeyword(Arrays.asList(value)));
	return this;
    }

    public JsonSchema exclusiveMaximum(double maximum) {
	this.add(new ExclusiveMaximumKeyword(maximum));
	return this;
    }

    public JsonSchema exclusiveMinimum(double minimum) {
	this.add(new ExclusiveMinimumKeyword(minimum));
	return this;
    }

    public JsonSchema format(String format) {
	this.add(new FormatKeyword(format));
	return this;
    }

    public JsonSchema pattern(String pattern) {
	this.add(new PatternKeyword(pattern));
	return this;
    }

    public JsonSchema id(String id) {
	this.add(new IdKeyword(id));
	return this;
    }

    public JsonSchema if_(JsonSchema ifSchema) {
	this.add(new IfKeyword(ifSchema));
	return this;
    }

    public Optional<JsonSchema> if_() {
	return this.stream().filter(keyword -> keyword instanceof IfKeyword)
		.map(keyword -> ((IfKeyword) keyword).value()).findFirst();
    }

    public JsonSchema item(JsonSchema definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof ItemsKeyword)
		.findFirst();

	ItemsKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new ItemsKeyword();
	    this.add(keyword);
	} else {
	    keyword = (ItemsKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(definition));

	return this;
    }

    public JsonSchema items(JsonSchema schema) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof ItemsKeyword)
		.findFirst();

	ItemsKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new ItemsKeyword();
	    this.add(keyword);
	} else {
	    keyword = (ItemsKeyword) lookup.get();
	}

	keyword.add(schema);

	return this;
    }

    public JsonSchema maximum(double maximum) {
	this.add(new MaximumKeyword(maximum));

	return this;
    }

    public JsonSchema maxLength(int maxLength) {
	this.add(new MaxLengthKeyword(maxLength));

	return this;
    }

    public JsonSchema minLength(int minLength) {
	this.add(new MinLengthKeyword(minLength));

	return this;
    }

    public JsonSchema maxProperties(int count) {
	this.add(new MaxPropertiesKeyword(count));
	return this;
    }

    public JsonSchema maxItems(int count) {
	this.add(new MaxItemsKeyword(count));
	return this;
    }

    public JsonSchema minimum(double minimum) {
	this.add(new MinimumKeyword(minimum));
	return this;
    }

    public JsonSchema multipleOf(double multipleOf) {
	this.add(new MultipleOfKeyword(multipleOf));
	return this;
    }

    public JsonSchema minProperties(int count) {
	this.add(new MinPropertiesKeyword(count));
	return this;
    }

    public JsonSchema minItems(int count) {
	this.add(new MinItemsKeyword(count));
	return this;
    }

    public JsonSchema not(JsonSchema notSchema) {
	this.add(new NotKeyword(notSchema));
	return this;
    }

    public JsonSchema oneOf(JsonSchema... definition) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof OneOfKeyword)
		.findFirst();

	OneOfKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new OneOfKeyword();
	    this.add(keyword);
	} else {
	    keyword = (OneOfKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(definition));

	return this;
    }

    /**
     * @param name name of a key within the "properties" member
     * @return the JSON Schema of the property, if already defined
     */
    public Optional<JsonSchema> property(String name) {

	Optional<LinkedHashMap<String, JsonSchema>> props = properties();
	if (props.isPresent() && props.get().containsKey(name)) {
	    return Optional.of(props.get().get(name));
	} else {
	    return Optional.empty();
	}
    }

    public JsonSchema property(String name, JsonSchema propertySchema) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof PropertiesKeyword)
		.findFirst();

	PropertiesKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new PropertiesKeyword();
	    this.add(keyword);
	} else {
	    keyword = (PropertiesKeyword) lookup.get();
	}

	keyword.put(name, propertySchema);

	return this;
    }

    public JsonSchema removeProperty(String propertyName) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof PropertiesKeyword)
		.findFirst();

	if (lookup.isPresent()) {
	    PropertiesKeyword properties = (PropertiesKeyword) lookup.get();
	    properties.remove(propertyName);
	    if (properties.isEmpty()) {
		this.remove(properties);
	    }
	}

	return this;
    }

    public JsonSchema readOnly(boolean isReadOnly) {
	this.add(new ReadOnlyKeyword(isReadOnly));
	return this;
    }

    public JsonSchema ref(String reference) {
	this.add(new RefKeyword(reference));
	return this;
    }

    public JsonSchema required(String... value) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof RequiredKeyword)
		.findFirst();

	RequiredKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new RequiredKeyword();
	    this.add(keyword);
	} else {
	    keyword = (RequiredKeyword) lookup.get();
	}

	keyword.addAll(Arrays.asList(value));

	return this;
    }

    public JsonSchema removeRequired(String propertyName) {

	Optional<JsonSchemaKeyword> lookup = this.stream().filter(keyword -> keyword instanceof RequiredKeyword)
		.findFirst();

	if (lookup.isPresent()) {
	    RequiredKeyword required = (RequiredKeyword) lookup.get();
	    required.remove(propertyName);
	    if (required.isEmpty()) {
		this.remove(required);
	    }
	}

	return this;
    }

    public JsonSchema schema(String value) {
	this.add(new SchemaKeyword(value));
	return this;
    }

    public JsonSchema then(JsonSchema thenSchema) {
	this.add(new ThenKeyword(thenSchema));

	return this;
    }

    public JsonSchema title(String title) {
	this.add(new TitleKeyword(title));

	return this;
    }

    public JsonSchema type(JsonSchemaType... type) {

	Optional<TypeKeyword> lookup = this.stream().filter(keyword -> keyword instanceof TypeKeyword)
		.map(kw -> (TypeKeyword) kw).findFirst();

	TypeKeyword keyword;

	if (lookup.isEmpty()) {
	    keyword = new TypeKeyword();
	    this.add(keyword);
	} else {
	    keyword = lookup.get();
	}

	keyword.addAll(Arrays.asList(type));

	return this;
    }

    public Optional<TypeKeyword> type() {
	return this.stream().filter(keyword -> keyword instanceof TypeKeyword).map(keyword -> (TypeKeyword) keyword)
		.findFirst();
    }

    public Optional<TypeKeyword> removeTypeKeyword() {
	Optional<TypeKeyword> lookup = this.stream().filter(keyword -> keyword instanceof TypeKeyword)
		.map(kw -> (TypeKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public JsonSchema uniqueItems(boolean unique) {
	this.add(new UniqueItemsKeyword(unique));
	return this;
    }

    public JsonSchema nullable(boolean nullable) {
	this.add(new NullableKeyword(nullable));
	return this;
    }

    public Optional<JsonSchema> additionalProperties() {
	return this.stream().filter(keyword -> keyword instanceof AdditionalPropertiesKeyword)
		.map(keyword -> ((AdditionalPropertiesKeyword) keyword).value()).findFirst();
    }

    public Optional<AdditionalPropertiesKeyword> removeAdditionalPropertiesKeyword() {
	Optional<AdditionalPropertiesKeyword> lookup = this.stream()
		.filter(keyword -> keyword instanceof AdditionalPropertiesKeyword)
		.map(kw -> (AdditionalPropertiesKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public Optional<List<JsonSchema>> allOf() {
	return this.stream().filter(keyword -> keyword instanceof AllOfKeyword)
		.map(keyword -> ((List<JsonSchema>) ((AllOfKeyword) keyword))).findFirst();
    }

    public Optional<List<JsonSchema>> anyOf() {
	return this.stream().filter(keyword -> keyword instanceof AnyOfKeyword)
		.map(keyword -> ((List<JsonSchema>) ((AnyOfKeyword) keyword))).findFirst();
    }

    public Optional<String> comment() {
	return this.stream().filter(keyword -> keyword instanceof CommentKeyword)
		.map(keyword -> ((CommentKeyword) keyword).value()).findFirst();
    }

    public Optional<String> anchor() {
	return this.stream().filter(keyword -> keyword instanceof AnchorKeyword)
		.map(keyword -> ((AnchorKeyword) keyword).value()).findFirst();
    }

    public Optional<JsonValue> const_() {
	return this.stream().filter(keyword -> keyword instanceof ConstKeyword)
		.map(keyword -> ((ConstKeyword) keyword).value()).findFirst();
    }

    public Optional<JsonValue> default_() {
	return this.stream().filter(keyword -> keyword instanceof DefaultKeyword)
		.map(keyword -> ((DefaultKeyword) keyword).value()).findFirst();
    }

    public Optional<SortedMap<String, JsonSchema>> definitions() {
	return this.stream().filter(keyword -> keyword instanceof DefinitionsKeyword)
		.map(keyword -> ((SortedMap<String, JsonSchema>) ((DefinitionsKeyword) keyword))).findFirst();
    }

    public Optional<SortedMap<String, JsonSchema>> defs() {
	return this.stream().filter(keyword -> keyword instanceof DefsKeyword)
		.map(keyword -> ((SortedMap<String, JsonSchema>) ((DefsKeyword) keyword))).findFirst();
    }

    public Optional<String> description() {
	return this.stream().filter(keyword -> keyword instanceof DescriptionKeyword)
		.map(keyword -> ((DescriptionKeyword) keyword).value()).findFirst();
    }

    public Optional<JsonSchema> else_() {
	return this.stream().filter(keyword -> keyword instanceof ElseKeyword)
		.map(keyword -> ((ElseKeyword) keyword).value()).findFirst();
    }

    public Optional<List<JsonValue>> enum_() {
	return this.stream().filter(keyword -> keyword instanceof EnumKeyword)
		.map(keyword -> ((List<JsonValue>) ((EnumKeyword) keyword))).findFirst();
    }

    public Optional<List<JsonValue>> examples() {
	return this.stream().filter(keyword -> keyword instanceof ExamplesKeyword)
		.map(keyword -> ((List<JsonValue>) ((ExamplesKeyword) keyword))).findFirst();
    }

    public Optional<Double> exclusiveMaximum() {
	return this.stream().filter(keyword -> keyword instanceof ExclusiveMaximumKeyword)
		.map(keyword -> ((ExclusiveMaximumKeyword) keyword).value()).findFirst();
    }

    public Optional<Double> exclusiveMinimum() {
	return this.stream().filter(keyword -> keyword instanceof ExclusiveMinimumKeyword)
		.map(keyword -> ((ExclusiveMinimumKeyword) keyword).value()).findFirst();
    }

    public Optional<String> format() {
	return this.stream().filter(keyword -> keyword instanceof FormatKeyword)
		.map(keyword -> ((FormatKeyword) keyword).value()).findFirst();
    }

    public Optional<String> pattern() {
	return this.stream().filter(keyword -> keyword instanceof PatternKeyword)
		.map(keyword -> ((PatternKeyword) keyword).value()).findFirst();
    }

    public Optional<String> id() {
	return this.stream().filter(keyword -> keyword instanceof IdKeyword)
		.map(keyword -> ((IdKeyword) keyword).value()).findFirst();
    }

    public Optional<List<JsonSchema>> items() {
	return this.stream().filter(keyword -> keyword instanceof ItemsKeyword)
		.map(keyword -> ((List<JsonSchema>) ((ItemsKeyword) keyword))).findFirst();
    }

    public Optional<Double> maximum() {
	return this.stream().filter(keyword -> keyword instanceof MaximumKeyword)
		.map(keyword -> ((MaximumKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> maxLength() {
	return this.stream().filter(keyword -> keyword instanceof MaxLengthKeyword)
		.map(keyword -> ((MaxLengthKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> minLength() {
	return this.stream().filter(keyword -> keyword instanceof MinLengthKeyword)
		.map(keyword -> ((MinLengthKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> maxItems() {
	return this.stream().filter(keyword -> keyword instanceof MaxItemsKeyword)
		.map(keyword -> ((MaxItemsKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> maxProperties() {
	return this.stream().filter(keyword -> keyword instanceof MaxPropertiesKeyword)
		.map(keyword -> ((MaxPropertiesKeyword) keyword).value()).findFirst();
    }

    public Optional<MaxPropertiesKeyword> removeMaxPropertiesKeyword() {
	Optional<MaxPropertiesKeyword> lookup = this.stream().filter(keyword -> keyword instanceof MaxPropertiesKeyword)
		.map(kw -> (MaxPropertiesKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public Optional<Double> minimum() {
	return this.stream().filter(keyword -> keyword instanceof MinimumKeyword)
		.map(keyword -> ((MinimumKeyword) keyword).value()).findFirst();
    }

    public Optional<Double> multipleOf() {
	return this.stream().filter(keyword -> keyword instanceof MultipleOfKeyword)
		.map(keyword -> ((MultipleOfKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> minItems() {
	return this.stream().filter(keyword -> keyword instanceof MinItemsKeyword)
		.map(keyword -> ((MinItemsKeyword) keyword).value()).findFirst();
    }

    public Optional<Integer> minProperties() {
	return this.stream().filter(keyword -> keyword instanceof MinPropertiesKeyword)
		.map(keyword -> ((MinPropertiesKeyword) keyword).value()).findFirst();
    }

    public Optional<MinPropertiesKeyword> removeMinPropertiesKeyword() {
	Optional<MinPropertiesKeyword> lookup = this.stream().filter(keyword -> keyword instanceof MinPropertiesKeyword)
		.map(kw -> (MinPropertiesKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public Optional<JsonSchema> not() {
	return this.stream().filter(keyword -> keyword instanceof NotKeyword)
		.map(keyword -> ((NotKeyword) keyword).value()).findFirst();
    }

    public Optional<List<JsonSchema>> oneOf() {
	return this.stream().filter(keyword -> keyword instanceof OneOfKeyword)
		.map(keyword -> ((List<JsonSchema>) ((OneOfKeyword) keyword))).findFirst();
    }

    public Optional<LinkedHashMap<String, JsonSchema>> properties() {
	return this.stream().filter(keyword -> keyword instanceof PropertiesKeyword)
		.map(keyword -> ((LinkedHashMap<String, JsonSchema>) ((PropertiesKeyword) keyword))).findFirst();
    }

    public Optional<PropertiesKeyword> removePropertiesKeyword() {
	Optional<PropertiesKeyword> lookup = this.stream().filter(keyword -> keyword instanceof PropertiesKeyword)
		.map(kw -> (PropertiesKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public Optional<Boolean> readOnly() {
	return this.stream().filter(keyword -> keyword instanceof ReadOnlyKeyword)
		.map(keyword -> ((ReadOnlyKeyword) keyword).value()).findFirst();
    }

    public Optional<String> ref() {
	return this.stream().filter(keyword -> keyword instanceof RefKeyword)
		.map(keyword -> ((RefKeyword) keyword).value()).findFirst();
    }

    public Optional<SortedSet<String>> required() {
	return this.stream().filter(keyword -> keyword instanceof RequiredKeyword)
		.map(keyword -> ((SortedSet<String>) ((RequiredKeyword) keyword))).findFirst();
    }

    public Optional<RequiredKeyword> removeRequiredKeyword() {
	Optional<RequiredKeyword> lookup = this.stream().filter(keyword -> keyword instanceof RequiredKeyword)
		.map(kw -> (RequiredKeyword) kw).findFirst();
	if (lookup.isPresent()) {
	    this.remove(lookup.get());
	}
	return lookup;
    }

    public Optional<String> schema() {
	return this.stream().filter(keyword -> keyword instanceof SchemaKeyword)
		.map(keyword -> ((SchemaKeyword) keyword).value()).findFirst();
    }

    public Optional<JsonSchema> then() {
	return this.stream().filter(keyword -> keyword instanceof ThenKeyword)
		.map(keyword -> ((ThenKeyword) keyword).value()).findFirst();
    }

    public Optional<String> title() {
	return this.stream().filter(keyword -> keyword instanceof TitleKeyword)
		.map(keyword -> ((TitleKeyword) keyword).value()).findFirst();
    }

    public Optional<Boolean> uniqueItems() {
	return this.stream().filter(keyword -> keyword instanceof UniqueItemsKeyword)
		.map(keyword -> ((UniqueItemsKeyword) keyword).value()).findFirst();
    }

    public Optional<Boolean> nullable() {
	return this.stream().filter(keyword -> keyword instanceof NullableKeyword)
		.map(keyword -> ((NullableKeyword) keyword).value()).findFirst();
    }

    public Optional<JsonValue> xOgcCollectionId() {
	return this.stream().filter(keyword -> keyword instanceof XOgcCollectionIdKeyword)
		.map(keyword -> ((XOgcCollectionIdKeyword) keyword).value()).findFirst();
    }

    public JsonSchema xOgcCollectionId(JsonValue value) {
	this.add(new XOgcCollectionIdKeyword(value));
	return this;
    }

    public Optional<String> xOgcRole() {
	return this.stream().filter(keyword -> keyword instanceof XOgcRoleKeyword)
		.map(keyword -> ((XOgcRoleKeyword) keyword).value()).findFirst();
    }

    public JsonSchema xOgcRole(String role) {
	this.add(new XOgcRoleKeyword(role));
	return this;
    }

    public Optional<String> xOgcUriTemplate() {
	return this.stream().filter(keyword -> keyword instanceof XOgcUriTemplateKeyword)
		.map(keyword -> ((XOgcUriTemplateKeyword) keyword).value()).findFirst();
    }

    public JsonSchema xOgcUriTemplate(String template) {
	this.add(new XOgcUriTemplateKeyword(template));
	return this;
    }

    @Override
    public JsonValue toJson(JsonSerializationContext context) {

	if (inherentValue != null) {

	    return new JsonBoolean(inherentValue);

	} else {

	    JsonObject obj = new JsonObject();

	    for (JsonSchemaKeyword keyword : this) {
		obj.put(keyword.name(), keyword.toJson(context));
	    }

	    for (Entry<String, JsonValue> e : otherData.entrySet()) {
		obj.putIfAbsent(e.getKey(), e.getValue());
	    }

	    return obj;
	}
    }
}
