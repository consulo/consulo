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
import consulo.execution.profiler.Stack;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.NoThreadInfoInProfilerData;
import consulo.execution.profiler.model.ThreadInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
final class CallTreeAggregator {
    private static final int CANCEL_CHECK_MASK = 0x3FF;
    private static final CallTreeNode[] NO_NODES = new CallTreeNode[0];

    private static final Comparator<CallTreeMethod> METHOD_ORDER = Comparator.comparingLong(CallTreeMethod::self)
        .reversed()
        .thenComparing(Comparator.comparingLong(CallTreeMethod::total).reversed())
        .thenComparing(method -> method.element().fullName());

    private CallTreeAggregator() {
    }

    static CallTreeData aggregate(Iterable<? extends Stack<BaseCallStackElement>> stacks, ProgressIndicator indicator) {
        List<Stack<BaseCallStackElement>> all = new ArrayList<>();
        Map<ThreadInfo, List<Stack<BaseCallStackElement>>> byThread = new LinkedHashMap<>();
        int processed = 0;
        for (Stack<BaseCallStackElement> stack : stacks) {
            if ((++processed & CANCEL_CHECK_MASK) == 0) {
                indicator.checkCanceled();
            }
            all.add(stack);
            byThread.computeIfAbsent(stack.getThread(), thread -> new ArrayList<>()).add(stack);
        }

        CallTree allThreads = buildTree(all, null, indicator);

        boolean hasThreads = !byThread.isEmpty()
            && !(byThread.size() == 1 && byThread.containsKey(NoThreadInfoInProfilerData.INSTANCE));
        if (!hasThreads) {
            return new CallTreeData(allThreads, List.of());
        }

        List<CallTree> threads = new ArrayList<>(byThread.size());
        for (Map.Entry<ThreadInfo, List<Stack<BaseCallStackElement>>> entry : byThread.entrySet()) {
            threads.add(buildTree(entry.getValue(), entry.getKey(), indicator));
        }
        threads.sort(Comparator.comparingLong(CallTree::getTotal).reversed());
        return new CallTreeData(allThreads, threads);
    }

    static CallTree buildTree(List<Stack<BaseCallStackElement>> stacks, @Nullable ThreadInfo thread, ProgressIndicator indicator) {
        CallTreeNode topDown = buildTopDown(stacks, thread, indicator);
        CallTreeNode bottomUp = buildBottomUp(topDown, thread, indicator);

        List<CallTreeMethod> methods = new ArrayList<>(bottomUp.getChildren().size());
        for (CallTreeNode node : bottomUp.getChildren()) {
            BaseCallStackElement element = node.getElement();
            if (element != null) {
                methods.add(new CallTreeMethod(element, node.getSelf(), node.getTotal(), node.getCount()));
            }
        }
        methods.sort(METHOD_ORDER);

        return new CallTree(thread, topDown, bottomUp, methods);
    }

    private static CallTreeNode buildTopDown(
        List<Stack<BaseCallStackElement>> stacks,
        @Nullable ThreadInfo thread,
        ProgressIndicator indicator
    ) {
        CallTreeNode root = new CallTreeNode(null, null, thread);
        int processed = 0;
        for (Stack<BaseCallStackElement> stack : stacks) {
            if ((++processed & CANCEL_CHECK_MASK) == 0) {
                indicator.checkCanceled();
            }

            long value = stack.getValue();
            CallTreeNode node = root;
            node.addTotal(value, 1);
            for (BaseCallStackElement frame : stack.getFrames()) {
                node = node.getOrCreateChild(frame);
                node.addTotal(value, 1);
            }
            node.addSelf(value);
        }
        root.freeze(indicator);
        return root;
    }

    private static CallTreeNode buildBottomUp(CallTreeNode topDown, @Nullable ThreadInfo thread, ProgressIndicator indicator) {
        CallTreeNode root = new CallTreeNode(null, null, thread);
        root.addTotal(topDown.getTotal(), topDown.getCount());
        root.addSelf(topDown.getSelf());

        List<BaseCallStackElement> path = new ArrayList<>();
        Deque<BottomUpVisit> visits = new ArrayDeque<>();
        visits.push(new BottomUpVisit(topDown, NO_NODES));
        int processed = 0;
        while (!visits.isEmpty()) {
            BottomUpVisit visit = visits.getFirst();
            List<CallTreeNode> children = visit.node().getChildren();
            int nextChild = visit.nextChild();
            if (nextChild < children.size()) {
                if ((++processed & CANCEL_CHECK_MASK) == 0) {
                    indicator.checkCanceled();
                }

                visit.advance();
                CallTreeNode child = children.get(nextChild);
                BaseCallStackElement element = child.getElement();
                if (element == null) {
                    continue;
                }
                path.add(element);
                visits.push(new BottomUpVisit(child, enter(root, path, child)));
            }
            else {
                visits.pop();
                for (CallTreeNode node : visit.chain()) {
                    node.closeAncestor();
                }
                if (visit.node() != topDown) {
                    path.remove(path.size() - 1);
                }
            }
        }

        root.freeze(indicator);
        return root;
    }

    private static CallTreeNode[] enter(CallTreeNode root, List<BaseCallStackElement> path, CallTreeNode topDownNode) {
        int depth = path.size();
        CallTreeNode[] chain = new CallTreeNode[depth];
        CallTreeNode parent = root;
        for (int i = 0; i < depth; i++) {
            CallTreeNode node = parent.getOrCreateChild(path.get(depth - 1 - i));
            if (node.openAncestor()) {
                node.addTotal(topDownNode.getTotal(), topDownNode.getCount());
            }
            node.addSelf(topDownNode.getSelf());
            chain[i] = node;
            parent = node;
        }
        return chain;
    }

    private static final class BottomUpVisit {
        private final CallTreeNode myNode;
        private final CallTreeNode[] myChain;
        private int myNextChild;

        private BottomUpVisit(CallTreeNode node, CallTreeNode[] chain) {
            myNode = node;
            myChain = chain;
        }

        private CallTreeNode node() {
            return myNode;
        }

        private CallTreeNode[] chain() {
            return myChain;
        }

        private int nextChild() {
            return myNextChild;
        }

        private void advance() {
            myNextChild++;
        }
    }
}
