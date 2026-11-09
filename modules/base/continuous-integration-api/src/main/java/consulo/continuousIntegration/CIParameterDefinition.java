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
package consulo.continuousIntegration;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Parameter that a job takes when it is started.
 *
 * @param typeId     provider type of the parameter, for example {@code hudson.model.StringParameterDefinition} or
 *                   {@code choice} for a GitHub workflow input
 * @param attributes other provider data the editor may need
 * @since 2026-11-09
 */
public record CIParameterDefinition(
    String name,
    @Nullable String description,
    CIParameterKind kind,
    String typeId,
    @Nullable String defaultValue,
    List<String> choices,
    boolean required,
    Map<String, String> attributes
) {
    public CIParameterDefinition {
        choices = List.copyOf(choices);
        attributes = Map.copyOf(attributes);
    }

    public CIParameterDefinition(String name, CIParameterKind kind, String typeId, @Nullable String defaultValue) {
        this(name, null, kind, typeId, defaultValue, List.of(), false, Map.of());
    }

    public @Nullable String attribute(String name) {
        return attributes.get(name);
    }
}
