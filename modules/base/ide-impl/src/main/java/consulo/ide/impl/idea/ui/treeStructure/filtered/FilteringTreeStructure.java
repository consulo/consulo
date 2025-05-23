/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.treeStructure.filtered;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.util.concurrent.ActionCallback;
import consulo.ui.ex.awt.speedSearch.ElementFilter;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class FilteringTreeStructure extends AbstractTreeStructure {
    private final ElementFilter<Object> myFilter;
    private final AbstractTreeStructure myBaseStructure;
    protected final FilteringNode myRoot;
    protected final HashSet<FilteringNode> myLeaves = new HashSet<>();
    private final Map<FilteringNode, List<FilteringNode>> myNodesCache = new HashMap<>();

    protected enum State {
        UNKNOWN,
        VISIBLE,
        HIDDEN
    }

    private final Map<Object, FilteringNode> myDescriptors2Nodes = new HashMap<>();

    public FilteringTreeStructure(@Nonnull ElementFilter filter, @Nonnull AbstractTreeStructure originalStructure) {
        this(filter, originalStructure, true);
    }

    public FilteringTreeStructure(@Nonnull ElementFilter filter, @Nonnull AbstractTreeStructure originalStructure, boolean initNow) {
        //noinspection unchecked
        myFilter = filter;
        myBaseStructure = originalStructure;
        myRoot = new FilteringNode(null, myBaseStructure.getRootElement());
        if (initNow) {
            rebuild();
        }
    }

    public void rebuild() {
        myLeaves.clear();
        myNodesCache.clear();
        myDescriptors2Nodes.clear();
        addToCache(myRoot, false);
    }

    private void addToCache(FilteringNode node, boolean duplicate) {
        Object delegate = node.getDelegate();
        Object[] delegates = myBaseStructure.getChildElements(delegate);
        if (delegates == null || delegates.length == 0 || duplicate) {
            myLeaves.add(node);
        }
        else {
            ArrayList<FilteringNode> nodes = new ArrayList<>(delegates.length);
            for (Object d : delegates) {
                boolean isDuplicate = myDescriptors2Nodes.containsKey(d);
                if (!isDuplicate) {
                    FilteringNode n = new FilteringNode(node, d);
                    myDescriptors2Nodes.put(d, n);
                    nodes.add(n);
                }
            }
            myNodesCache.put(node, nodes);
            for (FilteringNode n : nodes) {
                addToCache(n, false);
            }
            if (nodes.isEmpty()) {
                myLeaves.add(node);
            }
        }
    }

    public void refilter() {
        setUnknown(myRoot);
        for (FilteringNode node : myLeaves) {
            State state = getState(node);
            while (node != null && node.state != State.VISIBLE) {
                if (node.state != state) {
                    node.state = state;
                    node = node.getParentNode();
                    if (node != null && state == State.HIDDEN) {
                        state = getState(node);
                    }
                }
                else {
                    break;
                }
            }
        }
    }

    private State getState(@Nonnull FilteringNode node) {
        return myFilter.shouldBeShowing(node.getDelegate()) ? State.VISIBLE : State.HIDDEN;
    }

    private void setUnknown(FilteringNode node) {
        if (node.state == State.UNKNOWN) {
            return;
        }
        node.state = State.UNKNOWN;
        List<FilteringNode> nodes = myNodesCache.get(node);
        if (nodes != null) {
            for (FilteringNode n : nodes) {
                setUnknown(n);
            }
        }
    }

    public FilteringNode getVisibleNodeFor(Object nodeObject) {
        return myDescriptors2Nodes.get(nodeObject);
    }

    @Nonnull
    @Override
    public FilteringNode getRootElement() {
        return myRoot;
    }

    @Nonnull
    @Override
    public Object[] getChildElements(@Nonnull Object element) {
        return ((FilteringNode)element).getChildren();
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        return ((FilteringNode)element).getParent();
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull Object element) {
        return element instanceof FilteringNode && ((FilteringNode)element).isAlwaysLeaf();
    }

    @Override
    public boolean isToBuildChildrenInBackground(@Nonnull Object element) {
        return myBaseStructure.isToBuildChildrenInBackground(element);
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return element instanceof FilteringNode ? (FilteringNode)element : new FilteringNode((SimpleNode)parentDescriptor, element);
    }

    @Override
    public void commit() {
        myBaseStructure.commit();
    }

    @Override
    public boolean hasSomethingToCommit() {
        return myBaseStructure.hasSomethingToCommit();
    }

    @Nonnull
    @Override
    public ActionCallback asyncCommit() {
        return myBaseStructure.asyncCommit();
    }

    public FilteringNode createFilteringNode(Object delegate) {
        return new FilteringNode(null, delegate);
    }

    public class FilteringNode extends SimpleNode {
        private Object myDelegate;
        private State state = State.VISIBLE;

        public FilteringNode(SimpleNode parent, Object delegate) {
            super(parent);
            myDelegate = delegate;
        }

        public void setDelegate(Object delegate) {
            myDelegate = delegate;
        }

        public FilteringNode getParentNode() {
            return (FilteringNode)getParent();
        }

        public Object getDelegate() {
            return myDelegate;
        }

        public List<FilteringNode> children() {
            List<FilteringNode> nodes = myNodesCache.get(this);
            return nodes == null ? Collections.emptyList() : nodes;
        }

        @Override
        public String toString() {
            return String.valueOf(getDelegate());
        }

        @Override
        public boolean isContentHighlighted() {
            return myDelegate instanceof SimpleNode && ((SimpleNode)myDelegate).isContentHighlighted();
        }

        @Override
        public boolean isHighlightableContentNode(@Nonnull final PresentableNodeDescriptor kid) {
            return myDelegate instanceof PresentableNodeDescriptor node && node.isHighlightableContentNode(kid);
        }

        @Override
        @RequiredUIAccess
        protected void doUpdate() {
            clearColoredText();
            if (myDelegate instanceof PresentableNodeDescriptor node) {
                node.update();
                apply(node.getPresentation());
            }
            else if (myDelegate != null) {
                NodeDescriptor descriptor = myBaseStructure.createDescriptor(myDelegate, getParentDescriptor());
                descriptor.update();
                setIcon(descriptor.getIcon());
                setPlainText(myDelegate.toString());
            }
        }

        @Nonnull
        @Override
        public SimpleNode[] getChildren() {
            List<FilteringNode> nodes = myNodesCache.get(this);
            if (nodes == null) {
                return myDelegate instanceof SimpleNode simpleNode
                    ? ContainerUtil.map(simpleNode.getChildren(), node -> new FilteringNode(this, node), NO_CHILDREN)
                    : NO_CHILDREN;
            }

            ArrayList<FilteringNode> result = new ArrayList<>();
            for (FilteringNode node : nodes) {
                if (node.state == State.VISIBLE) {
                    result.add(node);
                }
            }
            return result.toArray(new FilteringNode[0]);
        }

        @Override
        public int getWeight() {
            if (getDelegate() instanceof SimpleNode simpleNode) {
                return simpleNode.getWeight();
            }
            return super.getWeight();
        }

        @Override
        @Nonnull
        public Object[] getEqualityObjects() {
            return new Object[]{myDelegate};
        }

        @Override
        public boolean isAlwaysShowPlus() {
            if (myDelegate instanceof SimpleNode simpleNode) {
                return simpleNode.isAlwaysShowPlus();
            }

            return super.isAlwaysShowPlus();
        }

        @Override
        public boolean isAlwaysLeaf() {
            if (myDelegate instanceof SimpleNode simpleNode) {
                return simpleNode.isAlwaysLeaf();
            }

            return super.isAlwaysLeaf();
        }
    }
}
