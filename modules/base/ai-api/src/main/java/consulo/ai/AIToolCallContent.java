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
 * What a {@linkplain AIContent.ToolCall tool call} of an agent produced - text, a change of a file, or the output of a
 * terminal the agent runs.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public sealed interface AIToolCallContent {
    record Text(String text) implements AIToolCallContent {
    }

    /**
     * @param oldText the text before, null for a file the call created
     */
    record Diff(String path, @Nullable String oldText, String newText) implements AIToolCallContent {
    }

    /**
     * A picture the call produced - a screenshot it took, for one.
     *
     * @param mediaType for example {@code image/png}
     */
    record Image(String mediaType, byte[] data) implements AIToolCallContent {
    }

    /**
     * The output of a terminal of the agent, which lives while the agent keeps it.
     */
    record Terminal(String terminalId) implements AIToolCallContent {
    }
}
