// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.codeEditor.util.EditorUtil;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.UserHomeFileUtil;
import consulo.build.ui.*;
import consulo.build.ui.event.*;
import consulo.build.ui.impl.internal.event.FailureResultImpl;
import consulo.build.ui.impl.internal.event.FileNavigatable;
import consulo.build.ui.impl.internal.event.SkippedResultImpl;
import consulo.build.ui.localize.BuildLocalize;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.codeEditor.action.ToggleUseSoftWrapsToolbarAction;
import consulo.compiler.internal.CompilerWorkspaceConfiguration;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.actions.EditSourceAction;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.navigation.NonNavigatable;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.TreeOptions;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.util.InvokerFactory;
import consulo.ui.image.Image;
import consulo.util.collection.SmartHashSet;
import consulo.util.concurrent.Promise;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.build.BuildView.CONSOLE_VIEW_NAME;
import static consulo.ui.ex.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static consulo.ui.ex.awt.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;
import static consulo.ui.ex.awt.UIUtil.*;
import static consulo.ui.ex.awt.util.RenderingHelper.SHRINK_LONG_RENDERER;
import static consulo.util.collection.ContainerUtil.addIfNotNull;
import static consulo.util.lang.StringUtil.isEmpty;

/**
 * @author Vladislav.Soroka
 */
public class BuildTreeConsoleView extends BaseBuildTreeConsoleView implements UiDataProvider, OccurenceNavigator {
    private static final Logger LOG = Logger.getInstance(BuildTreeConsoleView.class);

    private static final String TREE = "tree";
    private static final String SPLITTER_PROPERTY = "BuildView.Splitter.Proportion";
    private final JPanel myPanel = new JPanel();
    private final ConsoleViewHandler myConsoleViewHandler;
    private final StructureTreeModel<AbstractTreeStructure> myTreeModel;
    private final Tree myTree;
    private final ProblemOccurrenceNavigatorSupport myOccurrenceNavigatorSupport;

    public BuildTreeConsoleView(
        Project project,
        BuildDescriptor buildDescriptor,
        @Nullable ExecutionConsole executionConsole,
        BuildViewSettingsProvider buildViewSettingsProvider
    ) {
        super(project, buildDescriptor);

        AbstractTreeStructure treeStructure = new MyTreeStructure();
        myTreeModel = new StructureTreeModel<>(
            treeStructure,
            null,
            myInvoker,
            this
        );
        AsyncTreeModel asyncTreeModel = new AsyncTreeModel(myTreeModel, this);
        asyncTreeModel.addTreeModelListener(new ExecutionNodeAutoExpandingListener());
        myTree = initTree(asyncTreeModel);

        JPanel myContentPanel = new JPanel();
        myContentPanel.setLayout(new CardLayout());
        myContentPanel.add(ScrollPaneFactory.createScrollPane(myTree, SideBorder.NONE), TREE);

        myPanel.setLayout(new BorderLayout());
        OnePixelSplitter myThreeComponentsSplitter = new OnePixelSplitter(SPLITTER_PROPERTY, 0.33f);
        myThreeComponentsSplitter.setFirstComponent(myContentPanel);
        List<Filter> filters = myBuildDescriptor.getExecutionFilters();
        myConsoleViewHandler = new ConsoleViewHandler(
            myProject,
            myTree,
            myBuildProgressRootNode,
            this,
            executionConsole,
            filters,
            buildViewSettingsProvider
        );
        myThreeComponentsSplitter.setSecondComponent(myConsoleViewHandler.getComponent());
        myPanel.add(myThreeComponentsSplitter, BorderLayout.CENTER);
        BuildTreeFilters.install(this);
        myOccurrenceNavigatorSupport = new ProblemOccurrenceNavigatorSupport(myTree);
    }


