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
package consulo.module.impl.internal.extension;

import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.condition.ModuleExtensionCondition;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;

/**
 * @author VISTALL
 * @since 2013-09-10
 */
public class ModuleExtensionConditionImpl implements ModuleExtensionCondition {
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
