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

import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewRangedInlayModel;
import consulo.review.editor.CodeReviewThreadInlayModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewThreadInlayModel implements CodeReviewThreadInlayModel, CodeReviewRangedInlayModel {
    private final SandReviewThreadViewModel myThread;
    private final ObservableValue<Integer> myLine;
    private final ObservableValue<CodeReviewLineRange> myRange;

    public SandReviewThreadInlayModel(SandReviewThreadViewModel thread) {
        myThread = thread;
        myLine = ObservableValue.of(thread.getLine());
        myRange = ObservableValue.of(new CodeReviewLineRange(thread.getStartLine(), thread.getLine()));
    }

    @Override
    public ObservableValue<CodeReviewLineRange> getRange() {
        return myRange;
    }

    @Override
    public Object getKey() {
        return myThread.getTrackingId();
    }

    @Override
    public ObservableValue<Integer> getLine() {
        return myLine;
    }

    @Override
    public ObservableValue<Boolean> isVisible() {
        return myThread.getShownInEditor();
    }

    public void toggleVisible() {
        myThread.getShownInEditor().update(visible -> !visible);
    }

    @Override
    public CodeReviewThreadViewModel getThread() {
        return myThread;
    }
}
