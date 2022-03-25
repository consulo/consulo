// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.search;

import com.intellij.ide.scratch.RootType;
import consulo.language.psi.scope.EverythingGlobalScope;
import consulo.module.content.DirectoryInfo;
import com.intellij.openapi.roots.impl.ProjectFileIndexImpl;
import consulo.language.content.FileIndexFacade;
import consulo.language.psi.PsiBundle;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.UnloadedModuleDescription;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopeProvider;
import consulo.util.collection.impl.map.ConcurrentHashMap;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author yole
 */
@Singleton
public class ProjectScopeProviderImpl extends ProjectScopeProvider {

  public static class ContentSearchScope extends GlobalSearchScope {

    private final FileIndexFacade myFileIndexFacade;

    public ContentSearchScope(Project project, FileIndexFacade fileIndexFacade) {
      super(project);
      myFileIndexFacade = fileIndexFacade;
    }

    @Override
    public boolean contains(@Nonnull VirtualFile file) {
      return myFileIndexFacade.isInContent(file);
    }

    @Override
    public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
      return 0;
    }

    @Override
    public boolean isSearchInModuleContent(@Nonnull Module aModule) {
      return true;
    }

    @Override
    public boolean isSearchInLibraries() {
      return false;
    }
  }

  private enum ProjectScope {
    EVERYTHING,
    LIBRARIES,
    ALL,
    CONTENT,
    PROJECT
  }

  protected final Project myProject;

  private final Map<ProjectScope, ProjectAwareSearchScope> myScopes = new ConcurrentHashMap<>();

  @Inject
  public ProjectScopeProviderImpl(@Nonnull Project project) {
    myProject = project;
  }

  private ProjectAwareSearchScope buildOrGet(ProjectScope scope, Supplier<ProjectAwareSearchScope> supplier) {
    return myScopes.computeIfAbsent(scope, it -> supplier.get());
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getEverythingScope() {
    return buildOrGet(ProjectScope.EVERYTHING, () -> new EverythingGlobalScope(myProject) {
      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        RootType rootType = RootType.forFile(file);
        if (rootType != null && (rootType.isHidden() || rootType.isIgnored(myProject, file))) return false;
        return true;
      }
    });
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getLibrariesScope() {
    return buildOrGet(ProjectScope.LIBRARIES, () -> {
      ProjectAndLibrariesScope result = new ProjectAndLibrariesScope(myProject) {
        @Override
        public boolean contains(@Nonnull VirtualFile file) {
          return myProjectFileIndex.isInLibrary(file);
        }

        @Override
        public boolean isSearchInModuleContent(@Nonnull Module aModule) {
          return false;
        }

        @Nonnull
        @Override
        public Collection<UnloadedModuleDescription> getUnloadedModulesBelongingToScope() {
          return Collections.emptySet();
        }
      };
      result.setDisplayName(PsiBundle.message("psi.search.scope.libraries"));
      return result;
    });
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getAllScope() {
    return buildOrGet(ProjectScope.ALL, () -> {
      ProjectRootManager projectRootManager = myProject.isDefault() ? null : ProjectRootManager.getInstance(myProject);
      if (projectRootManager == null) {
        return new EverythingGlobalScope(myProject);
      }

      return new ProjectAndLibrariesScope(myProject) {
        @Override
        public boolean contains(@Nonnull VirtualFile file) {
          DirectoryInfo info = ((ProjectFileIndexImpl)myProjectFileIndex).getInfoForFileOrDirectory(file);
          return info.isInProject(file) && (info.getModule() != null || info.hasLibraryClassRoot() || info.isInLibrarySource(file));
        }
      };
    });
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getProjectScope() {
    return buildOrGet(ProjectScope.PROJECT, () -> new ProjectScopeImpl(myProject, FileIndexFacade.getInstance(myProject)));
  }

  @Nonnull
  @Override
  public ProjectAwareSearchScope getContentScope() {
    return buildOrGet(ProjectScope.CONTENT, () -> new ContentSearchScope(myProject, FileIndexFacade.getInstance(myProject)));
  }
}
