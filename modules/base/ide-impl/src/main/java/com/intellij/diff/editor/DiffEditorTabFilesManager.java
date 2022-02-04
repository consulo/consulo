package com.intellij.diff.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

// from kotlin
public interface DiffEditorTabFilesManager {

  @Nonnull
  public static DiffEditorTabFilesManager getInstance(@Nonnull Project project) {
    return project.getInstance(DiffEditorTabFilesManager.class);
  }

  FileEditor[] showDiffFile(VirtualFile diffFile, boolean focusEditor);
}
