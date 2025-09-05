/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.change.FileHolder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.virtualFileSystem.VirtualFile;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author max
 */
public class VirtualFileHolder implements FileHolder {
  private final Set<VirtualFile> myFiles = new HashSet<>();
  private final Project myProject;
  private final HolderType myType;
  private int myNumDirs;

  public VirtualFileHolder(Project project, HolderType type) {
    myProject = project;
    myType = type;
  }

  @Override
  public HolderType getType() {
    return myType;
  }

  @Override
  public void notifyVcsStarted(AbstractVcs vcs) {
  }

  @Override
  public void cleanAll() {
    myFiles.clear();
    myNumDirs = 0;
  }

  // returns number of removed directories
  static int cleanScope(Project project, Collection<VirtualFile> files, VcsModifiableDirtyScope scope) {
    return ApplicationManager.getApplication().runReadAction((Supplier<Integer>) () -> {
      int result = 0;
      // to avoid deadlocks caused by incorrect lock ordering, need to lock on this after taking read action
      if (project.isDisposed() || files.isEmpty()) return 0;

      if (scope.getRecursivelyDirtyDirectories().size() == 0) {
        Set<FilePath> dirtyFiles = scope.getDirtyFiles();
        boolean cleanDroppedFiles = false;

        for (FilePath dirtyFile : dirtyFiles) {
          VirtualFile f = dirtyFile.getVirtualFile();
          if (f != null) {
            if (files.remove(f)) {
              if (f.isDirectory()) ++result;
            }
          }
          else {
            cleanDroppedFiles = true;
          }
        }
        if (cleanDroppedFiles) {
          for (Iterator<VirtualFile> iterator = files.iterator(); iterator.hasNext(); ) {
            VirtualFile file = iterator.next();
            if (fileDropped(file)) {
              iterator.remove();
              scope.addDirtyFile(new FilePathImpl(file));
              if (file.isDirectory()) ++result;
            }
          }
        }
      }
      else {
        for (Iterator<VirtualFile> iterator = files.iterator(); iterator.hasNext(); ) {
          VirtualFile file = iterator.next();
          boolean fileDropped = fileDropped(file);
          if (fileDropped) {
            scope.addDirtyFile(new FilePathImpl(file));
          }
          if (fileDropped || scope.belongsTo(new FilePathImpl(file))) {
            iterator.remove();
            if (file.isDirectory()) ++result;
          }
        }
      }
      return result;
    });
  }

  @Override
  public void cleanAndAdjustScope(VcsModifiableDirtyScope scope) {
    myNumDirs -= cleanScope(myProject, myFiles, scope);
  }

  private static boolean fileDropped(VirtualFile file) {
    return !file.isValid();
  }

  public void addFile(VirtualFile file) {
    myFiles.add(file);
    if (file.isDirectory()) ++myNumDirs;
  }

  public void removeFile(VirtualFile file) {
    if (myFiles.remove(file)) {
      if (file.isDirectory()) {
        --myNumDirs;
      }
    }
  }

  // todo track number of copies made
  public List<VirtualFile> getFiles() {
    return new ArrayList<VirtualFile>(myFiles);
  }

  @Override
  public VirtualFileHolder copy() {
    VirtualFileHolder copyHolder = new VirtualFileHolder(myProject, myType);
    copyHolder.myFiles.addAll(myFiles);
    copyHolder.myNumDirs = myNumDirs;
    return copyHolder;
  }

  public boolean containsFile(VirtualFile file) {
    return myFiles.contains(file);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VirtualFileHolder that = (VirtualFileHolder)o;

    if (!myFiles.equals(that.myFiles)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myFiles.hashCode();
  }

  public int getSize() {
    return myFiles.size();
  }

  public int getNumDirs() {
    assert myNumDirs >= 0;
    return myNumDirs;
  }
}
