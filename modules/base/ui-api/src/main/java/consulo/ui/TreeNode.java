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
package consulo.ui;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2017-09-13
 */
public interface TreeNode<T> {
    void setRenderer(BiConsumer<T, TextItemPresentation> renderer);

    void setLeaf(boolean leaf);

    boolean isLeaf();

    /**
     * @return if rootValue is null and treeNode wraps it
     */
    @Nullable T getValue();

    /**
     * Builds the children when they are not there yet and answers the first one the predicate takes. A tree
     * holds a level at a time, so walking down to a node is a chain of these rather than a search over nodes
     * which do not exist yet.
     */
    default CompletableFuture<TreeNode<T>> findChild(Predicate<T> predicate) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The same over the whole subtree below this node, level by level.
     */
    default CompletableFuture<TreeNode<T>> findChildDeep(Predicate<T> predicate) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * The children built so far, empty when the node was never opened. Unlike {@link #findChild(Predicate)}
     * this loads nothing - a change notification concerns what is on screen, and reacting to one must not
     * build the rest of the tree.
     */
    default List<TreeNode<T>> getLoadedChildren() {
        return List.of();
    }
}
