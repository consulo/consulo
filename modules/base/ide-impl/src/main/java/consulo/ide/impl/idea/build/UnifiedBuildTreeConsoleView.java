/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.BuildViewSettingsProvider;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.event.BuildEventPresentationData;
import consulo.build.ui.event.Failure;
import consulo.application.HelpManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.impl.internal.console.UnifiedConsoleViewImpl;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.console.Filter;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.TextAttribute;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Analog of {@link BuildTreeConsoleView} for the frontends which render {@link consulo.ui} components rather
 * than swing - the very same tree of nodes, beside the console of whichever node is selected.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public class UnifiedBuildTreeConsoleView extends BaseBuildTreeConsoleView {
    private final UnifiedConsoleViewHandler myConsoleViewHandler;
    private final Tree<ExecutionNodeImpl> myTree;
    private final TwoComponentSplitLayout myRoot;

    /**
     * Which row of the tree stands for which node. A node changes long after the row was built, and the tree
     * is told to re-read that row rather than the whole of it.
     */
    private final Map<ExecutionNodeImpl, TreeNode<ExecutionNodeImpl>> myTreeNodes = new ConcurrentHashMap<>();

    @RequiredUIAccess
    public UnifiedBuildTreeConsoleView(
        Project project,
        BuildDescriptor buildDescriptor,
        @Nullable ExecutionConsole executionConsole,
        BuildViewSettingsProvider buildViewSettingsProvider
    ) {
        super(project, buildDescriptor);

        myTree = Tree.create(getRootElement(), new BuildTreeModel(), new InvokerTreeExecutor());

        myConsoleViewHandler = new UnifiedConsoleViewHandler(
            project,
            getBuildProgressRootNode(),
            executionConsole,
            myBuildDescriptor.getExecutionFilters()
        );
        Disposer.register(this, myConsoleViewHandler);

        myTree.addSelectListener(event -> {
            TreeNode<ExecutionNodeImpl> selected = myTree.getSelectedNode();
            myConsoleViewHandler.setNode(selected == null ? null : selected.getValue());
        });

        myRoot = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        // the actions of a build read the project and the selected node from the context of the component
        // they sit next to, and a component answers for that only when it carries a provider
        myRoot.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(HelpManager.HELP_ID, "reference.build.tool.window");
            sink.set(Project.KEY, myProject);
            sink.lazy(Navigatable.KEY, this::getSelectedNavigatable);
        });
        myRoot.setFirstComponent(myTree);
        myRoot.setSecondComponent(myConsoleViewHandler.getUIComponent());
        myRoot.setProportion(33);

        BuildTreeFilters.install(this);
    }

    private class BuildTreeModel implements TreeModel<ExecutionNodeImpl> {
        @Override
        public void buildChildren(
            Function<ExecutionNodeImpl, TreeNode<ExecutionNodeImpl>> nodeFactory,
            @Nullable ExecutionNodeImpl parentValue
        ) {
            ExecutionNodeImpl parent = parentValue == null ? getRootElement() : parentValue;

            for (ExecutionNodeImpl child : parent.getChildList()) {
                TreeNode<ExecutionNodeImpl> treeNode = nodeFactory.apply(child);
                treeNode.setLeaf(child.getChildList().isEmpty());
                treeNode.setRenderer((value, presentation) -> {
                    presentation.withIcon(value.getPresentableIcon());

                    String title = value.getTitle();
                    if (title != null && !title.isEmpty()) {
                        presentation.append(LocalizeValue.of(title + ": "), TextAttribute.REGULAR_BOLD);
                    }

                    String name = value.getName();
                    if (name != null && !name.isEmpty()) {
                        presentation.append(LocalizeValue.of(name));
                    }

                    String hint = value.getCurrentHint();
                    if (hint != null && !hint.isEmpty()) {
                        presentation.append(LocalizeValue.of((name == null || name.isEmpty() ? "" : " ") + hint), TextAttribute.GRAY);
                    }
                });

                myTreeNodes.put(child, treeNode);
            }
        }
    }

    /**
     * The tree reads its model on the same thread the nodes are built and changed on, which is what lets the
     * model be walked without locking.
     */
    private class InvokerTreeExecutor implements TreeExecutor {
        @Override
        public <T> CompletableFuture<T> execute(Tree<?> tree, Supplier<T> task) {
            CompletableFuture<T> result = new CompletableFuture<>();
            myInvoker.invokeLater(() -> {
                try {
                    result.complete(task.get());
                }
                catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            });
            return result;
        }
    }

    @Override
    protected void scheduleUpdate(ExecutionNodeImpl executionNode, boolean parentStructureChanged) {
        ExecutionNodeImpl node =
            (executionNode.getParent() == null || !parentStructureChanged) ? executionNode : executionNode.getParent();

        myProject.getUIAccess().give(() -> {
            if (myProject.isDisposed() || isDisposed()) {
                return;
            }

            TreeNode<ExecutionNodeImpl> treeNode = myTreeNodes.get(node);
            if (treeNode == null) {
                // a node the tree never built a row for - the level it belongs to is read when it is opened
                myTree.refreshAll();
                return;
            }

            myTree.refreshItem(treeNode, parentStructureChanged);
        });
    }

    @Override
    protected void invalidateAll(boolean structure, @Nullable Runnable after) {
        myProject.getUIAccess().give(() -> {
            if (myProject.isDisposed() || isDisposed()) {
                return;
            }

            CompletableFuture<?> refreshed = myTree.refreshAll();
            if (after != null) {
                refreshed.whenComplete((ignored, throwable) -> after.run());
            }
        });
    }

    @Override
    protected void makeNodeVisible(ExecutionNodeImpl node) {
        myProject.getUIAccess().give(() -> {
            TreeNode<ExecutionNodeImpl> treeNode = myTreeNodes.get(node);
            if (treeNode != null) {
                myTree.expand(treeNode);
            }
        });
    }

    @Override
    protected void selectNode(ExecutionNodeImpl node) {
        myProject.getUIAccess().give(() -> {
            TreeNode<ExecutionNodeImpl> treeNode = myTreeNodes.get(node);
            if (treeNode != null) {
                myTree.select(treeNode);
            }
        });
    }

    /**
     * The actions of a node are offered by the toolbar of the view rather than by a menu of the tree, which
     * not every frontend can put up.
     */
    @Override
    protected void installContextMenu() {
    }

    private @Nullable Navigatable getSelectedNavigatable() {
        TreeNode<ExecutionNodeImpl> selected = myTree.getSelectedNode();
        ExecutionNodeImpl node = selected == null ? null : selected.getValue();
        if (node == null) {
            return null;
        }

        List<Navigatable> navigatables = node.getNavigatables();
        return navigatables.size() == 1 ? navigatables.get(0) : null;
    }

    @Override
    protected BuildNodeConsoleHandler getConsoleHandler() {
        return myConsoleViewHandler;
    }

    @Override
    public AnAction[] createConsoleActions() {
        return AnAction.EMPTY_ARRAY;
    }

    @Override
    public @Nullable Component getUIComponent() {
        return myRoot;
    }

    @Override
    public @Nullable Component getUIPreferredFocusableComponent() {
        return myTree;
    }

    @Override
    public void clear() {
        myConsoleViewHandler.clear();
    }

    @Override
    public void dispose() {
        super.dispose();
        myTreeNodes.clear();
    }

    /**
     * The consoles of the nodes, one per node, of which the selected one is on screen. Analog of the handler
     * of the same name inside {@link BuildTreeConsoleView}.
     */
    private static class UnifiedConsoleViewHandler implements BuildNodeConsoleHandler {
        private final Project myProject;
        private final List<Filter> myExecutionConsoleFilters;

        private final Map<String, ExecutionConsole> myViews = new HashMap<>();
        private final Map<String, List<Consumer<? super BuildTextConsole>>> myDeferredNodeOutput = new ConcurrentHashMap<>();

        private final WrappedLayout myRoot;

        private @Nullable ExecutionNodeImpl myExecutionNode;

        @RequiredUIAccess
        private UnifiedConsoleViewHandler(
            Project project,
            ExecutionNodeImpl buildProgressRootNode,
            @Nullable ExecutionConsole executionConsole,
            List<Filter> executionConsoleFilters
        ) {
            myProject = project;
            myExecutionConsoleFilters = executionConsoleFilters;
            myRoot = WrappedLayout.create();

            if (executionConsole != null) {
                myViews.put(getNodeConsoleViewName(buildProgressRootNode), executionConsole);
            }
        }

        private static String getNodeConsoleViewName(ExecutionNodeImpl node) {
            return String.valueOf(System.identityHashCode(node));
        }

        @RequiredUIAccess
        private Component getUIComponent() {
            return myRoot;
        }

        @Override
        public boolean setNode(@Nullable ExecutionNodeImpl node) {
            if (myProject.isDisposed() || node == null) {
                return false;
            }

            myExecutionNode = node;

            myProject.getUIAccess().give(() -> {
                if (myProject.isDisposed()) {
                    return;
                }

                myRoot.set(getOrCreateConsole(node).getUIComponent());
            });

            return true;
        }

        @RequiredUIAccess
        private ExecutionConsole getOrCreateConsole(ExecutionNodeImpl node) {
            String name = getNodeConsoleViewName(node);

            ExecutionConsole console = myViews.get(name);
            if (console != null) {
                return console;
            }

            UnifiedBuildTextConsoleView textConsole = new UnifiedBuildTextConsoleView(myProject, true, myExecutionConsoleFilters);
            myViews.put(name, textConsole);

            List<Consumer<? super BuildTextConsole>> deferred = myDeferredNodeOutput.remove(name);
            if (deferred != null) {
                deferred.forEach(consumer -> consumer.accept(textConsole));
            }

            return textConsole;
        }

        @Override
        public boolean hasNode() {
            return myExecutionNode != null;
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, Object buildId, BuildEvent event) {
            addOutput(node, view -> view.onEvent(buildId, event));
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, String text, boolean stdOut) {
            addOutput(node, view -> view.append(text, stdOut));
        }

        @Override
        public void addOutput(ExecutionNodeImpl node, Failure failure) {
            addOutput(node, view -> view.append(failure));
        }

        private void addOutput(ExecutionNodeImpl node, Consumer<? super BuildTextConsole> consumer) {
            String name = getNodeConsoleViewName(node);

            ExecutionConsole console = myViews.get(name);
            if (console instanceof BuildTextConsole textConsole) {
                consumer.accept(textConsole);
                return;
            }

            if (console == null) {
                // the console of a node is built when the node is first shown, so what it printed before that
                // is kept until there is something to print it into
                myDeferredNodeOutput.computeIfAbsent(name, s -> new ArrayList<>()).add(consumer);
            }
        }

        @Override
        public void maybeAddExecutionConsole(ExecutionNodeImpl node, BuildEventPresentationData presentationData) {
            ExecutionConsole executionConsole = presentationData.getExecutionConsole();
            if (executionConsole == null) {
                return;
            }

            myViews.put(getNodeConsoleViewName(node), executionConsole);
        }

        @Override
        public void updateProgressBar(long total, long progress) {
        }

        @Override
        public void stopProgressBar() {
        }

        @Override
        public void clear() {
            myProject.getUIAccess().give(() -> myRoot.set((Component) null));
        }

        @Override
        public @Nullable ExecutionConsole getCurrentConsole() {
            ExecutionNodeImpl node = myExecutionNode;
            return node == null ? null : myViews.get(getNodeConsoleViewName(node));
        }

        @Override
        public void dispose() {
            myDeferredNodeOutput.clear();
            myViews.clear();
        }
    }
}
