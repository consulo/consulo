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

import consulo.diff.util.Side;
import consulo.review.editor.CodeReviewLineRange;
import consulo.review.editor.CodeReviewRangedInlayModel;
import consulo.review.editor.CodeReviewThreadInlayModel;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.diff.DiffEditorLineMapper;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewThreadInlayModel implements CodeReviewThreadInlayModel, CodeReviewRangedInlayModel {
    private final SandReviewThreadViewModel myThread;
    private final ObservableValue<@Nullable Integer> myLine;
    private final ObservableValue<CodeReviewLineRange> myRange;

    public SandReviewThreadInlayModel(SandReviewThreadViewModel thread) {
        this(thread, null);
    }

    public SandReviewThreadInlayModel(SandReviewThreadViewModel thread, @Nullable DiffEditorLineMapper lineMapper) {
        myThread = thread;
        Integer line = toEditorLine(lineMapper, thread.getLine());
        Integer startLine = toEditorLine(lineMapper, thread.getStartLine());
        myLine = ObservableValue.of(line);
        myRange = ObservableValue.of(line == null
            ? new CodeReviewLineRange(thread.getStartLine(), thread.getLine())
            : new CodeReviewLineRange(startLine == null ? line : startLine, line));
    }

    private static @Nullable Integer toEditorLine(@Nullable DiffEditorLineMapper lineMapper, int line) {
        return lineMapper == null ? Integer.valueOf(line) : lineMapper.locationToLine(new DiffLineLocation(Side.RIGHT, line));
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
    public ObservableValue<@Nullable Integer> getLine() {
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
