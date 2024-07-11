/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileChooser.ex;

import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.execution.wsl.WSLUtil;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RootFileElement extends FileElement {
  private List<VirtualFile> myFiles;
  private Object[] myChildren;

  public RootFileElement(@Nonnull List<VirtualFile> files, String name, boolean showFileSystemRoots) {
    super(files.size() == 1 ? files.get(0) : null, name);
    myFiles = files.isEmpty() && showFileSystemRoots ? null : files;
  }

  public Object[] getChildren() {
    if (myChildren == null) {
      if (myFiles == null) {
        myFiles = getFileSystemRoots();
      }

      myChildren = myFiles.stream().
              filter(Objects::nonNull).
              map(file -> new FileElement(file, file.getPresentableUrl())).
              toArray();
    }
    return myChildren;
  }

  private static List<VirtualFile> getFileSystemRoots() {
    LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

    final List<VirtualFile> result = new ArrayList<>(
      StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), false)
        .map(root -> localFileSystem.findFileByPath(FileUtil.toSystemIndependentName(root.toString())))
        .collect(Collectors.toList())
    );

    if (SystemInfo.isWin10OrNewer && Boolean.getBoolean("wsl.p9.show.roots.in.file.chooser")) {
      final List<VirtualFile> wslRoots = ContainerUtil.mapNotNull(
        WSLUtil.getExistingUNCRoots(),
        root -> localFileSystem.findFileByPath(FileUtil.toSystemIndependentName(root.getAbsolutePath()))
      );
      result.addAll(wslRoots);
    }
    return result;
  }
}
