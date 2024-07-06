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

import consulo.diff.DiffContentVirtualFile;
import consulo.fileEditor.history.SkipFromDocumentHistory;
import consulo.language.file.light.LightVirtualFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import jakarta.annotation.Nonnull;

// from kotlin
public abstract class DiffVirtualFile extends LightVirtualFile implements VirtualFileWithoutContent, DiffContentVirtualFile, SkipFromDocumentHistory {
  public DiffVirtualFile(@Nonnull String name) {
    super(name, DiffFileType.INSTANCE, "");
  }

  public abstract Object createProcessor(Project project);

  @Override
  public boolean isWritable() {
    return false;
  }
}
