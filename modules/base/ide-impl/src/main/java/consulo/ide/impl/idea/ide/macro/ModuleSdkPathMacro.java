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

package consulo.ide.impl.idea.ide.macro;

import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.content.bundle.Sdk;
import consulo.pathMacro.Macro;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import consulo.module.extension.ModuleExtensionWithSdk;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

public abstract class ModuleSdkPathMacro extends Macro {
  @Nonnull
  public abstract Class<? extends ModuleExtensionWithSdk<?>> getExtensionClass();

  @Override
  public String expand(DataContext dataContext) {
    Module module = dataContext.getData(LangDataKeys.MODULE);
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
    String jdkHomePath = VirtualFilePathUtil.getLocalPath(anySdk.getHomeDirectory());
    if (jdkHomePath != null) {
      jdkHomePath = jdkHomePath.replace('/', File.separatorChar);
    }
    return jdkHomePath;
  }
}
