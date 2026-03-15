// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.content.scope;

import consulo.project.Project;


/**
 * @author max
 */
public class ProjectScopes {
  private ProjectScopes() {
  }

  /**
   * @return Scope for all things inside the project: files in the project content plus files in libraries/libraries sources
   */
  
  public static ProjectAwareSearchScope getAllScope(Project project) {
    return ProjectScopeProvider.getInstance(project).getAllScope();
  }

  
  public static ProjectAwareSearchScope getProjectScope(Project project) {
    return ProjectScopeProvider.getInstance(project).getProjectScope();
  }

  
  public static ProjectAwareSearchScope getLibrariesScope(Project project) {
    return ProjectScopeProvider.getInstance(project).getLibrariesScope();
  }

  
  public static ProjectAwareSearchScope getContentScope(Project project) {
    return ProjectScopeProvider.getInstance(project).getContentScope();
  }

  /**
   * @return The biggest possible scope: every file on the planet belongs to this.
   */
  
  public static ProjectAwareSearchScope getEverythingScope(Project project) {
    return ProjectScopeProvider.getInstance(project).getEverythingScope();
  }
}