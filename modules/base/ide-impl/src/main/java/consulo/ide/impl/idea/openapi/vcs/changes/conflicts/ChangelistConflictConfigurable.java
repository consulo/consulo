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
package consulo.ide.impl.idea.openapi.vcs.changes.conflicts;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.ide.impl.idea.openapi.options.binding.BindControl;
import consulo.ide.impl.idea.openapi.options.binding.BindableConfigurable;
import consulo.ide.impl.idea.openapi.options.binding.ControlBinder;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ui.ex.awt.JBList;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public class ChangelistConflictConfigurable extends BindableConfigurable implements SearchableConfigurable, Configurable.NoScroll {

  private JPanel myPanel;
  private JPanel myOptionsPanel;

  @BindControl("TRACKING_ENABLED")
  private JCheckBox myEnableCheckBox;

  @BindControl("SHOW_DIALOG")
  private JCheckBox myShowDialogCheckBox;

  @BindControl("HIGHLIGHT_CONFLICTS")
  private JCheckBox myHighlightConflictsCheckBox;

  @BindControl("HIGHLIGHT_NON_ACTIVE_CHANGELIST")
  private JCheckBox myHighlightNonActiveCheckBox;

  private JBList myIgnoredFiles;
  private JButton myClearButton;
  private boolean myIgnoredFilesCleared;

  private final ChangelistConflictTracker myConflictTracker;

  public ChangelistConflictConfigurable(ChangeListManagerImpl manager) {
    super(new ControlBinder(manager.getConflictTracker().getOptions()));
    
    myEnableCheckBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        UIUtil.setEnabled(myOptionsPanel, myEnableCheckBox.isSelected(), true);
      }
    });
    myConflictTracker = manager.getConflictTracker();

    myClearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        myIgnoredFiles.setModel(new DefaultListModel());
        myIgnoredFilesCleared = true;
        myClearButton.setEnabled(false);
      }
    });

    myIgnoredFiles.getEmptyText().setText(VcsLocalize.noIgnoredFiles());
  }

  public JComponent createComponent() {
    getBinder().bindAnnotations(this);
    return myPanel;
  }

  @Override
  public void reset() {
    super.reset();
    Collection<String> conflicts = myConflictTracker.getIgnoredConflicts();
    myIgnoredFiles.setListData(ArrayUtil.toStringArray(conflicts));
    myClearButton.setEnabled(!conflicts.isEmpty());
    UIUtil.setEnabled(myOptionsPanel, myEnableCheckBox.isSelected(), true);    
  }

  @Override
  public void apply() throws ConfigurationException {
    super.apply();
    if (myIgnoredFilesCleared) {
      for (ChangelistConflictTracker.Conflict conflict : myConflictTracker.getConflicts().values()) {
        conflict.ignored = false;        
      }
    }
    myConflictTracker.optionsChanged();
  }

  @Override
  public boolean isModified() {
    return super.isModified() || myIgnoredFiles.getModel().getSize() != myConflictTracker.getIgnoredConflicts().size();
  }

  @Nls
  public String getDisplayName() {
    return "Changelist Conflicts";
  }

  @Nonnull
  public String getId() {
    return "project.propVCSSupport.ChangelistConflict";
  }

  public Runnable enableSearch(String option) {
    return null;
  }
}
