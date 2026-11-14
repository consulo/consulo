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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.diff.DiffContext;
import consulo.diff.DiffExtension;
import consulo.diff.EditorDiffViewer;
import consulo.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import consulo.review.editor.CodeReviewEditorRenderer;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

/**
 * The diff of the changes of an agent shows their review - the gutter to comment on a changed file, and the comments.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIChangesDiffExtension extends DiffExtension {
    @RequiredUIAccess
    @Override
    public void onViewerCreated(FrameDiffTool.DiffViewer viewer, DiffContext context, DiffRequest request) {
        AIChangesReview review = request.getUserData(AIChangesReview.KEY);
        String path = request.getUserData(AIChangesReview.PATH);
        if (review == null || path == null || !(viewer instanceof EditorDiffViewer editorViewer)) {
            return;
        }
        List<? extends Editor> editors = editorViewer.getEditors();
        if (editors.isEmpty()) {
            return;
        }
        // the comments are on the file as the agent left it - the last side of the diff
        Editor after = editors.getLast();
        Application.get().getInstance(CodeReviewEditorRenderer.class).setupIn(viewer, after, review.model(path, after.getDocument()));
    }
}
