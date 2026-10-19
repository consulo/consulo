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
 * @author VISTALL
 * @since 2026-10-19
 */
final class GitHubSuggestionSyntax implements CodeReviewSuggestionSyntax {
    static final String SUGGESTION_BLOCK_START = "```suggestion";
    static final String SUGGESTION_BLOCK_END = "```";

    @Override
    public boolean isSuggestionStart(String line) {
        return line.startsWith(SUGGESTION_BLOCK_START);
    }

    @Override
    public boolean isSuggestionEnd(String line) {
        return line.endsWith(SUGGESTION_BLOCK_END);
    }

    @Override
    public String createSuggestion(List<String> lines) {
        return SUGGESTION_BLOCK_START + "\n" + String.join("\n", lines) + "\n" + SUGGESTION_BLOCK_END;
    }
}
