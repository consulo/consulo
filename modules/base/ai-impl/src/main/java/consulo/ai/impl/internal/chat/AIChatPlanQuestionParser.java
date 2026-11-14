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

import consulo.ai.AIQuestion;
import consulo.ai.AIQuestionOption;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recovers the choices a plan-mode agent writes as prose - {@code A) ...}, {@code 1. ...} - so the chat can offer them as
 * a question to answer. Conservative: a run of lines counts only when its markers go on one by one from {@code A} or
 * {@code 1}, and a letter run, a cue to choose or a question before it says it is a choice, not an ordinary list.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIChatPlanQuestionParser {
    private static final Pattern OPTION = Pattern.compile("^\\s*(?:[-*+]\\s+)?\\**\\s*([A-Za-z]|\\d{1,2})\\s*\\**\\s*[.)]\\s*\\**\\s*(\\S.*?)\\s*$");
    private static final List<String> SELECTION_CUES = List.of(
        "reply with",
        "pick one",
        "pick a",
        "choose one",
        "select one",
        "rank them",
        "do you prefer",
        "would you prefer",
        "which option",
        "let me know",
        "your call"
    );
    private static final int INDENT_TOLERANCE = 1;
    private static final int MAX_PROMPT_LOOKBACK = 3;

    private AIChatPlanQuestionParser() {
    }

    static List<AIQuestion> parse(String text) {
        if (text.isBlank()) {
            return List.of();
        }
        String[] lines = text.split("\\R", -1);
        String lower = text.toLowerCase(Locale.ROOT);
        boolean hasCue = SELECTION_CUES.stream().anyMatch(lower::contains);
        List<AIQuestion> questions = new ArrayList<>();

        int i = 0;
        while (i < lines.length) {
            AIChatPlanQuestionOption first = match(lines[i]);
            if (first == null) {
                i++;
                continue;
            }
            int baseIndent = indent(lines[i]);
            List<AIChatPlanQuestionOption> run = new ArrayList<>();
            run.add(first);
            int lastConsumed = i;
            int j = i + 1;
            while (j < lines.length) {
                String line = lines[j];
                if (line.isBlank()) {
                    j++;
                    continue;
                }
                int indent = indent(line);
                AIChatPlanQuestionOption option = match(line);
                if (option != null && indent <= baseIndent + INDENT_TOLERANCE && isNext(run.getLast().marker(), option.marker())) {
                    run.add(option);
                    lastConsumed = j;
                    j++;
                }
                else if (indent > baseIndent) {
                    j++;
                }
                else {
                    break;
                }
            }

            if (run.size() >= 2 && isHead(run.getFirst().marker())) {
                String prompt = prompt(lines, i);
                if (isLetterRun(run) || hasCue || looksLikeQuestion(prompt)) {
                    List<AIQuestionOption> options = new ArrayList<>();
                    for (AIChatPlanQuestionOption option : run) {
                        options.add(new AIQuestionOption(option.marker() + ") " + option.label(), ""));
                    }
                    String question = prompt != null ? prompt : "#" + (questions.size() + 1);
                    questions.add(new AIQuestion(question, "", options, false));
                }
            }
            i = lastConsumed + 1;
        }
        return questions;
    }

    private static @Nullable AIChatPlanQuestionOption match(String line) {
        Matcher matcher = OPTION.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        String label = stripMarkdown(matcher.group(2));
        return label.isBlank() ? null : new AIChatPlanQuestionOption(matcher.group(1), label);
    }

    private static boolean isHead(String marker) {
        return marker.equalsIgnoreCase("a") || marker.equals("1");
    }

    private static boolean isLetterRun(List<AIChatPlanQuestionOption> run) {
        return run.stream().allMatch(option -> option.marker().length() == 1 && Character.isLetter(option.marker().charAt(0)));
    }

    private static boolean isNext(String previous, String current) {
        if (previous.length() == 1 && Character.isLetter(previous.charAt(0)) && current.length() == 1 && Character.isLetter(current.charAt(0))) {
            return Character.toLowerCase(current.charAt(0)) == Character.toLowerCase(previous.charAt(0)) + 1;
        }
        try {
            return Integer.parseInt(current) == Integer.parseInt(previous) + 1;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static @Nullable String prompt(String[] lines, int runStart) {
        int seen = 0;
        for (int index = runStart - 1; index >= 0 && seen < MAX_PROMPT_LOOKBACK; index--) {
            String line = lines[index];
            if (line.isBlank()) {
                continue;
            }
            seen++;
            // a numbered question over lettered choices - "1. Where?" then "A) ..."
            AIChatPlanQuestionOption numbered = match(line);
            if (numbered != null && Character.isDigit(numbered.marker().charAt(0)) && Character.isLetter(lines[runStart].strip().replace("*", "").charAt(0))) {
                return numbered.label();
            }
            if (numbered == null) {
                String trimmed = line.strip();
                if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                    trimmed = trimmed.substring(1);
                }
                String cleaned = stripMarkdown(trimmed);
                return cleaned.isBlank() ? null : cleaned;
            }
        }
        return null;
    }

    private static boolean looksLikeQuestion(@Nullable String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        if (prompt.stripTrailing().endsWith("?")) {
            return true;
        }
        String lower = prompt.toLowerCase(Locale.ROOT);
        return SELECTION_CUES.stream().anyMatch(lower::contains);
    }

    private static String stripMarkdown(String raw) {
        String text = raw.replace("**", "").replace("`", "").strip();
        while (text.startsWith("*")) {
            text = text.substring(1);
        }
        while (text.endsWith("*")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.strip();
    }

    private static int indent(String line) {
        int count = 0;
        while (count < line.length() && (line.charAt(count) == ' ' || line.charAt(count) == '\t')) {
            count++;
        }
        return count;
    }
}
