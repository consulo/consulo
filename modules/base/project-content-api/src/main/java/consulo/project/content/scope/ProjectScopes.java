// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.project.content.scope;

import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class ProjectScopes {
  private ProjectScopes() {
  }

  /**
   * @return Scope for all things inside the project: files in the project content plus files in libraries/libraries sources
   */
  @Nonnull
  public static ProjectAwareSearchScope getAllScope(@Nonnull Project project) {
    return ProjectScopeProvider.getInstance(project).getAllScope();
  }

  @Nonnull
  public static ProjectAwareSearchScope getProjectScope(@Nonnull Project project) {
    return ProjectScopeProvider.getInstance(project).getProjectScope();
  }

  @Nonnull
  public static ProjectAwareSearchScope getLibrariesScope(@Nonnull Project project) {
    return ProjectScopeProvider.getInstance(project).getLibrariesScope();
  }

  @Nonnull
  public static ProjectAwareSearchScope getContentScope(@Nonnull Project project) {
    return ProjectScopeProvider.getInstance(project).getContentScope();
  }

  /**
   * @return The biggest possible scope: every file on the planet belongs to this.
   */
  @Nonnull
  public static ProjectAwareSearchScope getEverythingScope(@Nonnull Project project) {
    return ProjectScopeProvider.getInstance(project).getEverythingScope();
  }
}