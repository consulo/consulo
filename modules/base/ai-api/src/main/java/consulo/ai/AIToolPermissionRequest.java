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
package consulo.ai;

import org.jspecify.annotations.Nullable;

/**
 * The model wants to run a tool that needs the user's consent first.
 *
 * @param toolUseId     correlates the request with the tool call in the conversation
 * @param toolName      the tool as the model calls it
 * @param displayName   the tool as it is shown to the user
 * @param description   what the call is about to do, when the provider can tell
 * @param argumentsJson arguments as a JSON object
 * @author VISTALL
 * @since 2026-10-13
 */
public record AIToolPermissionRequest(String toolUseId, String toolName, String displayName, @Nullable String description, String argumentsJson) {
}
