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

import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.vcs.IssueNavigationConfiguration;
import consulo.ide.impl.idea.openapi.vcs.IssueNavigationLink;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.vcs.VcsBundle;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class IssueNavigationConfigurationPanel implements SearchableConfigurable, Configurable.NoScroll {
  private JBTable myLinkTable;
  private final Project myProject;
  private List<IssueNavigationLink> myLinks;
  private ListTableModel<IssueNavigationLink> myModel;

  private final ColumnInfo<IssueNavigationLink, String> ISSUE_COLUMN = new ColumnInfo<>(VcsBundle.message("issue.link.issue.column")) {
    @Override
    public String valueOf(IssueNavigationLink issueNavigationLink) {
      return issueNavigationLink.getIssueRegexp();
    }
  };

  private final ColumnInfo<IssueNavigationLink, String> LINK_COLUMN = new ColumnInfo<>(VcsBundle.message("issue.link.link.column")) {
    @Override
    public String valueOf(IssueNavigationLink issueNavigationLink) {
      return issueNavigationLink.getLinkRegexp();
    }
  };

  private JPanel myPanel;

  public IssueNavigationConfigurationPanel(Project project) {
    myProject = project;
  }

  private JPanel createPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    myLinkTable = new JBTable();
    myLinkTable.getEmptyText().setText(VcsBundle.message("issue.link.no.patterns"));
    panel.add(new JLabel(XmlStringUtil.wrapInHtml(
            ApplicationNamesInfo.getInstance().getFullProductName() + " will search for the specified patterns in " + "checkin comments and link them to issues in your issue tracker:")),
              BorderLayout.NORTH);
    panel.add(ToolbarDecorator.createDecorator(myLinkTable).setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        IssueLinkConfigurationDialog dlg = new IssueLinkConfigurationDialog(myProject);
        dlg.setTitle(VcsBundle.message("issue.link.add.title"));
        dlg.show();
        if (dlg.isOK()) {
          myLinks.add(dlg.getLink());
          myModel.fireTableDataChanged();
        }
      }
    }).setRemoveAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        if (Messages.showOkCancelDialog(myProject, VcsBundle.message("issue.link.delete.prompt"), VcsBundle.message("issue.link.delete.title"), Messages.getQuestionIcon()) == 0) {
          int selRow = myLinkTable.getSelectedRow();
          myLinks.remove(selRow);
          myModel.fireTableDataChanged();
          if (myLinkTable.getRowCount() > 0) {
            if (selRow >= myLinkTable.getRowCount()) {
              selRow--;
            }
            myLinkTable.getSelectionModel().setSelectionInterval(selRow, selRow);
          }
        }
      }
    }).setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        IssueNavigationLink link = (IssueNavigationLink)myModel.getItem(myLinkTable.getSelectedRow());
        IssueLinkConfigurationDialog dlg = new IssueLinkConfigurationDialog(myProject);
        dlg.setTitle(VcsBundle.message("issue.link.edit.title"));
        dlg.setLink(link);
        dlg.show();
        if (dlg.isOK()) {
          final IssueNavigationLink editedLink = dlg.getLink();
          link.setIssueRegexp(editedLink.getIssueRegexp());
          link.setLinkRegexp(editedLink.getLinkRegexp());
          myModel.fireTableDataChanged();
        }
      }
    }).addExtraAction(new AnActionButton("Add JIRA Pattern", IconUtil.getAddJiraPatternIcon()) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(AnActionEvent e) {
        String s = Messages.showInputDialog(panel, "Enter JIRA installation URL:", "Add JIRA Issue Navigation Pattern", Messages.getQuestionIcon());
        if (s == null) {
          return;
        }
        if (!s.endsWith("/")) {
          s += "/";
        }
        myLinks.add(new IssueNavigationLink("[A-Z]+\\-\\d+", s + "browse/$0"));
        myModel.fireTableDataChanged();
      }
    }).addExtraAction(new AnActionButton("Add YouTrack Pattern", IconUtil.getAddYouTrackPatternIcon()) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(AnActionEvent e) {
        String s = Messages.showInputDialog(panel, "Enter YouTrack installation URL:", "Add YouTrack Issue Navigation Pattern", Messages.getQuestionIcon());
        if (s == null) {
          return;
        }
        if (!s.endsWith("/")) {
          s += "/";
        }
        myLinks.add(new IssueNavigationLink("[A-Z]+\\-\\d+", s + "issue/$0"));
        myModel.fireTableDataChanged();
      }
    }).setButtonComparator("Add", "Add JIRA Pattern", "Add YouTrack Pattern", "Edit", "Remove").disableUpDownActions().createPanel(), BorderLayout.CENTER);

    return panel;
  }

  @RequiredUIAccess
  @Override
  public void apply() {
    IssueNavigationConfiguration configuration = IssueNavigationConfiguration.getInstance(myProject);
    configuration.setLinks(myLinks);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    IssueNavigationConfiguration configuration = IssueNavigationConfiguration.getInstance(myProject);
    return !myLinks.equals(configuration.getLinks());
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    IssueNavigationConfiguration configuration = IssueNavigationConfiguration.getInstance(myProject);
    myLinks = new ArrayList<>();
    for (IssueNavigationLink link : configuration.getLinks()) {
      myLinks.add(new IssueNavigationLink(link.getIssueRegexp(), link.getLinkRegexp()));
    }
    myModel = new ListTableModel<>(new ColumnInfo[]{ISSUE_COLUMN, LINK_COLUMN}, myLinks, 0);
    myLinkTable.setModel(myModel);
  }

  @Override
  @Nls
  public String getDisplayName() {
    return "Issue Navigation";
  }

  @Override
  @Nonnull
  public String getId() {
    return "project.propVCSSupport.Issue.Navigation";
  }

  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    myPanel = createPanel();
    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myPanel = null;
    myLinkTable = null;
  }
}
