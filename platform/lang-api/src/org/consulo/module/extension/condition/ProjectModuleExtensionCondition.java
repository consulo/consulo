/*
 * Copyright 2013 Consulo.org
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
package org.consulo.module.extension.condition;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import org.consulo.module.extension.ModuleExtension;

/**
 * @author VISTALL
 * @since 2:26/10.09.13
 */
public class ProjectModuleExtensionCondition implements Condition<Project> {

  public static Condition<Project> create(String ids) {
    return StringUtil.isEmptyOrSpaces(ids) ? Conditions.<Project>alwaysTrue() : new ProjectModuleExtensionCondition(ids);
  }

  private Class<? extends ModuleExtension<?>>[] myExtensionClasses;

  private ProjectModuleExtensionCondition(String ids) {
    myExtensionClasses = ModuleExtensionConvertUtil.toModuleExtensionClassArray(ids);
  }

  @Override
  public boolean value(Project project) {
    if (myExtensionClasses.length == 0) {
      return false;
    }

    ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

      for (Class<? extends ModuleExtension<?>> extensionClass : myExtensionClasses) {
        ModuleExtension<?> extension = moduleRootManager.getExtension(extensionClass);
        if(extension != null) {
          return true;
        }
      }
    }
    return false;
  }
}
