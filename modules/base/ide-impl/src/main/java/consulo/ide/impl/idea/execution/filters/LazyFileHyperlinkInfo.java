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
package consulo.ide.impl.idea.execution.filters;

import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.NullableLazyValue;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14/01/2021
 * <p>
 * from kotlin
 */
public class LazyFileHyperlinkInfo extends FileHyperlinkInfoBase {
  private final NullableLazyValue<VirtualFile> myFile;

  public LazyFileHyperlinkInfo(@Nonnull Project project, String filePath, int line, int column) {
    super(project, line, column);
    myFile = NullableLazyValue.createValue(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath));
  }

  @Nullable
  @Override
  protected VirtualFile getVirtualFile() {
    return myFile.getValue();
  }
}