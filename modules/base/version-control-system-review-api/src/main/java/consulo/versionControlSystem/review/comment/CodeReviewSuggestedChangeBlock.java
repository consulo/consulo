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

import consulo.review.comment.CodeReviewComponentCommentBodyBlock;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A suggested change of a comment body: the lines it replaces, the lines it suggests and the patch applying them.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public interface CodeReviewSuggestedChangeBlock extends CodeReviewComponentCommentBodyBlock {
    /**
     * @return the patch replacing the commented lines by the suggested ones, {@code null} when the suggestion cannot
     * be applied any more
     */
    @Nullable TextFilePatch getPatch();

    CodeReviewSuggestionApplicability getApplicability();

    /**
     * @return the commented lines the suggestion replaces
     */
    List<String> getChangedLines();

    List<String> getSuggestedLines();
}
