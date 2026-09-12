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
package consulo.sandboxPlugin.lang.parser;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Nesting stack of {@code #if}/{@code #elif}/{@code #else}/{@code #end} evaluation —
 * the consulo-csharp {@code PreprocessorState} shape: one {@code SubState} per open block,
 * one boolean per chain segment, disabled = any enclosing segment inactive.
 */
public class SandPreprocessorState {
    public static class SubState {
        final Deque<Boolean> mySegments = new ArrayDeque<>();

        SubState(Boolean initialValue) {
            mySegments.add(initialValue);
        }

        boolean haveActive() {
            for (Boolean segment : mySegments) {
                if (segment) {
                    return true;
                }
            }
            return false;
        }

        boolean isActive() {
            Boolean value = mySegments.peekLast();
            return value != null && value;
        }

        void addSegment(Boolean value) {
            mySegments.addLast(value);
        }
    }

    private final Deque<SubState> myStates = new ArrayDeque<>();

    public SubState newState(Boolean value) {
        SubState state = new SubState(value);
        myStates.addLast(state);
        return state;
    }

    public @Nullable SubState last() {
        return myStates.peekLast();
    }

    public @Nullable SubState removeLast() {
        return myStates.pollLast();
    }

    public boolean isDisabled(boolean skipCurrent) {
        if (myStates.isEmpty()) {
            return false;
        }

        SubState last = last();
        Iterator<SubState> iterator = myStates.descendingIterator();
        while (iterator.hasNext()) {
            SubState next = iterator.next();
            if (skipCurrent && last == next) {
                continue;
            }
            if (!next.isActive()) {
                return true;
            }
        }
        return false;
    }
}
