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
package consulo.ide.impl.idea.diff.actions;

import consulo.diff.DiffContentFactory;
import consulo.ide.impl.idea.diff.DiffRequestFactory;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.ide.impl.idea.diff.tools.util.DiffDataKeys;
import consulo.diff.DiffUserDataKeys;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.ide.impl.idea.openapi.diff.DiffBundle;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CompareClipboardWithSelectionAction extends BaseShowDiffAction {
  @Nullable
  private static Editor getEditor(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return null;

    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor != null) return editor;

    editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) return editor;

    return null;
  }

  @Nullable
  private static FileType getEditorFileType(@Nonnull AnActionEvent e) {
    DiffContent content = e.getData(DiffDataKeys.CURRENT_CONTENT);
    if (content != null && content.getContentType() != null) return content.getContentType();

    DiffRequest request = e.getData(DiffDataKeys.DIFF_REQUEST);
    if (request instanceof ContentDiffRequest) {
      for (DiffContent diffContent : ((ContentDiffRequest)request).getContents()) {
        FileType type = diffContent.getContentType();
        if (type != null && type != UnknownFileType.INSTANCE) return type;
      }
    }

    return null;
  }

  @Override
  protected boolean isAvailable(@Nonnull AnActionEvent e) {
    Editor editor = getEditor(e);
    return editor != null;
  }

  @jakarta.annotation.Nullable
  @Override
  protected DiffRequest getDiffRequest(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    Editor editor = getEditor(e);
    FileType editorFileType = getEditorFileType(e);
    assert editor != null;

    DocumentContent content2 = createContent(project, editor, editorFileType);
    DocumentContent content1 = DiffContentFactory.getInstance().createClipboardContent(project, content2);

    String title1 = DiffBundle.message("diff.content.clipboard.content.title");
    String title2 = createContentTitle(editor);

    String title = DiffBundle.message("diff.clipboard.vs.editor.dialog.title");

    SimpleDiffRequest request = new SimpleDiffRequest(title, content1, content2, title1, title2);
    if (editor.isViewer()) {
      request.putUserData(DiffUserDataKeys.FORCE_READ_ONLY_CONTENTS, new boolean[]{false, true});
    }
    return request;
  }

  @Nonnull
  private static DocumentContent createContent(@Nonnull Project project, @Nonnull Editor editor, @Nullable FileType type) {
    DocumentContent content = DiffContentFactory.getInstance().create(project, editor.getDocument(), type);

    SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.hasSelection()) {
      TextRange range = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
      content = DiffContentFactory.getInstance().createFragment(project, content, range);
    }

    return content;
  }

  @Nonnull
  private static String createContentTitle(@Nonnull Editor editor) {
    VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    String title = file != null ? DiffRequestFactory.getInstance().getContentTitle(file) : "Editor";

    if (editor.getSelectionModel().hasSelection()) {
      title = DiffBundle.message("diff.content.selection.from.file.content.title", title);
    }

    return title;
  }
}
