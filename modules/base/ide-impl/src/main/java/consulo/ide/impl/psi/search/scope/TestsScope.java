/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author Konstantin Bulenkov
 */
public class TestsScope extends NamedScope {
  public static final String NAME = IdeBundle.message("predefined.scope.tests.name");
  public TestsScope() {
    super(NAME, AllIcons.Modules.TestRoot, new AbstractPackageSet("test:*..*") {
      @Override
      public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
        return file != null && TestSourcesFilter.isTestSources(file, project);
      }
    });
  }

  @Override
  public String getDefaultColorName() {
    return "Green";
  }
}
