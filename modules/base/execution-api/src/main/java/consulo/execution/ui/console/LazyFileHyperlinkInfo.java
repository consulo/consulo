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
package consulo.execution.ui.console;

import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 14/01/2021
 * <p>
 * from kotlin
 */
public class LazyFileHyperlinkInfo extends FileHyperlinkInfoBase {
  private final LazyValue<VirtualFile> myFile;

  public LazyFileHyperlinkInfo(Project project, String filePath, int line, int column) {
    super(project, line, column);
    myFile = LazyValue.nullable(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath));
  }

  /**
   * @param path a path of any machine - a remote platform's file is found through its file system
   */
  public LazyFileHyperlinkInfo(Project project, Path path, int line, int column) {
    super(project, line, column);
    myFile = LazyValue.nullable(() -> VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path));
  }

  @Override
  protected @Nullable VirtualFile getVirtualFile() {
    return myFile.get();
  }
}