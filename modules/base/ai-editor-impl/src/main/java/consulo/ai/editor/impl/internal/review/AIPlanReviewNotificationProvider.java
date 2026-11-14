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
package consulo.ai.editor.impl.internal.review;

import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The bar over a plan an agent wrote - how to comment on it, sending the comments back, and Build.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class AIPlanReviewNotificationProvider implements EditorNotificationProvider, DumbAware {
    private final Project myProject;

    @Inject
    public AIPlanReviewNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "ai-plan-review";
    }

    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(VirtualFile file,
                                                                 FileEditor fileEditor,
                                                                 Supplier<EditorNotificationBuilder> builderFactory) {
        AIPlanReviewManager manager = AIPlanReviewManager.getInstance(myProject);
        if (!manager.isPlan(file)) {
            return null;
        }
        int drafts = manager.getDraftCount(file);
        EditorNotificationBuilder builder = builderFactory.get()
            .withText(drafts == 0 ? AIEditorLocalize.labelPlanReviewHint() : AIEditorLocalize.labelPlanReviewComments(drafts))
            .asInfo();
        if (drafts > 0) {
            builder.withAction(AIEditorLocalize.buttonPlanReviewSend(), event -> manager.sendFeedback(file));
        }
        builder.withAction(AIEditorLocalize.buttonPlanReviewBuild(), event -> manager.build(file));
        return builder;
    }
}
