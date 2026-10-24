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

import consulo.review.diff.DiscussionsViewOption;
import consulo.review.editor.CodeReviewInEditorViewModel;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;

/**
 * The review mode of the sand demo editor; the branch is always in sync.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class SandReviewInEditorViewModel implements CodeReviewInEditorViewModel {
    private final MutableObservableValue<DiscussionsViewOption> myDiscussionsViewOption = MutableObservableValue.of(DiscussionsViewOption.ALL);
    private final MutableObservableValue<Boolean> myUpdateRequired = MutableObservableValue.of(false);

    @Override
    public ObservableValue<DiscussionsViewOption> getDiscussionsViewOption() {
        return myDiscussionsViewOption;
    }

    @Override
    public void setDiscussionsViewOption(DiscussionsViewOption viewOption) {
        myDiscussionsViewOption.set(viewOption);
    }

    @Override
    public ObservableValue<Boolean> getUpdateRequired() {
        return myUpdateRequired;
    }

    @Override
    public void updateBranch() {
        myUpdateRequired.set(false);
    }
}
