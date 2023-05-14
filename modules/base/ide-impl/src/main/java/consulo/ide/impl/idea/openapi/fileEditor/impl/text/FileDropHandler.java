/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorDropHandler;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.*;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ide.dnd.FileCopyPasteUtil;
import consulo.ide.impl.idea.openapi.editor.CustomFileDropHandler;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

/**
 * @author yole
 */
public class FileDropHandler implements EditorDropHandler {
  private final Editor myEditor;

  public FileDropHandler(Editor editor) {
    myEditor = editor;
  }

  @Override
  public boolean canHandleDrop(final DataFlavor[] transferFlavors) {
    return transferFlavors != null && FileCopyPasteUtil.isFileListFlavorAvailable(transferFlavors);
  }

  @RequiredUIAccess
  @Override
  public void handleDrop(@Nonnull final Transferable t, @Nullable final Project project, Object editorWindow) {
    if (project != null) {
      final List<File> fileList = FileCopyPasteUtil.getFileList(t);
      if (fileList != null) {
        boolean dropResult = ContainerUtil.process(CustomFileDropHandler.CUSTOM_DROP_HANDLER_EP.getExtensionList(project),
                                                   handler -> !(handler.canHandle(t, myEditor) && handler.handleDrop(t, myEditor, project)));
        if (!dropResult) return;

        openFiles(project, fileList, editorWindow);
      }
    }
  }

  @RequiredUIAccess
  private void openFiles(final Project project, final List<File> fileList, Object editorWindow) {
    if (editorWindow == null && myEditor != null) {
      editorWindow = findEditorWindow(project);
    }
    final LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    for (File file : fileList) {
      final VirtualFile vFile = fileSystem.refreshAndFindFileByIoFile(file);
      final FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManager.getInstance(project);
      if (vFile != null) {
        NonProjectFileWritingAccessProvider.allowWriting(vFile);

        if (editorWindow != null) {
          fileEditorManager.openFileWithProviders(vFile, true, (FileEditorWindow)editorWindow);
        }
        else {
          new OpenFileDescriptorImpl(project, vFile).navigate(true);
        }
      }
    }
  }

  @Nullable
  private FileEditorWindow findEditorWindow(Project project) {
    final Document document = myEditor.getDocument();
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      final FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManager.getInstance(project);
      final FileEditorWindow[] windows = fileEditorManager.getWindows();
      for (FileEditorWindow window : windows) {
        final FileEditorWithProviderComposite composite = window.findFileComposite(file);
        if (composite == null) {
          continue;
        }
        for (FileEditor editor : composite.getEditors()) {
          if (editor instanceof TextEditor && ((TextEditor)editor).getEditor() == myEditor) {
            return window;
          }
        }
      }
    }
    return null;
  }
}
