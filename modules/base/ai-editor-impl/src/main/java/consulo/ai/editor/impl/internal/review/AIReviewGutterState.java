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
package consulo.ai.editor.impl.internal.review;

import consulo.review.editor.CodeReviewEditorGutterControlsState;

import java.util.Set;

/**
 * Every line of a plan takes a comment - the lines with one get the mark of it in the gutter.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
record AIReviewGutterState(Set<Integer> commented, Set<Integer> newComments) implements CodeReviewEditorGutterControlsState {
    @Override
    public Set<Integer> getLinesWithComments() {
        return commented;
    }

    @Override
    public Set<Integer> getLinesWithNewComments() {
        return newComments;
    }

    @Override
    public boolean isLineCommentable(int lineIdx) {
        return true;
    }
}
