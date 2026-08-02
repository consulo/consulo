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

import consulo.logging.Logger;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringHash;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The counterpart of {@code TreeState} for {@link Tree}, written in the same format so that the two frontends
 * read the same workspace entry. The tree fetches its children as they are opened, so restoring is a walk that
 * opens one level at a time rather than a pass over nodes already there.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class UITreeState implements JDOMExternalizable {
    private static final Logger LOG = Logger.getInstance(UITreeState.class);

    private static final String EXPAND_TAG = "expand";
    private static final String SELECT_TAG = "select";
    private static final String PATH_TAG = "path";

    private enum Match {
        OBJECT,
        ID_TYPE
    }

    @Tag("item")
    public static class PathElement {
        @Attribute("name")
        public String id;
        @Attribute("type")
        public String type;
        @Attribute("user")
        public String userStr;

        Object userObject;
        int index;

        @SuppressWarnings("unused")
        public PathElement() {
            this(null, null, -1, null);
        }

        public PathElement(String itemId, String itemType, int itemIndex, @Nullable Object userObject) {
            id = itemId;
            type = itemType;
            index = itemIndex;
            userStr = userObject instanceof String s ? s : null;
            this.userObject = userObject;
        }

        private @Nullable Match getMatchTo(@Nullable Object value) {
            if (userObject != null && userObject.equals(value)) {
                return Match.OBJECT;
            }
            return Comparing.equal(id, calcId(value)) && Comparing.equal(type, calcType(value)) ? Match.ID_TYPE : null;
        }

        @Override
        public String toString() {
            return id + ": " + type;
        }
    }

    private final List<List<PathElement>> myExpandedPaths;
    private final List<List<PathElement>> mySelectedPaths;

    // xml deserialization
    @SuppressWarnings("unused")
    public UITreeState() {
        this(new SmartList<>(), new SmartList<>());
    }

    private UITreeState(List<List<PathElement>> expandedPaths, List<List<PathElement>> selectedPaths) {
        myExpandedPaths = expandedPaths;
        mySelectedPaths = selectedPaths;
    }

    public boolean isEmpty() {
        return myExpandedPaths.isEmpty() && mySelectedPaths.isEmpty();
    }

    public static <E> UITreeState createOn(Tree<E> tree) {
        return createOn(tree, true, true);
    }

    public static <E> UITreeState createOn(Tree<E> tree, boolean persistExpand, boolean persistSelect) {
        List<List<PathElement>> expanded = new SmartList<>();
        if (persistExpand) {
            for (List<TreeNode<E>> path : tree.getExpandedPaths()) {
                if (!path.isEmpty()) {
                    expanded.add(createPath(path));
                }
            }
        }

        List<List<PathElement>> selected = new SmartList<>();
        if (persistSelect) {
            List<TreeNode<E>> path = tree.getSelectedPath();
            if (!path.isEmpty()) {
                selected.add(createPath(path));
            }
        }

        return new UITreeState(expanded, selected);
    }

    public static UITreeState createFrom(@Nullable Element element) {
        UITreeState state = new UITreeState();
        try {
            if (element != null) {
                state.readExternal(element);
            }
        }
        catch (InvalidDataException e) {
            LOG.warn(e);
        }
        return state;
    }

    private static <E> List<PathElement> createPath(List<TreeNode<E>> path) {
        List<PathElement> result = new ArrayList<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            Object value = path.get(i).getValue();
            result.add(new PathElement(calcId(value), calcType(value), i, value));
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

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        readExternal(element, myExpandedPaths, EXPAND_TAG);
        readExternal(element, mySelectedPaths, SELECT_TAG);
    }

    private static void readExternal(Element root, List<? super List<PathElement>> list, String name) {
        list.clear();
        for (Element element : root.getChildren(name)) {
            for (Element child : element.getChildren(PATH_TAG)) {
                PathElement[] path = XmlSerializer.deserialize(child, PathElement[].class);
                list.add(List.of(path));
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        writeExternal(element, myExpandedPaths, EXPAND_TAG);
        writeExternal(element, mySelectedPaths, SELECT_TAG);
    }

    private static void writeExternal(Element element, List<? extends List<PathElement>> list, String name) {
        Element root = new Element(name);
        for (List<PathElement> path : list) {
            Element e = XmlSerializer.serialize(path.toArray());
            e.setName(PATH_TAG);
            root.addContent(e);
        }
        element.addContent(root);
    }

    public <E> void applyTo(Tree<E> tree) {
        TreeNode<E> root = tree.getRootNode();
        if (root == null) {
            return;
        }

        for (List<PathElement> path : myExpandedPaths) {
            if (startsAtRoot(path, root)) {
                walk(tree, root, path, 1, true, node -> {
                });
            }
        }

        for (List<PathElement> path : mySelectedPaths) {
            if (startsAtRoot(path, root) && path.size() > 1) {
                walk(tree, root, path, 1, false, tree::select);
            }
        }
    }

    /**
     * The first element stands for the root the tree was built on, so a path written for another tree is left
     * alone rather than walked against nodes it never described.
     */
    private static <E> boolean startsAtRoot(List<PathElement> path, TreeNode<E> root) {
        return !path.isEmpty() && path.get(0).getMatchTo(root.getValue()) != null;
    }

    /**
     * @param expandTarget the last node of the path is opened as well - what an expanded path means, while a
     *                     selected one only needs its parents open
     */
    private static <E> void walk(
        Tree<E> tree,
        TreeNode<E> node,
        List<PathElement> path,
        int index,
        boolean expandTarget,
        Consumer<TreeNode<E>> onTarget
    ) {
        if (index >= path.size()) {
            onTarget.accept(node);
            return;
        }

        PathElement pathElement = path.get(index);

        // the node builds the level below it, so a path is walked one step at a time rather than searched
        node.findChild(value -> pathElement.getMatchTo(value) != null).thenAccept(match -> {
            if (match == null) {
                return;
            }

            if (index == path.size() - 1 && !expandTarget) {
                onTarget.accept(match);
                return;
            }

            tree.expandAsync(match).thenRun(() -> walk(tree, match, path, index + 1, expandTarget, onTarget));
        });
    }

}
