/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.LoadedModuleDescriptionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Combobox which may show not only regular loaded modules but also unloaded modules. Use it instead of {@link com.intellij.openapi.roots.ui.configuration.ModulesCombobox} for
 * configuration elements which may refer to unloaded modules.
 *
 * @author nik
 */
public final class ModuleDescriptionsComboBox extends ComboBox<ModuleDescription> {
  private final SortedComboBoxModel<ModuleDescription> myModel;
  private boolean myAllowEmptySelection;

  public ModuleDescriptionsComboBox() {
    myModel = new SortedComboBoxModel<>(Comparator.comparing(description -> description != null ? description.getName() : "", String.CASE_INSENSITIVE_ORDER));
    setModel(myModel);
    new ComboboxSpeedSearch(this) {
      @Override
      protected String getElementText(Object element) {
        if (element instanceof ModuleDescription) {
          return ((ModuleDescription)element).getName();
        }
        else {
          return "";
        }
      }
    };
    setRenderer(new ModuleDescriptionListCellRenderer());
  }

  public void allowEmptySelection(@Nonnull String emptySelectionText) {
    myAllowEmptySelection = true;
    myModel.add(null);
    setRenderer(new ModuleDescriptionListCellRenderer(emptySelectionText));
  }

  public void setModules(@Nonnull Collection<Module> modules) {
    myModel.clear();
    for (Module module : modules) {
      myModel.add(new LoadedModuleDescriptionImpl(module));
    }
    if (myAllowEmptySelection) {
      myModel.add(null);
    }
  }

  public void setAllModulesFromProject(@Nonnull Project project) {
    setModules(Arrays.asList(ModuleManager.getInstance(project).getModules()));
  }

  public void setSelectedModule(@Nullable Module module) {
    myModel.setSelectedItem(module != null ? new LoadedModuleDescriptionImpl(module) : null);
  }

  public void setSelectedModule(@Nonnull Project project, @Nonnull String moduleName) {
    Module module = ModuleManager.getInstance(project).findModuleByName(moduleName);
    if (module != null) {
      setSelectedModule(module);
    }
    else {
      UnloadedModuleDescription description = ModuleManager.getInstance(project).getUnloadedModuleDescription(moduleName);
      if (description != null) {
        if (myModel.indexOf(description) < 0) {
          myModel.add(description);
        }
        myModel.setSelectedItem(description);
      }
      else {
        myModel.setSelectedItem(null);
      }
    }
  }

  @Nullable
  public Module getSelectedModule() {
    ModuleDescription selected = myModel.getSelectedItem();
    if (selected instanceof LoadedModuleDescription) {
      return ((LoadedModuleDescription)selected).getModule();
    }
    return null;
  }

  @Nullable
  public String getSelectedModuleName() {
    ModuleDescription selected = myModel.getSelectedItem();
    return selected != null ? selected.getName() : null;
  }

  private static class ModuleDescriptionListCellRenderer extends ColoredListCellRenderer<ModuleDescription> {
    private final String myEmptySelectionText;

    public ModuleDescriptionListCellRenderer() {
      this("[none]");
    }

    public ModuleDescriptionListCellRenderer(@Nonnull String emptySelectionText) {
      myEmptySelectionText = emptySelectionText;
    }

    @Override
    protected void customizeCellRenderer(@Nonnull JList<? extends ModuleDescription> list, ModuleDescription moduleDescription, int index, boolean selected, boolean hasFocus) {
      if (moduleDescription == null) {
        append(myEmptySelectionText);
      }
      else {
        if (moduleDescription instanceof LoadedModuleDescription) {
          setIcon(AllIcons.Nodes.Module);
          setForeground(null);
        }
        else {
          setIcon(AllIcons.Nodes.Module); //FIXME [VISTALL] another icon?
          setForeground(JBColor.RED);
        }
        append(moduleDescription.getName());
      }
    }
  }
}
