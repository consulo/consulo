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
package consulo.sandboxPlugin.ide.ai;

import consulo.ai.AIChatInfo;
import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Generated chats of the Sand provider - long conversations of every kind of markdown and tool traffic, to try how the
 * chat renders and scrolls a big history.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
final class SandChats {
    static final String LONG_500 = "sand-long-500";
    static final String LONG_2000 = "sand-long-2000";
    static final String MARKDOWN = "sand-markdown";

    static List<AIChatInfo> list() {
        Instant now = Instant.now();
        return List.of(
            new AIChatInfo(LONG_500, "Sand: long conversation (500 messages)", now.minus(1, ChronoUnit.MINUTES), null),
            new AIChatInfo(LONG_2000, "Sand: very long conversation (2000 messages)", now.minus(2, ChronoUnit.MINUTES), null),
            new AIChatInfo(MARKDOWN, "Sand: markdown showcase", now.minus(3, ChronoUnit.MINUTES), null)
        );
    }

    static List<AIMessage> load(String chatId) {
        return switch (chatId) {
            case LONG_500 -> conversation(500);
            case LONG_2000 -> conversation(2000);
            case MARKDOWN -> List.of(AIMessage.user("Show every kind of markdown"), AIMessage.assistant(markdown(1)));
            default -> List.of();
        };
    }

    /**
     * Turns of a question, an answer of markdown, and every few turns a tool call with its result.
     */
    private static List<AIMessage> conversation(int count) {
        List<AIMessage> messages = new ArrayList<>(count);
        for (int i = 0; messages.size() < count; i++) {
            messages.add(AIMessage.user("Question " + (i + 1) + ": how does part " + (i % 17) + " of the project work?"));
            if (i % 4 == 3) {
                String id = "sand-tool-" + i;
                messages.add(AIMessage.of(AIRole.ASSISTANT, AIContent.text("Let me look at the sources."),
                    AIContent.toolUse(id, "read_file", "{\"path\":\"src/Part" + i + ".java\"}")));
                messages.add(AIMessage.of(AIRole.USER, AIContent.toolResult(id, "class Part" + i + " {}", false)));
            }
            messages.add(AIMessage.assistant(i % 3 == 0 ? markdown(i) : paragraph(i)));
        }
        return messages.subList(0, count);
    }

    static String paragraph(int i) {
        return "Part **" + i + "** keeps its state in a map and hands it to the `Renderer` once the layout is done. "
            + "It reads the configuration lazily, so the first call is the slow one, and the rest come from the cache. "
            + "See [the docs](https://consulo.io) for the details.";
    }

    static String markdown(int i) {
        return "## Answer " + i + "\n\n"
            + "The part works in three steps:\n\n"
            + "1. it **parses** the input,\n"
            + "2. it *validates* it against the schema,\n"
            + "3. it writes the result with `Writer.flush()`.\n\n"
            + "- a bullet with `inline code`\n"
            + "- a bullet with a [link](https://consulo.io)\n"
            + "  - a nested bullet\n\n"
            + "```java\n"
            + "public final class Part" + i + " {\n"
            + "    private final Map<String, Object> myState = new HashMap<>();\n\n"
            + "    public Object get(String key) {\n"
            + "        return myState.computeIfAbsent(key, this::load);\n"
            + "    }\n"
            + "}\n"
            + "```\n\n"
            + "| Step | Time | Result |\n"
            + "|------|------|--------|\n"
            + "| parse | 12 ms | ok |\n"
            + "| validate | 3 ms | ok |\n"
            + "| write | 40 ms | **slow** |\n\n"
            + "> A quote: the write is the slow step, the cache does not help it.\n";
    }

    private SandChats() {
    }
}
