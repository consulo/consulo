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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfigurableProvider;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.conflicts.ChangelistConflictConfigurable;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.IgnoredSettingsPanel;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtensionImpl
public class VcsManagerConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll, ProjectConfigurable {
  private final Project myProject;
  private VcsDirectoryConfigurationPanel myMappings;
  private VcsGeneralConfigurationPanel myGeneralPanel;

  @Inject
  public VcsManagerConfigurable(Project project) {
    myProject = project;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    myMappings = new VcsDirectoryConfigurationPanel(myProject, uiDisposable);
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

  @Nonnull
  @Override
  public String getDisplayName() {
    return VcsBundle.message("version.control.main.configurable.name");
  }

  @Override
  @Nonnull
  public String getId() {
    return StandardConfigurableIds.VCS_GROUP;
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

    List<Configurable> result = new ArrayList<>();

    result.add(myGeneralPanel);
    result.add(new VcsBackgroundOperationsConfigurationPanel(myProject));

    if (!myProject.isDefault()) {
      result.add(new IgnoredSettingsPanel(myProject));
    }

    result.add(new IssueNavigationConfigurationPanel(myProject));
    if (!myProject.isDefault()) {
      result.add(new ChangelistConflictConfigurable(ChangeListManagerImpl.getInstanceImpl(myProject)));
    }

    myProject.getExtensionPoint(VcsConfigurableProvider.class).forEachExtensionSafe(provider -> {
      final Configurable configurable = provider.getConfigurable(myProject);
      if (configurable != null) {
        result.add(configurable);
      }
    });

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
