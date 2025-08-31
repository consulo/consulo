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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.configurable.ConfigurationException;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.configurable.Settings;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Kirill Likhodedov
 */
class VcsUpdateInfoScopeFilterConfigurable implements NamedScopesHolder.ScopeListener {
  
  private final JCheckBox myCheckbox;
  private final JComboBox myComboBox;
  private final Project myProject;
  private final VcsConfiguration myVcsConfiguration;
  private final NamedScopesHolder[] myNamedScopeHolders;

  VcsUpdateInfoScopeFilterConfigurable(Project project, VcsConfiguration vcsConfiguration) {
    myProject = project;
    myVcsConfiguration = vcsConfiguration;
    myCheckbox = new JCheckBox(VcsLocalize.settingsFilterUpdateProjectInfoByScope().get());
    myComboBox = new JComboBox();
    
    myComboBox.setEnabled(myCheckbox.isSelected());
    myCheckbox.addChangeListener(e -> myComboBox.setEnabled(myCheckbox.isSelected()));

    myNamedScopeHolders = NamedScopesHolder.getAllNamedScopeHolders(myProject);
    for (NamedScopesHolder holder : myNamedScopeHolders) {
      holder.addScopeListener(this);
    }
  }
  
  @Override
  public void scopesChanged() {
    reset();
  }
  

  @RequiredUIAccess
  @Nullable
  public JComponent createComponent(Disposable uiDisposable) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    panel.add(myCheckbox);
    panel.add(myComboBox);
    panel.add(Box.createHorizontalStrut(UIUtil.DEFAULT_HGAP));
    panel.add(new LinkLabel<>("Edit scopes", null, (aSource, aLinkData) -> {
      Settings optionsEditor = DataManager.getInstance().getDataContext(panel).getData(Settings.KEY);
      if (optionsEditor != null) {
        optionsEditor.select(ScopeChooserConfigurable.class);
      }
    }));
    return panel;
  }

  @RequiredUIAccess
  public boolean isModified() {
    return !Comparing.equal(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME, getScopeFilterName());
  }

  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME = getScopeFilterName();
  }

  @RequiredUIAccess
  public void reset() {
    myComboBox.removeAllItems();
    boolean selection = false;
    for (NamedScopesHolder holder : myNamedScopeHolders) {
      for (NamedScope scope : holder.getEditableScopes()) {
        myComboBox.addItem(scope.getName());
        if (!selection && scope.getName().equals(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME)) {
          selection = true;
        }
      }
    }
    if (selection) {
      myComboBox.setSelectedItem(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME);
    }
    myCheckbox.setSelected(selection);
  }

  @RequiredUIAccess
  public void disposeUIResources() {
    for (NamedScopesHolder holder : myNamedScopeHolders) {
      holder.removeScopeListener(this);
    }
  }
  
  private String getScopeFilterName() {
    if (!myCheckbox.isSelected()) {
      return null;
    }
    return (String)myComboBox.getSelectedItem();
  }

}
