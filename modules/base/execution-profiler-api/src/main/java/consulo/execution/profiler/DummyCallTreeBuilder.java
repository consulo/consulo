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
package consulo.execution.profiler;

import consulo.execution.profiler.model.ThreadInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link CallTreeBuilder} which keeps the stacks as they are added. Safe to fill from one thread while others read it.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public class DummyCallTreeBuilder<T> implements CallTreeBuilder<T> {
    private final Object myLock = new Object();
    private final List<Stack<T>> myStacks = new ArrayList<>();

    /**
     * Adds one stack.
     *
     * @param framesRootFirst the frames, outermost call first
     * @param value           the samples or time the stack accounts for
     */
    public void addStack(ThreadInfo thread, List<? extends T> framesRootFirst, long value) {
        Stack<T> stack = new Stack<>(thread, framesRootFirst, value);
        synchronized (myLock) {
            myStacks.add(stack);
        }
    }

    /**
     * Replaces every frame by what the mapper returns for it, such as a demangled or resolved frame. Each distinct frame is
     * mapped once, so frames which were shared stay shared.
     */
    public void mapTreeElements(Function<T, T> mapper) {
        synchronized (myLock) {
            Map<T, T> mapped = new HashMap<>();
            for (int i = 0; i < myStacks.size(); i++) {
                Stack<T> stack = myStacks.get(i);
                List<T> frames = new ArrayList<>(stack.getFrames().size());
                for (T frame : stack.getFrames()) {
                    frames.add(mapped.computeIfAbsent(frame, mapper));
                }
                myStacks.set(i, new Stack<>(stack.getThread(), frames, stack.getValue()));
            }
        }
    }

    @Override
    public Iterable<Stack<T>> getAllStacks() {
        synchronized (myLock) {
            return List.copyOf(myStacks);
        }
    }
}
