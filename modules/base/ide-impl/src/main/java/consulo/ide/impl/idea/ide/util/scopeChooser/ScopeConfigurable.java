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

package consulo.ide.impl.idea.ide.util.scopeChooser;

import consulo.configurable.ConfigurationException;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.util.lang.Comparing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * @author anna
 * @since 2006-07-01
 */
public class ScopeConfigurable extends NamedConfigurable<NamedScope> {
  private NamedScope myScope;
  private ScopeEditorPanel myPanel;
  private String myPackageSet;
  private final JCheckBox mySharedCheckbox;
  private boolean myShareScope = false;
  private final Project myProject;

  public ScopeConfigurable(final NamedScope scope, final boolean shareScope, final Project project, final Runnable updateTree) {
    super(true, updateTree);
    myScope = scope;
    myShareScope = shareScope;
    myProject = project;
    mySharedCheckbox = new JCheckBox(IdeLocalize.shareScopeCheckboxTitle().get(), shareScope);
    myPanel = new ScopeEditorPanel(project, getHolder());
    mySharedCheckbox.addActionListener(e -> myPanel.setHolder(getHolder()));
  }

  @Override
  public void setDisplayName(final String name) {
    if (Comparing.strEqual(myScope.getName(), name)){
      return;
    }
    final PackageSet packageSet = myScope.getValue();
    myScope = new NamedScope(name, packageSet != null ? packageSet.createCopy() : null);
  }

  @Override
  public NamedScope getEditableObject() {
    return new NamedScope(myScope.getName(), myPanel.getCurrentScope());
  }

  @Override
  public String getBannerSlogan() {
    return IdeLocalize.scopeBannerText(myScope.getName()).get();
  }

  @Override
  public String getDisplayName() {
    return myScope.getName();
  }

  public NamedScopesHolder getHolder() {
    return getHolder(mySharedCheckbox.isSelected());
  }

  private NamedScopesHolder getHolder(boolean local) {
    return local
            ? DependencyValidationManager.getInstance(myProject)
            : NamedScopeManager.getInstance(myProject);
  }

  @Override
  public JComponent createOptionsPanel() {
    final JPanel wholePanel = new JPanel(new BorderLayout());
    wholePanel.setBorder(new EmptyBorder(0, 8, 0, 8));
    wholePanel.add(myPanel.getPanel(), BorderLayout.CENTER);
    wholePanel.add(mySharedCheckbox, BorderLayout.SOUTH);
    return wholePanel;
  }

  @Override
  public boolean isModified() {
    if (mySharedCheckbox.isSelected() != myShareScope) return true;
    final PackageSet currentScope = myPanel.getCurrentScope();
    return !Comparing.strEqual(myPackageSet, currentScope != null ? currentScope.getText() : null);
  }

  @Override
  public void apply() throws ConfigurationException {
    try {
      myPanel.apply();
      final PackageSet packageSet = myPanel.getCurrentScope();
      myScope = new NamedScope(myScope.getName(), packageSet);
      myPackageSet = packageSet != null ? packageSet.getText() : null;
      myShareScope = mySharedCheckbox.isSelected();
    }
    catch (ConfigurationException e) {
      //was canceled - didn't change anything
    }
  }

  @Override
  public void reset() {
    mySharedCheckbox.setSelected(myShareScope);
    myPanel.reset(myScope.getValue(), null);
    final PackageSet packageSet = myScope.getValue();
    myPackageSet = packageSet != null ? packageSet.getText() : null;
  }

  @Override
  public void disposeUIResources() {
    if (myPanel != null){
      myPanel.cancelCurrentProgress();
      myPanel.clearCaches();
      myPanel = null;
    }
  }

  public void cancelCurrentProgress(){
    if (myPanel != null) { //not disposed
      myPanel.cancelCurrentProgress();
    }
  }

  public NamedScope getScope() {
    return myScope;
  }

  public void restoreCanceledProgress() {
    if (myPanel != null) {
      myPanel.restoreCanceledProgress();
    }
  }
}
