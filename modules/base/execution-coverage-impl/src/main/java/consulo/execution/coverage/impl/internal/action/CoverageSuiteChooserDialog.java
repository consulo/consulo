package consulo.execution.coverage.impl.internal.action;

import consulo.application.Application;
import consulo.application.util.DateFormatUtil;
import consulo.execution.coverage.*;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 2014-11-27
 */
public class CoverageSuiteChooserDialog extends DialogWrapper {
    private static final String LOCAL = "Local";
    private final Project myProject;
    private final CheckboxTree mySuitesTree;
    private final CoverageDataManager myCoverageManager;
    private static final Logger LOG = Logger.getInstance(CoverageSuiteChooserDialog.class);
    private final CheckedTreeNode myRootNode;
    private CoverageEngine myEngine;

    public CoverageSuiteChooserDialog(Project project) {
        super(project, true);
        myProject = project;
        myCoverageManager = CoverageDataManager.getInstance(project);

        myRootNode = new CheckedTreeNode("");
        initTree();
        mySuitesTree = new CheckboxTree(new SuitesRenderer(), myRootNode) {
            @Override
            protected void installSpeedSearch() {
                new TreeSpeedSearch(
                    this,
                    path -> {
                        DefaultMutableTreeNode component = (DefaultMutableTreeNode)path.getLastPathComponent();
                        Object userObject = component.getUserObject();
                        if (userObject instanceof CoverageSuite coverageSuite) {
                            return coverageSuite.getPresentableName();
                        }
                        return userObject.toString();
                    }
                );
            }
        };
        mySuitesTree.getEmptyText().appendText(ExecutionCoverageLocalize.noCoverageSuitesConfigured());
        mySuitesTree.setRootVisible(false);
        mySuitesTree.setShowsRootHandles(false);
        TreeUtil.installActions(mySuitesTree);
        TreeUtil.expandAll(mySuitesTree);
        TreeUtil.selectFirstNode(mySuitesTree);
        mySuitesTree.setMinimumSize(new Dimension(25, -1));
        setOKButtonText(ExecutionCoverageLocalize.coverageDataShowSelectedButton());
        init();
        setTitle(ExecutionCoverageLocalize.chooseCoverageSuiteToDisplay());
    }