    @Override
    protected void installContextMenu() {
        invokeLaterIfNeeded(() -> {
            DefaultActionGroup rerunActionGroup = new DefaultActionGroup();
            List<AnAction> restartActions = myBuildDescriptor.getRestartActions();
            rerunActionGroup.addAll(restartActions);
            if (!restartActions.isEmpty()) {
                rerunActionGroup.addSeparator();
            }

            DefaultActionGroup sourceActionGroup = new DefaultActionGroup();
            EditSourceAction edit = new EditSourceAction();
            ActionUtil.copyFrom(edit, IdeActions.ACTION_EDIT_SOURCE);
            sourceActionGroup.add(edit);
            DefaultActionGroup filteringActionsGroup = BuildTreeFilters.createFilteringActionsGroup(this);
            DefaultActionGroup navigationActionGroup = new DefaultActionGroup();
            CommonActionsManager actionsManager = CommonActionsManager.getInstance();
            AnAction prevAction = actionsManager.createPrevOccurenceAction(this);
            navigationActionGroup.add(prevAction);
            AnAction nextAction = actionsManager.createNextOccurenceAction(this);
            navigationActionGroup.add(nextAction);

            myTree.addMouseListener(new PopupHandler() {
                @Override
                public void invokePopup(Component comp, int x, int y) {
                    DefaultActionGroup group = new DefaultActionGroup();
                    group.addAll(rerunActionGroup);
                    group.addAll(sourceActionGroup);
                    group.addSeparator();
                    ExecutionNodeImpl[] selectedNodes = getSelectedNodes();
                    if (selectedNodes.length == 1) {
                        ExecutionNodeImpl selectedNode = selectedNodes[0];
                        List<AnAction> contextActions = myBuildDescriptor.getContextActions(selectedNode);
                        if (!contextActions.isEmpty()) {
                            group.addAll(contextActions);
                            group.addSeparator();
                        }
                    }
                    group.addAll(filteringActionsGroup);
                    group.addSeparator();
                    group.addAll(navigationActionGroup);
                    ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu("BuildView", group);
                    popupMenu.setTargetComponent(myTree);
                    JPopupMenu menu = popupMenu.getComponent();
                    menu.show(comp, x, y);
                }
            });
        });
    }

    @Override
    public void clear() {
        myTreeModel.getInvoker().runOrInvokeLater(() -> {
            getRootElement().removeChildren();
            nodesMap.clear();
            myConsoleViewHandler.clear();
        });
        scheduleUpdate(getRootElement(), true);
    }













    //@ApiStatus.Internal
    @RequiredUIAccess
    @TestOnly
    public @Nullable ExecutionConsole getSelectedNodeConsole() {
        ExecutionConsole console = myConsoleViewHandler.getCurrentConsole();
        if (console instanceof ConsoleViewImpl consoleView) {
            consoleView.flushDeferredText();
        }
        return console;
    }




    @Override
    public boolean hasNextOccurence() {
        return myOccurrenceNavigatorSupport.hasNextOccurence();
    }

    @Override
    public boolean hasPreviousOccurence() {
        return myOccurrenceNavigatorSupport.hasPreviousOccurence();
    }

    @Override
    public OccurenceInfo goNextOccurence() {
        return myOccurrenceNavigatorSupport.goNextOccurence();
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
        return myOccurrenceNavigatorSupport.goPreviousOccurence();
    }

    @Override
    public String getNextOccurenceActionName() {
        return myOccurrenceNavigatorSupport.getNextOccurenceActionName();
    }

    @Override
    public String getPreviousOccurenceActionName() {
        return myOccurrenceNavigatorSupport.getPreviousOccurenceActionName();
    }

    private static TreeVisitor visitor(ExecutionNodeImpl executionNode) {
        TreePath treePath = TreePathUtil.pathToCustomNode(executionNode, ExecutionNodeImpl::getParent);
        return new TreeVisitor.ByTreePath<>(treePath, o -> (ExecutionNodeImpl)TreeUtil.getUserObject(o));
    }
















    @Override
    public AnAction[] createConsoleActions() {
        return AnAction.EMPTY_ARRAY;
    }


    @Override
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    public JComponent getPreferredFocusableComponent() {
        return myTree;
    }




    @Override
    protected void scheduleUpdate(ExecutionNodeImpl executionNode, boolean parentStructureChanged) {
        ExecutionNodeImpl node = (executionNode.getParent() == null || !parentStructureChanged) ? executionNode : executionNode.getParent();
        myTreeModel.invalidate(node, parentStructureChanged);
    }

