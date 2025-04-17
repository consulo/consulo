// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.projectView;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.wm.FocusRequestor;
import consulo.application.util.registry.Registry;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.ui.ex.UiActivity;
import consulo.ui.ex.UiActivityMonitor;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.AbstractTreeUpdater;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class BaseProjectTreeBuilder extends AbstractTreeBuilder {
    protected final Project myProject;

    public BaseProjectTreeBuilder(
        @Nonnull Project project,
        @Nonnull JTree tree,
        @Nonnull DefaultTreeModel treeModel,
        @Nonnull AbstractTreeStructure treeStructure,
        @Nullable Comparator<NodeDescriptor> comparator
    ) {
        init(tree, treeModel, treeStructure, comparator, DEFAULT_UPDATE_INACTIVE);
        getUi().setClearOnHideDelay(Registry.intValue("ide.tree.clearOnHideTime"));
        myProject = project;
    }

    @Nonnull
    @Override
    public Promise<Object> revalidateElement(@Nonnull Object element) {
        if (!(element instanceof AbstractTreeNode)) {
            return Promises.rejectedPromise();
        }

        final AsyncPromise<Object> result = new AsyncPromise<>();
        AbstractTreeNode node = (AbstractTreeNode)element;
        final Object value = node.getValue();
        final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(ObjectUtil.tryCast(value, PsiElement.class));
        batch(indicator -> {
            final Ref<Object> target = new Ref<>();
            Promise<Object> callback = _select(element, virtualFile, true, Conditions.alwaysTrue());
            callback.onSuccess(it -> result.setResult(target.get())).onError(e -> result.setError(e));
        });
        return result;
    }

    @Override
    protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
        return nodeDescriptor instanceof AbstractTreeNode && ((AbstractTreeNode)nodeDescriptor).isAlwaysShowPlus();
    }

    @Override
    protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return nodeDescriptor.getParentDescriptor() == null || nodeDescriptor instanceof AbstractTreeNode && ((AbstractTreeNode)nodeDescriptor).isAlwaysExpand();
    }

    @Override
    protected final void expandNodeChildren(@Nonnull final DefaultMutableTreeNode node) {
        final NodeDescriptor userObject = (NodeDescriptor)node.getUserObject();
        if (userObject == null) {
            return;
        }
        Object element = userObject.getElement();
        VirtualFile virtualFile = getFileToRefresh(element);
        super.expandNodeChildren(node);
        if (virtualFile != null) {
            virtualFile.refresh(true, false);
        }
    }

    private static VirtualFile getFileToRefresh(Object element) {
        Object object = element;
        if (element instanceof AbstractTreeNode) {
            object = ((AbstractTreeNode)element).getValue();
        }

        return object instanceof PsiDirectory ? ((PsiDirectory)object).getVirtualFile() : object instanceof PsiFile ? ((PsiFile)object).getVirtualFile() : null;
    }

    @Nonnull
    private static List<AbstractTreeNode> collectChildren(@Nonnull DefaultMutableTreeNode node) {
        int childCount = node.getChildCount();
        List<AbstractTreeNode> result = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            TreeNode childAt = node.getChildAt(i);
            DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)childAt;
            if (defaultMutableTreeNode.getUserObject() instanceof AbstractTreeNode) {
                AbstractTreeNode treeNode = (AbstractTreeNode)defaultMutableTreeNode.getUserObject();
                result.add(treeNode);
            }
            else if (defaultMutableTreeNode.getUserObject() instanceof FavoritesTreeNodeDescriptor) {
                AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)defaultMutableTreeNode.getUserObject()).getElement();
                result.add(treeNode);
            }
        }
        return result;
    }

    /**
     * @deprecated Use {@link #selectAsync}
     */
    @Deprecated
    @Nonnull
    public ActionCallback select(Object element, VirtualFile file, final boolean requestFocus) {
        return Promises.toActionCallback(_select(element, file, requestFocus, Conditions.alwaysTrue()));
    }

    @Nonnull
    public Promise<Object> selectAsync(Object element, VirtualFile file, final boolean requestFocus) {
        return _select(element, file, requestFocus, Conditions.alwaysTrue());
    }

    public ActionCallback selectInWidth(
        final Object element,
        final boolean requestFocus,
        final Condition<AbstractTreeNode> nonStopCondition
    ) {
        return Promises.toActionCallback(_select(element, null, requestFocus, nonStopCondition));
    }

    @Nonnull
    private Promise<Object> _select(
        final Object element,
        final VirtualFile file,
        final boolean requestFocus,
        final Condition<? super AbstractTreeNode> nonStopCondition
    ) {
        AbstractTreeUpdater updater = getUpdater();
        if (updater == null) {
            return Promises.rejectedPromise();
        }

        final AsyncPromise<Object> result = new AsyncPromise<>();
        UiActivityMonitor.getInstance()
            .addActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"), updater.getModalityState());
        batch(indicator -> {
            _select(element, file, requestFocus, nonStopCondition, result, indicator, null, null, false);
            UiActivityMonitor.getInstance().removeActivity(myProject, new UiActivity.AsyncBgOperation("projectViewSelect"));
        });
        return result;
    }

    private void _select(
        final Object element,
        final VirtualFile file,
        final boolean requestFocus,
        final Condition<? super AbstractTreeNode> nonStopCondition,
        final AsyncPromise<Object> result,
        @Nonnull final ProgressIndicator indicator,
        @Nullable final Ref<Object> virtualSelectTarget,
        final FocusRequestor focusRequestor,
        final boolean isSecondAttempt
    ) {
        final AbstractTreeNode alreadySelected = alreadySelectedNode(element);

        final Runnable onDone = () -> {
            JTree tree = getTree();
            if (tree != null && requestFocus && virtualSelectTarget == null && getUi().isReady()) {
                tree.requestFocus();
            }

            result.setResult(null);
        };

        final Condition<AbstractTreeNode> condition =
            abstractTreeNode -> result.getState() == Promise.State.PENDING && nonStopCondition.value(abstractTreeNode);

        if (alreadySelected == null) {
            expandPathTo(
                file,
                (AbstractTreeNode)getTreeStructure().getRootElement(),
                element,
                condition,
                indicator,
                virtualSelectTarget
            ).onSuccess(node -> {
                if (virtualSelectTarget == null) {
                    select(node, onDone);
                }
                else {
                    onDone.run();
                }
            }).onError(error -> {
                if (isSecondAttempt) {
                    result.cancel();
                }
                else {
                    _select(file, file, requestFocus, nonStopCondition, result, indicator, virtualSelectTarget, focusRequestor, true);
                }
            });
        }
        else if (virtualSelectTarget == null) {
            scrollTo(alreadySelected, onDone);
        }
        else {
            onDone.run();
        }
    }

    private AbstractTreeNode alreadySelectedNode(final Object element) {
        final TreePath[] selectionPaths = getTree().getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length == 0) {
            return null;
        }
        for (TreePath selectionPath : selectionPaths) {
            Object selected = selectionPath.getLastPathComponent();
            if (selected instanceof DefaultMutableTreeNode && elementIsEqualTo(selected, element)) {
                Object userObject = ((DefaultMutableTreeNode)selected).getUserObject();
                if (userObject instanceof AbstractTreeNode) {
                    return (AbstractTreeNode)userObject;
                }
            }
        }
        return null;
    }

    private static boolean elementIsEqualTo(final Object node, final Object element) {
        if (node instanceof DefaultMutableTreeNode) {
            final Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
            if (userObject instanceof ProjectViewNode) {
                final AbstractTreeNode projectViewNode = (ProjectViewNode)userObject;
                return projectViewNode.canRepresent(element);
            }
        }
        return false;
    }

    @SuppressWarnings("WeakerAccess")
    protected boolean canExpandPathTo(@Nonnull final AbstractTreeNode root, final Object element) {
        return true;
    }

    @Nonnull
    private Promise<AbstractTreeNode> expandPathTo(
        final VirtualFile file,
        @Nonnull final AbstractTreeNode root,
        final Object element,
        @Nonnull final Condition<AbstractTreeNode> nonStopCondition,
        @Nonnull final ProgressIndicator indicator,
        @Nullable final Ref<Object> target
    ) {
        final AsyncPromise<AbstractTreeNode> async = new AsyncPromise<>();
        if (root.canRepresent(element)) {
            if (target == null) {
                expand(root, () -> async.setResult(root));
            }
            else {
                target.set(root);
                async.setResult(root);
            }
            return async;
        }

        if (!canExpandPathTo(root, element)) {
            async.setError("cannot expand");
            return async;
        }

        if (root instanceof ProjectViewNode && file != null && !((ProjectViewNode)root).contains(file)) {
            async.setError("not applicable");
            return async;
        }

        if (target == null) {
            expand(root, () -> {
                indicator.checkCanceled();

                final DefaultMutableTreeNode rootNode = getNodeForElement(root);
                if (rootNode != null) {
                    final List<AbstractTreeNode> kids = collectChildren(rootNode);
                    expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
                }
                else {
                    async.cancel();
                }
            });
        }
        else {
            if (indicator.isCanceled()) {
                async.cancel();
            }
            else {
                final DefaultMutableTreeNode rootNode = getNodeForElement(root);
                final ArrayList<AbstractTreeNode> kids = new ArrayList<>();
                if (rootNode != null && getTree().isExpanded(new TreePath(rootNode.getPath()))) {
                    kids.addAll(collectChildren(rootNode));
                }
                else {
                    Object[] childElements = getTreeStructure().getChildElements(root);
                    for (Object each : childElements) {
                        kids.add((AbstractTreeNode)each);
                    }
                }

                yieldToEDT(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    expandChild(kids, 0, nonStopCondition, file, element, async, indicator, target);
                });
            }
        }

        return async;
    }

    private void expandChild(
        @Nonnull final List<? extends AbstractTreeNode> kids,
        int i,
        @Nonnull final Condition<AbstractTreeNode> nonStopCondition,
        final VirtualFile file,
        final Object element,
        @Nonnull final AsyncPromise<? super AbstractTreeNode> async,
        @Nonnull final ProgressIndicator indicator,
        final Ref<Object> virtualSelectTarget
    ) {
        while (i < kids.size()) {
            final AbstractTreeNode eachKid = kids.get(i);
            final boolean[] nodeWasCollapsed = {true};
            final DefaultMutableTreeNode nodeForElement = getNodeForElement(eachKid);
            if (nodeForElement != null) {
                nodeWasCollapsed[0] = getTree().isCollapsed(new TreePath(nodeForElement.getPath()));
            }

            if (nonStopCondition.value(eachKid)) {
                final Promise<AbstractTreeNode> result =
                    expandPathTo(file, eachKid, element, nonStopCondition, indicator, virtualSelectTarget);
                result.onSuccess(abstractTreeNode -> {
                    indicator.checkCanceled();
                    async.setResult(abstractTreeNode);
                });

                if (result.getState() == Promise.State.PENDING) {
                    final int next = i + 1;
                    result.onError(error -> {
                        indicator.checkCanceled();

                        if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
                            collapseChildren(eachKid, null);
                        }
                        expandChild(kids, next, nonStopCondition, file, element, async, indicator, virtualSelectTarget);
                    });
                    return;
                }
                else {
                    if (result.getState() == Promise.State.REJECTED) {
                        indicator.checkCanceled();
                        if (nodeWasCollapsed[0] && virtualSelectTarget == null) {
                            collapseChildren(eachKid, null);
                        }
                        i++;
                    }
                    else {
                        return;
                    }
                }
            }
            else {
                //filter tells us to stop here (for instance, in case of module nodes)
                break;
            }
        }
        async.cancel();
    }

    @Override
    protected boolean validateNode(@Nonnull final Object child) {
        if (child == null) {
            return false;
        }
        if (child instanceof ProjectViewNode) {
            final ProjectViewNode projectViewNode = (ProjectViewNode)child;
            return projectViewNode.validate();
        }
        return true;
    }
}
