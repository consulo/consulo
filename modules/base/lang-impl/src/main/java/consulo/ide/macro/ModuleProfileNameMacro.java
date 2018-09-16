/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.macro.Macro;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;

/**
 * @author VISTALL
 * @since 18.08.14
 */
public class ModuleProfileNameMacro extends Macro {
  @Override
  public String getName() {
    return "ModuleProfileName";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.module.profile.name");
  }

  @Override
  public String expand(DataContext dataContext) {
    final Module module = dataContext.getData(LangDataKeys.MODULE);
    if(module == null) {
      return null;
    }
    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    return rootManager.getCurrentLayerName();
  }
}