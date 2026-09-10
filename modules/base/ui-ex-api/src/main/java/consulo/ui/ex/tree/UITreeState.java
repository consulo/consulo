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
package consulo.ui.ex.tree;

import com.dslplatform.json.CompiledJson;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringHash;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The counterpart of {@code TreeState} for {@link Tree}, written in the same format so that the two frontends
 * read the same workspace entry. The tree fetches its children as they are opened, so restoring is a walk that
 * opens one level at a time rather than a pass over nodes already there.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@CompiledJson
public class UITreeState {
    @Tag("expand")
    @AbstractCollection(surroundWithTag = false)
    public List<UITreePath> expandedPaths = new ArrayList<>();

    @Tag("select")
    @AbstractCollection(surroundWithTag = false)
    public List<UITreePath> selectedPaths = new ArrayList<>();

    public boolean isEmpty() {
        return expandedPaths.isEmpty() && selectedPaths.isEmpty();
    }

    public static <E> UITreeState createOn(Tree<E> tree) {
        return createOn(tree, true, true);
    }

    public static <E> UITreeState createOn(Tree<E> tree, boolean persistExpand, boolean persistSelect) {
        UITreeState state = new UITreeState();

        if (persistExpand) {
            for (List<TreeNode<E>> path : tree.getExpandedPaths()) {
                if (!path.isEmpty()) {
                    state.expandedPaths.add(createPath(path));
                }
            }
        }

        if (persistSelect) {
            List<TreeNode<E>> path = tree.getSelectedPath();
            if (!path.isEmpty()) {
                state.selectedPaths.add(createPath(path));
            }
        }

        return state;
    }

    private static <E> UITreePath createPath(List<TreeNode<E>> path) {
        UITreePath result = new UITreePath();
        for (TreeNode<E> node : path) {
            Object value = node.getValue();

            UITreePathElement element = new UITreePathElement();
            element.id = calcId(value);
            element.type = calcType(value);
            element.userStr = value instanceof String s ? s : null;
            result.items.add(element);
        }
        return result;
    }

    /**
     * Kept in step with {@code TreeState} - a node which knows its own id says so, and everything else is
     * named by {@code toString()}, which must not resolve the node value.
     */
    private static String calcId(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof PathElementIdProvider provider) {
            return provider.getPathElementId();
        }
        return StringUtil.notNullize(value.toString());
    }

    private static String calcType(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof PathElementIdProvider provider) {
            String type = provider.getPathElementType();
            if (type != null) {
                return type;
            }
        }
        String name = value.getClass().getName();
        return Integer.toHexString(StringHash.murmur(name, 31)) + ":" + StringUtil.getShortName(name);
    }

    private static boolean isMatchTo(UITreePathElement element, @Nullable Object value) {
        return Comparing.equal(element.id, calcId(value)) && Comparing.equal(element.type, calcType(value));
    }

    /**
     * @return a future completed once every path has been walked, so that a caller which writes the state back
     * can wait for the tree to carry it rather than snapshot a tree still being opened
     */
    public <E> CompletableFuture<?> applyTo(Tree<E> tree) {
        TreeNode<E> root = tree.getRootNode();
        if (root == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> walks = new ArrayList<>();

        for (UITreePath path : expandedPaths) {
            if (startsAtRoot(path, root)) {
                walks.add(walk(tree, root, path.items, 1, true, node -> {
                }));
            }
        }

        for (UITreePath path : selectedPaths) {
            if (startsAtRoot(path, root) && path.items.size() > 1) {
                walks.add(walk(tree, root, path.items, 1, false, tree::select));
            }
        }

        return CompletableFuture.allOf(walks.toArray(CompletableFuture[]::new));
    }

    /**
     * The first element stands for the root the tree was built on, so a path written for another tree is left
     * alone rather than walked against nodes it never described.
     */
    private static <E> boolean startsAtRoot(UITreePath path, TreeNode<E> root) {
        return !path.items.isEmpty() && isMatchTo(path.items.get(0), root.getValue());
    }

    /**
     * @param expandTarget the last node of the path is opened as well - what an expanded path means, while a
     *                     selected one only needs its parents open
     */
    private static <E> CompletableFuture<Void> walk(
        Tree<E> tree,
        TreeNode<E> node,
        List<UITreePathElement> path,
        int index,
        boolean expandTarget,
        Consumer<TreeNode<E>> onTarget
    ) {
        if (index >= path.size()) {
            onTarget.accept(node);
            return CompletableFuture.completedFuture(null);
        }

        UITreePathElement pathElement = path.get(index);

        // the node builds the level below it, so a path is walked one step at a time rather than searched
        return node.findChild(value -> isMatchTo(pathElement, value)).thenCompose(match -> {
            if (match == null) {
                return CompletableFuture.completedFuture(null);
            }

            if (index == path.size() - 1 && !expandTarget) {
                onTarget.accept(match);
                return CompletableFuture.completedFuture(null);
            }

            return tree.expand(match).thenCompose(ignored -> walk(tree, match, path, index + 1, expandTarget, onTarget));
        });
    }
}
