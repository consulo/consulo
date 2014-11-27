/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.ide.macro;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import org.consulo.compiler.ModuleCompilerPathsManager;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 18.08.14
 */
public abstract class ModuleOutputDirPathMacro extends Macro {
  @NotNull
  public abstract ContentFolderTypeProvider getContentFolderTypeProvider();

  @Override
  public String expand(DataContext dataContext) {
    final Module module = LangDataKeys.MODULE.getData(dataContext);
    if(module == null) {
      return null;
    }
    String compilerOutputUrl = ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(getContentFolderTypeProvider());
    if(compilerOutputUrl == null) {
      return null;
    }
    return VfsUtil.urlToPath(compilerOutputUrl);
  }
}