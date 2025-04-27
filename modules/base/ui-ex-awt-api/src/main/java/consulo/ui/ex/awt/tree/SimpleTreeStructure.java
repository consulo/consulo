/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.tree;

import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import jakarta.annotation.Nonnull;

public abstract class SimpleTreeStructure extends AbstractTreeStructure {
    @Nonnull
    @Override
    public Object[] getChildElements(@Nonnull Object element) {
        return ((SimpleNode)element).getChildren();
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        return ((SimpleNode)element).getParent();
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull Object element) {
        return ((SimpleNode)element).isAlwaysLeaf();
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return (NodeDescriptor)element;
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    public final void clearCaches() {
        cleanUpCaches((SimpleNode)getRootElement());
    }

    private void cleanUpCaches(SimpleNode node) {
        if (!(node instanceof CachingSimpleNode)) {
            return;
        }

        final CachingSimpleNode cachingNode = ((CachingSimpleNode)node);
        if (cachingNode.getCached() == null) {
            return;
        }

        for (SimpleNode eachChild : cachingNode.myChildren) {
            cleanUpCaches(eachChild);
        }

        cachingNode.cleanUpCache();
    }

    public static class Impl extends SimpleTreeStructure {
        private final SimpleNode myRoot;

        public Impl(SimpleNode root) {
            myRoot = root;
        }

        @Nonnull
        @Override
        public Object getRootElement() {
            return myRoot;
        }
    }
}
