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
package consulo.versionControlSystem.review.comment;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
final class GitLabSuggestionSyntax implements CodeReviewSuggestionSyntax {
    private static final Pattern START = Pattern.compile("^```suggestion(?::-(\\d+)\\+(\\d+))?\\s*$");

    @Override
    public boolean isSuggestionStart(String line) {
        return START.matcher(line).matches();
    }

    @Override
    public boolean isSuggestionEnd(String line) {
        return line.endsWith("```");
    }

    @Override
    public int getLinesAbove(String startLine) {
        return group(startLine, 1);
    }

    @Override
    public int getLinesBelow(String startLine) {
        return group(startLine, 2);
    }

    private static int group(String startLine, int group) {
        Matcher matcher = START.matcher(startLine);
        if (!matcher.matches() || matcher.group(group) == null) {
            return 0;
        }
        return Integer.parseInt(matcher.group(group));
    }

    @Override
    public String createSuggestion(List<String> lines) {
        return "```suggestion:-0+0\n" + String.join("\n", lines) + "\n```";
    }
}
