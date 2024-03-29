/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl
public class ScratchFileEditorTabTitleProvider implements EditorTabTitleProvider {
  private ScratchFileService myScratchFileService;

  @Inject
  public ScratchFileEditorTabTitleProvider(ScratchFileService scratchFileService) {
    myScratchFileService = scratchFileService;
  }

  @Nullable
  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    RootType rootType = myScratchFileService.getRootType(file);
    if (rootType == null) return null;
    return rootType.substituteName(project, file);
  }
}
