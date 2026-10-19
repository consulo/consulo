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
package consulo.versionControlSystem.review.timeline;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.diff.util.LineRange;
import consulo.disposer.Disposable;
import consulo.review.timeline.CollapsibleTimelineItemViewModel;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.patch.PatchHunk;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * A read-only editor showing a {@link PatchHunk} as a unified diff - the code a review thread or a suggested change is
 * about.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface TimelineDiffComponentFactory {
    int DIFF_CONTEXT_SIZE = 3;

    /**
     * Shows the hunk around the anchor only - {@link #DIFF_CONTEXT_SIZE} lines before its start and after its end -
     * with the anchor lines highlighted.
     */
    @RequiredUIAccess
    Component createDiffComponent(Disposable parentDisposable,
                                  PatchHunk patchHunk,
                                  DiffLineLocation anchor,
                                  @Nullable DiffLineLocation anchorStart);

    /**
     * @param anchorLineRange the lines of the hunk to highlight, {@code null} for none
     */
    @RequiredUIAccess
    Component createDiffComponent(Disposable parentDisposable, PatchHunk patchHunk, @Nullable LineRange anchorLineRange);

    /**
     * The diff under a header naming its file: the name - a link while {@code fileNameClickListener} holds a listener -
     * and the directory of the file, with a button collapsing the diff while the item is collapsible. The diff is
     * created once the item is expanded and disposed when it collapses.
     *
     * @param diffComponentFactory creates the diff for the disposable it is shown until
     */
    @RequiredUIAccess
    Component createDiffWithHeader(Disposable parentDisposable,
                                   CollapsibleTimelineItemViewModel collapseVm,
                                   String filePath,
                                   ObservableValue<@Nullable Runnable> fileNameClickListener,
                                   Function<Disposable, Component> diffComponentFactory);

    /**
     * The diff under a header naming its file, never collapsed.
     */
    @RequiredUIAccess
    Component createDiffWithHeader(Disposable parentDisposable,
                                   String filePath,
                                   ObservableValue<@Nullable Runnable> fileNameClickListener,
                                   Component diffComponent);
}
