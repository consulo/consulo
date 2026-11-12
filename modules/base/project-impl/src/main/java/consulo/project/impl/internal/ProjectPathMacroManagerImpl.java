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
package consulo.project.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.macro.PathMacrosImpl;
import consulo.application.macro.PathMacros;
import consulo.component.impl.internal.macro.BasePathMacroManager;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.platform.PlatformNioFileSystem;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

@Singleton
@ServiceImpl
public class ProjectPathMacroManagerImpl extends BasePathMacroManager implements ProjectPathMacroManager {
  private final Project myProject;

  @Inject
  public ProjectPathMacroManagerImpl(PathMacros pathMacros, Project project) {
    super(pathMacros);
    myProject = project;
  }

  @Override
  public ExpandMacroToPathMap getExpandMacroMap() {
    ExpandMacroToPathMap result = super.getExpandMacroMap();
    String platformUrlPrefix = getPlatformUrlPrefix();
    if (platformUrlPrefix != null) {
      // a remote project: macros expand to the paths on its platform, its stored file:// urls are urls of that platform
      String basePath = myProject.getBasePath();
      if (basePath != null) {
        addPathHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, basePath);
      }
      result.mapUrlPrefix(STORED_URL_PREFIX, platformUrlPrefix);
    }
    else {
      addFileHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, getProjectDir(myProject));
    }
    return result;
  }

  @Override
  public ReplacePathToMacroMap getReplacePathMap() {
    ReplacePathToMacroMap result = super.getReplacePathMap();
    String platformUrlPrefix = getPlatformUrlPrefix();
    if (platformUrlPrefix != null) {
      addFileHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, myProject.getBasePath(), null);
      result.mapUrlPrefix(STORED_URL_PREFIX, platformUrlPrefix);
    }
    else {
      addFileHierarchyReplacements(result, PathMacrosImpl.PROJECT_DIR_MACRO_NAME, getProjectDir(myProject), null);
    }
    return result;
  }

  @Override
  public @Nullable String getPlatformUrlPrefix() {
    if (myProject.getPlatformTargetId() == null || !(myProject instanceof ProjectImpl project)) {
      return null;
    }
    Path dirPath = project.getDirPath();
    if (!(dirPath.getFileSystem() instanceof PlatformNioFileSystem platformFileSystem)) {
      return null;
    }
    // the platform virtual file system names a file <platform id>/<path>
    return StandardFileSystems.PLATFORM_PROTOCOL + URLUtil.SCHEME_SEPARATOR + platformFileSystem.getPlatformId();
  }

  private static @Nullable String getProjectDir(Project project) {
    VirtualFile baseDir = project.getBaseDir();
    return baseDir != null ? baseDir.getPath() : null;
  }
}
