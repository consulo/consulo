package consulo.execution.coverage.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.view.ProjectViewAutoScrollFromSourceHandler;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * @author anna
 * @since 2012-01-02
 */
public class CoverageView extends JPanel implements DataProvider, Disposable {
    private static final String ACTION_DRILL_DOWN = "DrillDown";
    private static final String ACTION_GO_UP = "GoUp";
    private static final String HELP_ID = "reference.toolWindows.Coverage";

    private CoverageTableModel myModel;
    private JBTable myTable;
    private CoverageViewBuilder myBuilder;
    private final Project myProject;
    private final CoverageViewManager.StateBean myStateBean;

    public CoverageView(Project project, CoverageDataManager dataManager, CoverageViewManager.StateBean stateBean) {
        super(new BorderLayout());
        myProject = project;
        myStateBean = stateBean;
        JLabel titleLabel = new JLabel();
        CoverageSuitesBundle suitesBundle = dataManager.getCurrentSuitesBundle();
        myModel = new CoverageTableModel(suitesBundle, stateBean, project);

        myTable = new JBTable(myModel);
        StatusText emptyText = myTable.getEmptyText();
        emptyText.setText(ExecutionCoverageLocalize.coverageViewNoCoverageResults());
        RunConfigurationBase configuration = suitesBundle.getRunConfiguration();
        if (configuration != null) {
            emptyText.appendText(LocalizeValue.join(
                LocalizeValue.space(),
                ExecutionCoverageLocalize.coverageViewEditRunConfiguration0(),
                LocalizeValue.space()
            ));
            emptyText.appendText(
                ExecutionCoverageLocalize.coverageViewEditRunConfiguration1(),
                SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> {
                    String configurationName = configuration.getName();
                    RunnerAndConfigurationSettings configurationSettings =
                        RunManager.getInstance(project).findConfigurationByName(configurationName);
                    if (configurationSettings != null) {
                        RunConfigurationEditor.getInstance(project)
                            .editConfiguration(
                                project,
                                configurationSettings,
                                ExecutionLocalize.editRunConfigurationForItemDialogTitle(configurationName)
                            );
                    }
                    else {
                        Messages.showErrorDialog(
                            project,
                            ExecutionCoverageLocalize.coverageViewConfigurationWasNotFound(configurationName).get(),
                            CommonLocalize.titleError().get()
                        );
                    }
                }
            );
            emptyText.appendText(LocalizeValue.join(LocalizeValue.space(), ExecutionCoverageLocalize.coverageViewEditRunConfiguration2()));
        }
        myTable.getColumnModel().getColumn(0).setCellRenderer(new NodeDescriptorTableCellRenderer());
        myTable.getTableHeader().setReorderingAllowed(false);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);
        centerPanel.add(titleLabel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        final CoverageViewTreeStructure structure = new CoverageViewTreeStructure(project, suitesBundle, stateBean);
        myBuilder = new CoverageViewBuilder(project, new JBList(), myModel, structure, myTable);
        myBuilder.setParentTitle(titleLabel);
        new DoubleClickListener() {
            @Override
            @RequiredReadAction
            protected boolean onDoubleClick(MouseEvent e) {
                drillDown(structure);
                return true;
            }
        }.installOn(myTable);
        TableSpeedSearch speedSearch = new TableSpeedSearch(myTable);
        speedSearch.setClearSearchOnNavigateNoMatch(true);
        PopupHandler.installUnknownPopupHandler(myTable, createPopupGroup(), ActionManager.getInstance());
        ScrollingUtil.installActions(myTable);

        myTable.registerKeyboardAction(
            e -> {
                if (myBuilder == null) {
                    return;
                }
                myBuilder.buildRoot();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK),
            JComponent.WHEN_FOCUSED
        );

        myTable.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACTION_DRILL_DOWN);
        myTable.getInputMap(WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK),
            ACTION_DRILL_DOWN
        );
        myTable.getActionMap().put(
            ACTION_DRILL_DOWN,
            new AbstractAction() {
                @Override
                @RequiredReadAction
                public void actionPerformed(ActionEvent e) {
                    drillDown(structure);
                }
            }
        );
        myTable.getInputMap(WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK),
            ACTION_GO_UP
        );
        myTable.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), ACTION_GO_UP);
        myTable.getActionMap().put(ACTION_GO_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goUp();
            }
        });

        ActionToolbar actionToolbar =
            ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, createToolbarActions(structure), false);
        actionToolbar.setTargetComponent(this);
        JComponent component = actionToolbar.getComponent();
        add(component, BorderLayout.WEST);
    }

    @Override
    public void dispose() {
    }

    private static ActionGroup createPopupGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE));
        return actionGroup;
    }

    private ActionGroup createToolbarActions(CoverageViewTreeStructure treeStructure) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new GoUpAction(treeStructure));
        if (treeStructure.supportFlattenPackages()) {
            actionGroup.add(new FlattenPackagesAction());
        }

        installAutoScrollToSource(actionGroup);
        installAutoScrollFromSource(actionGroup);

        actionGroup.add(ActionManager.getInstance().getAction("GenerateCoverageReport"));
        actionGroup.add(new CloseTabToolbarAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                CoverageDataManager.getInstance(myProject).chooseSuitesBundle(null);
            }
        });
        actionGroup.add(new ContextHelpAction(HELP_ID));
        return actionGroup;
    }

    private void installAutoScrollFromSource(DefaultActionGroup actionGroup) {
        MyAutoScrollFromSourceHandler handler = new MyAutoScrollFromSourceHandler();
        handler.install();
        actionGroup.add(handler.createToggleAction());
    }

    private void installAutoScrollToSource(DefaultActionGroup actionGroup) {
        AutoScrollToSourceHandler autoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return myStateBean.myAutoScrollToSource;
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
                myStateBean.myAutoScrollToSource = state;
            }
        };
        autoScrollToSourceHandler.install(myTable);
        actionGroup.add(autoScrollToSourceHandler.createToggleAction());
    }

    public void goUp() {
        if (myBuilder == null) {
            return;
        }
        myBuilder.goUp();
    }

    @RequiredReadAction
    private void drillDown(CoverageViewTreeStructure treeStructure) {
        AbstractTreeNode element = getSelectedValue();
        if (element == null) {
            return;
        }
        if (treeStructure.getChildElements(element).length == 0) {
            if (element.canNavigate()) {
                element.navigate(true);
            }
            return;
        }
        myBuilder.drillDown();
    }

    public void updateParentTitle() {
        myBuilder.updateParentTitle();
    }

    private AbstractTreeNode getSelectedValue() {
        return (AbstractTreeNode) myBuilder.getSelectedValue();
    }

    private boolean topElementIsSelected(CoverageViewTreeStructure treeStructure) {
        if (myTable == null) {
            return false;
        }
        if (myModel.getSize() >= 1) {
            AbstractTreeNode rootElement = (AbstractTreeNode) treeStructure.getRootElement();
            AbstractTreeNode node = (AbstractTreeNode) myModel.getElementAt(0);
            if (node.getParent() == rootElement) {
                return true;
            }
        }
        return false;
    }

    @RequiredReadAction
    public boolean canSelect(VirtualFile file) {
        return myBuilder.canSelect(file);
    }

    @RequiredUIAccess
    public void select(VirtualFile file) {
        myBuilder.select(file);
    }

    @Override
    public Object getData(@Nonnull Key dataId) {
        if (Navigatable.KEY == dataId) {
            return getSelectedValue();
        }
        if (HelpManager.HELP_ID == dataId) {
            return HELP_ID;
        }
        return null;
    }

    private static class NodeDescriptorTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof NodeDescriptor descriptor) {
                setIcon(TargetAWT.to(descriptor.getIcon()));
                setText(descriptor.toString());
                if (!isSelected) {
                    setForeground(TargetAWT.to(((CoverageListNode) descriptor).getFileStatus().getColor()));
                }
            }
            return component;
        }
    }

    private class FlattenPackagesAction extends ToggleAction {
        private FlattenPackagesAction() {
            super(
                ExecutionCoverageLocalize.coverageFlattenPackages(),
                ExecutionCoverageLocalize.coverageFlattenPackages(),
                PlatformIconGroup.objectbrowserFlattenpackages()
            );
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myStateBean.myFlattenPackages;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myStateBean.myFlattenPackages = state;
            Object selectedValue = myBuilder.getSelectedValue();
            myBuilder.buildRoot();

            if (selectedValue != null) {
                myBuilder.select(((CoverageListNode) selectedValue).getValue());
            }
            myBuilder.ensureSelectionExist();
            myBuilder.updateParentTitle();
        }
    }

    private class GoUpAction extends AnAction {
        private final CoverageViewTreeStructure myTreeStructure;

        public GoUpAction(CoverageViewTreeStructure treeStructure) {
            super(LocalizeValue.localizeTODO("Go Up"), LocalizeValue.localizeTODO("Go to Upper Level"), PlatformIconGroup.nodesUplevel());
            myTreeStructure = treeStructure;
            registerCustomShortcutSet(KeyEvent.VK_BACK_SPACE, 0, myTable);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            goUp();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!topElementIsSelected(myTreeStructure));
        }
    }

    private class MyAutoScrollFromSourceHandler extends ProjectViewAutoScrollFromSourceHandler {
        public MyAutoScrollFromSourceHandler() {
            super(CoverageView.this.myProject, CoverageView.this, CoverageView.this);
        }

        @Override
        protected boolean isAutoScrollEnabled() {
            return myStateBean.myAutoScrollFromSource;
        }

        @Override
        protected void setAutoScrollEnabled(boolean state) {
            myStateBean.myAutoScrollFromSource = state;
        }

        @Override
        @RequiredUIAccess
        protected void selectElementFromEditor(@Nonnull FileEditor editor) {
            if (myProject.isDisposed() || !CoverageView.this.isShowing()) {
                return;
            }
            if (myStateBean.myAutoScrollFromSource) {
                VirtualFile file = FileEditorManager.getInstance(myProject).getFile(editor);
                if (file != null && canSelect(file)) {
                    PsiElement e = null;
                    if (editor instanceof TextEditor textEditor) {
                        int offset = textEditor.getEditor().getCaretModel().getOffset();
                        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
                        if (psiFile != null) {
                            e = psiFile.findElementAt(offset);
                        }
                    }
                    myBuilder.select(e != null ? e : file);
                }
            }
        }
    }
}