    @Override
    protected void invalidateAll(boolean structure, @Nullable Runnable after) {
        Promise<?> promise = myTreeModel.invalidate(getRootElement(), structure);
        if (after != null) {
            promise.onProcessed(p -> after.run());
        }
    }

    @Override
    protected void makeNodeVisible(ExecutionNodeImpl node) {
        TreeUtil.promiseMakeVisible(myTree, visitor(node));
    }

    @Override
    protected void selectNode(ExecutionNodeImpl node) {
        TreeUtil.promiseSelect(myTree, visitor(node));
    }

    @Override
    protected BuildNodeConsoleHandler getConsoleHandler() {
        return myConsoleViewHandler;
    }




    public void hideRootNode() {
        invokeLaterIfNeeded(() -> {
            if (myTree != null) {
                myTree.setRootVisible(false);
                myTree.setShowsRootHandles(true);
            }
        });
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(HelpManager.HELP_ID, "reference.build.tool.window");
        sink.set(Project.KEY, myProject);
        sink.lazy(Navigatable.KEY_OF_ARRAY, () -> (Navigatable[]) extractSelectedNodesNavigatables());
        sink.lazy(Navigatable.KEY, () -> (Navigatable) extractSelectedNodeNavigatable());
    }

    private
    @Nullable Object extractSelectedNodeNavigatable() {
        TreePath selectedPath = TreeUtil.getSelectedPathIfOne(myTree);
        if (selectedPath == null) {
            return null;
        }
        DefaultMutableTreeNode node = ObjectUtil.tryCast(selectedPath.getLastPathComponent(), DefaultMutableTreeNode.class);
        if (node == null) {
            return null;
        }
        ExecutionNodeImpl executionNode = ObjectUtil.tryCast(node.getUserObject(), ExecutionNodeImpl.class);
        if (executionNode == null) {
            return null;
        }
        List<Navigatable> navigatables = executionNode.getNavigatables();
        if (navigatables.size() != 1) {
            return null;
        }
        return navigatables.get(0);
    }

    private Object extractSelectedNodesNavigatables() {
        List<Navigatable> navigatables = new ArrayList<>();
        for (ExecutionNodeImpl each : getSelectedNodes()) {
            List<Navigatable> navigatable = each.getNavigatables();
            navigatables.addAll(navigatable);
        }
        return navigatables.isEmpty() ? null : navigatables.toArray(new Navigatable[0]);
    }

    private ExecutionNodeImpl[] getSelectedNodes() {
        ExecutionNodeImpl[] result = new ExecutionNodeImpl[0];
        if (myTree != null) {
            List<ExecutionNodeImpl> nodes =
                TreeUtil.collectSelectedObjects(myTree, path -> TreeUtil.getLastUserObject(ExecutionNodeImpl.class, path));
            return nodes.toArray(result);
        }
        return result;
    }

    //@ApiStatus.Internal
    public JTree getTree() {
        return myTree;
    }

    private static Tree initTree(AsyncTreeModel model) {
        Tree tree = new Tree(model);
        tree.setLargeModel(true);
        ComponentUtil.putClientProperty(tree, ANIMATION_IN_RENDERER_ALLOWED, true);
        ComponentUtil.putClientProperty(tree, TreeOptions.AUTO_EXPAND_ALLOWED, false);
        tree.setRootVisible(false);
        EditSourceOnDoubleClickHandler.install(tree);
        EditSourceOnEnterKeyHandler.install(tree);
        new TreeSpeedSearch(tree).setComparator(new SpeedSearchComparator(false));
        TreeUtil.installActions(tree);
        tree.setCellRenderer(new MyNodeRenderer());
        tree.putClientProperty(SHRINK_LONG_RENDERER, true);
        return tree;
    }


    //@ApiStatus.Internal
    public Promise<?> invokeLater(Runnable task) {
        return myTreeModel.getInvoker().invokeLater(task);
    }

