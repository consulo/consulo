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

import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffRequestFactory;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CompareClipboardWithSelectionAction extends BaseShowDiffAction {
  @Nullable
  private static Editor getEditor(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) return null;

    Editor editor = e.getData(Editor.KEY);
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
    if (request instanceof ContentDiffRequest contentDiffRequest) {
      for (DiffContent diffContent : contentDiffRequest.getContents()) {
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

  @Nullable
  @Override
  protected DiffRequest getDiffRequest(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    Editor editor = getEditor(e);
    FileType editorFileType = getEditorFileType(e);
    assert editor != null;

    DocumentContent content2 = createContent(project, editor, editorFileType);
    DocumentContent content1 = DiffContentFactory.getInstance().createClipboardContent(project, content2);

    LocalizeValue title1 = DiffLocalize.diffContentClipboardContentTitle();
    String title2 = createContentTitle(editor);

    LocalizeValue title = DiffLocalize.diffClipboardVsEditorDialogTitle();

    SimpleDiffRequest request = new SimpleDiffRequest(title.get(), content1, content2, title1.get(), title2);
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
      title = DiffLocalize.diffContentSelectionFromFileContentTitle(title).get();
    }

    return title;
  }
}
