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
            default -> null;
        };
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private AIChatTranscript() {
    }
}
