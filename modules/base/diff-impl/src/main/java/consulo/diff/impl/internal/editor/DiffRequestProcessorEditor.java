/*
 * Copyright 2013-2021 consulo.io
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
package consulo.diff.impl.internal.editor;

import consulo.diff.DiffEditorViewer;
import consulo.diff.DiffViewerVirtualFile;
import consulo.diff.localize.DiffLocalize;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

// from kotlin
public class DiffRequestProcessorEditor extends FileEditorBase {
  private final DiffViewerVirtualFile myFile;
  private final DiffEditorViewer myProcessor;

  private boolean myDisposed;

  public DiffRequestProcessorEditor(DiffViewerVirtualFile file, DiffEditorViewer processor) {
    myFile = file;
    myProcessor = processor;
  }

  public DiffEditorViewer getProcessor() {
    return myProcessor;
  }

  @Override
  public void dispose() {
    myDisposed = true;

    Disposer.dispose(myProcessor);

    super.dispose();
  }

  @Override
  public boolean isValid() {
    return !myDisposed && !myProcessor.isDisposed();
  }

  @Override
  @RequiredUIAccess
  public void selectNotify() {
    myProcessor.fireProcessorActivated();
  }

  @Override
  public Component getUIComponent() {
    return myProcessor.getComponent();
  }

  @Override
  public FileEditorState getState(FileEditorStateLevel level) {
    return myProcessor.getState(level);
  }

  @Override
  @RequiredUIAccess
  public void setState(FileEditorState state) {
    myProcessor.setState(state);
  }

  @Override
  public @Nullable Component getPreferredFocusedUIComponent() {
    return myProcessor.getPreferredFocusedComponent();
  }

  @Override
  public @Nullable DiffViewerVirtualFile getFile() {
    return myFile;
  }

  @Override
  public String getName() {
    return DiffLocalize.diffFileEditorName().get();
  }

  @RequiredUIAccess
  public static void reloadDiffEditorsForFiles(Project project, Predicate<VirtualFile> condition) {
    FileEditorManager editorManager = FileEditorManager.getInstance(project);
    Set<VirtualFile> diffFiles = new LinkedHashSet<>();
    for (FileEditor editor : editorManager.getAllEditors()) {
      if (editor instanceof DiffRequestProcessorEditor) {
        VirtualFile file = editor.getFile();
        if (file != null && condition.test(file)) {
          diffFiles.add(file);
        }
      }
    }

    Set<VirtualFile> selected = Set.of(editorManager.getSelectedFiles());
    for (VirtualFile file : diffFiles) {
      editorManager.closeFile(file);
      editorManager.openFile(file, selected.contains(file));
    }
  }
}
