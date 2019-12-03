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
package consulo.roots.ui.configuration.classpath;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import consulo.roots.ui.configuration.classpath.dependencyTab.AddModuleDependencyTabContext;
import consulo.roots.ui.configuration.classpath.dependencyTab.AddModuleDependencyTabFactory;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.ui.DialogWrapper;
import consulo.util.dataholder.Key;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import javax.annotation.Nonnull;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.roots.ui.StripeTabPanel;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class AddModuleDependencyDialog extends DialogWrapper {
  public static final Key<AddModuleDependencyTabContext> CONTEXT_KEY = Key.create("context.key");

  private StripeTabPanel myTabs;

  @RequiredUIAccess
  public AddModuleDependencyDialog(@Nonnull ClasspathPanel panel, StructureConfigurableContext context) {
    super(panel.getComponent(), true);

    ModifiableRootModel rootModel = panel.getRootModel();

    myTabs = new StripeTabPanel();

    for (AddModuleDependencyTabFactory factory : AddModuleDependencyTabFactory.EP_NAME.getExtensions()) {
      if(!factory.isAvailable(rootModel)) {
        continue;
      }
      AddModuleDependencyTabContext tabContext = factory.createTabContext(myDisposable, panel, context);

      JComponent component = tabContext.getComponent();
      JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);

      StripeTabPanel.TabInfo tabInfo = myTabs.addTab(tabContext.getTabName(), scrollPane, component);
      tabInfo.setEnabled(!tabContext.isEmpty());
      tabInfo.putUserData(CONTEXT_KEY, tabContext);
    }

    setTitle("Add Dependencies");
    init();
  }

  @Nullable
  @Override
  protected Border createContentPaneBorder() {
    return null;
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    JComponent southPanel = super.createSouthPanel();
    if(southPanel != null) {
      southPanel.setBorder(JBUI.Borders.empty(ourDefaultBorderInsets));
      BorderLayoutPanel borderLayoutPanel = JBUI.Panels.simplePanel(southPanel);
      borderLayoutPanel.setBorder(new CustomLineBorder(JBUI.scale(1), 0, 0, 0));
      return borderLayoutPanel;
    }
    return null;
  }

  @Override
  protected void doOKAction() {
    StripeTabPanel.TabInfo selectedInfo = myTabs.getSelectedTab();
    if(selectedInfo != null) {
      AddModuleDependencyTabContext tabContext = selectedInfo.getUserData(CONTEXT_KEY);
      assert tabContext != null;
      tabContext.processAddOrderEntries(this);
    }
    super.doOKAction();
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    setScalableSize(350, 600);
    return getClass().getSimpleName() + "#dialog";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myTabs;
  }
}
