/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.mcp.tool;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolSchema {
    public static JsonObject of(List<McpParameter> parameters) {
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();

        for (McpParameter parameter : parameters) {
            properties.add(parameter.getName(), describe(parameter));

            if (parameter.isRequired()) {
                required.add(parameter.getName());
            }
        }

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", required);
        return schema;
    }

    private static JsonObject describe(McpParameter parameter) {
        JsonObject property = new JsonObject();
        property.addProperty("type", parameter.getType().getJsonType());
        property.addProperty("description", parameter.getDescription());

        if (parameter.getType() == McpParameterType.STRING_ARRAY) {
            JsonObject items = new JsonObject();
            items.addProperty("type", "string");
            property.add("items", items);
        }

        List<String> enumValues = parameter.getEnumValues();
        if (!enumValues.isEmpty()) {
            JsonArray values = new JsonArray();
            for (String value : enumValues) {
                values.add(value);
            }
            property.add("enum", values);
        }

        Object defaultValue = parameter.getDefaultValue();
        if (defaultValue instanceof String stringValue) {
            property.addProperty("default", stringValue);
        }
        else if (defaultValue instanceof Number numberValue) {
            property.addProperty("default", numberValue);
        }
        else if (defaultValue instanceof Boolean booleanValue) {
            property.addProperty("default", booleanValue);
        }

        return property;
    }

    private McpToolSchema() {
    }
}
