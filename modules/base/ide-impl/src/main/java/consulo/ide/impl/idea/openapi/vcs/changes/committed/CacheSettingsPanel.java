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

import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;

import javax.swing.*;

/**
 * @author yole
 */
public class CacheSettingsPanel {
  private JSpinner myCountSpinner;
  private JPanel myTopPanel;
  private JSpinner myRefreshSpinner;
  private JCheckBox myRefreshCheckbox;
  private JSpinner myDaysSpinner;
  private JLabel myCountLabel;
  private JLabel myDaysLabel;
  private CommittedChangesCache myCache;

  public CacheSettingsPanel() {
    myRefreshCheckbox.addActionListener(e -> updateControls());
  }

  public void initPanel(final Project project) {
    myCache = CommittedChangesCache.getInstance(project);
  }

  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    final CommittedChangesCache.State state = new CommittedChangesCache.State();
    state.setInitialCount(((SpinnerNumberModel)myCountSpinner.getModel()).getNumber().intValue());
    state.setInitialDays(((SpinnerNumberModel)myDaysSpinner.getModel()).getNumber().intValue());
    state.setRefreshInterval(((SpinnerNumberModel)myRefreshSpinner.getModel()).getNumber().intValue());
    state.setRefreshEnabled(myRefreshCheckbox.isSelected());
    myCache.loadState(state);
  }

  @RequiredUIAccess
  public boolean isModified() {
    CommittedChangesCache.State state = myCache.getState();

    if (state.getInitialCount() != ((SpinnerNumberModel)myCountSpinner.getModel()).getNumber().intValue()) return true;
    if (state.getInitialDays() != ((SpinnerNumberModel)myDaysSpinner.getModel()).getNumber().intValue()) return true;
    if (state.getRefreshInterval() != ((SpinnerNumberModel)myRefreshSpinner.getModel()).getNumber().intValue()) return true;
    if (state.isRefreshEnabled() != myRefreshCheckbox.isSelected()) return true;

    return false;
  }

  @RequiredUIAccess
  public void reset() {
    final CommittedChangesCache.State state = myCache.getState();

    myCountSpinner.setModel(new SpinnerNumberModel(state.getInitialCount(), 1, 100000, 10));
    myDaysSpinner.setModel(new SpinnerNumberModel(state.getInitialDays(), 1, 720, 10));
    myRefreshSpinner.setModel(new SpinnerNumberModel(state.getRefreshInterval(), 1, 60 * 24, 1));
    if (myCache.isMaxCountSupportedForProject()) {
      myDaysLabel.setVisible(false);
      myDaysSpinner.setVisible(false);
    }
    else {
      myCountLabel.setVisible(false);
      myCountSpinner.setVisible(false);
    }
    myRefreshCheckbox.setSelected(state.isRefreshEnabled());
    updateControls();

  }

  private void updateControls() {
    myRefreshSpinner.setEnabled(myRefreshCheckbox.isSelected());
  }

  public JComponent getPanel() {
    return myTopPanel;
  }

  public void setEnabled(final boolean value) {
    myRefreshCheckbox.setEnabled(value);
  }
}
