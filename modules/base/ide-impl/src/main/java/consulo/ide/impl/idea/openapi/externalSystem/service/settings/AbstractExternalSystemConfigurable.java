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
package consulo.ide.impl.idea.openapi.externalSystem.service.settings;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.execution.ExternalSystemSettingsControl;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class that simplifies external system settings management.
 * <p/>
 * The general idea is to provide a control which looks like below:
 * <pre>
 *    ----------------------------------------------
 *   |   linked external projects list              |
 *   |----------------------------------------------
 *   |   linked project-specific settings           |
 *   |----------------------------------------------
 *   |   external system-wide settings (optional)   |
 * ----------------------------------------------
 * </pre>
 *
 * @author Denis Zhdanov
 * @since 4/30/13 12:50 PM
 */
public abstract class AbstractExternalSystemConfigurable<ProjectSettings extends ExternalProjectSettings, L extends ExternalSystemSettingsListener<ProjectSettings>, SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>>
        implements SearchableConfigurable {

  @Nonnull
  private final List<ExternalSystemSettingsControl<ProjectSettings>> myProjectSettingsControls = new ArrayList<>();

  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final Project myProject;

  @Nullable
  private ExternalSystemSettingsControl<SystemSettings> mySystemSettingsControl;
  @Nullable
  private ExternalSystemSettingsControl<ProjectSettings> myActiveProjectSettingsControl;

  private PaintAwarePanel myComponent;
  private JBList<String> myProjectsList;
  private DefaultListModel<String> myProjectsModel;

  protected AbstractExternalSystemConfigurable(@Nonnull Project project, @Nonnull ProjectSystemId externalSystemId) {
    myProject = project;
    myExternalSystemId = externalSystemId;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myExternalSystemId.getReadableName().get();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (myComponent == null) {
      myComponent = new PaintAwarePanel(new GridBagLayout());
      SystemSettings settings = getSettings();
      prepareProjectSettings(settings, uiDisposable);
      prepareSystemSettings(settings, uiDisposable);
      ExternalSystemUiUtil.fillBottom(myComponent);
    }
    return myComponent;
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  private SystemSettings getSettings() {
    ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?> manager = (ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?>)ExternalSystemApiUtil.getManager(myExternalSystemId);
    assert manager != null;
    return manager.getSettingsProvider().apply(myProject);
  }

  private void prepareProjectSettings(@Nonnull SystemSettings s, Disposable uiDisposable) {
    myProjectsModel = new DefaultListModel<>();
    myProjectsList = new JBList<>(myProjectsModel);
    myProjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    addTitle(ExternalSystemBundle.message("settings.title.linked.projects", myExternalSystemId.getReadableName()));
    myComponent.add(new JBScrollPane(myProjectsList), ExternalSystemUiUtil.getFillLineConstraints(1));

    addTitle(ExternalSystemBundle.message("settings.title.project.settings"));
    List<ProjectSettings> settings = new ArrayList<ProjectSettings>(s.getLinkedProjectsSettings());
    myProjectsList.setVisibleRowCount(Math.max(3, Math.min(5, settings.size())));
    ContainerUtil.sort(settings, (s1, s2) -> getProjectName(s1.getExternalProjectPath()).compareTo(getProjectName(s2.getExternalProjectPath())));

    myProjectSettingsControls.clear();
    for (ProjectSettings setting : settings) {
      ExternalSystemSettingsControl<ProjectSettings> control = createProjectSettingsControl(setting);
      control.fillUi(uiDisposable, myComponent, 1);
      myProjectsModel.addElement(getProjectName(setting.getExternalProjectPath()));
      myProjectSettingsControls.add(control);
      control.showUi(false);
    }

    myProjectsList.addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      int i = myProjectsList.getSelectedIndex();
      if (i < 0) {
        return;
      }
      if (myActiveProjectSettingsControl != null) {
        myActiveProjectSettingsControl.showUi(false);
      }
      myActiveProjectSettingsControl = myProjectSettingsControls.get(i);
      myActiveProjectSettingsControl.showUi(true);
    });


    if (!myProjectsModel.isEmpty()) {
      addTitle(ExternalSystemBundle.message("settings.title.system.settings", myExternalSystemId.getReadableName()));
      myProjectsList.setSelectedIndex(0);
    }
  }

  private void addTitle(@Nonnull String title) {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(IdeBorderFactory.createTitledBorder(title, false, JBUI.insetsTop(ExternalSystemUiUtil.INSETS)));
    myComponent.add(panel, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  /**
   * Creates a control for managing given project settings.
   *
   * @param settings target external project settings
   * @return control for managing given project settings
   */
  @Nonnull
  protected abstract ExternalSystemSettingsControl<ProjectSettings> createProjectSettingsControl(@Nonnull ProjectSettings settings);

  @SuppressWarnings("MethodMayBeStatic")
  @Nonnull
  protected String getProjectName(@Nonnull String path) {
    File file = new File(path);
    return file.isDirectory() || file.getParentFile() == null ? file.getName() : file.getParentFile().getName();
  }

  private void prepareSystemSettings(@Nonnull SystemSettings s, @Nonnull Disposable uiDisposable) {
    mySystemSettingsControl = createSystemSettingsControl(s);
    if (mySystemSettingsControl != null) {
      mySystemSettingsControl.fillUi(uiDisposable, myComponent, 1);
    }
  }

  /**
   * Creates a control for managing given system-level settings (if any).
   *
   * @param settings target system settings
   * @return a control for managing given system-level settings;
   * <code>null</code> if current external system doesn't have system-level settings (only project-level settings)
   */
  @Nullable
  protected abstract ExternalSystemSettingsControl<SystemSettings> createSystemSettingsControl(@Nonnull SystemSettings settings);

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      if (control.isModified()) {
        return true;
      }
    }
    return mySystemSettingsControl != null && mySystemSettingsControl.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    SystemSettings systemSettings = getSettings();
    L publisher = systemSettings.getPublisher();
    publisher.onBulkChangeStart();
    try {
      List<ProjectSettings> projectSettings = new ArrayList<>();
      for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
        ProjectSettings s = newProjectSettings();
        control.apply(s);
        projectSettings.add(s);
      }
      systemSettings.setLinkedProjectsSettings(projectSettings);
      for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
        if (control instanceof AbstractExternalProjectSettingsControl) {
          AbstractExternalProjectSettingsControl.class.cast(control).updateInitialSettings();
        }
      }
      if (mySystemSettingsControl != null) {
        mySystemSettingsControl.apply(systemSettings);
      }
    }
    finally {
      publisher.onBulkChangeEnd();
    }
  }

  /**
   * @return new empty project-level settings object
   */
  @Nonnull
  protected abstract ProjectSettings newProjectSettings();

  @RequiredUIAccess
  @Override
  public void reset() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      control.reset();
    }
    if (mySystemSettingsControl != null) {
      mySystemSettingsControl.reset();
    }
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    for (ExternalSystemSettingsControl<ProjectSettings> control : myProjectSettingsControls) {
      control.disposeUIResources();
    }
    myProjectSettingsControls.clear();
    myComponent = null;
    myProjectsList = null;
    myProjectsModel = null;
    mySystemSettingsControl = null;
  }
}
