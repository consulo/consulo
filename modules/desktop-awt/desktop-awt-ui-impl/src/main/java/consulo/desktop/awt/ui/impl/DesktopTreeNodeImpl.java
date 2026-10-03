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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.tree.DesktopStructureTreeModel;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import consulo.ui.impl.TreeNodeSupport;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeNodeImpl<K> implements TreeNode<K> {
    private volatile boolean myLeaf;

    private volatile @Nullable K myValue;
    private final @Nullable DesktopTreeNodeImpl<K> myParent;
    private final DesktopTreeStructure<K> myStructure;

    private @Nullable List<DesktopTreeNodeImpl<K>> myChildren;
    private int myEpoch;
    private int myChildrenEpoch;

    private volatile BiConsumer<K, TextItemPresentation> myRenderer = (e, t) -> t.append(e == null ? "null" : e.toString());

    DesktopTreeNodeImpl(@Nullable K value, @Nullable DesktopTreeNodeImpl<K> parent, DesktopTreeStructure<K> structure) {
        myValue = value;
        myParent = parent;
        myStructure = structure;
    }

    @Nullable
    DesktopTreeNodeImpl<K> getParent() {
        return myParent;
    }

    BiConsumer<K, TextItemPresentation> getRenderer() {
        return myRenderer;
    }

    /**
     * A node stands for a place in the tree, so the same one is handed out for the same place every time -
     * a level built anew on each call would give a caller nodes the tree does not hold, and selecting or
     * opening one of those finds nothing.
     */
    List<DesktopTreeNodeImpl<K>> getChildren() {
        int epoch;
        synchronized (this) {
            List<DesktopTreeNodeImpl<K>> children = myChildren;
            if (children != null && myChildrenEpoch == myEpoch) {
                return children;
            }
            epoch = myEpoch;
        }

        List<DesktopTreeNodeImpl<K>> built = myStructure.buildChildren(this);

        synchronized (this) {
            List<DesktopTreeNodeImpl<K>> current = myChildren;
            if (current != null && epoch < myChildrenEpoch) {
                return current;
            }

            List<DesktopTreeNodeImpl<K>> children = List.copyOf(current == null ? built : reuse(current, built));
            myChildren = children;
            myChildrenEpoch = epoch;
            return children;
        }
    }

    synchronized @Nullable List<DesktopTreeNodeImpl<K>> getKnownChildren() {
        List<DesktopTreeNodeImpl<K>> children = myChildren;
        if (children == null) {
            return myLeaf ? List.of() : null;
        }
        return myChildrenEpoch == myEpoch ? children : null;
    }

    synchronized List<DesktopTreeNodeImpl<K>> getBuiltChildren() {
        List<DesktopTreeNodeImpl<K>> children = myChildren;
        return children == null ? List.of() : children;
    }

    /**
     * A node the model built again for the same value keeps the place the old one had - what
     * {@link DesktopStructureTreeModel} does with its own nodes - so what was open below it stays open. The value
     * itself is the one just built, since a refresh is there to show what changed about it.
     */
    private List<DesktopTreeNodeImpl<K>> reuse(List<DesktopTreeNodeImpl<K>> oldChildren, List<DesktopTreeNodeImpl<K>> newChildren) {
        Map<K, DesktopTreeNodeImpl<K>> byValue = new HashMap<>();
        for (DesktopTreeNodeImpl<K> child : oldChildren) {
            K value = child.myValue;
            if (value != null) {
                byValue.put(value, child);
            }
        }

        List<DesktopTreeNodeImpl<K>> children = new ArrayList<>(newChildren.size());
        for (DesktopTreeNodeImpl<K> child : newChildren) {
            K value = child.myValue;
            DesktopTreeNodeImpl<K> old = value == null ? null : byValue.get(value);
            if (old != null) {
                old.myValue = value;
                old.myLeaf = child.myLeaf;
                old.myRenderer = child.myRenderer;
                children.add(old);
            }
            else {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * The level is marked rather than dropped, so a refresh of a branch nobody opened costs nothing and the
     * nodes which are on screen live until the model asks for them again.
     */
    synchronized void outdateChildren() {
        myEpoch++;

        List<DesktopTreeNodeImpl<K>> children = myChildren;
        if (children == null) {
            return;
        }

        for (DesktopTreeNodeImpl<K> child : children) {
            child.outdateChildren();
        }
    }

    @Override
    public CompletableFuture<TreeNode<K>> findChild(Predicate<K> predicate) {
        return myStructure.loadChildren(this).thenApply(children -> TreeNodeSupport.findFirst(children, predicate));
    }

    @Override
    public CompletableFuture<TreeNode<K>> findChildDeep(Predicate<K> predicate) {
        return myStructure.loadChildren(this).thenCompose(children -> TreeNodeSupport.findDeep(children, predicate));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized List<TreeNode<K>> getLoadedChildren() {
        List<DesktopTreeNodeImpl<K>> children = myChildren;
        return children == null ? List.of() : List.copyOf((List) children);
    }

    @Override
    public void setRenderer(BiConsumer<K, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void setLeaf(boolean leaf) {
        myLeaf = leaf;
    }

    @Override
    public boolean isLeaf() {
        return myLeaf;
    }

    @Override
    public @Nullable K getValue() {
        return myValue;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof DesktopTreeNodeImpl<?> node && Objects.equals(myValue, node.myValue);
    }

    @Override
    public int hashCode() {
        K value = myValue;
        return value == null ? 0 : value.hashCode();
    }
}
