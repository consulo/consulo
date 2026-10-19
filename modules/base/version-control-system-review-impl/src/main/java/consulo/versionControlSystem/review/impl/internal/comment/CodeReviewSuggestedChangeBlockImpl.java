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
package consulo.versionControlSystem.review.impl.internal.comment;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.patch.TextFilePatch;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeBlock;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionApplicability;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
final class CodeReviewSuggestedChangeBlockImpl implements CodeReviewSuggestedChangeBlock {
    private final CodeReviewCommentBodyViewModelImpl myViewModel;
    private final @Nullable TextFilePatch myPatch;
    private final CodeReviewSuggestionApplicability myApplicability;
    private final List<String> myChangedLines;
    private final List<String> mySuggestedLines;

    CodeReviewSuggestedChangeBlockImpl(CodeReviewCommentBodyViewModelImpl viewModel,
                                       @Nullable TextFilePatch patch,
                                       CodeReviewSuggestionApplicability applicability,
                                       List<String> changedLines,
                                       List<String> suggestedLines) {
        myViewModel = viewModel;
        myPatch = patch;
        myApplicability = applicability;
        myChangedLines = changedLines;
        mySuggestedLines = suggestedLines;
    }

    @Override
    public @Nullable TextFilePatch getPatch() {
        return myPatch;
    }

    @Override
    public CodeReviewSuggestionApplicability getApplicability() {
        return myApplicability;
    }

    @Override
    public List<String> getChangedLines() {
        return myChangedLines;
    }

    @Override
    public List<String> getSuggestedLines() {
        return mySuggestedLines;
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable parentDisposable) {
        return CodeReviewSuggestedChangeComponentFactory.createIn(parentDisposable, myViewModel, this);
    }
}