    @Override
    protected JComponent createCenterPanel() {
        return ScrollPaneFactory.createScrollPane(mySuitesTree);
    }

    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        return mySuitesTree;
    }

    @Override
    protected JComponent createNorthPanel() {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new AddExternalSuiteAction());
        group.add(new DeleteSuiteAction());
        group.add(new SwitchEngineAction());
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group.build(), true);
        toolbar.setTargetComponent(mySuitesTree);
        return toolbar.getComponent();
    }

    @Override
    protected void doOKAction() {
        List<CoverageSuite> suites = collectSelectedSuites();
        myCoverageManager.chooseSuitesBundle(suites.isEmpty() ? null : new CoverageSuitesBundle(suites.toArray(new CoverageSuite[suites.size()])));
        super.doOKAction();
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), new NoCoverageAction(), getCancelAction()};
    }

    private Set<CoverageEngine> collectEngines() {
        Set<CoverageEngine> engines = new HashSet<>();
        for (CoverageSuite suite : myCoverageManager.getSuites()) {
            engines.add(suite.getCoverageEngine());
        }
        return engines;
    }

    private static LocalizeValue getCoverageRunnerTitle(CoverageRunner coverageRunner) {
        return ExecutionCoverageLocalize.coverageDataRunnerName(coverageRunner.getPresentableName());
    }

    @Nullable
    private static CoverageRunner getCoverageRunner(VirtualFile file) {
        String fileExtension = file.getExtension();
        return Application.get().getExtensionPoint(CoverageRunner.class)
            .findFirstSafe(runner -> Objects.equals(fileExtension, runner.getDataFileExtension()));
    }

    private List<CoverageSuite> collectSelectedSuites() {
        List<CoverageSuite> suites = new ArrayList<>();
        TreeUtil.traverse(
            myRootNode,
            treeNode -> {
                if (treeNode instanceof CheckedTreeNode checkedTreeNode
                    && checkedTreeNode.isChecked()
                    && checkedTreeNode.getUserObject() instanceof CoverageSuite coverageSuite) {
                    suites.add(coverageSuite);
                }
                return true;
            }
        );
        return suites;
    }

    private void initTree() {
        myRootNode.removeAllChildren();
        HashMap<CoverageRunner, Map<String, List<CoverageSuite>>> grouped = new HashMap<>();
        groupSuites(grouped, myCoverageManager.getSuites(), myEngine);
        CoverageSuitesBundle currentSuite = myCoverageManager.getCurrentSuitesBundle();
        List<CoverageRunner> runners = new ArrayList<>(grouped.keySet());
        Collections.sort(runners, (o1, o2) -> o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName()));
        for (CoverageRunner runner : runners) {
            DefaultMutableTreeNode runnerNode = new DefaultMutableTreeNode(getCoverageRunnerTitle(runner));
            Map<String, List<CoverageSuite>> providers = grouped.get(runner);
            DefaultMutableTreeNode remoteNode = new DefaultMutableTreeNode(ExecutionCoverageLocalize.remoteSuitesNode().get());
            if (providers.size() == 1) {
                String providersKey = providers.keySet().iterator().next();
                DefaultMutableTreeNode suitesNode = runnerNode;
                if (!Comparing.strEqual(providersKey, DefaultCoverageFileProvider.class.getName())) {
                    suitesNode = remoteNode;
                    runnerNode.add(remoteNode);
                }
                List<CoverageSuite> suites = providers.get(providersKey);
                Collections.sort(suites, (o1, o2) -> o1.getPresentableName().compareToIgnoreCase(o2.getPresentableName()));
                for (CoverageSuite suite : suites) {
                    CheckedTreeNode treeNode = new CheckedTreeNode(suite);
                    treeNode.setChecked(currentSuite != null && currentSuite.contains(suite) ? Boolean.TRUE : Boolean.FALSE);
                    suitesNode.add(treeNode);
                }
            }
            else {
                DefaultMutableTreeNode localNode = new DefaultMutableTreeNode(LOCAL);
                runnerNode.add(localNode);
                runnerNode.add(remoteNode);
                for (String aClass : providers.keySet()) {
                    DefaultMutableTreeNode node =
                        Comparing.strEqual(aClass, DefaultCoverageFileProvider.class.getName()) ? localNode : remoteNode;
                    for (CoverageSuite suite : providers.get(aClass)) {
                        CheckedTreeNode treeNode = new CheckedTreeNode(suite);
                        treeNode.setChecked(currentSuite != null && currentSuite.contains(suite) ? Boolean.TRUE : Boolean.FALSE);
                        node.add(treeNode);
                    }
                }
            }
            myRootNode.add(runnerNode);
        }
    }

    private static void groupSuites(
        HashMap<CoverageRunner, Map<String, List<CoverageSuite>>> grouped,
        CoverageSuite[] suites,
        CoverageEngine engine
    ) {
        for (CoverageSuite suite : suites) {
            if (engine != null && suite.getCoverageEngine() != engine) {
                continue;
            }
            if (suite.getCoverageDataFileProvider() instanceof DefaultCoverageFileProvider provider
                && Comparing.strEqual(provider.getSourceProvider(), DefaultCoverageFileProvider.class.getName())
                && !provider.ensureFileExists()) {
                continue;
            }
            CoverageRunner runner = suite.getRunner();
            Map<String, List<CoverageSuite>> byProviders = grouped.get(runner);
            if (byProviders == null) {
                byProviders = new HashMap<>();
                grouped.put(runner, byProviders);
            }
            String sourceProvider = suite.getCoverageDataFileProvider() instanceof DefaultCoverageFileProvider provider
                ? provider.getSourceProvider()
                : suite.getCoverageDataFileProvider().getClass().getName();
            List<CoverageSuite> list = byProviders.get(sourceProvider);
            if (list == null) {
                list = new ArrayList<>();
                byProviders.put(sourceProvider, list);
            }
            list.add(suite);
        }
    }

    private void updateTree() {
        ((DefaultTreeModel)mySuitesTree.getModel()).reload();
        TreeUtil.expandAll(mySuitesTree);
    }

    private static class SuitesRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        @Override
        public void customizeRenderer(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (value instanceof CheckedTreeNode checkedTreeNode) {
                if (checkedTreeNode.getUserObject() instanceof CoverageSuite suite) {
                    getTextRenderer().append(LocalizeValue.of(suite.getPresentableName()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                    String date = " (" + DateFormatUtil.formatPrettyDateTime(suite.getLastCoverageTimeStamp()) + ")";
                    getTextRenderer().append(LocalizeValue.of(date), SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }
            else if (value instanceof DefaultMutableTreeNode mutableTreeNode) {
                if (mutableTreeNode.getUserObject() instanceof String str) {
                    getTextRenderer().append(str);
                }
            }
        }
    }

    private class NoCoverageAction extends DialogWrapperAction {
        public NoCoverageAction() {
            super(ExecutionCoverageLocalize.coverageDataNoCoverageButton());
        }

        @Override
        protected void doAction(ActionEvent e) {
            myCoverageManager.chooseSuitesBundle(null);
            CoverageSuiteChooserDialog.this.close(DialogWrapper.OK_EXIT_CODE);
        }
    }

    private class AddExternalSuiteAction extends AnAction {
        public AddExternalSuiteAction() {
            super(CommonLocalize.buttonAdd(), CommonLocalize.buttonAdd(), PlatformIconGroup.generalAdd());
            registerCustomShortcutSet(CommonShortcuts.getInsert(), mySuitesTree);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            VirtualFile file = IdeaFileChooser.chooseFile(
                new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    @RequiredUIAccess
                    public boolean isFileSelectable(VirtualFile file) {
                        return getCoverageRunner(file) != null;
                    }
                },
                myProject,
                null
            );
            if (file != null) {
                CoverageRunner coverageRunner = getCoverageRunner(file);
                LOG.assertTrue(coverageRunner != null);

                CoverageSuite coverageSuite = myCoverageManager.addExternalCoverageSuite(
                    file.getName(),
                    file.getTimeStamp(),
                    coverageRunner,
                    new DefaultCoverageFileProvider(file.getPath())
                );

                LocalizeValue coverageRunnerTitle = getCoverageRunnerTitle(coverageRunner);
                DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(myRootNode, coverageRunnerTitle.get());
                if (node == null) {
                    node = new DefaultMutableTreeNode(coverageRunnerTitle.get());
                    myRootNode.add(node);
                }
                if (node.getChildCount() > 0) {
                    TreeNode childNode = node.getChildAt(0);
                    if (!(childNode instanceof CheckedTreeNode)) {
                        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)childNode;
                        if (LOCAL.equals(defaultMutableTreeNode.getUserObject())) {
                            node = defaultMutableTreeNode;
                        }
                        else {
                            DefaultMutableTreeNode localNode = new DefaultMutableTreeNode(LOCAL);
                            node.add(localNode);
                            node = localNode;
                        }
                    }
                }
                CheckedTreeNode suiteNode = new CheckedTreeNode(coverageSuite);
                suiteNode.setChecked(true);
                node.add(suiteNode);
                TreeUtil.sort(
                    node,
                    (o1, o2) -> {
                        if (o1 instanceof CheckedTreeNode node1
                            && o2 instanceof CheckedTreeNode node2
                            && node1.getUserObject() instanceof CoverageSuite suite1
                            && node2.getUserObject() instanceof CoverageSuite suite2) {
                            return suite1.getPresentableName().compareToIgnoreCase(suite2.getPresentableName());
                        }
                        return 0;
                    }
                );
                updateTree();
                TreeUtil.selectNode(mySuitesTree, suiteNode);
            }
        }
    }

    private class DeleteSuiteAction extends AnAction {
        public DeleteSuiteAction() {
            super(CommonLocalize.buttonDelete(), CommonLocalize.buttonDelete(), PlatformIconGroup.generalRemove());
            registerCustomShortcutSet(CommonShortcuts.getDelete(), mySuitesTree);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            CheckedTreeNode[] selectedNodes = mySuitesTree.getSelectedNodes(CheckedTreeNode.class, null);
            for (CheckedTreeNode selectedNode : selectedNodes) {
                if (selectedNode.getUserObject() instanceof CoverageSuite selectedSuite
                    && selectedSuite.getCoverageDataFileProvider() instanceof DefaultCoverageFileProvider provider
                    && Comparing.strEqual(provider.getSourceProvider(), DefaultCoverageFileProvider.class.getName())) {
                    myCoverageManager.removeCoverageSuite(selectedSuite);
                    TreeUtil.removeLastPathComponent(mySuitesTree, new TreePath(selectedNode.getPath()));
                }
            }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            CheckedTreeNode[] selectedSuites = mySuitesTree.getSelectedNodes(CheckedTreeNode.class, null);
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(false);
            for (CheckedTreeNode node : selectedSuites) {
                if (node.getUserObject() instanceof CoverageSuite selectedSuite
                    && selectedSuite.getCoverageDataFileProvider() instanceof DefaultCoverageFileProvider provider
                    && Comparing.strEqual(provider.getSourceProvider(), DefaultCoverageFileProvider.class.getName())) {
                    presentation.setEnabled(true);
                }
            }
        }
    }

    private class SwitchEngineAction extends ComboBoxAction {
        @Nonnull
        @Override
        public DefaultActionGroup createPopupActionGroup(JComponent component) {
            DefaultActionGroup engChooser = new DefaultActionGroup();
            for (CoverageEngine engine : collectEngines()) {
                engChooser.add(new AnAction(engine.getPresentableText()) {
                    @Override
                    @RequiredUIAccess
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        myEngine = engine;
                        initTree();
                        updateTree();
                    }
                });
            }
            return engChooser;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setVisible(collectEngines().size() > 1);
        }
    }
}
