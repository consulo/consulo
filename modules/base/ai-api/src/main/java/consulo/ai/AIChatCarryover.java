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

import java.util.ArrayList;
import java.util.List;

/**
 * What a new session of a provider is told of the chat it continues - the end of the conversation, as text in front of
 * the prompt. An agent which keeps its own sessions has no way to take a history handed over.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class AIChatCarryover {
    private static final int MAX_MESSAGES = 16;
    private static final int MAX_CHARS = 12_000;

    private static final String HEADER = "Previous conversation context restored from local transcript (server session not recovered):";
    private static final String INTRO = "Context from a previous chat:";
    private static final String TRUNCATED = "[Transcript truncated to fit context budget.]";
    private static final String CURRENT = "Current prompt:";

    private AIChatCarryover() {
    }

    /**
     * @param messages the whole chat, the prompt to send as its last message - which is not part of the carryover
     * @return null when nothing was said before the prompt
     */
    public static @Nullable String build(List<AIMessage> messages) {
        int end = messages.size();
        if (end > 0 && messages.get(end - 1).role() == AIRole.USER) {
            end--;
        }
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            AIMessage message = messages.get(i);
            String text = text(message).strip();
            if (text.isEmpty()) {
                continue;
            }
            String role = message.role() == AIRole.ASSISTANT ? "Assistant" : message.role() == AIRole.USER ? "User" : "System";
            entries.add("[" + role + "]\n" + text + "\n\n");
        }
        if (entries.isEmpty()) {
            return null;
        }
        List<String> selected = entries.subList(Math.max(0, entries.size() - MAX_MESSAGES), entries.size());

        StringBuilder builder = new StringBuilder(HEADER).append('\n');
        int remaining = MAX_CHARS - builder.length();
        boolean truncated = false;
        for (String entry : selected) {
            if (entry.length() <= remaining) {
                builder.append(entry);
                remaining -= entry.length();
                continue;
            }
            if (remaining > 40) {
                builder.append(entry, 0, remaining);
            }
            truncated = true;
            break;
        }
        if (truncated) {
            builder.append('\n').append(TRUNCATED);
        }
        return INTRO + "\n\n" + builder.toString().strip() + "\n\n" + CURRENT;
    }

    /**
     * The history as the user wrote it - a prompt which carried the chat over to a new session loses the carryover.
     */
    public static List<AIMessage> strip(List<AIMessage> history) {
        List<AIMessage> result = new ArrayList<>(history.size());
        for (AIMessage message : history) {
            if (message.role() != AIRole.USER) {
                result.add(message);
                continue;
            }
            List<AIContent> content = new ArrayList<>(message.content().size());
            for (AIContent part : message.content()) {
                content.add(part instanceof AIContent.Text text ? AIContent.text(strip(text.text())) : part);
            }
            result.add(new AIMessage(message.role(), content));
        }
        return result;
    }

    private static String strip(String text) {
        if (!text.startsWith(INTRO)) {
            return text;
        }
        int current = text.indexOf(CURRENT);
        return current < 0 ? text : text.substring(current + CURRENT.length()).stripLeading();
    }

    /**
     * What the message says - its text, without thinking, tool calls or images.
     */
    private static String text(AIMessage message) {
        StringBuilder text = new StringBuilder();
        for (AIContent content : message.content()) {
            if (content instanceof AIContent.Text part) {
                text.append(AIChatHiddenContext.strip(part.text()));
            }
        }
        return text.toString();
    }
}
