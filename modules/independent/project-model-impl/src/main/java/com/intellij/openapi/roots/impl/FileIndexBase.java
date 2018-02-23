package com.intellij.openapi.roots.impl;

import com.intellij.openapi.file.exclude.ProjectFileExclusionManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class FileIndexBase implements FileIndex {
  protected final FileTypeRegistry myFileTypeRegistry;
  protected final DirectoryIndex myDirectoryIndex;
  protected final ProjectFileExclusionManager myExclusionManager;

  public FileIndexBase(@Nonnull DirectoryIndex directoryIndex, @Nonnull FileTypeRegistry fileTypeManager, @Nonnull Project project) {
    myDirectoryIndex = directoryIndex;
    myFileTypeRegistry = fileTypeManager;
    myExclusionManager = ProjectFileExclusionManager.SERVICE.getInstance(project);
  }

  @Nonnull
  protected DirectoryInfo getInfoForFileOrDirectory(@Nonnull VirtualFile file) {
    return myDirectoryIndex.getInfoForFile(file);
  }

  @Override
  public boolean isContentSourceFile(@Nonnull VirtualFile file) {
    return !file.isDirectory() &&
           !myFileTypeRegistry.isFileIgnored(file) &&
           isInSourceContent(file);
  }
}
