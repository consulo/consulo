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

package consulo.language.copyright.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.MasterDetailsStateService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;

@ExtensionImpl
public class CopyrightProjectConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll, ProjectConfigurable {
  private final Project project;
  private ProjectSettingsPanel myOptionsPanel = null;
  private final CopyrightProfilesPanel myProfilesPanel;

  @Inject
  public CopyrightProjectConfigurable(Project project, Provider<MasterDetailsStateService> masterDetailsStateService) {
    this.project = project;
    myProfilesPanel = new CopyrightProfilesPanel(project, masterDetailsStateService);
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Copyright";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    myOptionsPanel = new ProjectSettingsPanel(project, myProfilesPanel);
    myProfilesPanel.setUpdate(() -> reloadProfiles());
    return myOptionsPanel.getMainComponent();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (myOptionsPanel != null) {
      return myOptionsPanel.isModified();
    }

    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (myOptionsPanel != null) {
      myOptionsPanel.apply();
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myOptionsPanel != null) {
      myOptionsPanel.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myOptionsPanel = null;
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  @Nonnull
  public String getId() {
    return "copyright";
  }

  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Override
  protected Configurable[] buildConfigurables() {
    return new Configurable[]{myProfilesPanel, new CopyrightFormattingConfigurable(project)};
  }

  private void reloadProfiles() {
    if (myOptionsPanel != null) {
      myOptionsPanel.reloadCopyrightProfiles();
    }
  }

}
