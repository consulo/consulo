/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
@Deprecated
public abstract class AbstractModuleBuilder extends ProjectBuilder {
  public abstract Icon getNodeIcon();

  public abstract ModuleWizardStep[] createWizardSteps(WizardContext wizardContext, ModulesProvider modulesProvider);

  @Nullable
  public ModuleWizardStep modifySettingsStep(SettingsStep settingsStep) {
    return null;
  }

  public abstract void setName(String name);

  public abstract void setModuleDirPath(@NonNls String path);

  public abstract void setContentEntryPath(String moduleRootPath);
}