    private static class ConsoleViewHandler implements BuildNodeConsoleHandler {
        private static final String EMPTY_CONSOLE_NAME = "empty";
        private final Project myProject;
        private final JPanel myPanel;
        private final CompositeView<ExecutionConsole> myView;
        private final AtomicReference<String> myNodeConsoleViewName = new AtomicReference<>();
        private final Map<String, List<Consumer<? super BuildTextConsoleView>>> deferredNodeOutput = new ConcurrentHashMap<>();
        private final BuildViewSettingsProvider myViewSettingsProvider;
        private @Nullable ExecutionNodeImpl myExecutionNode;
        private final List<Filter> myExecutionConsoleFilters;
        private final BuildProgressStripe myPanelWithProgress;
        private final DefaultActionGroup myConsoleToolbarActionGroup;
        private final ActionToolbar myToolbar;

        ConsoleViewHandler(
            Project project,
            Tree tree,
            ExecutionNodeImpl buildProgressRootNode,
            Disposable parentDisposable,
            @Nullable ExecutionConsole executionConsole,
            List<Filter> executionConsoleFilters,
            BuildViewSettingsProvider buildViewSettingsProvider
        ) {
            myProject = project;
            myPanel = new NonOpaquePanel(new BorderLayout());
            myPanelWithProgress = new BuildProgressStripe(
                myPanel,
                parentDisposable,
                ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS
            );
            myViewSettingsProvider = buildViewSettingsProvider;
            myExecutionConsoleFilters = executionConsoleFilters;
            Disposer.register(parentDisposable, this);
            myView = new CompositeView<>(null) {
                @Override
                public void addView(ExecutionConsole view, String viewName) {
                    super.addView(view, viewName);
                    removeScrollBorder(view.getComponent());
                }
            };
            Disposer.register(this, myView);
            if (executionConsole != null) {
                String nodeConsoleViewName = getNodeConsoleViewName(buildProgressRootNode);
                myView.addViewAndShowIfNeeded(executionConsole, nodeConsoleViewName, true);
                myNodeConsoleViewName.set(nodeConsoleViewName);
            }
            ConsoleView emptyConsole = new ConsoleViewImpl(project, GlobalSearchScope.EMPTY_SCOPE, true, false);
            myView.addView(emptyConsole, EMPTY_CONSOLE_NAME);
            JComponent consoleComponent = emptyConsole.getComponent();
            consoleComponent.setFocusable(true);
            myPanel.add(myView.getComponent(), BorderLayout.CENTER);
            myConsoleToolbarActionGroup = new DefaultActionGroup();
            myToolbar = ActionManager.getInstance().createActionToolbar("BuildConsole", myConsoleToolbarActionGroup, false);
            myToolbar.setTargetComponent(myPanel);
            showTextConsoleToolbarActions();
            myPanel.add(myToolbar.getComponent(), BorderLayout.EAST);
            tree.addTreeSelectionListener(e -> {
                TreePath path = e.getPath();
                if (path == null || !e.isAddedPath()) {
                    return;
                }
                TreePath selectionPath = tree.getSelectionPath();
                setNode(selectionPath != null ? (DefaultMutableTreeNode)selectionPath.getLastPathComponent() : null);
            });
        }

        private void showTextConsoleToolbarActions() {
            myConsoleToolbarActionGroup.copyFromGroup(createDefaultTextConsoleToolbar());
            updateToolbarActionsImmediately();
        }

        private void showCustomConsoleToolbarActions(@Nullable ActionGroup actionGroup) {
            if (actionGroup instanceof DefaultActionGroup defaultActionGroup) {
                myConsoleToolbarActionGroup.copyFromGroup(defaultActionGroup);
            }
            else if (actionGroup != null) {
                myConsoleToolbarActionGroup.copyFrom(actionGroup);
            }
            else {
                myConsoleToolbarActionGroup.removeAll();
            }
            updateToolbarActionsImmediately();
        }

        private void updateToolbarActionsImmediately() {
            invokeLaterIfNeeded(myToolbar::updateActionsImmediately);
        }

