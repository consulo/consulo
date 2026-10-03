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
package consulo.execution.profiler.impl.internal.calltree;

import consulo.application.progress.ProgressIndicator;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.ThreadInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeNode {
    private static final Comparator<CallTreeNode> ORDER = Comparator.comparingLong(CallTreeNode::getTotal)
        .reversed()
        .thenComparing(Comparator.comparingLong(CallTreeNode::getSelf).reversed())
        .thenComparing(CallTreeNode::getSortName);

    private final @Nullable CallTreeNode myParent;
    private final @Nullable BaseCallStackElement myElement;
    private final @Nullable ThreadInfo myThread;
    private final int myDepth;

    private long myTotal;
    private long mySelf;
    private long myCount;
    private List<CallTreeNode> myChildren = List.of();
    private @Nullable Map<BaseCallStackElement, CallTreeNode> myChildrenByElement;
    private int myOpenAncestors;

    CallTreeNode(@Nullable CallTreeNode parent, @Nullable BaseCallStackElement element, @Nullable ThreadInfo thread) {
        myParent = parent;
        myElement = element;
        myThread = thread;
        myDepth = parent == null ? 0 : parent.myDepth + 1;
    }

    public @Nullable CallTreeNode getParent() {
        return myParent;
    }

    public boolean isRoot() {
        return myParent == null;
    }

    public @Nullable BaseCallStackElement getElement() {
        return myElement;
    }

    public @Nullable ThreadInfo getThread() {
        return myThread;
    }

    public int getDepth() {
        return myDepth;
    }

    public long getTotal() {
        return myTotal;
    }

    public long getSelf() {
        return mySelf;
    }

    public long getCount() {
        return myCount;
    }

    public List<CallTreeNode> getChildren() {
        return myChildren;
    }

    public List<BaseCallStackElement> getPath() {
        BaseCallStackElement[] path = new BaseCallStackElement[myDepth + 1];
        int size = 0;
        CallTreeNode node = this;
        while (node != null) {
            BaseCallStackElement element = node.myElement;
            if (element != null) {
                path[path.length - 1 - size] = element;
                size++;
            }
            node = node.myParent;
        }
        return List.of(Arrays.copyOfRange(path, path.length - size, path.length));
    }

    @Override
    public String toString() {
        BaseCallStackElement element = myElement;
        return (element == null ? "<root>" : element.fullName()) + " total=" + myTotal + " self=" + mySelf;
    }

    CallTreeNode getOrCreateChild(BaseCallStackElement element) {
        Map<BaseCallStackElement, CallTreeNode> children = myChildrenByElement;
        if (children == null) {
            children = new HashMap<>();
            myChildrenByElement = children;
        }
        CallTreeNode child = children.get(element);
        if (child == null) {
            child = new CallTreeNode(this, element, myThread);
            children.put(element, child);
        }
        return child;
    }

    void addTotal(long value, long count) {
        myTotal += value;
        myCount += count;
    }

    void addSelf(long value) {
        mySelf += value;
    }

    boolean openAncestor() {
        return myOpenAncestors++ == 0;
    }

    void closeAncestor() {
        myOpenAncestors--;
    }

    void freeze(ProgressIndicator indicator) {
        Deque<CallTreeNode> queue = new ArrayDeque<>();
        queue.add(this);
        int processed = 0;
        while (!queue.isEmpty()) {
            if ((++processed & 0x3FF) == 0) {
                indicator.checkCanceled();
            }

            CallTreeNode node = queue.removeFirst();
            Map<BaseCallStackElement, CallTreeNode> children = node.myChildrenByElement;
            node.myChildrenByElement = null;
            if (children == null || children.isEmpty()) {
                node.myChildren = List.of();
                continue;
            }

            List<CallTreeNode> sorted = new ArrayList<>(children.values());
            sorted.sort(ORDER);
            node.myChildren = List.copyOf(sorted);
            queue.addAll(sorted);
        }
    }

    private String getSortName() {
        BaseCallStackElement element = myElement;
        return element == null ? "" : element.fullName();
    }
}
