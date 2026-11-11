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
package consulo.ai.impl.internal.setting.state;

import com.dslplatform.json.CompiledJson;

import java.util.ArrayList;
import java.util.List;

/**
 * The configured instances and the default selection. API keys are never stored here - they live in the password
 * safe, see {@link consulo.ai.AICredentials}.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@CompiledJson
public class AISettingsState {
    public List<ProviderEntry> PROVIDERS = new ArrayList<>();
    public String DEFAULT_PROVIDER_NAME = "";
    public String DEFAULT_MODEL_ID = "";
}
