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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What to start.
 *
 * @param ref        branch or tag to build, when the server supports it ({@link CICapability#RUN_WITH_REF}),
 *                   {@code null} for the default
 * @param parameters values by parameter name, in the order of {@link CIServer#loadParameters(CIJob)}
 * @since 2026-11-09
 */
public record CIRunRequest(@Nullable String ref, Map<String, CIParameterValue> parameters) {
    public static final CIRunRequest DEFAULT = new CIRunRequest(null, Map.of());

    public CIRunRequest {
        parameters = new LinkedHashMap<>(parameters);
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }
}
