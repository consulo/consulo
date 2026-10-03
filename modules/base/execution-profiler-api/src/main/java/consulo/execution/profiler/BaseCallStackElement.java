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

import consulo.language.psi.NavigatablePsiElement;
import consulo.project.Project;

/**
 * One frame of a recorded call stack, made by the language or runtime that parsed it.
 * <p>
 * Call trees merge frames by {@link #equals(Object)}, so parsers usually intern their frames.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class BaseCallStackElement {
    /**
     * @return the complete text of the frame, as the profiler reported it
     */
    public abstract String fullName();

    /**
     * @return whether {@link #calcNavigatables(Project)} may find source elements for this frame
     */
    public boolean isNavigatable() {
        return false;
    }

    /**
     * Finds the source elements of this frame.
     * <p>
     * Called on a background thread without a read lock: an implementation takes its own read action and returns nothing
     * when the project is already disposed.
     */
    public NavigatablePsiElement[] calcNavigatables(Project project) {
        return NavigatablePsiElement.EMPTY_ARRAY;
    }

    @Override
    public String toString() {
        return fullName();
    }
}
