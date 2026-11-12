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

package consulo.module.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.macro.PathMacrosImpl;
import consulo.application.macro.PathMacros;
import consulo.component.impl.internal.macro.BasePathMacroManager;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.PathMacroUtil;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.module.Module;
import consulo.module.macro.ModulePathMacroManager;
import consulo.project.macro.ProjectPathMacroManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class ModulePathMacroManagerImpl extends BasePathMacroManager implements ModulePathMacroManager {
  private final Module myModule;

  @Inject
  public ModulePathMacroManagerImpl(PathMacros pathMacros, Module module) {
    super(pathMacros);
    myModule = module;
  }

  @Override
  public ExpandMacroToPathMap getExpandMacroMap() {
    ExpandMacroToPathMap result = new ExpandMacroToPathMap();

    String platformUrlPrefix = getPlatformUrlPrefix();
    if (!myModule.isDisposed()) {
      if (platformUrlPrefix != null) {
        String moduleDirPath = getPlatformModuleDirPath(platformUrlPrefix);
        if (moduleDirPath != null) {
          addPathHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, moduleDirPath);
        }
      }
      else {
        addFileHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, myModule.getModuleDirPath());
      }
    }

    result.putAll(super.getExpandMacroMap());

    if (platformUrlPrefix != null) {
      result.mapUrlPrefix(ProjectPathMacroManager.STORED_URL_PREFIX, platformUrlPrefix);
    }
    return result;
  }

  @Override
  public ReplacePathToMacroMap getReplacePathMap() {
    ReplacePathToMacroMap result = super.getReplacePathMap();

    String platformUrlPrefix = getPlatformUrlPrefix();
    if (!myModule.isDisposed()) {
      if (platformUrlPrefix != null) {
        // the user home of the IDE machine says nothing about a path of a remote platform
        addFileHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, getPlatformModuleDirPath(platformUrlPrefix), null);
      }
      else {
        addFileHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, myModule.getModuleDirPath(), PathMacroUtil.getUserHomePath());
      }
    }

    if (platformUrlPrefix != null) {
      result.mapUrlPrefix(ProjectPathMacroManager.STORED_URL_PREFIX, platformUrlPrefix);
    }
    return result;
  }

  /**
   * The url prefix the project's stored file:// urls map to, null for a local project.
   */
  private @Nullable String getPlatformUrlPrefix() {
    return ProjectPathMacroManager.getInstance(myModule.getProject()).getPlatformUrlPrefix();
  }

  /**
   * The module directory as a path of the remote platform - its url is {@code <platform url prefix><path>}.
   */
  private @Nullable String getPlatformModuleDirPath(String platformUrlPrefix) {
    String dirUrl = myModule.getModuleDirUrl();
    if (dirUrl == null || !dirUrl.startsWith(platformUrlPrefix)) {
      return null;
    }
    String path = dirUrl.substring(platformUrlPrefix.length());
    // /C:/a - a drive of a windows platform is C:/a, as the project base path is
    if (path.length() >= 3 && path.charAt(0) == '/' && Character.isLetter(path.charAt(1)) && path.charAt(2) == ':') {
      return path.substring(1);
    }
    return path;
  }
}
