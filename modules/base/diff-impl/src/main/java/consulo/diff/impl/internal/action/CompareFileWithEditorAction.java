/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.diff.DiffRequestFactory;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DocumentContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.Side;
import consulo.fileEditor.FileEditorManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "CompareFileWithEditor")
public class CompareFileWithEditorAction extends BaseShowDiffAction {
    public CompareFileWithEditorAction() {
        super(ActionLocalize.actionComparefilewitheditorText(), ActionLocalize.actionComparefilewitheditorDescription());
    }

    @Override
    protected boolean isAvailable(@Nonnull AnActionEvent e) {
        VirtualFile selectedFile = getSelectedFile(e);
        if (selectedFile == null) {
            return false;
        }

        VirtualFile currentFile = getEditingFile(e);
        return currentFile != null && canCompare(selectedFile, currentFile);
    }

    @Nullable
    private static VirtualFile getSelectedFile(@Nonnull AnActionEvent e) {
        VirtualFile[] array = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (array == null || array.length != 1 || array[0].isDirectory()) {
            return null;
        }

        return array[0];
    }

    @Nullable
    private static VirtualFile getEditingFile(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return null;
        }

        return FileEditorManager.getInstance(project).getCurrentFile();
    }

    private static boolean canCompare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
        return !file1.equals(file2) && hasContent(file1) && hasContent(file2);
    }

    @Nullable
    @Override
    protected DiffRequest getDiffRequest(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);

        VirtualFile selectedFile = getSelectedFile(e);
        VirtualFile currentFile = getEditingFile(e);

        assert selectedFile != null && currentFile != null;

        ContentDiffRequest request = DiffRequestFactory.getInstance().createFromFiles(project, selectedFile, currentFile);

        if (request.getContents().get(1) instanceof DocumentContent documentContent) {
            Editor[] editors = EditorFactory.getInstance().getEditors(documentContent.getDocument());
            if (editors.length != 0) {
                request.putUserData(
                    DiffUserDataKeys.SCROLL_TO_LINE,
                    Pair.create(Side.RIGHT, editors[0].getCaretModel().getLogicalPosition().line)
                );
            }
        }

        return request;
    }
}
