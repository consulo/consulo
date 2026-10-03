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
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTree {
    private final @Nullable ThreadInfo myThread;
    private final CallTreeNode myTopDown;
    private final CallTreeNode myBottomUp;
    private final List<CallTreeMethod> myMethods;
    private final Map<BaseCallStackElement, CallTreeNode> myBackTraces;

    CallTree(@Nullable ThreadInfo thread, CallTreeNode topDown, CallTreeNode bottomUp, List<CallTreeMethod> methods) {
        myThread = thread;
        myTopDown = topDown;
        myBottomUp = bottomUp;
        myMethods = List.copyOf(methods);

        Map<BaseCallStackElement, CallTreeNode> backTraces = new HashMap<>();
        for (CallTreeNode node : bottomUp.getChildren()) {
            BaseCallStackElement element = node.getElement();
            if (element != null) {
                backTraces.put(element, node);
            }
        }
        myBackTraces = backTraces;
    }

    public @Nullable ThreadInfo getThread() {
        return myThread;
    }

    public boolean isAllThreads() {
        return myThread == null;
    }

    public long getTotal() {
        return myTopDown.getTotal();
    }

    public CallTreeNode getTopDown() {
        return myTopDown;
    }

    public CallTreeNode getBottomUp() {
        return myBottomUp;
    }

    public List<CallTreeMethod> getMethods() {
        return myMethods;
    }

    public @Nullable CallTreeNode getBackTraces(BaseCallStackElement element) {
        return myBackTraces.get(element);
    }

    public CallTreeNode buildMergedCallees(BaseCallStackElement element, ProgressIndicator indicator) {
        CallTreeNode merged = new CallTreeNode(null, element, myThread);

        Deque<CallTreeNode> search = new ArrayDeque<>(myTopDown.getChildren());
        Deque<CallTreeNode> sources = new ArrayDeque<>();
        Deque<CallTreeNode> targets = new ArrayDeque<>();
        int processed = 0;
        while (!search.isEmpty()) {
            if ((++processed & 0x3FF) == 0) {
                indicator.checkCanceled();
            }

            CallTreeNode node = search.removeFirst();
            if (element.equals(node.getElement())) {
                sources.addLast(node);
                targets.addLast(merged);
            }
            else {
                search.addAll(node.getChildren());
            }
        }

        while (!sources.isEmpty()) {
            if ((++processed & 0x3FF) == 0) {
                indicator.checkCanceled();
            }

            CallTreeNode source = sources.removeFirst();
            CallTreeNode target = targets.removeFirst();
            target.addTotal(source.getTotal(), source.getCount());
            target.addSelf(source.getSelf());

            for (CallTreeNode child : source.getChildren()) {
                BaseCallStackElement childElement = child.getElement();
                if (childElement != null) {
                    sources.addLast(child);
                    targets.addLast(target.getOrCreateChild(childElement));
                }
            }
        }

        merged.freeze(indicator);
        return merged;
    }
}
