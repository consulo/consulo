/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots.ui.configuration.classpath;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.classpath.dependencyTab.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class AddModuleDependencyDialog extends DialogWrapper {
  private JBEditorTabs myTabs;

  public AddModuleDependencyDialog(@NotNull ClasspathPanel panel, StructureConfigurableContext context) {
    super(panel.getComponent(), true);

    ModifiableRootModel rootModel = panel.getRootModel();

    myTabs = new JBEditorTabs(rootModel.getProject(), ActionManager.getInstance(), IdeFocusManager.getInstance(rootModel.getProject()), myDisposable);
    myTabs.setTabsPosition(JBTabsPosition.left);

    AddModuleDependencyTabFactory[] tabs = AddModuleDependencyTabFactory.EP_NAME.getExtensions();
    for (int i = 0; i < tabs.length; i++) {
      AddModuleDependencyTabFactory tab = tabs[i];

      AddModuleDependencyTabContext tabContext = tab.createTabContext(myDisposable, panel, context);

      JComponent component = tabContext.getComponent();
      JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);
      TabInfo tabInfo = new TabInfo(scrollPane);
      tabInfo.setObject(tabContext);
      tabInfo.setText(tabContext.getTabName());
      tabInfo.setEnabled(!tabContext.isEmpty());

      myTabs.addTab(tabInfo, i);
    }
    setTitle("Add Dependencies");
    init();
  }

  @Override
  protected void doOKAction() {
    TabInfo selectedInfo = myTabs.getSelectedInfo();
    if(selectedInfo != null) {
      AddModuleDependencyTabContext tabContext = (AddModuleDependencyTabContext)selectedInfo.getObject();

      tabContext.processAddOrderEntries(this);
    }
    super.doOKAction();
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    setSize(450, 600);
    return getClass().getSimpleName() + "#dialog";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JComponent component = myTabs.getComponent();
    component.setBorder(IdeBorderFactory.createEmptyBorder(0, 1, 1, 1));
    return component;
  }
}
