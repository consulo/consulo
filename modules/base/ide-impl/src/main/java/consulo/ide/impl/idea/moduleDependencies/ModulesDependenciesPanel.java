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
package consulo.ide.impl.idea.moduleDependencies;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.graph.GraphAlgorithms;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.Graph;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.navigation.NavigatableWithText;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.ui.view.internal.ProjectSettingsService;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.content.Content;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 2005-02-10
 */
public class ModulesDependenciesPanel extends JPanel implements ModuleRootListener, Disposable {
    private static final String DIRECTION = "FORWARD_ANALIZER";
    private Content myContent;
    private final Project myProject;
    private Tree myLeftTree;
    private DefaultTreeModel myLeftTreeModel;

    private final Tree myRightTree;
    private final DefaultTreeModel myRightTreeModel;

    private Graph<Module> myModulesGraph;
    private final Module[] myModules;

    private JTextField myPathField = new JTextField();

    private final Splitter mySplitter;
    private static final String ourHelpID = "module.dependencies.tool.window";

    @RequiredReadAction
    public ModulesDependenciesPanel(Project project, Module[] modules) {
        super(new BorderLayout());
        myProject = project;
        myModules = modules;

        //noinspection HardCodedStringLiteral
        myRightTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Root"));
        myRightTree = new Tree(myRightTreeModel);
        initTree(myRightTree, true);

        initLeftTree();

        mySplitter = new Splitter();
        mySplitter.setFirstComponent(new MyTreePanel(myLeftTree, myProject));
        mySplitter.setSecondComponent(new MyTreePanel(myRightTree, myProject));

        setSplitterProportion();
        add(mySplitter, BorderLayout.CENTER);
        add(createNorthPanel(), BorderLayout.NORTH);

        project.getMessageBus().connect(this).subscribe(ModuleRootListener.class, this);
    }

    @RequiredReadAction
    private void setSplitterProportion() {
        if (mySplitter == null) {
            return;
        }
        myModulesGraph = buildGraph();
        DFSTBuilder<Module> builder = new DFSTBuilder<>(myModulesGraph);
        if (builder.isAcyclic()) {
            mySplitter.setProportion(1.f);
        }
        else {
            mySplitter.setProportion(0.5f);
        }
    }

    @Override
    public void dispose() {
    }

    @RequiredReadAction
    public ModulesDependenciesPanel(Project project) {
        this(project, ModuleManager.getInstance(project).getModules());
    }

