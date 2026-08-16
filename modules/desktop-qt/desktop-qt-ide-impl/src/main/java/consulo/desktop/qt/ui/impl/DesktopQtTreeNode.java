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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import consulo.ui.image.Image;
import io.qt.widgets.QTreeWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTreeNode<E> implements TreeNode<E> {
    private BiConsumer<E, TextItemPresentation> myRenderer;

    private final @Nullable DesktopQtTreeNode<E> myParent;

    private final E myValue;

    private @Nullable QTreeWidgetItem myTreeItem;

    private boolean myLeaf;

    /**
     * The children built so far and the build which produced them. The future is what a walk down a path chains
     * on - the level is fetched off the ui thread, so a node which is asked for its children while a previous
     * ask is still running has to be handed the very same build rather than starting a second one.
     */
    private volatile List<DesktopQtTreeNode<E>> myChildren = List.of();
    private volatile @Nullable CompletableFuture<List<DesktopQtTreeNode<E>>> myChildrenFuture;

    private @Nullable Function<DesktopQtTreeNode<E>, CompletableFuture<List<DesktopQtTreeNode<E>>>> myLoader;

    /**
     * Whether the row of this node is open. Kept here rather than read off the widget, since the state of the
     * tree is written out from whichever thread the component store runs on.
     */
    private volatile boolean myExpanded;

    public DesktopQtTreeNode(@Nullable DesktopQtTreeNode<E> parent, E value) {
        myParent = parent;
        myValue = value;
    }

    public @Nullable DesktopQtTreeNode<E> getParent() {
        return myParent;
    }

    public void setTreeItem(QTreeWidgetItem treeItem) {
        myTreeItem = treeItem;
    }

    public @Nullable QTreeWidgetItem getTreeItem() {
        return myTreeItem;
    }

    public boolean isExpanded() {
        return myExpanded;
    }

    public void setExpanded(boolean expanded) {
        myExpanded = expanded;
    }

    public void setLoader(Function<DesktopQtTreeNode<E>, CompletableFuture<List<DesktopQtTreeNode<E>>>> loader) {
        myLoader = loader;
    }

    public List<DesktopQtTreeNode<E>> getChildren() {
        return myChildren;
    }

    public void setChildren(List<DesktopQtTreeNode<E>> children) {
        myChildren = List.copyOf(children);
    }

    /**
     * Builds the level below this node unless it was built already.
     */
    public synchronized CompletableFuture<List<DesktopQtTreeNode<E>>> loadChildren() {
        if (myLeaf) {
            return CompletableFuture.completedFuture(List.of());
        }

        CompletableFuture<List<DesktopQtTreeNode<E>>> future = myChildrenFuture;
        if (future != null) {
            return future;
        }

        Function<DesktopQtTreeNode<E>, CompletableFuture<List<DesktopQtTreeNode<E>>>> loader = myLoader;
        if (loader == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        future = loader.apply(this);
        myChildrenFuture = future;
        return future;
    }

    /**
     * Throws away what was built, so that the next ask fetches the level again. The leaf mark goes with it - a
     * node is marked one when a build came back empty, and a folder which has since been filled would otherwise
     * never be asked again.
     */
    public synchronized void resetChildren() {
        myChildrenFuture = null;
        myChildren = List.of();
        myLeaf = false;
    }

    public void render() {
        if (myTreeItem == null || myTreeItem.isDisposed()) {
            return;
        }

        if (myRenderer == null) {
            myRenderer = (e, presentation) -> {
                if (e == null) {
                    presentation.append("");
                }
                else {
                    presentation.append(e.toString());
                }
            };
        }

        DesktopQtTextItemPresentation presentation = new DesktopQtTextItemPresentation();
        myRenderer.accept(myValue, presentation);
        myTreeItem.setText(0, presentation.toString());

        Image uiImage = presentation.getImage();
        if (uiImage != null) {
            myTreeItem.setIcon(0, DesktopQtImage.toQIcon(uiImage));
        }
    }

    @Override
    public void setRenderer(BiConsumer<E, TextItemPresentation> renderer) {
        myRenderer = renderer;
    }

    @Override
    public void setLeaf(boolean leaf) {
        myLeaf = leaf;

        if (leaf) {
            myChildren = List.of();
        }
    }

    @Override
    public boolean isLeaf() {
        return myLeaf;
    }

    @Override
    public @Nullable E getValue() {
        return myValue;
    }

    @Override
    public List<TreeNode<E>> getLoadedChildren() {
        return List.copyOf(myChildren);
    }

    @Override
    public CompletableFuture<TreeNode<E>> findChild(Predicate<E> predicate) {
        return loadChildren().thenApply(children -> {
            for (DesktopQtTreeNode<E> child : children) {
                if (predicate.test(child.getValue())) {
                    return child;
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<TreeNode<E>> findChildDeep(Predicate<E> predicate) {
        return loadChildren().thenCompose(children -> findDeep(children, predicate, 0));
    }

    private CompletableFuture<TreeNode<E>> findDeep(List<DesktopQtTreeNode<E>> children, Predicate<E> predicate, int index) {
        if (index >= children.size()) {
            return CompletableFuture.completedFuture(null);
        }

        DesktopQtTreeNode<E> child = children.get(index);
        if (predicate.test(child.getValue())) {
            return CompletableFuture.completedFuture(child);
        }

        return child.findChildDeep(predicate)
            .thenCompose(found -> found != null
                ? CompletableFuture.completedFuture(found)
                : findDeep(children, predicate, index + 1));
    }

    @Override
    public String toString() {
        return "DesktopQtTreeNode{myValue=" + myValue + "}";
    }
}
