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
package consulo.ide.impl.psi.search.scope;

import consulo.application.AllIcons;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.IdeBundle;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23:40/19.09.13
 */
public class TestResourcesScope extends NamedScope {
  public static final String NAME = IdeBundle.message("predefined.scope.test.resources.name");

  public TestResourcesScope() {
    super(NAME, AllIcons.Modules.TestResourcesRoot, new AbstractPackageSet("test-rsc:*..*") {
      @Override
      public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        return file != null && index.isInTestResource(file);
      }
    });
  }
}