        private DefaultActionGroup createDefaultTextConsoleToolbar() {
            DefaultActionGroup textConsoleToolbarActionGroup = new DefaultActionGroup();
            textConsoleToolbarActionGroup.add(new ToggleUseSoftWrapsToolbarAction(SoftWrapAppliancePlaces.CONSOLE) {
                @Override
                protected @Nullable Editor getEditor(AnActionEvent e) {
                    return ConsoleViewHandler.this.getEditor();
                }
            });
            textConsoleToolbarActionGroup.add(new ScrollEditorToTheEndAction(this));
            return textConsoleToolbarActionGroup;
        }

        @Override
        public void updateProgressBar(long total, long progress) {
            myProject.getUIAccess().give(() -> myPanelWithProgress.updateProgress(total, progress));
        }

        @Override
        public @Nullable ExecutionConsole getCurrentConsole() {
            String nodeConsoleViewName = myNodeConsoleViewName.get();
            if (nodeConsoleViewName == null) {
                return null;
            }
            return myView.getView(nodeConsoleViewName);
        }

        private
        @Nullable Editor getEditor() {
            ExecutionConsole console = getCurrentConsole();
            if (console instanceof ConsoleViewImpl consoleView) {
                return consoleView.getEditor();
            }
            return null;
        }

        @Override
        public boolean setNode(@Nullable ExecutionNodeImpl node) {
            String nodeConsoleViewName = getNodeConsoleViewName(node);
            myNodeConsoleViewName.set(nodeConsoleViewName);
            ExecutionConsole view = myView.getView(nodeConsoleViewName);
            if (view != null) {
                List<Consumer<? super BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
                if (view instanceof BuildTextConsoleView buildTextConsoleView && deferredOutput != null && !deferredOutput.isEmpty()) {
                    deferredNodeOutput.remove(nodeConsoleViewName);
                    deferredOutput.forEach(consumer -> consumer.accept(buildTextConsoleView));
                }
                else {
                    deferredNodeOutput.remove(nodeConsoleViewName);
                }
                myView.showView(nodeConsoleViewName, false);
                if (view instanceof PresentableBuildEventExecutionConsole executionConsole) {
                    showCustomConsoleToolbarActions(executionConsole.myActions);
                }
                else {
                    showTextConsoleToolbarActions();
                }
                myPanel.setVisible(true);
                return true;
            }

            List<Consumer<? super BuildTextConsoleView>> deferredOutput = deferredNodeOutput.get(nodeConsoleViewName);
            if (deferredOutput != null && !deferredOutput.isEmpty()) {
                BuildTextConsoleView textConsoleView = new BuildTextConsoleView(myProject, true, myExecutionConsoleFilters);
                deferredNodeOutput.remove(nodeConsoleViewName);
                deferredOutput.forEach(consumer -> consumer.accept(textConsoleView));
                myView.addView(textConsoleView, nodeConsoleViewName);
                myView.showView(nodeConsoleViewName, false);
            }
            else {
                myView.showView(EMPTY_CONSOLE_NAME, false);
                return true;
            }
            return true;
        }

        public void maybeAddExecutionConsole(ExecutionNodeImpl node, BuildEventPresentationData presentationData) {
            invokeLaterIfNeeded(() -> {
                ExecutionConsole executionConsole = presentationData.getExecutionConsole();
                if (executionConsole == null) {
                    return;
                }
                String nodeConsoleViewName = getNodeConsoleViewName(node);
                PresentableBuildEventExecutionConsole presentableEventView =
                    new PresentableBuildEventExecutionConsole(executionConsole, presentationData.consoleToolbarActions());
                myView.addView(presentableEventView, nodeConsoleViewName);
            });
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, String text, boolean stdOut) {
            addOutput(node, view -> view.append(text, stdOut));
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, Object buildId, BuildEvent event) {
            addOutput(node, view -> view.onEvent(buildId, event));
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, Failure failure) {
            addOutput(node, view -> view.append(failure));
        }

        private void addOutput(ExecutionNodeImpl node, Consumer<? super BuildTextConsoleView> consumer) {
            String nodeConsoleViewName = getNodeConsoleViewName(node);
            ExecutionConsole viewView = myView.getView(nodeConsoleViewName);
            if (viewView instanceof BuildTextConsoleView buildTextConsoleView) {
                consumer.accept(buildTextConsoleView);
            }
            if (viewView == null) {
                deferredNodeOutput.computeIfAbsent(nodeConsoleViewName, s -> new ArrayList<>()).add(consumer);
            }
        }

