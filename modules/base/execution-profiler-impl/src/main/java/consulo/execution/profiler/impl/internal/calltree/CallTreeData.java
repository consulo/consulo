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
import consulo.execution.profiler.CallTreeBuilder;
import consulo.execution.profiler.Stack;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.execution.profiler.model.ThreadInfo;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class CallTreeData {
    private final CallTree myAllThreads;
    private final List<CallTree> myThreads;

    CallTreeData(CallTree allThreads, List<CallTree> threads) {
        myAllThreads = allThreads;
        myThreads = List.copyOf(threads);
    }

    public static CallTreeData build(CallTreeBuilder<BaseCallStackElement> builder, ProgressIndicator indicator) {
        return build(builder.getAllStacks(), indicator);
    }

    public static CallTreeData build(Iterable<? extends Stack<BaseCallStackElement>> stacks, ProgressIndicator indicator) {
        return CallTreeAggregator.aggregate(stacks, indicator);
    }

    public CallTree getAllThreads() {
        return myAllThreads;
    }

    public List<CallTree> getThreads() {
        return myThreads;
    }

    public List<CallTree> getTrees() {
        List<CallTree> trees = new ArrayList<>(myThreads.size() + 1);
        trees.add(myAllThreads);
        trees.addAll(myThreads);
        return trees;
    }

    public CallTree getTree(@Nullable ThreadInfo thread) {
        if (thread == null) {
            return myAllThreads;
        }
        for (CallTree tree : myThreads) {
            if (thread.equals(tree.getThread())) {
                return tree;
            }
        }
        return myAllThreads;
    }
}
