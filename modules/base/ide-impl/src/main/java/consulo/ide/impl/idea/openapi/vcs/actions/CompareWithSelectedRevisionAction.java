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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.util.DateFormatUtil;
import consulo.ide.impl.idea.openapi.vcs.history.VcsHistoryProviderBackgroundableProxy;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.ui.ex.awt.dualView.TreeTableView;
import consulo.ui.ex.awt.tree.table.ListTreeTableModelOnColumns;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.TreeItem;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.HistoryAsTreeProvider;
import consulo.versionControlSystem.history.VcsFileRevision;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

@ActionImpl(id = "Compare.Selected")
public class CompareWithSelectedRevisionAction extends AbstractVcsAction {
    private static final ColumnInfo<TreeNodeAdapter, String> BRANCH_COLUMN =
        new ColumnInfo<TreeNodeAdapter, String>(VcsLocalize.columnNameRevisionsListBranch()) {
            @Override
            public String valueOf(TreeNodeAdapter object) {
                return object.getRevision().getBranchName();
            }
        };

    private static final ColumnInfo<TreeNodeAdapter, String> REVISION_COLUMN =
        new ColumnInfo<TreeNodeAdapter, String>(VcsLocalize.columnNameRevisionListRevision()) {
            @Override
            public String valueOf(TreeNodeAdapter object) {
                return object.getRevision().getRevisionNumber().asString();
            }
        };

    private static final ColumnInfo<TreeNodeAdapter, String> DATE_COLUMN =
        new ColumnInfo<TreeNodeAdapter, String>(VcsLocalize.columnNameRevisionsListFilter()) {
            @Override
            public String valueOf(TreeNodeAdapter object) {
                return DateFormatUtil.formatPrettyDateTime(object.getRevision().getRevisionDate());
            }
        };

    private static final ColumnInfo<TreeNodeAdapter, String> AUTHOR_COLUMN =
        new ColumnInfo<TreeNodeAdapter, String>(VcsLocalize.columnNameRevisionListAuthor()) {
            @Override
            public String valueOf(TreeNodeAdapter object) {
                return object.getRevision().getAuthor();
            }
        };

    private static final ColumnInfo<VcsFileRevision, String> REVISION_TABLE_COLUMN =
        new ColumnInfo<VcsFileRevision, String>(VcsLocalize.columnNameRevisionListRevision()) {
            @Override
            public String valueOf(VcsFileRevision vcsFileRevision) {
                return vcsFileRevision.getRevisionNumber().asString();
            }
        };

    private static final ColumnInfo<VcsFileRevision, String> DATE_TABLE_COLUMN =
        new ColumnInfo<VcsFileRevision, String>(VcsLocalize.columnNameRevisionListRevision()) {
            @Override
            public String valueOf(VcsFileRevision vcsFileRevision) {
                Date date = vcsFileRevision.getRevisionDate();
                return date == null ? "" : DateFormatUtil.formatPrettyDateTime(date);
            }
        };

    private static final ColumnInfo<VcsFileRevision, String> AUTHOR_TABLE_COLUMN =
        new ColumnInfo<VcsFileRevision, String>(VcsLocalize.columnNameRevisionListAuthor()) {
            @Override
            public String valueOf(VcsFileRevision vcsFileRevision) {
                return vcsFileRevision.getAuthor();
            }
        };

    private static final ColumnInfo<VcsFileRevision, String> BRANCH_TABLE_COLUMN =
        new ColumnInfo<VcsFileRevision, String>(VcsLocalize.columnNameRevisionsListBranch()) {
            @Override
            public String valueOf(VcsFileRevision vcsFileRevision) {
                return vcsFileRevision.getBranchName();
            }
        };

    public CompareWithSelectedRevisionAction() {
        super(ActionLocalize.actionCompareSelectedText());
    }

    @Override
    public void update(@Nonnull VcsContext e, @Nonnull Presentation presentation) {
        AbstractShowDiffAction.updateDiffAction(presentation, e, VcsBackgroundableActions.COMPARE_WITH);
    }

    @Override
    protected boolean forceSyncUpdate(@Nonnull AnActionEvent e) {
        return true;
    }

