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

import java.util.regex.Pattern;

/**
 * Context sent in front of a prompt but never shown in the chat - the rules the user gave every agent and the
 * instructions of a mode of the chat. Each goes in a tagged block which {@link #strip} takes out of the text again.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class AIChatHiddenContext {
    private static final String RULES_TAG = "user_rules";
    private static final String RULES_INTRO = "Global user rules - always apply them, they are hidden from the chat:";
    private static final String MODE_TAG = "mode_instructions";
    private static final Pattern BLOCK = Pattern.compile("<(" + RULES_TAG + "|" + MODE_TAG + ")>.*?</\\1>\\s*", Pattern.DOTALL);

    private AIChatHiddenContext() {
    }

    /**
     * The block of the rules of the user - null for no rules.
     */
    public static @Nullable String userRules(String rules) {
        String text = rules.strip();
        return text.isEmpty() ? null : block(RULES_TAG, RULES_INTRO + "\n\n" + text);
    }

    /**
     * The block of the instructions of the mode the chat is in.
     */
    public static String modeInstructions(String instructions) {
        return block(MODE_TAG, instructions.strip());
    }

    /**
     * The text as the user wrote it - without the hidden blocks it was sent with.
     */
    public static String strip(String text) {
        return text.contains("</" + RULES_TAG + ">") || text.contains("</" + MODE_TAG + ">") ? BLOCK.matcher(text).replaceAll("") : text;
    }

    private static String block(String tag, String text) {
        return "<" + tag + ">\n" + text + "\n</" + tag + ">";
    }
}
