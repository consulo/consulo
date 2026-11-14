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
package consulo.ai.impl.internal.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIRole;
import consulo.ai.AIToolCallContent;
import consulo.ai.AIToolCallStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * The history of a chat as the file our store keeps it in - every block of every message, the tool traffic too, so a
 * provider without a memory of its own gets the whole conversation sent again.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
final class AIChatTranscript {
    private static final int VERSION = 1;

    static String write(List<AIMessage> messages) {
        JsonArray array = new JsonArray();
        for (AIMessage message : messages) {
            JsonObject object = new JsonObject();
            object.addProperty("role", message.role().name());
            JsonArray content = new JsonArray();
            for (AIContent block : message.content()) {
                content.add(write(block));
            }
            object.add("content", content);
            array.add(object);
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("messages", array);
        return root.toString();
    }

    private static JsonObject write(AIContent block) {
        JsonObject object = new JsonObject();
        switch (block) {
            case AIContent.Text text -> {
                object.addProperty("type", "text");
                object.addProperty("text", text.text());
            }
            case AIContent.Image image -> {
                object.addProperty("type", "image");
                object.addProperty("mediaType", image.mediaType());
                object.addProperty("data", Base64.getEncoder().encodeToString(image.data()));
            }
            case AIContent.ToolUse toolUse -> {
                object.addProperty("type", "toolUse");
                object.addProperty("id", toolUse.id());
                object.addProperty("name", toolUse.toolName());
                object.addProperty("arguments", toolUse.argumentsJson());
            }
            case AIContent.ToolResult result -> {
                object.addProperty("type", "toolResult");
                object.addProperty("toolUseId", result.toolUseId());
                object.addProperty("content", result.content());
                object.addProperty("error", result.error());
            }
            case AIContent.Thinking thinking -> {
                object.addProperty("type", "thinking");
                object.addProperty("text", thinking.text());
            }
            case AIContent.ResourceLink link -> {
                object.addProperty("type", "resourceLink");
                object.addProperty("uri", link.uri());
                object.addProperty("name", link.name());
                object.addProperty("mimeType", link.mimeType());
            }
            case AIContent.Resource resource -> {
                object.addProperty("type", "resource");
                object.addProperty("uri", resource.uri());
                object.addProperty("mimeType", resource.mimeType());
                object.addProperty("text", resource.text());
            }
            case AIContent.ToolCall toolCall -> {
                object.addProperty("type", "toolCall");
                object.addProperty("id", toolCall.id());
                object.addProperty("parentId", toolCall.parentId());
                object.addProperty("title", toolCall.title());
                object.addProperty("kind", toolCall.kind());
                object.addProperty("status", toolCall.status().name());
                JsonArray locations = new JsonArray();
                toolCall.locations().forEach(locations::add);
                object.add("locations", locations);
                JsonArray content = new JsonArray();
                for (AIToolCallContent part : toolCall.content()) {
                    content.add(write(part));
                }
                object.add("content", content);
                object.addProperty("input", toolCall.inputJson());
                object.addProperty("output", toolCall.outputJson());
            }
        }
        return object;
    }

    private static JsonObject write(AIToolCallContent part) {
        JsonObject object = new JsonObject();
        switch (part) {
            case AIToolCallContent.Text text -> {
                object.addProperty("type", "text");
                object.addProperty("text", text.text());
            }
            case AIToolCallContent.Diff diff -> {
                object.addProperty("type", "diff");
                object.addProperty("path", diff.path());
                object.addProperty("oldText", diff.oldText());
                object.addProperty("newText", diff.newText());
            }
            case AIToolCallContent.Terminal terminal -> {
                object.addProperty("type", "terminal");
                object.addProperty("terminalId", terminal.terminalId());
            }
            case AIToolCallContent.Image image -> {
                object.addProperty("type", "image");
                object.addProperty("mediaType", image.mediaType());
                object.addProperty("data", Base64.getEncoder().encodeToString(image.data()));
            }
        }
        return object;
    }

    static List<AIMessage> read(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<AIMessage> messages = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("messages")) {
            JsonObject object = element.getAsJsonObject();
            AIRole role = AIRole.valueOf(object.get("role").getAsString());
            List<AIContent> content = new ArrayList<>();
            for (JsonElement block : object.getAsJsonArray("content")) {
                AIContent value = read(block.getAsJsonObject());
                if (value != null) {
                    content.add(value);
                }
            }
            messages.add(new AIMessage(role, content));
        }
        return messages;
    }

    private static @Nullable AIContent read(JsonObject object) {
        return switch (string(object, "type")) {
            case "text" -> AIContent.text(string(object, "text"));
            case "image" -> AIContent.image(string(object, "mediaType"), Base64.getDecoder().decode(string(object, "data")));
            case "toolUse" -> AIContent.toolUse(string(object, "id"), string(object, "name"), string(object, "arguments"));
            case "toolResult" -> AIContent.toolResult(
                string(object, "toolUseId"),
                string(object, "content"),
                object.has("error") && object.get("error").getAsBoolean()
            );
            case "thinking" -> AIContent.thinking(string(object, "text"));
            case "resourceLink" -> AIContent.resourceLink(string(object, "uri"), string(object, "name"), nullable(object, "mimeType"));
            case "resource" -> AIContent.resource(string(object, "uri"), nullable(object, "mimeType"), string(object, "text"));
            case "toolCall" -> toolCall(object);
            default -> null;
        };
    }

    private static AIContent.ToolCall toolCall(JsonObject object) {
        List<String> locations = new ArrayList<>();
        JsonElement locationsElement = object.get("locations");
        if (locationsElement != null && locationsElement.isJsonArray()) {
            locationsElement.getAsJsonArray().forEach(location -> locations.add(location.getAsString()));
        }
        List<AIToolCallContent> content = new ArrayList<>();
        JsonElement contentElement = object.get("content");
        if (contentElement != null && contentElement.isJsonArray()) {
            for (JsonElement element : contentElement.getAsJsonArray()) {
                JsonObject part = element.getAsJsonObject();
                switch (string(part, "type")) {
                    case "text" -> content.add(new AIToolCallContent.Text(string(part, "text")));
                    case "diff" -> content.add(new AIToolCallContent.Diff(string(part, "path"), nullable(part, "oldText"), string(part, "newText")));
                    case "terminal" -> content.add(new AIToolCallContent.Terminal(string(part, "terminalId")));
                    case "image" -> content.add(new AIToolCallContent.Image(string(part, "mediaType"), Base64.getDecoder().decode(string(part, "data"))));
                    default -> {
                    }
                }
            }
        }
        AIToolCallStatus status;
        try {
            status = AIToolCallStatus.valueOf(string(object, "status"));
        }
        catch (IllegalArgumentException e) {
            status = AIToolCallStatus.COMPLETED;
        }
        return new AIContent.ToolCall(
            string(object, "id"),
            nullable(object, "parentId"),
            string(object, "title"),
            string(object, "kind"),
            status,
            List.copyOf(locations),
            List.copyOf(content),
            string(object, "input"),
            string(object, "output")
        );
    }

    private static @Nullable String nullable(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private AIChatTranscript() {
    }
}
