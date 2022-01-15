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

package com.intellij.ide.macro;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.PathUtil;
import consulo.module.extension.ModuleExtensionWithSdk;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

public abstract class ModuleSdkPathMacro extends Macro {
  @Nonnull
  public abstract Class<? extends ModuleExtensionWithSdk<?>> getExtensionClass();

  @Override
  public String expand(DataContext dataContext) {
    final Module module = dataContext.getData(LangDataKeys.MODULE);
    if(module == null) {
      return null;
    }
    return sdkPath(ModuleUtilCore.getSdk(module, getExtensionClass()));
  }

  @Nullable
  static String sdkPath(@Nullable Sdk anySdk) {
    if (anySdk == null) {
      return null;
    }
    String jdkHomePath = PathUtil.getLocalPath(anySdk.getHomeDirectory());
    if (jdkHomePath != null) {
      jdkHomePath = jdkHomePath.replace('/', File.separatorChar);
    }
    return jdkHomePath;
  }
}
