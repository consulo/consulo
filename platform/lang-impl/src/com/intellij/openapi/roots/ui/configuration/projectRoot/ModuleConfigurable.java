/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExistsException;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.roots.ui.configuration.projectRoot.moduleLayerActions.DeleteLayerAction;
import consulo.roots.ui.configuration.projectRoot.moduleLayerActions.NewLayerAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.MutableCollectionComboBoxModel;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * User: anna
 * Date: 04-Jun-2006
 */
public class ModuleConfigurable extends ProjectStructureElementConfigurable<Module> implements Place.Navigator {
  private final Module myModule;
  private final ModulesConfigurator myConfigurator;
  private String myModuleName;
  private final ModuleProjectStructureElement myProjectStructureElement;
  private final StructureConfigurableContext myContext;

  public ModuleConfigurable(ModulesConfigurator modulesConfigurator,
                            Module module,
                            final Runnable updateTree) {
    super(true, updateTree);
    myModule = module;
    myModuleName = myModule.getName();
    myConfigurator = modulesConfigurator;
    myContext = ModuleStructureConfigurable.getInstance(myModule.getProject()).getContext();
    myProjectStructureElement = new ModuleProjectStructureElement(myContext, myModule);
  }

  @Override
  public void setDisplayName(String name) {
    name = name.trim();
    final ModifiableModuleModel modifiableModuleModel = myConfigurator.getModuleModel();
    if (StringUtil.isEmpty(name)) return; //empty string comes on double click on module node
    if (Comparing.strEqual(name, myModuleName)) return; //nothing changed
    try {
      modifiableModuleModel.renameModule(myModule, name);
    }
    catch (ModuleWithNameAlreadyExistsException ignored) {
    }
    myConfigurator.moduleRenamed(myModule, myModuleName, name);
    myModuleName = name;
    myConfigurator.setModified(!Comparing.strEqual(myModuleName, myModule.getName()));
    myContext.getDaemonAnalyzer().queueUpdateForAllElementsWithErrors();
  }

  @Nullable
  @Override
  protected JComponent createTopRightComponent(JTextField nameField) {
    return createLayerConfigurationPanel(getModuleEditor());
  }

  @NotNull
  private static JPanel createLayerConfigurationPanel(@NotNull final ModuleEditor moduleEditor) {
    BorderLayoutPanel panel = JBUI.Panels.simplePanel();

    ModifiableRootModel moduleRootModel = moduleEditor.getModifiableRootModelProxy();

    final MutableCollectionComboBoxModel<String> model =
            new MutableCollectionComboBoxModel<String>(new ArrayList<String>(moduleRootModel.getLayers().keySet()), moduleRootModel.getCurrentLayerName());

    final ComboBox comboBox = new ComboBox(model);
    comboBox.setEnabled(model.getSize() > 1);

    moduleEditor.addChangeListener(new ModuleEditor.ChangeListener() {
      @Override
      public void moduleStateChanged(ModifiableRootModel moduleRootModel) {
        model.update(new ArrayList<String>(moduleRootModel.getLayers().keySet()));
        model.setSelectedItem(moduleRootModel.getCurrentLayerName());
        model.update();

        comboBox.setEnabled(comboBox.getItemCount() > 1);
      }
    });

    comboBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          moduleEditor.getModifiableRootModelProxy().setCurrentLayer((String)comboBox.getSelectedItem());
        }
      }
    });

    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new NewLayerAction(moduleEditor, false));
    group.add(new DeleteLayerAction(moduleEditor));
    group.add(new NewLayerAction(moduleEditor, true));

    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    JComponent toolbarComponent = actionToolbar.getComponent();
    toolbarComponent.setBorder(JBUI.Borders.empty());

    panel.addToLeft(LabeledComponent.left(comboBox, "Layer")).addToRight(toolbarComponent);
    return panel;
  }

  @Override
  public ProjectStructureElement getProjectStructureElement() {
    return myProjectStructureElement;
  }

  @Override
  public Module getEditableObject() {
    return myModule;
  }

  @Override
  public String getBannerSlogan() {
    return ProjectBundle.message("project.roots.module.banner.text", myModuleName);
  }

  @Override
  public String getDisplayName() {
    return myModuleName;
  }

  @Override
  public Icon getIcon(final boolean open) {
    return AllIcons.Nodes.Module;
  }

  public Module getModule() {
    return myModule;
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    final ModuleEditor moduleEditor = getModuleEditor();
    return moduleEditor != null ? moduleEditor.getHelpTopic() : null;
  }


  @Override
  public JComponent createOptionsPanel() {
    return getModuleEditor().getPanel();
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    //do nothing
  }

  @Override
  public void reset() {
    //do nothing
  }

  @Override
  public void disposeUIResources() {
    //do nothing
  }

  public ModuleEditor getModuleEditor() {
    return myConfigurator.getModuleEditor(myModule);
  }

  @Override
  public ActionCallback navigateTo(@Nullable final Place place, final boolean requestFocus) {
    return getModuleEditor().navigateTo(place, requestFocus);
  }

  @Override
  public void queryPlace(@NotNull final Place place) {
    final ModuleEditor editor = getModuleEditor();
    if (editor != null) {
      editor.queryPlace(place);
    }
  }

  @Override
  public void setHistory(final History history) {
  }
}
