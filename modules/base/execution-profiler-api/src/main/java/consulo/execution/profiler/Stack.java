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

import java.util.List;

/**
 * One recorded call stack: its thread, its frames root first, and its value, such as a sample count or a duration.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public final class Stack<T> {
    private final ThreadInfo myThread;
    private final List<T> myFrames;
    private final long myValue;

    public Stack(ThreadInfo thread, List<? extends T> framesRootFirst, long value) {
        myThread = thread;
        myFrames = List.copyOf(framesRootFirst);
        myValue = value;
    }

    public ThreadInfo getThread() {
        return myThread;
    }

    /**
     * @return the frames, root first
     */
    public List<T> getFrames() {
        return myFrames;
    }

    public long getValue() {
        return myValue;
    }
}
