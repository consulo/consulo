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

import consulo.application.util.DateFormatUtil;
import consulo.localHistory.LocalHistoryBundle;
import consulo.localHistory.impl.internal.IdeaGateway;
import consulo.localHistory.impl.internal.LocalHistoryFacade;
import consulo.localHistory.impl.internal.revision.RecentChange;
import consulo.project.Project;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.popup.AWTPopupFactory;
import consulo.ui.ex.popup.JBPopupFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RecentChangesPopup {
    private final Project myProject;
    private final IdeaGateway myGateway;
    private final LocalHistoryFacade myVcs;

    public RecentChangesPopup(Project project, IdeaGateway gw, LocalHistoryFacade vcs) {
        myProject = project;
        myGateway = gw;
        myVcs = vcs;
    }

    public void show() {
        List<RecentChange> cc = myVcs.getRecentChanges(myGateway.createTransientRootEntry());
        if (cc.isEmpty()) {
            Messages.showInfoMessage(myProject, LocalHistoryBundle.message("recent.changes.to.changes"), getTitle());
            return;
        }

        final JList list = new JBList(createModel(cc));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RecentChangesListCellRenderer());

        Runnable selectAction = new Runnable() {
            public void run() {
                RecentChange c = (RecentChange) list.getSelectedValue();
                showRecentChangeDialog(c);
            }
        };

        showList(list, selectAction);
    }

    private ListModel createModel(List<RecentChange> cc) {
        DefaultListModel m = new DefaultListModel();
        for (RecentChange c : cc) {
            m.addElement(c);
        }
        return m;
    }

    private void showList(JList list, Runnable selectAction) {
        AWTPopupFactory factory = (AWTPopupFactory) JBPopupFactory.getInstance();

        factory.createListPopupBuilder(list).
            setTitle(getTitle()).
            setItemChosenCallback(o -> selectAction.run()).
            createPopup().
            showCenteredInCurrentWindow(myProject);
    }

    private void showRecentChangeDialog(RecentChange c) {
        new RecentChangeDialog(myProject, myGateway, c).show();
    }

    private String getTitle() {
        return LocalHistoryBundle.message("recent.changes.popup.title");
    }

    private static class RecentChangesListCellRenderer implements ListCellRenderer {
        private final JPanel myPanel = new JPanel(new BorderLayout());
        private final JLabel myActionLabel = new JLabel("", JLabel.LEFT);
        private final JLabel myDateLabel = new JLabel("", JLabel.RIGHT);
        private final JPanel mySpacePanel = new JPanel();

        public RecentChangesListCellRenderer() {
            myPanel.add(myActionLabel, BorderLayout.WEST);
            myPanel.add(myDateLabel, BorderLayout.EAST);
            myPanel.add(mySpacePanel, BorderLayout.CENTER);

            Dimension d = new Dimension(40, mySpacePanel.getPreferredSize().height);
            mySpacePanel.setMinimumSize(d);
            mySpacePanel.setMaximumSize(d);
            mySpacePanel.setPreferredSize(d);
        }

        public Component getListCellRendererComponent(JList l, Object val, int i, boolean isSelected, boolean cellHasFocus) {
            RecentChange c = (RecentChange) val;
            myActionLabel.setText(c.getChangeName());
            myDateLabel.setText(DateFormatUtil.formatPrettyDateTime(c.getTimestamp()));

            updateColors(isSelected);
            return myPanel;
        }

        private void updateColors(boolean isSelected) {
            Color bg = isSelected ? UIUtil.getTableSelectionBackground() : UIUtil.getTableBackground();
            Color fg = isSelected ? UIUtil.getTableSelectionForeground() : UIUtil.getTableForeground();

            setColors(bg, fg, myPanel, myActionLabel, myDateLabel, mySpacePanel);
        }

        private void setColors(Color bg, Color fg, JComponent... cc) {
            for (JComponent c : cc) {
                c.setBackground(bg);
                c.setForeground(fg);
            }
        }
    }
}
