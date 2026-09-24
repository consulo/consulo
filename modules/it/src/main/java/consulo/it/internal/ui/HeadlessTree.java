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
import consulo.ui.Length;
import consulo.ui.TransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.TreeStyle;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Dummy-but-creatable headless {@link Tree}. The model is never asked for a level, so the tree holds no node
 * and every query is answered by the empty defaults of the interface.
 *
 * @author VISTALL
 */
public class HeadlessTree<E> extends HeadlessComponentBase implements Tree<E> {
    private final Disposable myDestroyHook = Disposable.newDisposable();

    private @Nullable TransferHandler<TreeNode<E>> myTransferHandler;

    @Override
    public @Nullable TreeNode<E> getSelectedNode() {
        return null;
    }

    @Override
    public void refreshItem(TreeNode<E> node, boolean refreshChildren) {
    }

    @Override
    public CompletableFuture<?> refreshAll() {
        return CompletableFuture.completedFuture(null);
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
