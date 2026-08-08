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

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Immutable description of one tool argument. The JSON schema advertised to clients is derived
 * from these, so descriptions here are model-facing and stay plain strings.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpParameter {
    public static McpParameter string(String name, String description) {
        return new McpParameter(name, description, McpParameterType.STRING, true, null, List.of());
    }

    public static McpParameter integer(String name, String description) {
        return new McpParameter(name, description, McpParameterType.INTEGER, true, null, List.of());
    }

    public static McpParameter number(String name, String description) {
        return new McpParameter(name, description, McpParameterType.NUMBER, true, null, List.of());
    }

    public static McpParameter bool(String name, String description) {
        return new McpParameter(name, description, McpParameterType.BOOLEAN, true, null, List.of());
    }

    public static McpParameter stringArray(String name, String description) {
        return new McpParameter(name, description, McpParameterType.STRING_ARRAY, true, null, List.of());
    }

    public static McpParameter enumOf(String name, String description, List<String> values) {
        return new McpParameter(name, description, McpParameterType.STRING, true, null, List.copyOf(values));
    }

    private final String myName;
    private final String myDescription;
    private final McpParameterType myType;
    private final boolean myRequired;
    private final @Nullable Object myDefaultValue;
    private final List<String> myEnumValues;

    private McpParameter(String name,
                         String description,
                         McpParameterType type,
                         boolean required,
                         @Nullable Object defaultValue,
                         List<String> enumValues) {
        myName = name;
        myDescription = description;
        myType = type;
        myRequired = required;
        myDefaultValue = defaultValue;
        myEnumValues = enumValues;
    }

    public McpParameter optional() {
        return new McpParameter(myName, myDescription, myType, false, myDefaultValue, myEnumValues);
    }

    /**
     * Also makes the parameter optional — a value the caller can omit is what a default is for.
     */
    public McpParameter defaultValue(Object value) {
        return new McpParameter(myName, myDescription, myType, false, value, myEnumValues);
    }

    public String getName() {
        return myName;
    }

    public String getDescription() {
        return myDescription;
    }

    public McpParameterType getType() {
        return myType;
    }

    public boolean isRequired() {
        return myRequired;
    }

    public @Nullable Object getDefaultValue() {
        return myDefaultValue;
    }

    public List<String> getEnumValues() {
        return myEnumValues;
    }
}
