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
package consulo.module.extension.condition;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.util.ArrayUtil;
import consulo.module.extension.ModuleExtension;
import consulo.extensions.ListOfElementsEP;

/**
 * @author VISTALL
 * @since 2:26/10.09.13
 */
public class ModuleExtensionConditionImpl implements ModuleExtensionCondition{

  public static ModuleExtensionCondition create(String ids) {
    String[] valuesOfVariableIfFound = ListOfElementsEP.getValuesOfVariableIfFound(ids);
    if (valuesOfVariableIfFound.length == 0) {
      return new ModuleExtensionCondition() {
        @Override
        public boolean value(ModuleExtension<?> rootModel) {
          return true;
        }

        @Override
        public boolean value(Project project) {
          return true;
        }
      };
    }
    else {
      return new ModuleExtensionConditionImpl(valuesOfVariableIfFound);
    }
  }

  private String[] myExtensionIds;

  private ModuleExtensionConditionImpl(String[] ids) {
    myExtensionIds = ids;
  }

  @Override
  public boolean value(Project project) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

      for (String extensionId : myExtensionIds) {
        ModuleExtension<?> extension = moduleRootManager.getExtension(extensionId);
        if(extension != null) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean value(ModuleExtension<?> extension) {
    return extension.isEnabled() && ArrayUtil.contains(extension.getId(), myExtensionIds);
  }
}
