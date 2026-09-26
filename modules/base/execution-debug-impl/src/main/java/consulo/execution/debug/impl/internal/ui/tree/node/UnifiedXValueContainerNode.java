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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.frame.*;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A node whose children come from a {@link XValueContainer} - the counterpart of the swing {@link XValueContainerNode}.
 * The swing node shows each child the moment the debug process hands it over; a level of the unified tree is built at
 * once, so {@link #loadChildren()} waits for the children, and for their presentations, on a background thread of the
 * tree.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public abstract class UnifiedXValueContainerNode<ValueContainer extends XValueContainer> extends UnifiedXDebuggerTreeNode implements XCompositeNode {
    /**
     * How long a level waits for the debug process to hand over its children.
     */
    private static final long CHILDREN_TIMEOUT_MS = 10_000;
    /**
     * How long the children of a level wait for their presentations before the level is shown anyway.
     */
    private static final long PRESENTATION_TIMEOUT_MS = 3_000;

    protected final ValueContainer myValueContainer;

    private volatile boolean myObsolete;
    private boolean myAlreadySorted;

    private @Nullable List<UnifiedMessageTreeNode> myMessageChildren;
    private @Nullable List<UnifiedXValueGroupNodeImpl> myTopGroups;
    private @Nullable List<UnifiedXValueNodeImpl> myValueChildren;
    private @Nullable List<UnifiedXValueGroupNodeImpl> myBottomGroups;
    private @Nullable List<UnifiedMessageTreeNode> myTemporaryMessageChildren;

    private CountDownLatch myChildrenLoaded = new CountDownLatch(0);

    protected UnifiedXValueContainerNode(UnifiedXDebuggerTree tree, @Nullable UnifiedXDebuggerTreeNode parent, ValueContainer valueContainer) {
        super(tree, parent, true);
        myValueContainer = valueContainer;
    }

    public ValueContainer getValueContainer() {
        return myValueContainer;
    }

    /**
     * Asks the debug process for the children and waits for them. Called on a background thread of the tree.
     */
    public List<UnifiedXDebuggerTreeNode> loadChildren() {
        CountDownLatch loaded;
        synchronized (this) {
            // the info message belongs to the node rather than to a computation of its children
            myTopGroups = null;
            myValueChildren = null;
            myBottomGroups = null;
            myTemporaryMessageChildren = null;
            loaded = myChildrenLoaded = new CountDownLatch(1);
        }

        startComputingChildren();

        try {
            if (!loaded.await(CHILDREN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                synchronized (this) {
                    myTemporaryMessageChildren = List.of(UnifiedMessageTreeNode.createLoadingMessage(myTree, this));
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<UnifiedXDebuggerTreeNode> children = getChildren();

        long deadline = System.currentTimeMillis() + PRESENTATION_TIMEOUT_MS;
        for (UnifiedXDebuggerTreeNode child : children) {
            if (child instanceof UnifiedXValueNodeImpl valueNode) {
                valueNode.awaitPresentation(deadline);
            }
        }
        return children;
    }

    public void startComputingChildren() {
        myValueContainer.computeChildren(this);
    }

    @Override
    public void setAlreadySorted(boolean alreadySorted) {
        myAlreadySorted = alreadySorted;
    }

    @Override
    public synchronized void addChildren(XValueChildrenList children, boolean last) {
        if (myObsolete) {
            myChildrenLoaded.countDown();
            return;
        }

        if (myValueChildren == null) {
            myValueChildren = new ArrayList<>(children.size());
        }
        for (int i = 0; i < children.size(); i++) {
            myValueChildren.add(new UnifiedXValueNodeImpl(myTree, this, LocalizeValue.of(children.getName(i)), children.getValue(i)));
        }
        if (!myAlreadySorted && XDebuggerSettingsManager.getInstance().getDataViewSettings().isSortValues()) {
            myValueChildren.sort(Comparator.comparing(UnifiedXValueNodeImpl::getName));
        }

        myTopGroups = createGroupNodes(children.getTopGroups(), myTopGroups);
        myBottomGroups = createGroupNodes(children.getBottomGroups(), myBottomGroups);

        if (last) {
            myChildrenLoaded.countDown();
        }
    }

    private @Nullable List<UnifiedXValueGroupNodeImpl> createGroupNodes(List<XValueGroup> groups, @Nullable List<UnifiedXValueGroupNodeImpl> prevNodes) {
        if (groups.isEmpty()) {
            return prevNodes;
        }

        List<UnifiedXValueGroupNodeImpl> nodes = prevNodes != null ? prevNodes : new ArrayList<>();
        for (XValueGroup group : groups) {
            nodes.add(new UnifiedXValueGroupNodeImpl(myTree, this, group));
        }
        return nodes;
    }

    @Override
    public synchronized void tooManyChildren(int remaining) {
        myTemporaryMessageChildren = List.of(UnifiedMessageTreeNode.createEllipsisNode(myTree, this, remaining));
        myChildrenLoaded.countDown();
    }

    @Override
    public boolean isObsolete() {
        return myObsolete;
    }

    @Override
    public void setErrorMessage(LocalizeValue errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
        setMessage(errorMessage, XDebuggerUIConstants.ERROR_MESSAGE_ICON, XDebuggerUIConstants.ERROR_MESSAGE_ATTRIBUTES, link);
    }

    @Override
    public synchronized void setMessage(
        LocalizeValue message,
        @Nullable Image icon,
        SimpleTextAttributes attributes,
        @Nullable XDebuggerTreeNodeHyperlink link
    ) {
        myTemporaryMessageChildren = List.of(UnifiedMessageTreeNode.createMessageNode(myTree, this, message, icon, attributes));
        myChildrenLoaded.countDown();
    }

    @RequiredUIAccess
    public void setInfoMessage(LocalizeValue message) {
        synchronized (this) {
            myMessageChildren = List.of(UnifiedMessageTreeNode.createInfoMessage(myTree, this, message));
        }
        myTree.nodeStructureChanged(this);
    }

    public synchronized List<UnifiedXDebuggerTreeNode> getChildren() {
        List<UnifiedXDebuggerTreeNode> children = new ArrayList<>();
        if (myMessageChildren != null) {
            children.addAll(myMessageChildren);
        }
        if (myTopGroups != null) {
            children.addAll(myTopGroups);
        }
        if (myValueChildren != null) {
            children.addAll(myValueChildren);
        }
        if (myBottomGroups != null) {
            children.addAll(myBottomGroups);
        }
        if (myTemporaryMessageChildren != null) {
            children.addAll(myTemporaryMessageChildren);
        }
        return children;
    }

    public synchronized List<UnifiedXValueContainerNode<?>> getLoadedChildren() {
        List<UnifiedXValueContainerNode<?>> children = new ArrayList<>();
        if (myTopGroups != null) {
            children.addAll(myTopGroups);
        }
        if (myValueChildren != null) {
            children.addAll(myValueChildren);
        }
        if (myBottomGroups != null) {
            children.addAll(myBottomGroups);
        }
        return children;
    }

    /**
     * The root was replaced - the node and what was loaded below it no longer take what the debug process sends.
     */
    public void setObsolete() {
        myObsolete = true;
        for (UnifiedXValueContainerNode<?> child : getLoadedChildren()) {
            child.setObsolete();
        }
    }
}
