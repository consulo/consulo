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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.internal.VcsContextWrapper;
import consulo.versionControlSystem.impl.internal.change.LocalChangeListImpl;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.platform.PlatformNioFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.internal.PlatformPathVirtualFileSystem;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

@Singleton
@ServiceImpl
public class VcsContextFactoryImpl implements VcsContextFactory {
  @Override
  public VcsContext createCachedContextOn(AnActionEvent event) {
    return VcsContextWrapper.createCachedInstanceOn(event);
  }

  @Override
  public VcsContext createContextOn(AnActionEvent event, Key<Editor> editorKey) {
    return new VcsContextWrapper(event.getDataContext(), editorKey, event.getModifiers(), event.getPlace(), event.getPresentation().getTextValue());
  }

  @Override
  public FilePath createFilePathOn(VirtualFile virtualFile) {
    if (virtualFile.getFileSystem() instanceof PlatformPathVirtualFileSystem) {
      return new PlatformFilePath(virtualFile.getPath(), virtualFile.isDirectory());
    }
    return createFilePath(virtualFile.getPath(), virtualFile.isDirectory());
  }

  @Override
  public FilePath createFilePath(Path path, boolean isDirectory) {
    if (path.getFileSystem() instanceof PlatformNioFileSystem) {
      PlatformPathVirtualFileSystem fileSystem = PlatformFilePath.getFileSystem();
      if (fileSystem != null) {
        return new PlatformFilePath(fileSystem.toVfsPath(path), isDirectory);
      }
    }
    return createFilePath(path.toString(), isDirectory);
  }

  @Override
  public FilePath createFilePathOn(File file) {
    String path = file.getPath();
    PlatformPathVirtualFileSystem platformFileSystem = PlatformFilePath.getFileSystem();
    if (!file.isAbsolute() && platformFileSystem != null && platformFileSystem.isPlatformPath(FileUtil.toSystemIndependentName(path))) {
      // not a file - a path of a file of a platform, as its virtual file answers it
      String vfsPath = FileUtil.toSystemIndependentName(path);
      VirtualFile vf = ((VirtualFileSystem) platformFileSystem).findFileByPath(vfsPath);
      return new PlatformFilePath(vfsPath, vf != null && vf.isDirectory());
    }
    VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path);
    return createFilePath(path, vf != null ? vf.isDirectory() : file.isDirectory());
  }

  @Override
  public FilePath createFilePathOn(File file, Function<File, Boolean> detector) {
    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
    if (virtualFile != null) {
      return createFilePathOn(virtualFile);
    }
    return createFilePathOn(file, detector.apply(file));
  }

  @Override
  public FilePath createFilePathOn(File file, boolean isDirectory) {
    return createFilePath(file.getPath(), isDirectory);
  }

  @Override
  public FilePath createFilePathOnNonLocal(String path, boolean isDirectory) {
    return new RemoteFilePath(path, isDirectory);
  }

  @Override
  public FilePath createFilePathOnDeleted(File file, boolean isDirectory) {
    return createFilePathOn(file, isDirectory);
  }

  @Override
  public FilePath createFilePathOn(VirtualFile parent, String name) {
    return createFilePath(parent, name, false);
  }

  @Override
  public FilePath createFilePath(VirtualFile parent, String fileName, boolean isDirectory) {
    if (parent.getFileSystem() instanceof PlatformPathVirtualFileSystem) {
      return new PlatformFilePath(parent.getPath() + "/" + fileName, isDirectory);
    }
    return createFilePath(parent.getPath() + "/" + fileName, isDirectory);
  }

  @Override
  public LocalChangeList createLocalChangeList(Project project, String name) {
    return LocalChangeListImpl.createEmptyChangeListImpl(project, name);
  }

  @Override
  public FilePath createFilePath(String path, boolean isDirectory) {
    // a path of a file of a platform, as its virtual file answers it - stored or built from a virtual file path
    PlatformPathVirtualFileSystem fileSystem = path.isEmpty() || path.charAt(0) == '/' ? null : PlatformFilePath.getFileSystem();
    if (fileSystem != null && fileSystem.isPlatformPath(path)) {
      return new PlatformFilePath(path, isDirectory);
    }
    return new LocalFilePath(path, isDirectory);
  }
}