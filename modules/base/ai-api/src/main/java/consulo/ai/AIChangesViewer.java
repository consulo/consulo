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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Shows the changes of files an AI agent made, one after another in the diff viewer - the chat itself shows only how
 * many lines changed.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AIChangesViewer {
    static AIChangesViewer getInstance() {
        return Application.get().getInstance(AIChangesViewer.class);
    }

    /**
     * @param selected the change shown first
     */
    @RequiredUIAccess
    default void show(Project project, List<AIToolCallContent.Diff> changes, int selected) {
        show(project, changes, selected, null);
    }

    /**
     * @param selected the change shown first
     * @param feedback takes the review of the changes - comments on their lines - as a message to the agent; null
     *                 shows the changes without a review
     */
    @RequiredUIAccess
    void show(Project project, List<AIToolCallContent.Diff> changes, int selected, @Nullable Consumer<String> feedback);
}
