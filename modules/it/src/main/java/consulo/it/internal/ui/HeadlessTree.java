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
package consulo.it.internal.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.internal.HeadlessUIAccess;
import consulo.ui.Length;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import consulo.ui.UIAccess;
import consulo.ui.impl.tree.TreeController;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 */
public class HeadlessTree<E> extends HeadlessComponentBase implements Tree<E> {
    private final Disposable myDestroyHook = Disposable.newDisposable("Tree");

    private final TreeController<E> myController;

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;
    private @Nullable Function<TreeNode<E>, String> mySpeedSearchConverter;

    private volatile boolean myAttached = true;

    public HeadlessTree(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        myController = new TreeController<>(this, rootValue, model, executor, new HeadlessTreeWidget<>(this));
        Disposer.register(myDestroyHook, myController);
    }

    public TreeController<E> getController() {
        return myController;
    }

    public void setAttached(boolean attached) {
        myAttached = attached;
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        return myAttached ? HeadlessUIAccess.INSTANCE : null;
    }

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        return myController.getSelected();
    }

    @Override
    public CompletableFuture<?> expand(TreeNode<E> node, int depth) {
        return myController.expand(node, depth);
    }

    @Override
    public boolean isExpandCollapseAllSupported() {
        return true;
    }

    @Override
    public CompletableFuture<?> expandAll() {
        return myController.expandAll();
    }

    @Override
    public CompletableFuture<?> expandAll(int depth) {
        return myController.expandAll(depth);
    }

    @Override
    public CompletableFuture<?> collapseAll() {
        return myController.collapseAll();
    }

    @Override
    public TreeNode<E> getRootNode() {
        return myController.getRoot();
    }

    @Override
    public List<List<TreeNode<E>>> getExpandedPaths() {
        return myController.getExpandedPaths();
    }

    @Override
    public List<TreeNode<E>> getSelectedPath() {
        return myController.getSelectedPath();
    }

    @Override
    public void select(TreeNode<E> node) {
        myController.select(node);
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
        myController.refreshItem(node, refreshChildren);
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return myController.refreshAll();
    }

    @Override
    public Disposable destroyHook() {
        return myDestroyHook;
    }

    @Override
    public void addStyle(TreeStyle style) {
    }

    @Override
    public void setItemHeightGetter(@Nullable Function<TreeNode<E>, Length> getter) {
    }

    @Override
    public void setSpeedSearchConverter(@Nullable Function<TreeNode<E>, String> converter) {
        mySpeedSearchConverter = converter;
    }

    public @Nullable Function<TreeNode<E>, String> getSpeedSearchConverter() {
        return mySpeedSearchConverter;
    }

    @Override
    public @Nullable String getSpeedSearchText() {
        return null;
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<TreeNode<E>> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<TreeNode<E>> getTransferHandler() {
        return myTransferHandler;
    }
}
