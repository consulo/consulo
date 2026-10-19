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

/**
 * How the comments of a review platform write a suggested change: the opening and closing lines of the block that holds
 * the suggested lines.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public interface CodeReviewSuggestionSyntax {
    /**
     * A {@code suggestion} code fence replacing the commented lines.
     */
    CodeReviewSuggestionSyntax GITHUB = new GitHubSuggestionSyntax();

    /**
     * A {@code suggestion:-N+M} code fence replacing the commented lines together with {@code N} lines above and
     * {@code M} lines below them.
     */
    CodeReviewSuggestionSyntax GITLAB = new GitLabSuggestionSyntax();

    boolean isSuggestionStart(String line);

    boolean isSuggestionEnd(String line);

    /**
     * @param startLine the opening line of the suggestion
     * @return how many lines above the commented ones the suggestion replaces too
     */
    default int getLinesAbove(String startLine) {
        return 0;
    }

    /**
     * @param startLine the opening line of the suggestion
     * @return how many lines below the commented ones the suggestion replaces too
     */
    default int getLinesBelow(String startLine) {
        return 0;
    }

    /**
     * A suggestion holding the lines as they are, for a comment to start from.
     */
    String createSuggestion(List<String> lines);

    default boolean containsSuggestedChange(String markdownText) {
        for (String line : markdownText.split("\n", -1)) {
            if (isSuggestionStart(line)) {
                return true;
            }
        }
        return false;
    }
}
