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
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.review.editor.CodeReviewCommentableEditorModel;
import consulo.review.editor.CodeReviewEditorRenderer;
import consulo.review.editor.CodeReviewNavigableEditorViewModel;
import consulo.review.editor.ReviewInEditorUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

/**
 * Shows the sand review in every main editor of the demo file, the way a review plugin turns its review mode on.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class SandReviewEditorFactoryListener implements EditorFactoryListener {
    private static final Key<Disposable> REVIEW_DISPOSABLE = Key.create("sand.review.editor.disposable");

    private final CodeReviewEditorRenderer myRenderer;
    private final FileDocumentManager myFileDocumentManager;

    @Inject
    public SandReviewEditorFactoryListener(CodeReviewEditorRenderer renderer, FileDocumentManager fileDocumentManager) {
        myRenderer = renderer;
        myFileDocumentManager = fileDocumentManager;
    }

    @Override
    public void editorCreated(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        VirtualFile file = myFileDocumentManager.getFile(editor.getDocument());
        if (project == null || file == null || editor.getEditorKind() != EditorKind.MAIN_EDITOR
            || !SandReviewService.DEMO_FILE_NAME.equals(file.getName())) {
            return;
        }

        Disposable disposable = Disposable.newDisposable("SandReviewEditor");
        Disposer.register(project, disposable);
        editor.putUserData(REVIEW_DISPOSABLE, disposable);

        SandReviewService service = project.getInstance(SandReviewService.class);
        SandReviewInEditorViewModel inEditorVm = new SandReviewInEditorViewModel();
        SandReviewEditorModel model = new SandReviewEditorModel(service, editor, file.getName(), inEditorVm, disposable);
        editor.putUserData(CodeReviewCommentableEditorModel.KEY, model);
        editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, model);
        myRenderer.setupIn(disposable, editor, model);
        ReviewInEditorUtil.showReviewToolbar(disposable, inEditorVm, editor);
    }

    @Override
    public void editorReleased(EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Disposable disposable = editor.getUserData(REVIEW_DISPOSABLE);
        if (disposable != null) {
            editor.putUserData(REVIEW_DISPOSABLE, null);
            editor.putUserData(CodeReviewCommentableEditorModel.KEY, null);
            editor.putUserData(CodeReviewNavigableEditorViewModel.KEY, null);
            Disposer.dispose(disposable);
        }
    }
}
