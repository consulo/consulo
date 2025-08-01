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

package consulo.localHistory.impl.internal.ui.view;

import consulo.localHistory.LocalHistoryBundle;
import consulo.localHistory.impl.internal.IdeaGateway;
import consulo.localHistory.impl.internal.LocalHistoryFacade;
import consulo.localHistory.impl.internal.revision.RecentChange;
import consulo.localHistory.impl.internal.ui.model.DirectoryHistoryDialogModel;
import consulo.localHistory.impl.internal.ui.model.RecentChangeDialogModel;
import consulo.project.Project;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RecentChangeDialog extends DirectoryHistoryDialog {
  private final RecentChange myChange;

  public RecentChangeDialog(Project p, IdeaGateway gw, RecentChange c) {
    super(p, gw, null, false);
    myChange = c;
    init();
  }

  @Override
  protected DirectoryHistoryDialogModel createModel(LocalHistoryFacade vcs) {
    return new RecentChangeDialogModel(myProject, myGateway, vcs, myChange);
  }

  @Override
  protected JComponent createComponent() {
    JPanel result = new JPanel(new BorderLayout());
    result.add(super.createComponent(), BorderLayout.CENTER);
    result.add(createButtonsPanel(), BorderLayout.SOUTH);
    return result;
  }

  @Override
  protected boolean showRevisionsList() {
    return false;
  }

  @Override
  protected boolean showSearchField() {
    return false;
  }

  private JPanel createButtonsPanel() {
    AbstractAction revert = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        revert();
        close();
      }
    };
    UIUtil.setActionNameAndMnemonic(LocalHistoryBundle.message("action.revert"), revert);

    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    p.add(Box.createHorizontalGlue());
    p.add(new JButton(revert));
    return p;
  }

  @Override
  protected String getHelpId() {
    return "reference.dialogs.recentChanges";
  }
}
