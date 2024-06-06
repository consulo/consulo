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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.application.AllIcons;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleWithNameAlreadyExistsException;
import consulo.configurable.ConfigurationException;
import consulo.project.ProjectBundle;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.MutableCollectionComboBoxModel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.disposer.Disposable;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions.DeleteLayerAction;
import consulo.ide.impl.roots.ui.configuration.projectRoot.moduleLayerActions.NewLayerAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;

/**
 * User: anna
 * Date: 04-Jun-2006
 */
public class ModuleConfigurable extends ProjectStructureElementConfigurable<Module> {
  private final Module myModule;
  private final ModulesConfiguratorImpl myConfigurator;
  private String myModuleName;
  private final ModuleProjectStructureElement myProjectStructureElement;

  public ModuleConfigurable(ModulesConfigurator modulesConfigurator, Module module, Runnable updateTree) {
    super(true, updateTree);
    myModule = module;
    myModuleName = myModule.getName();
    myConfigurator = (ModulesConfiguratorImpl)modulesConfigurator;
    myProjectStructureElement = new ModuleProjectStructureElement(modulesConfigurator, myModule);
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
    
    // TODO [VISTALL] myContext.getDaemonAnalyzer().queueUpdateForAllElementsWithErrors();
  }

  @Nullable
  @Override
  protected JComponent createTopRightComponent(JTextField nameField) {
    return createLayerConfigurationPanel(getModuleEditor());
  }

  @Nonnull
  private static JPanel createLayerConfigurationPanel(@Nonnull final ModuleEditor moduleEditor) {
    BorderLayoutPanel panel = JBUI.Panels.simplePanel();

    ModifiableRootModel moduleRootModel = moduleEditor.getModifiableRootModelProxy();

    final MutableCollectionComboBoxModel<String> model = new MutableCollectionComboBoxModel<String>(new ArrayList<String>(moduleRootModel.getLayers().keySet()), moduleRootModel.getCurrentLayerName());

    final ComboBox comboBox = new ComboBox(model);
    comboBox.setEnabled(model.getSize() > 1);

    moduleEditor.addChangeListener(moduleRootModel1 -> {
      model.update(new ArrayList<String>(moduleRootModel1.getLayers().keySet()));
      model.setSelectedItem(moduleRootModel1.getCurrentLayerName());
      model.update();

      comboBox.setEnabled(comboBox.getItemCount() > 1);
    });

    comboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        moduleEditor.getModifiableRootModelProxy().setCurrentLayer((String)comboBox.getSelectedItem());
      }
    });

    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new NewLayerAction(moduleEditor, false));
    group.add(new DeleteLayerAction(moduleEditor));
    group.add(new NewLayerAction(moduleEditor, true));

    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    actionToolbar.setTargetComponent(panel);
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
  public Image getIcon(final boolean open) {
    return AllIcons.Nodes.Module;
  }

  public Module getModule() {
    return myModule;
  }

  @Override
  @Nullable
  public String getHelpTopic() {
    final ModuleEditor moduleEditor = getModuleEditor();
    return moduleEditor != null ? moduleEditor.getHelpTopic() : null;
  }

  @RequiredUIAccess
  @Override
  public JComponent createOptionsPanel(@Nonnull Disposable parentUIDisposable) {
    return getModuleEditor().getPanel(parentUIDisposable);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
  }

  public ModuleEditor getModuleEditor() {
    return myConfigurator.getModuleEditor(myModule);
  }
}
