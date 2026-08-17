/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.ui.impl.internal;

import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2017-09-09
 */
public class WebTreeNodeImpl<N> implements TreeNode<N> {
    public static class NotLoaded<K> extends WebTreeNodeImpl<K> {
        public NotLoaded(
            @Nullable WebTreeNodeImpl<K> parent,
            @Nullable K node,
            Map<String, WebTreeNodeImpl<K>> stringWebTreeNodeMap
        ) {
            super(parent, node, stringWebTreeNodeMap);

            setLeaf(true);
        }
    }

    private final WebTreeNodeImpl<N> myParent;
    private final String myId;

    private N myNode;

    private List<WebTreeNodeImpl<N>> myChildren = List.of();
    private BiConsumer<N, TextItemPresentation> myRenderer =
        (n, itemPresentation) -> itemPresentation.append(String.valueOf(n));
    private boolean myLeaf;

    /**
     * The grid keeps the open nodes of the ui it belongs to, and a refresh builds a new one - the nodes outlive
     * it, so what was open is remembered here and put back once the new grid has the data.
     */
    private boolean myExpanded;

    private Function<WebTreeNodeImpl<N>, CompletableFuture<List<WebTreeNodeImpl<N>>>> myLoader;

    public WebTreeNodeImpl(@Nullable WebTreeNodeImpl<N> parent, @Nullable N node, Map<String, WebTreeNodeImpl<N>> nodeMap) {
        myParent = parent;
        myNode = node;
        myId = parent == null ? "root" : UUID.randomUUID().toString();

        if (!(this instanceof NotLoaded)) {
            myChildren = List.of(new NotLoaded<>(this, null, nodeMap));
        }

        nodeMap.put(getId(), this);
    }

    public boolean isNotLoaded() {
        return myChildren.size() == 1 && myChildren.get(0) instanceof NotLoaded;
    }

    public @Nullable WebTreeNodeImpl<N> getParent() {
        return myParent;
    }

    @Override
    public @Nullable N getValue() {
        return myNode;
    }

    public String getId() {
        return myId;
    }

    public List<WebTreeNodeImpl<N>> getChildren() {
        return myChildren;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<TreeNode<N>> getLoadedChildren() {
        // the placeholder stands for a level which is not built yet - answering it would hand the caller a
        // node with no value where there are none
        return isNotLoaded() ? List.of() : (List<TreeNode<N>>) (List) myChildren;
    }

    public void setChildren(List<WebTreeNodeImpl<N>> children) {
        myChildren = children;
    }

    @Override
    public void setRenderer(BiConsumer<N, TextItemPresentation> renderer) {
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

    public BiConsumer<N, TextItemPresentation> getRenderer() {
        return myRenderer;
    }

    /**
     * Asks the tree for the children of this node, the way opening it would. Blocking - the model is read here -
     * so it belongs off the ui thread.
     */
    public void setLoader(Function<WebTreeNodeImpl<N>, CompletableFuture<List<WebTreeNodeImpl<N>>>> loader) {
        myLoader = loader;
    }

    @Override
    public CompletableFuture<TreeNode<N>> findChild(Predicate<N> predicate) {
        return loadChildren().thenApply(children -> {
            for (WebTreeNodeImpl<N> child : children) {
                if (predicate.test(child.getValue())) {
                    return child;
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<TreeNode<N>> findChildDeep(Predicate<N> predicate) {
        return loadChildren().thenCompose(children -> findDeep(children, predicate, 0));
    }

    private CompletableFuture<TreeNode<N>> findDeep(List<WebTreeNodeImpl<N>> children, Predicate<N> predicate, int index) {
        if (index >= children.size()) {
            return CompletableFuture.completedFuture(null);
        }

        WebTreeNodeImpl<N> child = children.get(index);
        if (predicate.test(child.getValue())) {
            return CompletableFuture.completedFuture(child);
        }

        return child.findChildDeep(predicate)
            .thenCompose(found -> found != null
                ? CompletableFuture.completedFuture(found)
                : findDeep(children, predicate, index + 1));
    }

    private CompletableFuture<List<WebTreeNodeImpl<N>>> loadChildren() {
        if (isNotLoaded()) {
            // the placeholder stands for a level which is not built yet and carries no value of its own, so a
            // caller searching the children must never be handed it as if it were one
            return myLoader == null ? CompletableFuture.completedFuture(List.of()) : myLoader.apply(this);
        }
        return CompletableFuture.completedFuture(myChildren);
    }

    public boolean isExpanded() {
        return myExpanded;
    }

    public void setExpanded(boolean expanded) {
        myExpanded = expanded;
    }

    public int getLevel() {
        // we start with -1, due we already has root node
        int level = -1;

        WebTreeNodeImpl parent = this;
        while ((parent = parent.getParent()) != null) {
            level++;
        }
        return level;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WebTreeNodeImpl{");
        sb.append("myNode=").append(myNode);
        sb.append('}');
        return sb.toString();
    }
}
