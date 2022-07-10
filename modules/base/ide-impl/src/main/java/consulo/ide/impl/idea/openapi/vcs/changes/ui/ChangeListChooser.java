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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.vcs.change.ChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListEditHandler;
import consulo.vcs.change.LocalChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.LocalChangeListImpl;
import consulo.ide.impl.idea.util.NullableConsumer;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.util.Collection;

/**
 * @author max
 */
public class ChangeListChooser extends DialogWrapper {
  private final Project myProject;
  private LocalChangeList mySelectedList;
  private final ChangeListChooserPanel myPanel;

  public ChangeListChooser(@Nonnull Project project,
                           @Nonnull Collection<? extends ChangeList> changelists,
                           @javax.annotation.Nullable ChangeList defaultSelection,
                           final String title,
                           @javax.annotation.Nullable final String suggestedName) {
    super(project, false);
    myProject = project;

    ChangeListEditHandler handler;
    for (ChangeList changelist : changelists) {
      handler = ((LocalChangeListImpl)changelist).getEditHandler();
      if (handler != null) {
        break;
      }
    }

    myPanel = new ChangeListChooserPanel(myProject, new NullableConsumer<String>() {
      public void consume(final @javax.annotation.Nullable String errorMessage) {
        setOKActionEnabled(errorMessage == null);
        setErrorText(errorMessage);
      }
    });

    myPanel.init();
    myPanel.setChangeLists(changelists);
    myPanel.setDefaultSelection(defaultSelection);

    setTitle(title);
    if (suggestedName != null) {
      myPanel.setSuggestedName(suggestedName);
    }

    init();
  }

  public JComponent getPreferredFocusedComponent() {
    return myPanel.getPreferredFocusedComponent();
  }

  protected String getDimensionServiceKey() {
    return "VCS.ChangelistChooser";
  }

  protected void doOKAction() {
    mySelectedList = myPanel.getSelectedList(myProject);
    if (mySelectedList != null) {
      super.doOKAction();
    }
  }

  @javax.annotation.Nullable
  public LocalChangeList getSelectedList() {
    return mySelectedList;
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }
}
