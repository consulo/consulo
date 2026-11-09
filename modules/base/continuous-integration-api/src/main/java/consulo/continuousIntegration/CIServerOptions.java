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

import com.dslplatform.json.CompiledJson;
import org.jspecify.annotations.Nullable;

/**
 * Per server settings. Subclasses add provider fields as public fields with defaults; credentials never go here, they
 * live in the account.
 *
 * @since 2026-11-09
 */
@CompiledJson
public class CIServerOptions {
    public @Nullable String accountId;
    public String serverUrl = "";
}
