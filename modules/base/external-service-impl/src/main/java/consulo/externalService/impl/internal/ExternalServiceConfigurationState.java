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
package consulo.externalService.impl.internal;

import com.dslplatform.json.CompiledJson;
import consulo.externalService.ExternalService;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@CompiledJson
public class ExternalServiceConfigurationState {
    public @Nullable String email;
    public @Nullable String oauthKey;

    public Map<ExternalService, ThreeState> states = new LinkedHashMap<>();
}
