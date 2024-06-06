/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.CommitSession;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SessionDialog extends DialogWrapper {

  @NonNls public static final String VCS_CONFIGURATION_UI_TITLE = "Vcs.SessionDialog.title";

  private final CommitSession mySession;
  private final List<Change> myChanges;

  private final String myCommitMessage;

  private final JPanel myCenterPanel = new JPanel(new BorderLayout());
  private final JComponent myConfigurationComponent;

  public SessionDialog(String title, Project project,
                       CommitSession session, List<Change> changes,
                       String commitMessage, @Nullable JComponent configurationComponent) {
    super(project, true);
    mySession = session;
    myChanges = changes;
    myCommitMessage = commitMessage;
    myConfigurationComponent =
            configurationComponent == null ? createConfigurationUI(mySession, myChanges, myCommitMessage) : configurationComponent;
    String configurationComponentName =
            myConfigurationComponent != null ? (String)myConfigurationComponent.getClientProperty(VCS_CONFIGURATION_UI_TITLE) : null;
    setTitle(StringUtil.isEmptyOrSpaces(configurationComponentName)
             ? CommitChangeListDialog.trimEllipsis(title) : configurationComponentName);
    init();
    initValidation();
  }

  public SessionDialog(String title, Project project,
                       CommitSession session, List<Change> changes,
                       String commitMessage) {
    this(title, project, session, changes, commitMessage, null);
  }

  @Nullable
  public static JComponent createConfigurationUI(final CommitSession session, final List<Change> changes, final String commitMessage) {
    try {
      return session.getAdditionalConfigurationUI(changes, commitMessage);
    }
    catch(AbstractMethodError e) {
      return session.getAdditionalConfigurationUI();
    }
  }

  @jakarta.annotation.Nullable
  protected JComponent createCenterPanel() {
    myCenterPanel.add(myConfigurationComponent, BorderLayout.CENTER);
    return myCenterPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myConfigurationComponent);
  }

  @jakarta.annotation.Nullable
  @Override
  protected ValidationInfo doValidate() {
    updateButtons();
    return mySession.validateFields();
  }

  private void updateButtons() {
    setOKActionEnabled(mySession.canExecute(myChanges, myCommitMessage));
  }

  @Override
  protected String getHelpId() {
    try {
      return mySession.getHelpId();
    }
    catch (AbstractMethodError e) {
      return null;
    }
  }
}
