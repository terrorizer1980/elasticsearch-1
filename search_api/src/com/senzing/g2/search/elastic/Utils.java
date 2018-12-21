package com.senzing.g2.search.elastic;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

public class Utils 
{
	public static JsonObject getDocumentFromJsonString(String jsonString)
	{
                StringReader reader = new StringReader(jsonString);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject resultDoc = jsonReader.readObject();
		jsonReader.close();
		return resultDoc;
	}

	public static String getSimpleRawValue(JsonValue sourceValue)
	{
		ValueType valueType = sourceValue.getValueType();
		String result;
		switch (valueType)
		{
		case STRING:
			result = ((JsonString) sourceValue).getString();
			break;
		case ARRAY:
		case OBJECT:
		case NUMBER:
		case TRUE:
		case FALSE:
		case NULL:
		default:
			result = sourceValue.toString();
		}
		return result;
	}

}
