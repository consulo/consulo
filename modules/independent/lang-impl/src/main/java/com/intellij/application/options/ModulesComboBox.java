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
package com.intellij.application.options;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesAlphaComparator;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.SortedComboBoxModel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * Combobox which allows to select a loaded modules from a project. If you need to show unloaded modules as well, use {@link ModuleDescriptionsComboBox}
 * instead.
 *
 * @author nik
 */
public class ModulesComboBox extends ComboBox<Module> {
  private final SortedComboBoxModel<Module> myModel;
  private boolean myAllowEmptySelection;

  public ModulesComboBox() {
    this(new SortedComboBoxModel<>(ModulesAlphaComparator.INSTANCE));
  }

  private ModulesComboBox(final SortedComboBoxModel<Module> model) {
    super(model);
    myModel = model;
    new ComboboxSpeedSearch(this){
      @Override
      protected String getElementText(Object element) {
        if (element instanceof Module) {
          return ((Module)element).getName();
        } else if (element == null) {
          return "";
        }
        return super.getElementText(element);
      }
    };
    setRenderer(new ModuleListCellRenderer());
  }

  public void allowEmptySelection(@Nonnull String emptySelectionText) {
    myAllowEmptySelection = true;
    myModel.add(null);
    setRenderer(new ModuleListCellRenderer(emptySelectionText));
  }

  public void setModules(@Nonnull Collection<Module> modules) {
    myModel.setAll(modules);
    if (myAllowEmptySelection) {
      myModel.add(null);
    }
  }

  public void fillModules(@Nonnull Project project) {
    Module[] allModules = ModuleManager.getInstance(project).getModules();
    setModules(Arrays.asList(allModules));
  }

  public void setSelectedModule(@Nullable Module module) {
    myModel.setSelectedItem(module);
  }

  @Nullable
  public Module getSelectedModule() {
    return myModel.getSelectedItem();
  }
}
