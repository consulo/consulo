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

package consulo.ide.impl.idea.openapi.module;

import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.DirectoryIndex;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.application.util.query.Query;
import consulo.logging.Logger;
import consulo.language.content.LanguageContentFolderScopes;

import jakarta.annotation.Nullable;

public class ResourceFileUtil {
  public static final Logger LOGGER = Logger.getInstance(ResourceFileUtil.class);

  private ResourceFileUtil() {
  }

  @Nullable
  public static VirtualFile findResourceFile(String name, Module inModule) {
    VirtualFile[] sourceRoots = ModuleRootManager.getInstance(inModule).getContentFolderFiles(LanguageContentFolderScopes.productionAndTest());
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(inModule.getProject()).getFileIndex();
    for (VirtualFile sourceRoot : sourceRoots) {
      String packagePrefix = fileIndex.getPackageNameByDirectory(sourceRoot);
      String prefix = packagePrefix == null || packagePrefix.isEmpty() ? null : packagePrefix.replace('.', '/') + "/";
      String relPath = prefix != null && name.startsWith(prefix) && name.length() > prefix.length() ? name.substring(prefix.length()) : name;
      String fullPath = sourceRoot.getPath() + "/" + relPath;
      VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(fullPath);
      if (fileByPath != null) {
        return fileByPath;
      }
    }
    return null;
  }

  @Nullable
  public static VirtualFile findResourceFileInDependents(Module searchFromModule, String fileName) {
    return findResourceFileInScope(fileName, searchFromModule.getProject(), GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(searchFromModule, true));
  }

  @Nullable
  public static VirtualFile findResourceFileInProject(Project project, String resourceName) {
    return findResourceFileInScope(resourceName, project, GlobalSearchScope.projectScope(project));
  }

  @Nullable
  public static VirtualFile findResourceFileInScope(String resourceName,
                                                    Project project,
                                                    GlobalSearchScope scope) {
    int index = resourceName.lastIndexOf('/');
    String packageName = index >= 0 ? resourceName.substring(0, index).replace('/', '.') : "";
    String fileName = index >= 0 ? resourceName.substring(index+1) : resourceName;
    Query<VirtualFile> directoriesByPackageName = DirectoryIndex.getInstance(project).getDirectoriesByPackageName(packageName, true);
    for (VirtualFile virtualFile : directoriesByPackageName) {
      VirtualFile child = virtualFile.findChild(fileName);
      if(child != null && scope.contains(child)) {
        return child;
      }
    }
    return null;
  }
}
