/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

import java.util.EventListener;
import java.util.List;

@TopicAPI(ComponentScope.PROJECT)
public interface ToolWindowManagerListener extends EventListener {
    default void toolWindowsRegistered(@Nonnull List<String> ids, @Nonnull ToolWindowManager toolWindowManager) {
        for (String id : ids) {
            toolWindowRegistered(id);
        }
    }

    default void toolWindowUnregistered(@Nonnull String id, @Nonnull ToolWindow toolWindow) {
    }

    /**
     * Invoked when tool window with specified <code>id</code> is registered in {@link ToolWindowManager}.
     *
     * @param id <code>id</code> of registered tool window.
     */
    default void toolWindowRegistered(@Nonnull String id) {
    }

    default void stateChanged(ToolWindowManager toolWindowManager) {
        stateChanged();
    }

    @Deprecated(forRemoval = true)
    @DeprecationInfo("Use with ToolWindowManager parameter")
    default void stateChanged() {
    }

    default void toolWindowShown(@Nonnull ToolWindow toolWindow) {
    }
}