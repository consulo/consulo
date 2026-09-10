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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Exposes an already built {@link KeymapGroupImpl} tree one level at a time, so nodes are materialized by
 * {@link consulo.ui.ex.awt.tree.StructureTreeModel} on its own thread instead of being converted up front.
 *
 * @author VISTALL
 */
public class KeymapTreeStructure extends AbstractTreeStructure {
    private static final Object[] EMPTY = new Object[0];

    private KeymapTreeElement myRoot;

    public KeymapTreeStructure(KeymapGroupImpl root) {
        setRootGroup(root);
    }

    public void setRootGroup(KeymapGroupImpl root) {
        myRoot = new KeymapTreeElement(null, root, 0);
    }

    @Override
    public Object getRootElement() {
        return myRoot;
    }

    @Override
    public Object[] getChildElements(Object element) {
        if (!(element instanceof KeymapTreeElement treeElement)) {
            return EMPTY;
        }
        KeymapGroupImpl group = treeElement.getGroup();
        if (group == null) {
            return EMPTY;
        }
        List<Object> children = group.getChildren();
        Object[] result = new Object[children.size()];
        for (int i = 0; i < children.size(); i++) {
            result[i] = new KeymapTreeElement(treeElement, children.get(i), i);
        }
        return result;
    }

    @Override
    public @Nullable Object getParentElement(Object element) {
        return element instanceof KeymapTreeElement treeElement ? treeElement.getParent() : null;
    }

    @Override
    public boolean isAlwaysLeaf(Object element) {
        return element instanceof KeymapTreeElement treeElement && !treeElement.isGroup();
    }

    @Override
    public NodeDescriptor createDescriptor(Object element, @Nullable NodeDescriptor parentDescriptor) {
        return new KeymapNodeDescriptor(parentDescriptor, (KeymapTreeElement) element);
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    private static class KeymapNodeDescriptor extends NodeDescriptor<KeymapTreeElement> {
        private final KeymapTreeElement myElement;

        KeymapNodeDescriptor(@Nullable NodeDescriptor parentDescriptor, KeymapTreeElement element) {
            super(parentDescriptor);
            myElement = element;
            myName = buildName(element);
        }

        private static String buildName(KeymapTreeElement element) {
            Object value = element.getValue();
            if (value instanceof KeymapGroupImpl keymapGroup) {
                String name = keymapGroup.getName();
                return name == null ? "" : name;
            }
            return String.valueOf(value);
        }

        @Override
        public boolean update() {
            return false;
        }

        @Override
        public KeymapTreeElement getElement() {
            return myElement;
        }
    }
}