    @Override
    @RequiredUIAccess
    protected void actionPerformed(@Nonnull VcsContext vcsContext) {
        VirtualFile file = vcsContext.getSelectedFiles()[0];
        Project project = vcsContext.getProject();
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
        VcsHistoryProvider vcsHistoryProvider = vcs.getVcsHistoryProvider();

        new VcsHistoryProviderBackgroundableProxy(vcs, vcsHistoryProvider, vcs.getDiffProvider())
            .createSessionFor(
                vcs.getKeyInstanceMethod(),
                new FilePathImpl(file),
                session -> {
                    if (session == null) {
                        return;
                    }
                    List<VcsFileRevision> revisions = session.getRevisionList();
                    HistoryAsTreeProvider treeHistoryProvider = session.getHistoryAsTreeProvider();
                    if (treeHistoryProvider != null) {
                        showTreePopup(treeHistoryProvider.createTreeOn(revisions), file, project, vcs.getDiffProvider());
                    }
                    else {
                        showListPopup(
                            revisions,
                            project,
                            revision -> DiffActionExecutor.showDiff(
                                vcs.getDiffProvider(),
                                revision.getRevisionNumber(),
                                file,
                                project,
                                VcsBackgroundableActions.COMPARE_WITH
                            ),
                            true
                        );
                    }
                },
                VcsBackgroundableActions.COMPARE_WITH,
                false,
                null
            );
    }

    private static void showTreePopup(
        List<TreeItem<VcsFileRevision>> roots,
        VirtualFile file,
        Project project,
        DiffProvider diffProvider
    ) {
        TreeTableView treeTable = new TreeTableView(new ListTreeTableModelOnColumns(
            new TreeNodeAdapter(null, null, roots),
            new ColumnInfo[]{BRANCH_COLUMN, REVISION_COLUMN, DATE_COLUMN, AUTHOR_COLUMN}
        ));
        Runnable runnable = () -> {
            int index = treeTable.getSelectionModel().getMinSelectionIndex();
            if (index == -1) {
                return;
            }
            VcsFileRevision revision = getRevisionAt(treeTable, index);
            if (revision != null) {
                DiffActionExecutor.showDiff(
                    diffProvider,
                    revision.getRevisionNumber(),
                    file,
                    project,
                    VcsBackgroundableActions.COMPARE_WITH
                );
            }
        };

        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        new PopupChooserBuilder(treeTable)
            .setTitle(VcsLocalize.lookupTitleVcsFileRevisions().get())
            .setItemChoosenCallback(runnable)
            .setSouthComponent(createCommentsPanel(treeTable))
            .setResizable(true)
            .setDimensionServiceKey("Vcs.CompareWithSelectedRevision.Popup")
            .createPopup()
            .showCenteredInCurrentWindow(project);
    }

    @Nullable
    private static VcsFileRevision getRevisionAt(TreeTableView treeTable, int index) {
        List items = treeTable.getItems();
        if (items.size() <= index) {
            return null;
        }
        else {
            return ((TreeNodeAdapter) items.get(index)).getRevision();
        }
    }

