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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.ChangesBrowserSettingsEditor;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.ex.awt.util.Alarm;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 * @since 2006-11-30
 */
public class CommittedChangesFilterDialog extends DialogWrapper {
  private final ChangesBrowserSettingsEditor myPanel;
  private ChangeBrowserSettings mySettings;
  private final JLabel myErrorLabel = new JLabel();
  private final Alarm myValidateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
  private final Runnable myValidateRunnable = new Runnable() {
    public void run() {
      validateInput();
      myValidateAlarm.addRequest(myValidateRunnable, 500, IdeaModalityState.stateForComponent(myPanel.getComponent()));
    }
  };

  public CommittedChangesFilterDialog(Project project, ChangesBrowserSettingsEditor panel, ChangeBrowserSettings settings) {
    super(project, false);
    myPanel = panel;
    //noinspection unchecked
    myPanel.setSettings(settings);
    setTitle(VcsBundle.message("browse.changes.filter.title"));
    init();
    myErrorLabel.setForeground(Color.red);
    validateInput();
    myValidateAlarm.addRequest(myValidateRunnable, 500, IdeaModalityState.stateForComponent(myPanel.getComponent()));
  }

  @jakarta.annotation.Nullable
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(myPanel.getComponent(), BorderLayout.CENTER);
    panel.add(myErrorLabel, BorderLayout.SOUTH);
    return panel;
  }

  private void validateInput() {
    String error = myPanel.validateInput();
    setOKActionEnabled(error == null);
    myErrorLabel.setText(error == null ? " " : error);
  }

  @Override
  protected void doOKAction() {
    validateInput();
    if (isOKActionEnabled()) {
      myValidateAlarm.cancelAllRequests();
      mySettings = myPanel.getSettings();
      super.doOKAction();
    }
  }

  public ChangeBrowserSettings getSettings() {
    return mySettings;
  }

  @Override @NonNls
  protected String getDimensionServiceKey() {
    return "AbstractVcsHelper.FilterDialog." + myPanel.getDimensionServiceKey();
  }
}
