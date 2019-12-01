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
package consulo.roots.impl.property;

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22:15/25.11.13
 */
public class GeneratedOnRootSourcesFilter extends GeneratedSourcesFilter {
  @RequiredReadAction
  @Override
  public boolean isGeneratedSource(@Nonnull VirtualFile file, @Nonnull Project project) {
    VirtualFile contentRootForFile = ProjectFileIndex.getInstance(project).getSourceRootForFile(file);
    if(contentRootForFile == null) {
      return false;
    }
    ContentFolder contentFolder = ProjectRootsUtil.findContentFolderForDirectory(contentRootForFile, project);
    if(contentFolder == null) {
      return false;
    }

    return contentFolder.getPropertyValue(GeneratedContentFolderPropertyProvider.IS_GENERATED) == Boolean.TRUE;
  }
}
