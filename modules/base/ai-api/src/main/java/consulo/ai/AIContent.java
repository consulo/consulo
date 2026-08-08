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

/**
 * One block of a message. A message is a list of these rather than a plain string, because tool use
 * and images have to travel alongside the text in the same turn.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public sealed interface AIContent {
    static Text text(String text) {
        return new Text(text);
    }

    static Image image(String mediaType, byte[] data) {
        return new Image(mediaType, data);
    }

    static ToolUse toolUse(String id, String toolName, String argumentsJson) {
        return new ToolUse(id, toolName, argumentsJson);
    }

    static ToolResult toolResult(String toolUseId, String content, boolean error) {
        return new ToolResult(toolUseId, content, error);
    }

    record Text(String text) implements AIContent {
    }

    /**
     * @param mediaType for example {@code image/png}
     */
    record Image(String mediaType, byte[] data) implements AIContent {
    }

    /**
     * The model asking for a tool to be run.
     *
     * @param id            correlates this request with its {@link ToolResult}
     * @param argumentsJson arguments as a JSON object, matching the tool's input schema
     */
    record ToolUse(String id, String toolName, String argumentsJson) implements AIContent {
    }

    /**
     * The answer to a {@link ToolUse}, sent back as part of the next user turn.
     */
    record ToolResult(String toolUseId, String content, boolean error) implements AIContent {
    }
}
