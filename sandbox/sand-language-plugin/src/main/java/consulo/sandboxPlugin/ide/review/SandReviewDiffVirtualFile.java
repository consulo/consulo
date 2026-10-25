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

import consulo.application.Application;
import consulo.diff.DiffEditorViewer;
import consulo.diff.combined.CombinedDiffKeys;
import consulo.diff.combined.CombinedDiffToggle;
import consulo.project.Project;
import consulo.review.util.KeyValuePair;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.editor.ComplexPathVirtualFileSystem;
import consulo.versionControlSystem.review.diff.AsyncDiffRequestProcessorFactory;
import consulo.versionControlSystem.review.diff.CodeReviewDiffVirtualFile;

import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class SandReviewDiffVirtualFile extends CodeReviewDiffVirtualFile {
    private final String mySessionId;
    private final Project myProject;
    private final String myReviewNumber;

    public SandReviewDiffVirtualFile(String sessionId, Project project, String reviewNumber) {
        super(sessionId);
        mySessionId = sessionId;
        myProject = project;
        myReviewNumber = reviewNumber;
    }

    @Override
    public String getPresentableName() {
        return "#" + myReviewNumber + " Diff";
    }

    @Override
    public String getName() {
        return getPresentableName();
    }

    @Override
    public String getPath() {
        return SandReviewVirtualFileSystem.getInstance().getPath(myProject, mySessionId, myReviewNumber);
    }

    @Override
    public String getPresentablePath() {
        return "sand/review-demo #" + myReviewNumber + " Diff";
    }

    @Override
    public boolean isValid() {
        return !myProject.isDisposed();
    }

    @Override
    public ComplexPathVirtualFileSystem<?> getFileSystem() {
        return SandReviewVirtualFileSystem.getInstance();
    }

    @RequiredUIAccess
    @Override
    public DiffEditorViewer createViewer(Project project) {
        SandReviewDiffProcessorViewModel diffVm = project.getInstance(SandReviewService.class).getDiffVm(myReviewNumber);
        AsyncDiffRequestProcessorFactory factory = Application.get().getInstance(AsyncDiffRequestProcessorFactory.class);
        DiffEditorViewer processor;
        if (CombinedDiffToggle.DEFAULT.isCombinedDiffEnabled()) {
            processor = factory.createCombinedIn(project,
                project,
                ObservableValue.of(diffVm),
                vm -> List.of(new KeyValuePair<>(SandReviewDiffProcessorViewModel.KEY, vm)),
                change -> change);
        }
        else {
            processor = factory.createIn(project,
                project,
                ObservableValue.of(diffVm),
                vm -> List.of(new KeyValuePair<>(SandReviewDiffProcessorViewModel.KEY, vm)),
                change -> change);
        }
        processor.getContext().putUserData(CombinedDiffKeys.COMBINED_DIFF_TOGGLE, CombinedDiffToggle.DEFAULT);
        return processor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SandReviewDiffVirtualFile that && mySessionId.equals(that.mySessionId) && myReviewNumber.equals(that.myReviewNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySessionId, myReviewNumber);
    }
}
