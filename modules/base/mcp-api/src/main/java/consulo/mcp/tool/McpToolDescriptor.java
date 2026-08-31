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

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolDescriptor {
    private final String myName;
    private final LocalizeValue myTitle;
    private final String myDescription;
    private final List<McpParameter> myParameters;
    private final @Nullable Boolean myReadOnlyHint;
    private final @Nullable Boolean myDestructiveHint;
    private final @Nullable Boolean myIdempotentHint;
    private final @Nullable Boolean myOpenWorldHint;

    public McpToolDescriptor(String name,
                             LocalizeValue title,
                             String description,
                             List<McpParameter> parameters,
                             @Nullable Boolean readOnlyHint,
                             @Nullable Boolean destructiveHint,
                             @Nullable Boolean idempotentHint,
                             @Nullable Boolean openWorldHint) {
        myName = name;
        myTitle = title;
        myDescription = description;
        myParameters = List.copyOf(parameters);
        myReadOnlyHint = readOnlyHint;
        myDestructiveHint = destructiveHint;
        myIdempotentHint = idempotentHint;
        myOpenWorldHint = openWorldHint;
    }

    public String getName() {
        return myName;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public String getDescription() {
        return myDescription;
    }

    public List<McpParameter> getParameters() {
        return myParameters;
    }

    public @Nullable McpParameter findParameter(String name) {
        for (McpParameter parameter : myParameters) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    public @Nullable Boolean getReadOnlyHint() {
        return myReadOnlyHint;
    }

    public @Nullable Boolean getDestructiveHint() {
        return myDestructiveHint;
    }

    public @Nullable Boolean getIdempotentHint() {
        return myIdempotentHint;
    }

    public @Nullable Boolean getOpenWorldHint() {
        return myOpenWorldHint;
    }
}
