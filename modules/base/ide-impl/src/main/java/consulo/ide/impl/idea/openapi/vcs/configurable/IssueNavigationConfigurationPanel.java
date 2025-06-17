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

import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.image.Image;
import consulo.versionControlSystem.IssueNavigationConfiguration;
import consulo.versionControlSystem.IssueNavigationLink;
import consulo.versionControlSystem.IssueNavigationLinkProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@SuppressWarnings("ExtensionImplIsNotAnnotated")
public class IssueNavigationConfigurationPanel implements SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable {
    private static class GeneralIssueLinkAction extends DumbAwareAction {
        private final Project myProject;
        private final ListTableModel<IssueNavigationLink> myModel;

        public GeneralIssueLinkAction(Project project, ListTableModel<IssueNavigationLink> model) {
            super(LocalizeValue.localizeTODO("General Pattern"), LocalizeValue.of());
            myProject = project;
            myModel = model;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            IssueLinkConfigurationDialog dlg = new IssueLinkConfigurationDialog(myProject);
            dlg.setTitle(VcsLocalize.issueLinkAddTitle());
            dlg.show();
            if (dlg.isOK()) {
                myModel.addRow(dlg.getLink());
            }
        }
    }

    private static class ProviderIssueLinkAction extends DumbAwareAction {
        private final IssueNavigationLinkProvider myProvider;
        private final JComponent myParent;
        private final ListTableModel<IssueNavigationLink> myModel;

        public ProviderIssueLinkAction(IssueNavigationLinkProvider provider, JComponent parent, ListTableModel<IssueNavigationLink> model) {
            super(provider.getDisplayName(), provider.getDisplayName(), provider.getIcon());
            myProvider = provider;
            myParent = parent;
            myModel = model;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myProvider.ask(myParent).whenComplete((issueNavigationLink, throwable) -> {
                if (issueNavigationLink != null) {
                    myModel.addRow(issueNavigationLink);
                }
            });
        }
    }

    private JBTable myLinkTable;
    private final Project myProject;
    private List<IssueNavigationLink> myLinks;
    private ListTableModel<IssueNavigationLink> myModel;

    private final ColumnInfo<IssueNavigationLink, String> ISSUE_COLUMN = new ColumnInfo<>(VcsLocalize.issueLinkIssueColumn().get()) {
        @Override
        public String valueOf(IssueNavigationLink issueNavigationLink) {
            return issueNavigationLink.getIssueRegexp();
        }
    };

    private final ColumnInfo<IssueNavigationLink, String> LINK_COLUMN = new ColumnInfo<>(VcsLocalize.issueLinkLinkColumn().get()) {
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
        myLinkTable.getEmptyText().setText(VcsLocalize.issueLinkNoPatterns());
        panel.add(
            new JLabel(
                XmlStringUtil.wrapInHtml(
                    Application.get().getName() + " will search for the specified patterns in " +
                        "checkin comments and link them to issues in your issue tracker:"
                )
            ),
            BorderLayout.NORTH
        );

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();

        builder.add(new ActionGroup() {
            @Nonnull
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                List<AnAction> list = new ArrayList<>();
                list.add(new GeneralIssueLinkAction(myProject, myModel));

                list.add(AnSeparator.create());
                
                myProject.getApplication().getExtensionPoint(IssueNavigationLinkProvider.class).forEachExtensionSafe(template -> {
                    list.add(new ProviderIssueLinkAction(template, myPanel, myModel));
                });

                return list.toArray(AnAction.ARRAY_FACTORY);
            }

            @Nullable
            @Override
            protected Image getTemplateIcon() {
                return PlatformIconGroup.generalAdd();
            }

            @Override
            public boolean isPopup() {
                return true;
            }

            @Override
            public boolean isDumbAware() {
                return true;
            }
        });

        builder.add(new DumbAwareAction(CommonLocalize.buttonRemove(), LocalizeValue.of(), PlatformIconGroup.generalRemove()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                if (Messages.showOkCancelDialog(
                    myProject,
                    VcsLocalize.issueLinkDeletePrompt().get(),
                    VcsLocalize.issueLinkDeleteTitle().get(),
                    Messages.getQuestionIcon()
                ) == Messages.OK) {
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

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(myLinkTable.getSelectedRow() != -1);
            }
        });

        builder.add(new DumbAwareAction(CommonLocalize.buttonEdit(), LocalizeValue.of(), PlatformIconGroup.actionsEdit()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                IssueNavigationLink link = myModel.getItem(myLinkTable.getSelectedRow());
                IssueLinkConfigurationDialog dlg = new IssueLinkConfigurationDialog(myProject);
                dlg.setTitle(VcsLocalize.issueLinkEditTitle());
                dlg.setLink(link);
                dlg.show();
                if (dlg.isOK()) {
                    final IssueNavigationLink editedLink = dlg.getLink();
                    link.setIssueRegexp(editedLink.getIssueRegexp());
                    link.setLinkRegexp(editedLink.getLinkRegexp());
                    myModel.fireTableDataChanged();
                }
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(myLinkTable.getSelectedRow() != -1);
            }
        });

        ActionManager manager = ActionManager.getInstance();

        ActionToolbar toolbar = manager.createActionToolbar("IssueNavigationPanel", builder.build(), true);
        toolbar.setTargetComponent(myLinkTable);

        JPanel tablePanel = new JPanel(new BorderLayout());
        panel.add(tablePanel, BorderLayout.CENTER);

        tablePanel.add(toolbar.getComponent(), BorderLayout.NORTH);
        tablePanel.add(ScrollPaneFactory.createScrollPane(myLinkTable), BorderLayout.CENTER);

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

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Issue Navigation";
    }

    @Override
    @Nonnull
    public String getId() {
        return "project.propVCSSupport.Issue.Navigation";
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