    private JComponent createNorthPanel() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new AnAction(
            CommonLocalize.actionClose(),
            AnalysisScopeLocalize.actionCloseModulesDependenciesDescription(),
            PlatformIconGroup.actionsCancel()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                DependenciesAnalyzeManager.getInstance(myProject).closeContent(myContent);
            }
        });

        appendDependenciesAction(group);

        group.add(new ToggleAction(
            AnalysisScopeLocalize.actionModuleDependenciesDirection(),
            LocalizeValue.absent(),
            isForwardDirection() ? PlatformIconGroup.actionsMoveup() : PlatformIconGroup.actionsMovedown()
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return isForwardDirection();
            }

            @Override
            @RequiredUIAccess
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                PropertiesComponent.getInstance(myProject).setValue(DIRECTION, String.valueOf(state));
                initLeftTreeModel();
            }

            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setIcon(isForwardDirection() ? PlatformIconGroup.actionsMoveup() : PlatformIconGroup.actionsMovedown());
            }
        });

        group.add(new ContextHelpAction(ourHelpID));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(toolbar.getComponent(), BorderLayout.NORTH);
        panel.add(myPathField, BorderLayout.SOUTH);
        myPathField.setEditable(false);
        return panel;
    }

    private boolean isForwardDirection() {
        String value = PropertiesComponent.getInstance(myProject).getValue(DIRECTION);
        return value == null || Boolean.parseBoolean(value);
    }

    private static void appendDependenciesAction(DefaultActionGroup group) {
        final AnAction analyzeDepsAction = ActionManager.getInstance().getAction(IdeActions.ACTION_ANALYZE_DEPENDENCIES);
        group.add(new AnAction(
            analyzeDepsAction.getTemplatePresentation().getTextValue(),
            analyzeDepsAction.getTemplatePresentation().getDescriptionValue(),
            PlatformIconGroup.toolwindowsToolwindowmoduledependencies()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                analyzeDepsAction.actionPerformed(e);
            }


            @Override
            public void update(@Nonnull AnActionEvent e) {
                analyzeDepsAction.update(e);
            }
        });
    }

    private void buildRightTree(Module module) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) myRightTreeModel.getRoot();
        root.removeAllChildren();
        Set<List<Module>> cycles = GraphAlgorithms.getInstance().findCycles(myModulesGraph, module);
        int index = 1;
        for (List<Module> modules : cycles) {
            DefaultMutableTreeNode cycle = new DefaultMutableTreeNode(
                AnalysisScopeLocalize.moduleDependenciesCycleNodeText(Integer.toString(index++).toUpperCase()).get()
            );
            root.add(cycle);
            cycle.add(new DefaultMutableTreeNode(new MyUserObject(false, module)));
            for (Module moduleInCycle : modules) {
                cycle.add(new DefaultMutableTreeNode(new MyUserObject(false, moduleInCycle)));
            }
        }
        ((DefaultTreeModel) myRightTree.getModel()).reload();
        TreeUtil.expandAll(myRightTree);
    }

    @RequiredReadAction
    private void initLeftTreeModel() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) myLeftTreeModel.getRoot();
        root.removeAllChildren();
        myModulesGraph = buildGraph();
        setSplitterProportion();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> {
                ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                Map<Module, Boolean> inCycle = new HashMap<>();
                for (Module module : myModules) {
                    if (progressIndicator != null) {
                        if (progressIndicator.isCanceled()) {
                            return;
                        }
                        progressIndicator.setTextValue(AnalysisScopeLocalize.updateModuleTreeProgressText(module.getName()));
                    }
                    if (!module.isDisposed()) {
                        Boolean isInCycle = inCycle.get(module);
                        if (isInCycle == null) {
                            isInCycle = !GraphAlgorithms.getInstance().findCycles(myModulesGraph, module).isEmpty();
                            inCycle.put(module, isInCycle);
                        }
                        DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(new MyUserObject(isInCycle, module));
                        root.add(moduleNode);
                        Iterator<Module> out = myModulesGraph.getOut(module);
                        while (out.hasNext()) {
                            moduleNode.add(new DefaultMutableTreeNode(new MyUserObject(false, out.next())));
                        }
                    }
                }
            },
            AnalysisScopeLocalize.updateModuleTreeProgressTitle().get(),
            true,
            myProject
        );
        sortSubTree(root);
        myLeftTreeModel.reload();
    }

    private static void sortSubTree(DefaultMutableTreeNode root) {
        TreeUtil.sort(
            root,
            (o1, o2) -> {
                DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) o1;
                DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) o2;
                if (!(node1.getUserObject() instanceof MyUserObject)) {
                    return 1;
                }
                else if (!(node2.getUserObject() instanceof MyUserObject)) {
                    return -1;
                }
                return (node1.getUserObject().toString().compareToIgnoreCase(node2.getUserObject().toString()));
            }
        );
    }

    private void selectCycleUpward(DefaultMutableTreeNode selection) {
        ArrayList<DefaultMutableTreeNode> selectionNodes = new ArrayList<>();
        selectionNodes.add(selection);
        DefaultMutableTreeNode current = (DefaultMutableTreeNode) selection.getParent();
        boolean flag = false;
        while (current != null && current.getUserObject() != null) {
            if (current.getUserObject().equals(selection.getUserObject())) {
                flag = true;
                selectionNodes.add(current);
                break;
            }
            selectionNodes.add(current);
            current = (DefaultMutableTreeNode) current.getParent();
        }
        if (flag) {
            for (DefaultMutableTreeNode node : selectionNodes) {
                ((MyUserObject) node.getUserObject()).setInCycle(true);
            }
        }
        if (current != null) {
            current = (DefaultMutableTreeNode) current.getParent();
        }
        while (current != null) {
            if (current.getUserObject() instanceof MyUserObject userObject) {
                userObject.setInCycle(false);
            }
            current = (DefaultMutableTreeNode) current.getParent();
        }
        myLeftTree.repaint();
    }

    @RequiredReadAction
    private void initLeftTree() {
        //noinspection HardCodedStringLiteral
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        myLeftTreeModel = new DefaultTreeModel(root);
        initLeftTreeModel();
        myLeftTree = new Tree(myLeftTreeModel);
        initTree(myLeftTree, false);

        myLeftTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
            }

            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                DefaultMutableTreeNode expandedNode = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                for (int i = 0; i < expandedNode.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) expandedNode.getChildAt(i);
                    if (child.getChildCount() == 0) {
                        Module module = ((MyUserObject) child.getUserObject()).getModule();
                        Iterator<Module> out = myModulesGraph.getOut(module);
                        while (out.hasNext()) {
                            Module nextModule = out.next();
                            child.add(new DefaultMutableTreeNode(new MyUserObject(false, nextModule)));
                        }
                        sortSubTree(child);
                    }
                }
            }
        });

        myLeftTree.addTreeSelectionListener(e -> {
            TreePath selectionPath = myLeftTree.getSelectionPath();
            if (selectionPath != null) {

                myPathField.setText(StringUtil.join(
                    selectionPath.getPath(),
                    o -> {
                        if (((DefaultMutableTreeNode) o).getUserObject() instanceof MyUserObject userObject) {
                            return userObject.getModule().getName();
                        }
                        return "";
                    },
                    ":"
                ));

                DefaultMutableTreeNode selection = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (selection != null) {
                    TreeUtil.traverseDepth(
                        selection,
                        node -> {
                            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                            if (treeNode.getUserObject() instanceof MyUserObject userObject) {
                                userObject.setInCycle(false);
                            }
                            return true;
                        }
                    );
                    selectCycleUpward(selection);
                    buildRightTree(((MyUserObject) selection.getUserObject()).getModule());
                }
            }
        });
        TreeUtil.selectFirstNode(myLeftTree);
    }

    private static ActionGroup createTreePopupActions(final boolean isRightTree, final Tree tree) {
        DefaultActionGroup group = new DefaultActionGroup();
        TreeExpander treeExpander = new TreeExpander() {
            @Override
            public void expandAll() {
                TreeUtil.expandAll(tree);
            }

            @Override
            public boolean canExpand() {
                return isRightTree;
            }

            @Override
            public void collapseAll() {
                TreeUtil.collapseAll(tree, 3);
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };

        CommonActionsManager actionManager = CommonActionsManager.getInstance();
        if (isRightTree) {
            group.add(actionManager.createExpandAllAction(treeExpander, tree));
        }
        group.add(actionManager.createCollapseAllAction(treeExpander, tree));
        ActionManager globalActionManager = ActionManager.getInstance();
        group.add(globalActionManager.getAction(IdeActions.ACTION_EDIT_SOURCE));
        group.add(AnSeparator.getInstance());
        group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_DEPENDENCIES));
        group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_BACK_DEPENDENCIES));
        //non exists in platform group.add(globalActionManager.getAction(IdeActions.ACTION_ANALYZE_CYCLIC_DEPENDENCIES));
        return group;
    }

    private static void initTree(Tree tree, boolean isRightTree) {
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new MyTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        UIUtil.setLineStyleAngled(tree);

        TreeUtil.installActions(tree);
        new TreeSpeedSearch(tree, o -> o.getLastPathComponent().toString(), true);
        PopupHandler.installUnknownPopupHandler(tree, createTreePopupActions(isRightTree, tree), ActionManager.getInstance());
    }

    @RequiredReadAction
    private Graph<Module> buildGraph() {
        Graph<Module> graph = ModuleManager.getInstance(myProject).moduleGraph();
        if (isForwardDirection()) {
            return graph;
        }
        else {
            return GraphAlgorithms.getInstance().invertEdgeDirections(graph);
        }
    }

    public void setContent(Content content) {
        myContent = content;
    }

    @Override
    public void beforeRootsChange(ModuleRootEvent event) {
    }

    @Override
    @RequiredReadAction
    public void rootsChanged(ModuleRootEvent event) {
        initLeftTreeModel();
        TreeUtil.selectFirstNode(myLeftTree);
    }

    private static class MyUserObject implements NavigatableWithText {
        private boolean myInCycle;
        private final Module myModule;

        public MyUserObject(boolean inCycle, Module module) {
            myInCycle = inCycle;
            myModule = module;
        }

        public boolean isInCycle() {
            return myInCycle;
        }

        public void setInCycle(boolean inCycle) {
            myInCycle = inCycle;
        }

        public Module getModule() {
            return myModule;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof MyUserObject userObject && myModule.equals(userObject.getModule());
        }

        @Override
        public int hashCode() {
            return myModule.hashCode();
        }

        @Override
        public String toString() {
            return myModule.getName();
        }

        @Override
        @RequiredUIAccess
        public void navigate(boolean requestFocus) {
            ProjectSettingsService.getInstance(myModule.getProject()).openModuleSettings(myModule);
        }

        @Override
        public boolean canNavigate() {
            return myModule != null && !myModule.isDisposed();
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Nonnull
        @Override
        public LocalizeValue getNavigateActionText(boolean focusEditor) {
            return ProjectUIViewLocalize.actionOpenModuleSettingsText();
        }
    }

    private static class MyTreePanel extends JPanel implements DataProvider {
        private final Tree myTree;
        private final Project myProject;

        public MyTreePanel(Tree tree, Project project) {
            super(new BorderLayout());
            myTree = tree;
            myProject = project;
            add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
        }

        @Override
        public Object getData(@Nonnull Key dataId) {
            if (Project.KEY == dataId) {
                return myProject;
            }
            if (LangDataKeys.MODULE_CONTEXT == dataId) {
                TreePath selectionPath = myTree.getLeadSelectionPath();
                if (selectionPath != null && selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                    if (node.getUserObject() instanceof MyUserObject) {
                        return ((MyUserObject) node.getUserObject()).getModule();
                    }
                }
            }
            if (HelpManager.HELP_ID == dataId) {
                return ourHelpID;
            }
            if (Navigatable.KEY == dataId) {
                TreePath selectionPath = myTree.getLeadSelectionPath();
                if (selectionPath != null && selectionPath.getLastPathComponent() instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof MyUserObject) {
                    return node.getUserObject();
                }
            }
            return null;
        }
    }

    private static class MyTreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(
            @Nonnull JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (!(userObject instanceof MyUserObject node)) {
                if (userObject != null) {
                    append(userObject.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
                return;
            }
            Module module = node.getModule();
            setIcon(PlatformIconGroup.nodesModule());
            if (node.isInCycle()) {
                append(module.getName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
            }
            else {
                append(module.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }
}
