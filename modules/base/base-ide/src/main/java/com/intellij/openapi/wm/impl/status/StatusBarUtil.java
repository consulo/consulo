/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.status;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.DockableEditorTabbedContainer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockManager;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Kirill Likhodedov
 */
public class StatusBarUtil {
  private static final Logger LOG = Logger.getInstance(StatusBarUtil.class);

  private StatusBarUtil() {
  }

  @Nullable
  public static Editor getCurrentTextEditor(@Nullable StatusBar statusBar) {
    if (statusBar == null) return null;

    FileEditor fileEditor = getCurrentFileEditor(statusBar);
    if (fileEditor instanceof TextEditor) {
      Editor editor = ((TextEditor)fileEditor).getEditor();
      return ensureValidEditorFile(editor, fileEditor) ? editor : null;
    }
    return null;
  }

  /**
   * Finds the current file editor.
   */
  @Nullable
  public static FileEditor getCurrentFileEditor(@Nullable StatusBar statusBar) {
    if (statusBar == null) {
      return null;
    }

    Project project = statusBar.getProject();
    if (project == null) {
      return null;
    }

    DockContainer c = null;
    if(statusBar.isUnified()) {
      c = DockManager.getInstance(project).getContainerFor(statusBar.getUIComponent());
    }
    else {
      c = DockManager.getInstance(project).getContainerFor(statusBar.getComponent());
    }
    
    EditorsSplitters splitters = null;
    if (c instanceof DockableEditorTabbedContainer) {
      splitters = ((DockableEditorTabbedContainer)c).getSplitters();
    }

    if (splitters != null && splitters.getCurrentWindow() != null) {
      EditorWithProviderComposite editor = splitters.getCurrentWindow().getSelectedEditor();
      if (editor != null) {
        return editor.getSelectedEditorWithProvider().getFileEditor();
      }
    }
    return null;
  }

  public static void setStatusBarInfo(@Nonnull Project project, @Nonnull @Nls String message) {
    final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    if (statusBar != null) {
      statusBar.setInfo(message);
    }
  }

  private static boolean ensureValidEditorFile(@Nonnull Editor editor, @Nullable FileEditor fileEditor) {
    Document document = editor.getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null && !file.isValid()) {
      Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
      Project project = editor.getProject();
      Boolean fileIsOpen = project == null ? null : ArrayUtil.contains(file, FileEditorManager.getInstance(project).getOpenFiles());
      LOG.error("Returned editor for invalid file: " + editor +
                "; disposed=" + editor.isDisposed() +
                (fileEditor == null ? "" : "; fileEditor=" + fileEditor + "; fileEditor.valid=" + fileEditor.isValid()) +
                "; file " + file.getClass() +
                "; cached document exists: " + (cachedDocument != null) +
                "; same as document: " + (cachedDocument == document) +
                "; file is open: " + fileIsOpen);
      return false;
    }
    return true;
  }
}
