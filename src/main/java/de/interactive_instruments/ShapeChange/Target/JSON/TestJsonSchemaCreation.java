package de.interactive_instruments.ShapeChange.Target.JSON;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchema;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSerializationContext;

public class TestJsonSchemaCreation {

    public static void main(String[] args) {

	JsonSchema schema = new JsonSchema();

	schema.schema("http://json-schema.org/draft-07/schema#")
		.id("https://example.org/json/schemas/testschema/schema_definitions.json")
		.definition("Type",
			new JsonSchema().property("entityType", new JsonSchema().type(JsonSchemaType.STRING))
				.property("property", new JsonSchema().type(JsonSchemaType.STRING))
				.required("entityType", "property"))
		.ref("#/definitions/Type")
		.maxItems(4)
		.exclusiveMaximum(5.5)
		.readOnly(false);

	JsonSerializationContext context = new JsonSerializationContext();
	
	JsonValue jValue = schema.toJson(context);
	
	JsonElement gValue = jValue.toGson();
	
	Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	
	String jsonstring = gson.toJson(gValue);
	
	System.out.println(jsonstring);
    }

}
