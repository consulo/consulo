/*
 * Copyright 2013 must-be.org
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
package com.intellij.openapi.wm;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import org.consulo.module.extension.ModuleExtension;

/**
 * @author VISTALL
 * @since 16:40/12.06.13
 */
@Deprecated
public class ToolWindowModuleExtensionCondition implements Condition<Project> {
  private final Class<? extends ModuleExtension> myExtensionClass;

  public ToolWindowModuleExtensionCondition(Class<? extends ModuleExtension<?>> extensionClass) {
    myExtensionClass = extensionClass;
  }

  @Override
  public boolean value(Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      final ModuleExtension extension = ModuleUtilCore.getExtension(module, myExtensionClass);
      if(extension != null) {
        return true;
      }
    }
    return false;
  }
}
