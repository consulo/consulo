/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.scope;

import consulo.project.Project;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 * @since 1/15/14
 */
public class ExternalModuleBuildGlobalSearchScope extends DelegatingGlobalSearchScope {

  @Nonnull
  private final String externalModulePath;

  public ExternalModuleBuildGlobalSearchScope(@Nonnull final Project project, @Nonnull GlobalSearchScope baseScope, @Nonnull String externalModulePath) {
    super(new DelegatingGlobalSearchScope(baseScope) {
      @Nonnull
      @Override
      public Project getProject() {
        return project;
      }
    });
    this.externalModulePath = externalModulePath;
  }

  @Nonnull
  public String getExternalModulePath() {
    return externalModulePath;
  }
}
