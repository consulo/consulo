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
 * A command the user can type as {@code /name} in a chat - one of the agent, or a skill it found.
 *
 * @param inputHint what to type after the name, null when the command takes nothing
 * @author VISTALL
 * @since 2026-11-13
 */
public record AICommand(String name, String description, @Nullable String inputHint) {
}
