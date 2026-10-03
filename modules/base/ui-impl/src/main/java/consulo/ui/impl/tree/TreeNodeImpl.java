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
package consulo.ui.impl.tree;

import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
public final class TreeNodeImpl<E> implements TreeNode<E> {
    private final TreeController<E> myController;
    private final @Nullable TreeNodeImpl<E> myParent;

    volatile @Nullable E myValue;
    volatile @Nullable BiConsumer<E, TextItemPresentation> myRenderer;
    volatile @Nullable TextItemPresentation myPresentation;
    volatile long myPresentationStamp;
    volatile List<@Nullable Object> myColumnValues = List.of();
    volatile int myColumnVersion;
    volatile long myColumnStamp;
    volatile int myOrdinal;

    volatile boolean myLeaf;
    volatile boolean myExpanded;
    volatile boolean myPrebuild;
    volatile boolean myRemoved;
    volatile @Nullable TreeNodeImpl<E> myReplacement;

    volatile List<TreeNodeImpl<E>> myChildren = List.of();
    volatile boolean myLoaded;
    @Nullable CompletableFuture<List<TreeNodeImpl<E>>> myChildrenFuture;
    boolean myOutdated;
    int myEpoch;
    boolean myLoading;

    TreeNodeImpl(TreeController<E> controller, @Nullable TreeNodeImpl<E> parent, @Nullable E value) {
        myController = controller;
        myParent = parent;
        myValue = value;
    }

    public @Nullable TreeNodeImpl<E> getParent() {
        return myParent;
    }

    public List<TreeNodeImpl<E>> getChildren() {
        return resolve().myChildren;
    }

    public boolean isLoaded() {
        return resolve().myLoaded;
    }

    public boolean isExpanded() {
        return resolve().myExpanded;
    }

    public boolean isRemoved() {
        return resolve().myRemoved;
    }

    public @Nullable TextItemPresentation getPresentation() {
        return resolve().myPresentation;
    }

    public List<@Nullable Object> getColumnValues() {
        return resolve().myColumnValues;
    }

    public @Nullable Object getColumnValue(int index) {
        List<@Nullable Object> values = resolve().myColumnValues;
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    public int getOrdinal() {
        return resolve().myOrdinal;
    }

    TreeNodeImpl<E> resolve() {
        TreeNodeImpl<E> node = this;
        TreeNodeImpl<E> replacement;
        while ((replacement = node.myReplacement) != null) {
            node = replacement;
        }
        return node;
    }

    boolean belongsTo(TreeController<?> controller) {
        return myController == controller;
    }

    TreeNodeUpdate<E> compute(long stamp,
                              @Nullable TextItemPresentation presentation,
                              @Nullable Function<E, List<@Nullable Object>> columnValueFactory,
                              int columnVersion) {
        E value = myValue;
        if (presentation != null) {
            BiConsumer<E, TextItemPresentation> renderer = myRenderer;
            if (renderer == null) {
                presentation.append(String.valueOf(value));
            }
            else {
                renderer.accept(value, presentation);
            }
        }

        List<@Nullable Object> columnValues = columnValueFactory == null || value == null ? List.of() : columnValueFactory.apply(value);
        return new TreeNodeUpdate<>(this, value, stamp, presentation, columnValues, columnVersion);
    }

    boolean apply(TreeNodeUpdate<E> update) {
        if (myRemoved || myValue != update.value()) {
            return false;
        }

        boolean applied = false;
        TextItemPresentation presentation = update.presentation();
        if (presentation != null && update.stamp() > myPresentationStamp) {
            myPresentation = presentation;
            myPresentationStamp = update.stamp();
            applied = true;
        }

        if (update.stamp() > myColumnStamp) {
            myColumnValues = update.columnValues();
            myColumnVersion = update.columnVersion();
            myColumnStamp = update.stamp();
            applied = true;
        }
        return applied;
    }

    void takeOver(TreeNodeImpl<E> fresh) {
        boolean sameValue = myValue == fresh.myValue;
        myValue = fresh.myValue;
        myRenderer = fresh.myRenderer;

        if (!sameValue || fresh.myPresentationStamp > myPresentationStamp) {
            myPresentation = fresh.myPresentation;
            myPresentationStamp = fresh.myPresentationStamp;
        }

        if (!sameValue || fresh.myColumnStamp > myColumnStamp) {
            myColumnValues = fresh.myColumnValues;
            myColumnVersion = fresh.myColumnVersion;
            myColumnStamp = fresh.myColumnStamp;
        }

        myOrdinal = fresh.myOrdinal;
        myPrebuild = fresh.myPrebuild;

        fresh.myReplacement = this;
    }

    @Override
    public void setRenderer(BiConsumer<E, TextItemPresentation> renderer) {
        resolve().myRenderer = renderer;
    }

    @Override
    public void setLeaf(boolean leaf) {
        resolve().myLeaf = leaf;
    }

    @Override
    public boolean isLeaf() {
        return resolve().myLeaf;
    }

    @Override
    public @Nullable E getValue() {
        return resolve().myValue;
    }

    @Override
    public List<TreeNode<E>> getLoadedChildren() {
        return List.copyOf(resolve().myChildren);
    }

    @Override
    public CompletableFuture<TreeNode<E>> findChild(Predicate<E> predicate) {
        return myController.findChild(resolve(), predicate);
    }

    @Override
    public CompletableFuture<TreeNode<E>> findChildDeep(Predicate<E> predicate) {
        return myController.findChildDeep(resolve(), predicate);
    }

    @Override
    public String toString() {
        return "TreeNodeImpl{value=" + myValue + "}";
    }
}
