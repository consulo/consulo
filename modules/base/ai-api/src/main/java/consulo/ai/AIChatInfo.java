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

import java.nio.file.Path;
import java.time.Instant;

/**
 * A chat as a provider which keeps its chats itself lists it - see {@link AIProviderTypeWithChats}.
 *
 * @param id               the {@linkplain AIChatSession#getId() id} of the conversation
 * @param title            what the provider calls the chat, empty when it has no title
 * @param updated          when the chat last changed
 * @param workingDirectory where the chat worked, when the provider knows it
 * @author VISTALL
 * @since 2026-11-10
 */
public record AIChatInfo(String id, String title, Instant updated, @Nullable Path workingDirectory) {
}
