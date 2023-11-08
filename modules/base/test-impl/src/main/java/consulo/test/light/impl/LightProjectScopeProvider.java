/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopeProvider;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2023-11-08
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightProjectScopeProvider implements ProjectScopeProvider {
  private final Project myProject;

  @Inject
  public LightProjectScopeProvider(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getEverythingScope() {
    return new EverythingGlobalScope(myProject);
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getLibrariesScope() {
    return new GlobalSearchScope() {
      @Override
      public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return false;
      }

      @Override
      public boolean isSearchInLibraries() {
        return false;
      }

      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        return false;
      }
    };
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getAllScope() {
    return new EverythingGlobalScope(myProject);
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getProjectScope() {
    return new EverythingGlobalScope(myProject);
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getContentScope() {
    return new EverythingGlobalScope(myProject);
  }
}