        @Override
        public boolean hasNode() {
            return myExecutionNode != null;
        }

        @Override
        public void dispose() {
            deferredNodeOutput.clear();
        }

        @Override
        public void stopProgressBar() {
            myPanelWithProgress.stopLoading();
        }

        private static
        String getNodeConsoleViewName(ExecutionNodeImpl node) {
            return String.valueOf(System.identityHashCode(node));
        }

        private void setNode(@Nullable DefaultMutableTreeNode node) {
            if (myProject.isDisposed()) {
                return;
            }
            if (node == null || node.getUserObject() == myExecutionNode) {
                return;
            }
            if (node.getUserObject() instanceof ExecutionNodeImpl executionNode) {
                myExecutionNode = executionNode;
                if (setNode(executionNode)) {
                    return;
                }
            }

            myExecutionNode = null;
            if (myView.getView(CONSOLE_VIEW_NAME) != null/* && myViewSettingsProvider.isSideBySideView()*/) {
                myView.showView(CONSOLE_VIEW_NAME, false);
                myPanel.setVisible(true);
            }
            else {
                myPanel.setVisible(false);
            }
        }

        public JComponent getComponent() {
            return myPanelWithProgress;
        }

        public void clear() {
            myPanel.setVisible(false);
        }

        private static class PresentableBuildEventExecutionConsole implements ExecutionConsole {
            private final ExecutionConsole myExecutionConsole;
            private final @Nullable ActionGroup myActions;

            private PresentableBuildEventExecutionConsole(
                ExecutionConsole executionConsole,
                @Nullable ActionGroup toolbarActions
            ) {
                myExecutionConsole = executionConsole;
                myActions = toolbarActions;
            }

            @Override
            public JComponent getComponent() {
                return myExecutionConsole.getComponent();
            }

            @Override
            public JComponent getPreferredFocusableComponent() {
                return myExecutionConsole.getPreferredFocusableComponent();
            }

            @Override
            public void dispose() {
                Disposer.dispose(myExecutionConsole);
            }
        }
    }

    private static class ProblemOccurrenceNavigatorSupport extends OccurenceNavigatorSupport {
        ProblemOccurrenceNavigatorSupport(Tree tree) {
            super(tree);
        }

        @Override
        protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
            Object userObject = node.getUserObject();
            if (!(userObject instanceof ExecutionNodeImpl executionNode)) {
                return null;
            }
            if (node.getChildCount() != 0 || !executionNode.hasWarnings() && !executionNode.isFailed()) {
                return null;
            }
            List<Navigatable> navigatables = executionNode.getNavigatables();
            if (!navigatables.isEmpty()) {
                return navigatables.get(0);
            }
            return null;
        }

        @Override
        public String getNextOccurenceActionName() {
            return IdeLocalize.actionNextProblem().get();
        }

