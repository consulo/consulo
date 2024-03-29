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
package consulo.ide.impl.idea.diff.editor;

import consulo.ide.impl.idea.diff.impl.DiffRequestProcessor;
import consulo.ide.impl.idea.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.language.file.light.LightVirtualFile;

import jakarta.annotation.Nonnull;

// from kotlin
public abstract class DiffVirtualFile extends LightVirtualFile implements VirtualFileWithoutContent, DiffContentVirtualFile, IdeDocumentHistoryImpl.SkipFromDocumentHistory {
  public DiffVirtualFile(@Nonnull String name) {
    super(name, DiffFileType.INSTANCE, "");
  }

  public abstract DiffRequestProcessor createProcessor(Project project);

  @Override
  public boolean isWritable() {
    return false;
  }
}
