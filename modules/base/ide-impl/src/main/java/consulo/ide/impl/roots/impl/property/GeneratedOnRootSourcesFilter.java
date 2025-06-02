/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.roots.impl.property;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.GeneratedContentFolderPropertyProvider;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.ContentFolder;
import consulo.project.content.GeneratedSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * TODO module-content-impl module source
 *
 * @author VISTALL
 * @since 22:15/25.11.13
 */
@ExtensionImpl
public class GeneratedOnRootSourcesFilter implements GeneratedSourcesFilter {
  private final ProjectFileIndex myProjectFileIndex;

  @Inject
  public GeneratedOnRootSourcesFilter(ProjectFileIndex projectFileIndex) {
    myProjectFileIndex = projectFileIndex;
  }

  @RequiredReadAction
  @Override
  public boolean isGeneratedSource(@Nonnull VirtualFile file) {
    ContentFolder folder = myProjectFileIndex.getContentFolder(file);
    return folder != null && Objects.equals(folder.getPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED), Boolean.TRUE);
  }
}
