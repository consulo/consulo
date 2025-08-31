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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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

    if (!myModule.isDisposed()) {
      addFileHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, myModule.getModuleDirPath());
    }

    result.putAll(super.getExpandMacroMap());

    return result;
  }

  @Override
  public ReplacePathToMacroMap getReplacePathMap() {
    ReplacePathToMacroMap result = super.getReplacePathMap();

    if (!myModule.isDisposed()) {

      addFileHierarchyReplacements(result, PathMacrosImpl.MODULE_DIR_MACRO_NAME, myModule.getModuleDirPath(), PathMacroUtil.getUserHomePath());
    }

    return result;
  }
}
