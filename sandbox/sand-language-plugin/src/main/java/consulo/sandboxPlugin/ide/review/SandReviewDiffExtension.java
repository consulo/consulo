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

import consulo.annotation.component.ExtensionImpl;
import consulo.diff.DiffContext;
import consulo.diff.DiffExtension;
import consulo.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.diff.CodeReviewDiffViewerRenderer;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandReviewDiffExtension extends DiffExtension {
    private final CodeReviewDiffViewerRenderer myRenderer;

    @Inject
    public SandReviewDiffExtension(CodeReviewDiffViewerRenderer renderer) {
        myRenderer = renderer;
    }

    @RequiredUIAccess
    @Override
    public void onViewerCreated(FrameDiffTool.DiffViewer viewer, DiffContext context, DiffRequest request) {
        SandReviewDiffProcessorViewModel diffVm = context.getUserData(SandReviewDiffProcessorViewModel.KEY);
        RefComparisonChange change = request.getUserData(RefComparisonChange.KEY);
        Project project = context.getProject();
        if (diffVm == null || change == null || project == null) {
            return;
        }
        FilePath after = change.filePathAfter();
        if (after == null || !SandReviewService.DEMO_FILE_NAME.equals(after.getName())) {
            return;
        }

        SandReviewService service = project.getInstance(SandReviewService.class);
        Disposable disposable = Disposable.newDisposable("SandReviewDiff");
        Disposer.register(viewer, disposable);
        myRenderer.showCodeReview(disposable, viewer, (scope, editor, side, mapper) ->
            new SandReviewEditorModel(service, editor, SandReviewService.DEMO_FILE_NAME, diffVm.getInEditorVm(), scope, mapper));
    }
}
