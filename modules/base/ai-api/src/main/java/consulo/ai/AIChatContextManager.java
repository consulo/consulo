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
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import java.nio.file.Path;

/**
 * Gives context to the AI chats of a project - the chat the user works with, or a new one with the default account
 * when no chat is open.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface AIChatContextManager {
    static AIChatContextManager getInstance(Project project) {
        return project.getInstance(AIChatContextManager.class);
    }

    /**
     * Attaches to the next message of the chat and brings the chat forward.
     */
    @RequiredUIAccess
    void attach(AIChatAttachment attachment);

    /**
     * Sends a message about what the user picked - code with a comment on it - to the chat the user works with, the
     * attachment going with the message.
     */
    @RequiredUIAccess
    void send(AIChatAttachment attachment, String message);

    /**
     * Whether the file is a plan an agent wrote - one a chat was told of, or one named as plans are.
     */
    boolean isPlan(Path file);

    /**
     * Sends the feedback on a plan to the chat which made it - the chat the user works with when none is known.
     */
    @RequiredUIAccess
    void revisePlan(Path file, String feedback);

    /**
     * The plan is accepted - the chat which made it implements it.
     */
    @RequiredUIAccess
    void buildPlan(Path file);
}