    private static JPanel createCommentsPanel(TreeTableView treeTable) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea textArea = createTextArea();
        treeTable.getSelectionModel().addListSelectionListener(e -> {
            int index = treeTable.getSelectionModel().getMinSelectionIndex();
            if (index == -1) {
                textArea.setText("");
            }
            else {
                VcsFileRevision revision = getRevisionAt(treeTable, index);
                if (revision != null) {
                    textArea.setText(revision.getCommitMessage());
                }
                else {
                    textArea.setText("");
                }
            }
        });
        JScrollPane textScrollPane = ScrollPaneFactory.createScrollPane(textArea);
        panel.add(textScrollPane, BorderLayout.CENTER);
        textScrollPane.setBorder(IdeBorderFactory.createTitledBorder(VcsLocalize.borderSelectedRevisionCommitMessage().get(), false));
        return panel;
    }

    private static JTextArea createTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setRows(5);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        return textArea;
    }

    public static void showListPopup(
        final List<VcsFileRevision> revisions,
        Project project,
        Consumer<VcsFileRevision> selectedRevisionConsumer,
        boolean showComments
    ) {
        ColumnInfo[] columns = new ColumnInfo[]{REVISION_TABLE_COLUMN, DATE_TABLE_COLUMN, AUTHOR_TABLE_COLUMN};
        for (VcsFileRevision revision : revisions) {
            if (revision.getBranchName() != null) {
                columns = new ColumnInfo[]{REVISION_TABLE_COLUMN, BRANCH_TABLE_COLUMN, DATE_TABLE_COLUMN, AUTHOR_TABLE_COLUMN};
                break;
            }
        }
        final TableView<VcsFileRevision> table = new TableView<>(new ListTableModel<>(columns, revisions, 0));
        table.setShowHorizontalLines(false);
        table.setTableHeader(null);
        Runnable runnable = () -> {
            VcsFileRevision revision = table.getSelectedObject();
            if (revision != null) {
                selectedRevisionConsumer.accept(revision);
            }
        };

        if (table.getModel().getRowCount() == 0) {
            table.clearSelection();
        }

        new SpeedSearchBase<TableView>(table) {
            @Override
            protected int getSelectedIndex() {
                return table.getSelectedRow();
            }

            @Override
            protected int convertIndexToModel(int viewIndex) {
                return table.convertRowIndexToModel(viewIndex);
            }

            @Nonnull
            @Override
            protected Object[] getAllElements() {
                return revisions.toArray();
            }

            @Override
            protected String getElementText(Object element) {
                VcsFileRevision revision = (VcsFileRevision) element;
                return revision.getRevisionNumber().asString() + " " + revision.getBranchName() + " " + revision.getAuthor();
            }

            @Override
            protected void selectElement(Object element, String selectedText) {
                VcsFileRevision revision = (VcsFileRevision) element;
                TableUtil.selectRows(myComponent, new int[]{myComponent.convertRowIndexToView(revisions.indexOf(revision))});
                TableUtil.scrollSelectionToVisible(myComponent);
            }
        };

        table.setMinimumSize(new Dimension(300, 50));
        PopupChooserBuilder builder = new PopupChooserBuilder(table);

        if (showComments) {
            builder.setSouthComponent(createCommentsPanel(table));
        }

        builder.setTitle(VcsLocalize.lookupTitleVcsFileRevisions().get())
            .setItemChoosenCallback(runnable)
            .setResizable(true)
            .setDimensionServiceKey("Vcs.CompareWithSelectedRevision.Popup")
            .setMinSize(new Dimension(300, 300));
        JBPopup popup = builder.createPopup();

        popup.showCenteredInCurrentWindow(project);
    }

    private static JPanel createCommentsPanel(TableView<VcsFileRevision> table) {
        JTextArea textArea = createTextArea();
        table.getSelectionModel().addListSelectionListener(e -> {
            VcsFileRevision revision = table.getSelectedObject();
            if (revision == null) {
                textArea.setText("");
            }
            else {
                textArea.setText(revision.getCommitMessage());
                textArea.select(0, 0);
            }
        });

        JPanel jPanel = new JPanel(new BorderLayout());
        JScrollPane textScrollPane = ScrollPaneFactory.createScrollPane(textArea);
        textScrollPane.setBorder(IdeBorderFactory.createTitledBorder(VcsLocalize.borderSelectedRevisionCommitMessage().get(), false));
        jPanel.add(textScrollPane, BorderLayout.SOUTH);

        jPanel.setPreferredSize(new Dimension(300, 100));
        return jPanel;
    }

    private static class TreeNodeAdapter extends DefaultMutableTreeNode {
        private final TreeItem<VcsFileRevision> myRevision;

        public TreeNodeAdapter(TreeNodeAdapter parent, TreeItem<VcsFileRevision> revision, List<TreeItem<VcsFileRevision>> children) {
            if (parent != null) {
                parent.add(this);
            }
            myRevision = revision;
            for (TreeItem<VcsFileRevision> treeItem : children) {
                new TreeNodeAdapter(this, treeItem, treeItem.getChildren());
            }
        }

        public VcsFileRevision getRevision() {
            return myRevision.getData();
        }
    }
}
