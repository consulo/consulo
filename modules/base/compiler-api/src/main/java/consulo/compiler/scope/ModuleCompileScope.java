/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 20, 2003
 * Time: 5:34:19 PM
 */
package consulo.compiler.scope;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.FileIndex;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ModuleCompileScope extends FileIndexCompileScope {
  private final Project myProject;
  private final Set<Module> myScopeModules;
  private final Module[] myModules;
  private final boolean myIncludeTestScope;

  @RequiredReadAction
  public ModuleCompileScope(final Module module, boolean includeDependentModules, boolean includeTestScope) {
    myProject = module.getProject();
    myIncludeTestScope = includeTestScope;
    myScopeModules = new HashSet<>();
    if (includeDependentModules) {
      buildScopeModulesSet(module);
    }
    else {
      myScopeModules.add(module);
    }
    myModules = ModuleManager.getInstance(myProject).getModules();
  }

  @RequiredReadAction
  public ModuleCompileScope(Project project, final Module[] modules, boolean includeDependentModules, boolean includeTestScope) {
    myProject = project;
    myScopeModules = new HashSet<>();
    myIncludeTestScope = includeTestScope;
    for (Module module : modules) {
      if (module == null) {
        continue;
      }
      if (includeDependentModules) {
        buildScopeModulesSet(module);
      }
      else {
        myScopeModules.add(module);
      }
    }
    myModules = ModuleManager.getInstance(myProject).getModules();
  }

  private void buildScopeModulesSet(Module module) {
    myScopeModules.add(module);
    final Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
    for (Module dependency : dependencies) {
      if (!myScopeModules.contains(dependency)) { // may be in case of module circular dependencies
        buildScopeModulesSet(dependency);
      }
    }
  }

  @Override
  @Nonnull
  public Module[] getAffectedModules() {
    return myScopeModules.toArray(new Module[myScopeModules.size()]);
  }

  @Override
  protected FileIndex[] getFileIndices() {
    final FileIndex[] indices = new FileIndex[myScopeModules.size()];
    int idx = 0;
    for (final Module module : myScopeModules) {
      indices[idx++] = ModuleRootManager.getInstance(module).getFileIndex();
    }
    return indices;
  }

  @Override
  public boolean includeTestScope() {
    return myIncludeTestScope;
  }

  @Override
  public boolean belongs(final String url) {
    if (myScopeModules.isEmpty()) {
      return false; // optimization
    }
    Module candidateModule = null;
    int maxUrlLength = 0;
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    for (final Module module : myModules) {
      final String[] contentRootUrls = getModuleContentUrls(module);
      for (final String contentRootUrl : contentRootUrls) {
        if (contentRootUrl.length() < maxUrlLength) {
          continue;
        }
        if (!isUrlUnderRoot(url, contentRootUrl)) {
          continue;
        }
        if (contentRootUrl.length() == maxUrlLength) {
          if (candidateModule == null) {
            candidateModule = module;
          }
          else {
            // the same content root exists in several modules
            if (!candidateModule.equals(module)) {
              candidateModule = ApplicationManager.getApplication().runReadAction(new Supplier<Module>() {
                @Override
                public Module get() {
                  final VirtualFile contentRootFile = VirtualFileManager.getInstance().findFileByUrl(contentRootUrl);
                  if (contentRootFile != null) {
                    return projectFileIndex.getModuleForFile(contentRootFile);
                  }
                  return null;
                }
              });
            }
          }
        }
        else {
          maxUrlLength = contentRootUrl.length();
          candidateModule = module;
        }
      }
    }

    if (candidateModule != null && myScopeModules.contains(candidateModule)) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(candidateModule);
      final String[] excludeRootUrls = moduleRootManager.getContentFolderUrls(ContentFolderTypeProvider.onlyExcluded());
      for (String excludeRootUrl : excludeRootUrls) {
        if (isUrlUnderRoot(url, excludeRootUrl)) {
          return false;
        }
      }
      final String[] sourceRootUrls = moduleRootManager.getContentFolderUrls(ContentFolderTypeProvider.allExceptExcluded());
      for (String sourceRootUrl : sourceRootUrls) {
        if (isUrlUnderRoot(url, sourceRootUrl)) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isUrlUnderRoot(final String url, final String root) {
    return (url.length() > root.length()) && url.charAt(root.length()) == '/' && FileUtil.startsWith(url, root);
  }

  private final Map<Module, String[]> myContentUrlsCache = new HashMap<>();

  private String[] getModuleContentUrls(final Module module) {
    String[] contentRootUrls = myContentUrlsCache.get(module);
    if (contentRootUrls == null) {
      contentRootUrls = ModuleRootManager.getInstance(module).getContentRootUrls();
      myContentUrlsCache.put(module, contentRootUrls);
    }
    return contentRootUrls;
  }

}
