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
 * How much of its context window a session filled, and what it cost so far.
 *
 * @param used     tokens in the context now
 * @param size     the size of the context window, in tokens
 * @param cost     the cost of the session so far, null when the agent tells none
 * @param currency the currency of the cost, an ISO 4217 code such as {@code USD}
 * @author VISTALL
 * @since 2026-11-13
 */
public record AIContextUsage(long used, long size, @Nullable Double cost, @Nullable String currency) {
}
