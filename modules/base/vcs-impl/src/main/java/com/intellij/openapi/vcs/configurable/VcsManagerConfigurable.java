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
package com.intellij.openapi.vcs.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.conflicts.ChangelistConflictConfigurable;
import com.intellij.openapi.vcs.changes.ui.IgnoredSettingsPanel;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VcsManagerConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll {
  private final Project myProject;
  private VcsDirectoryConfigurationPanel myMappings;
  private VcsGeneralConfigurationPanel myGeneralPanel;

  @Inject
  public VcsManagerConfigurable(Project project) {
    myProject = project;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDiposable) {
    myMappings = new VcsDirectoryConfigurationPanel(myProject, uiDiposable);
    if (myGeneralPanel != null) {
      addListenerToGeneralPanel();
    }
    return myMappings;
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myMappings != null && myMappings.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    myMappings.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    super.reset();
    myMappings.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    super.disposeUIResources();
    if (myMappings != null) {
      myMappings.disposeUIResources();
    }
    if (myGeneralPanel != null) {
      myGeneralPanel.disposeUIResources();
    }
    myMappings = null;
  }

  @Override
  public String getDisplayName() {
    return VcsBundle.message("version.control.main.configurable.name");
  }

  @Override
  @Nonnull
  public String getId() {
    return getDefaultConfigurableIdValue(this);
  }

  @Nonnull
  private static String getDefaultConfigurableIdValue(final Configurable configurable) {
    final String helpTopic = configurable.getHelpTopic();
    return helpTopic == null ? configurable.getClass().getName() : helpTopic;
  }

  @Override
  protected Configurable[] buildConfigurables() {
    myGeneralPanel = new VcsGeneralConfigurationPanel(myProject){
      @Override
      public void disposeUIResources() {
        super.disposeUIResources();
        myGeneralPanel = null;
      }
    };

    if (myMappings != null) {
      myGeneralPanel.updateAvailableOptions(myMappings.getActiveVcses());
      addListenerToGeneralPanel();
    }
    else {
      myGeneralPanel.updateAvailableOptions(Arrays.asList(ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss()));
    }

    List<Configurable> result = new ArrayList<Configurable>();

    result.add(myGeneralPanel);
    result.add(new VcsBackgroundOperationsConfigurationPanel(myProject));

    if (!myProject.isDefault()) {
      result.add(new IgnoredSettingsPanel(myProject));
    }

    result.add(new IssueNavigationConfigurationPanel(myProject));
    if (!myProject.isDefault()) {
      result.add(new ChangelistConflictConfigurable(ChangeListManagerImpl.getInstanceImpl(myProject)));
    }

    for (VcsConfigurableProvider provider : VcsConfigurableProvider.EP_NAME.getExtensionList()) {
      final Configurable configurable = provider.getConfigurable(myProject);
      if (configurable != null) {
        result.add(configurable);
      }
    }

    for (AbstractVcs<?> vcs : ProjectLevelVcsManager.getInstance(myProject).getAllSupportedVcss()) {
      Configurable configurable = vcs.getConfigurable();
      if(configurable != null) {
        result.add(createVcsConfigurableWrapper(vcs, configurable));
      }
    }
    return result.toArray(new Configurable[result.size()]);
  }

  private void addListenerToGeneralPanel() {
    myMappings.addVcsListener(activeVcses -> myGeneralPanel.updateAvailableOptions(activeVcses));
  }

  private Configurable createVcsConfigurableWrapper(AbstractVcs<?> vcs, Configurable delegate) {
    return new SearchableConfigurable(){

      @Override
      @Nls
      public String getDisplayName() {
        return vcs.getDisplayName();
      }

      @Override
      public String getHelpTopic() {
        return delegate.getHelpTopic();
      }

      @RequiredUIAccess
      @Override
      public JComponent createComponent() {
        return delegate.createComponent();
      }

      @RequiredUIAccess
      @Override
      public JComponent createComponent(@Nonnull Disposable uiDisposable) {
        return delegate.createComponent(uiDisposable);
      }

      @RequiredUIAccess
      @Override
      public Component createUIComponent() {
        return delegate.createUIComponent();
      }

      @RequiredUIAccess
      @Override
      public Component createUIComponent(@Nonnull Disposable uiDisposable) {
        return delegate.createUIComponent(uiDisposable);
      }

      @RequiredUIAccess
      @Override
      public boolean isModified() {
        return delegate.isModified();
      }

      @RequiredUIAccess
      @Override
      public void apply() throws ConfigurationException {
        delegate.apply();
      }

      @RequiredUIAccess
      @Override
      public void reset() {
        delegate.reset();
      }

      @RequiredUIAccess
      @Override
      public void disposeUIResources() {
        delegate.disposeUIResources();
      }

      @Override
      @Nonnull
      public String getId() {
        return "vcs." + getDisplayName();
      }

      @Override
      public Runnable enableSearch(String option) {
        return null;
      }

      @Override
      public String toString() {
        return "VcsConfigurable for "+vcs.getDisplayName();
      }
    };
  }

}
