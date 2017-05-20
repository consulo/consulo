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

package com.intellij.openapi.module.impl;

import com.intellij.openapi.module.ConfigurationErrorDescription;
import com.intellij.openapi.module.ConfigurationErrorType;
import com.intellij.openapi.project.ProjectBundle;

import java.io.File;

/**
 * @author nik
 */
public class ModuleLoadingErrorDescription extends ConfigurationErrorDescription {
  private static final ConfigurationErrorType INVALID_MODULE = new ConfigurationErrorType(ProjectBundle.message("element.kind.name.module"), false);
  private final ModuleManagerImpl.ModuleLoadItem moduleLoadItem;
  private final ModuleManagerImpl myModuleManager;

  private ModuleLoadingErrorDescription(final String description, final ModuleManagerImpl.ModuleLoadItem modulePath, ModuleManagerImpl moduleManager,
                                        final String elementName) {
    super(elementName, description, INVALID_MODULE);
    moduleLoadItem = modulePath;
    myModuleManager = moduleManager;
  }

  public ModuleManagerImpl.ModuleLoadItem getModuleLoadItem() {
    return moduleLoadItem;
  }

  @Override
  public void ignoreInvalidElement() {
    myModuleManager.removeFailedModulePath(moduleLoadItem);
  }

  @Override
  public String getIgnoreConfirmationMessage() {
    return ProjectBundle.message("module.remove.from.project.confirmation", getElementName());
  }

  public static ModuleLoadingErrorDescription create(final String description, final ModuleManagerImpl.ModuleLoadItem loadItem,
                                                     ModuleManagerImpl moduleManager) {
    return new ModuleLoadingErrorDescription(description, loadItem, moduleManager, loadItem.getName());
  }
}
