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
package consulo.mcpServer.impl.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpToolCallContext;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolCallContextImpl implements McpToolCallContext {
    private final McpToolDescriptor myDescriptor;
    private final JsonObject myArguments;
    private final @Nullable Project myProject;

    public McpToolCallContextImpl(McpToolDescriptor descriptor, JsonObject arguments, @Nullable Project project) {
        myDescriptor = descriptor;
        myArguments = arguments;
        myProject = project;
    }

    @Override
    public Project getProject() {
        Project project = myProject;
        if (project == null) {
            throw new McpToolException("Tool '" + myDescriptor.getName() + "' requires a project, but none is bound to this call.");
        }
        return project;
    }

    @Override
    public String getString(String name) {
        String value = findString(name);
        if (value == null) {
            throw missing(name);
        }
        return value;
    }

    @Override
    public @Nullable String findString(String name) {
        JsonElement value = resolve(name);
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        return value.getAsString();
    }

    @Override
    public int getInt(String name) {
        JsonElement value = resolve(name);
        if (value == null || !value.isJsonPrimitive()) {
            throw missing(name);
        }
        try {
            return value.getAsInt();
        }
        catch (NumberFormatException e) {
            throw new McpToolException("Argument '" + name + "' is not an integer: " + value);
        }
    }

    @Override
    public boolean getBoolean(String name) {
        JsonElement value = resolve(name);
        if (value == null || !value.isJsonPrimitive()) {
            throw missing(name);
        }
        return value.getAsBoolean();
    }

    @Override
    public List<String> getStringList(String name) {
        JsonElement value = resolve(name);
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        JsonArray array = value.getAsJsonArray();
        List<String> result = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    /**
     * Falls back to the declared default, so tools never have to special-case omitted arguments.
     */
    private @Nullable JsonElement resolve(String name) {
        JsonElement value = myArguments.get(name);
        if (value != null && !value.isJsonNull()) {
            return value;
        }

        McpParameter parameter = myDescriptor.findParameter(name);
        Object defaultValue = parameter == null ? null : parameter.getDefaultValue();
        if (defaultValue instanceof String stringValue) {
            return new JsonPrimitive(stringValue);
        }
        if (defaultValue instanceof Number numberValue) {
            return new JsonPrimitive(numberValue);
        }
        if (defaultValue instanceof Boolean booleanValue) {
            return new JsonPrimitive(booleanValue);
        }
        return null;
    }

    private McpToolException missing(String name) {
        return new McpToolException("Missing required argument '" + name + "' for tool '" + myDescriptor.getName() + "'.");
    }
}
