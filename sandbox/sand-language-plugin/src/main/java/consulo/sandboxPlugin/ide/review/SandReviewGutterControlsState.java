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
package consulo.sandboxPlugin.ide.review;

import consulo.review.editor.CodeReviewEditorGutterControlsState;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public record SandReviewGutterControlsState(Set<Integer> linesWithComments, Set<Integer> linesWithNewComments)
    implements CodeReviewEditorGutterControlsState {
    @Override
    public Set<Integer> getLinesWithComments() {
        return linesWithComments;
    }

    @Override
    public Set<Integer> getLinesWithNewComments() {
        return linesWithNewComments;
    }

    @Override
    public boolean isLineCommentable(int lineIdx) {
        return true;
    }
}