        @Override
        public String getPreviousOccurenceActionName() {
            return IdeLocalize.actionPreviousProblem().get();
        }
    }

    private static class ScrollEditorToTheEndAction extends ToggleAction implements DumbAware {
        private final
        ConsoleViewHandler myConsoleViewHandler;

        ScrollEditorToTheEndAction(ConsoleViewHandler handler) {
            super(
                ActionLocalize.actionEditorconsolescrolltotheendText(),
                LocalizeValue.empty(),
                PlatformIconGroup.runconfigurationsScroll_down()
            );
            myConsoleViewHandler = handler;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            Editor editor = myConsoleViewHandler.getEditor();
            if (editor == null) {
                return false;
            }
            Document document = editor.getDocument();
            return document.getLineCount() == 0
                || document.getLineNumber(ReadAction.compute(() -> editor.getCaretModel().getOffset())) == document.getLineCount() - 1;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            Editor editor = myConsoleViewHandler.getEditor();
            if (editor == null) {
                return;
            }
            if (state) {
                EditorUtil.scrollToTheEnd(editor);
            }
            else {
                int lastLine = Math.max(0, editor.getDocument().getLineCount() - 1);
                LogicalPosition currentPosition = editor.getCaretModel().getLogicalPosition();
                LogicalPosition position =
                    new LogicalPosition(Math.max(0, Math.min(currentPosition.line, lastLine - 1)), currentPosition.column);
                editor.getCaretModel().moveToLogicalPosition(position);
            }
        }
    }

    private static class MyNodeRenderer extends NodeRenderer {
        private String myDurationText;
        private Color myDurationColor;
        private int myDurationWidth;
        private int myDurationOffset;

        @RequiredUIAccess
        @Override
        public void customizeCellRenderer(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            myDurationText = null;
            myDurationColor = null;
            myDurationWidth = 0;
            myDurationOffset = 0;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
            Object userObj = node.getUserObject();
            if (userObj instanceof ExecutionNodeImpl executionNode) {
                myDurationText = executionNode.getDuration();
                if (myDurationText != null) {
                    FontMetrics metrics = getFontMetrics(RelativeFont.SMALL.derive(getFont()));
                    myDurationWidth = metrics.stringWidth(myDurationText);
                    myDurationOffset = metrics.getHeight() / 2; // an empty area before and after the text
                    myDurationColor = selected ? getTreeSelectionForeground(hasFocus) : GRAYED_ATTRIBUTES.getFgColor();
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            UISettingsUtil.setupAntialiasing(g);
            Shape clip = null;
            int width = getWidth();
            int height = getHeight();
            if (isOpaque()) {
                // paint background for expanded row
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
            if (myDurationWidth > 0) {
                width -= myDurationWidth + myDurationOffset;
                if (width > 0 && height > 0) {
                    g.setColor(myDurationColor);
                    g.setFont(RelativeFont.SMALL.derive(getFont()));
                    g.drawString(myDurationText, width + myDurationOffset / 2, getTextBaseLine(g.getFontMetrics(), height));
                    clip = g.getClip();
                    g.clipRect(0, 0, width, height);
                }
            }

            super.paintComponent(g);
            // restore clip area if needed
            if (clip != null) {
                g.setClip(clip);
            }
        }
    }

    private class MyTreeStructure extends AbstractTreeStructure {
        @Override
        public
        Object getRootElement() {
            return myRootNode;
        }

        @Override
        public
        Object[] getChildElements(Object element) {
            // This .toArray() is still slow but it is called less frequently because of batching in AsyncTreeModel and process less data if
            // filters are applied.
            return ((ExecutionNodeImpl)element).getChildList().toArray();
        }

        @Override
        public
        @Nullable Object getParentElement(Object element) {
            return ((ExecutionNodeImpl)element).getParent();
        }

        @Override
        public
        NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
            return ((NodeDescriptor)element);
        }

        @Override
        public void commit() {
        }

        @Override
        public boolean hasSomethingToCommit() {
            return false;
        }

        @Override
        public boolean isAlwaysLeaf(Object element) {
            return ((ExecutionNodeImpl)element).isAlwaysLeaf();
        }
    }

    private class ExecutionNodeAutoExpandingListener implements TreeModelListener {
        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            maybeExpand(e.getTreePath());
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            // ExecutionNodeImpl should never change its isAutoExpand state. Ignore calls and do nothing.
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            // A removed node is not a reason to expand it parent. Ignore calls and do nothing.
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            // We do not expect this event to happen in cases other than clearing the tree (including changing the filter).
            // Ignore calls and do nothing.
        }

        private boolean maybeExpand(TreePath path) {
            if (myTree == null || path == null) {
                return false;
            }
            Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode mutableTreeNode) {
                boolean expanded = false;
                Enumeration<?> children = mutableTreeNode.children();
                if (children.hasMoreElements()) {
                    while (children.hasMoreElements()) {
                        Object next = children.nextElement();
                        if (next != null) {
                            expanded = maybeExpand(path.pathByAddingChild(next)) || expanded;
                        }
                    }
                    if (expanded) {
                        return true;
                    }
                    if (mutableTreeNode.getUserObject() instanceof ExecutionNodeImpl lastUserObject
                        && lastUserObject.isAutoExpandNode() && !myTree.isExpanded(path)) {
                        myTree.expandPath(path);
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
