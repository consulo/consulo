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

import java.util.List;

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

    static Thinking thinking(String text) {
        return new Thinking(text);
    }

    static ResourceLink resourceLink(String uri, String name, @Nullable String mimeType) {
        return new ResourceLink(uri, name, mimeType);
    }

    static Resource resource(String uri, @Nullable String mimeType, String text) {
        return new Resource(uri, mimeType, text);
    }

    record Text(String text) implements AIContent {
    }

    /**
     * What the model reasoned before it answered, when it tells it.
     */
    record Thinking(String text) implements AIContent {
    }

    /**
     * A file or another resource the message points to - the provider reads it itself.
     *
     * @param uri for example {@code file:///home/user/project/Main.java}
     */
    record ResourceLink(String uri, String name, @Nullable String mimeType) implements AIContent {
    }

    /**
     * A resource the message carries with its text - a selection of a file, for example.
     */
    record Resource(String uri, @Nullable String mimeType, String text) implements AIContent {
    }

    /**
     * A tool an agent ran on its own side - unlike a {@link ToolUse}, it is not answered by the platform. A provider
     * reports the call again whenever it changed, and the call with the same {@code id} replaces the one before.
     *
     * @param parentId  the call of a subagent this call was made by, null for a call of the agent itself
     * @param kind      what the call does, as the agent calls it - {@code read}, {@code edit}, {@code execute},
     *                  {@code search}, {@code fetch}, {@code task} for a subagent, ...
     * @param locations the files the call works on
     * @param inputJson  the arguments of the call as a JSON object
     * @param outputJson what the call returned as a JSON value - the exit code and the output of a command an agent
     *                   ran itself, for one - empty while it returned nothing
     */
    record ToolCall(String id,
                    @Nullable String parentId,
                    String title,
                    String kind,
                    AIToolCallStatus status,
                    List<String> locations,
                    List<AIToolCallContent> content,
                    String inputJson,
                    String outputJson) implements AIContent {
        public static final String KIND_TASK = "task";
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
