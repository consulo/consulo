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
package consulo.ai.editor.impl.internal.plan;

import consulo.ai.AIChatContextManager;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.EditorNotifications;
import consulo.project.Project;
import consulo.review.editor.CodeReviewEditorRenderer;
import consulo.review.editor.CodeReviewLineRange;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The reviews of the plans open in the editors of a project - a plan keeps its drafts while it is open.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class AIPlanReviewManager implements Disposable {
    private final Project myProject;
    private final Map<VirtualFile, AIPlanReviewModel> myModels = new HashMap<>();

    @Inject
    public AIPlanReviewManager(Project project) {
        myProject = project;
    }

    public static AIPlanReviewManager getInstance(Project project) {
        return project.getInstance(AIPlanReviewManager.class);
    }

    static @Nullable Path pathOf(VirtualFile file) {
        return file.isInLocalFileSystem() ? Path.of(file.getPath()) : null;
    }

    public boolean isPlan(VirtualFile file) {
        Path path = pathOf(file);
        return path != null && AIChatContextManager.getInstance(myProject).isPlan(path);
    }

    /**
     * Shows the review of the plan in the editor - the comments, and the gutter to write new ones.
     */
    @RequiredUIAccess
    void install(Editor editor, VirtualFile file, Disposable editorDisposable) {
        AIPlanReviewModel model = myModels.computeIfAbsent(file, key -> {
            AIPlanReviewModel created = new AIPlanReviewModel(myProject, editor.getDocument(),
                () -> EditorNotifications.getInstance(myProject).updateNotifications(key));
            Disposer.register(this, created);
            return created;
        });
        myProject.getApplication().getInstance(CodeReviewEditorRenderer.class).setupIn(editorDisposable, editor, model);
    }

    /**
     * Opens the input of a new comment on the lines - the ones a selection spans.
     */
    @RequiredUIAccess
    void requestComment(VirtualFile file, CodeReviewLineRange lines) {
        AIPlanReviewModel model = myModels.get(file);
        if (model != null) {
            model.requestNewComment(lines);
        }
    }

    boolean hasReview(VirtualFile file) {
        return myModels.containsKey(file);
    }

    int getDraftCount(VirtualFile file) {
        AIPlanReviewModel model = myModels.get(file);
        return model == null ? 0 : model.getDraftCount();
    }

    /**
     * The comments go to the chat which made the plan, which revises it.
     */
    @RequiredUIAccess
    void sendFeedback(VirtualFile file) {
        AIPlanReviewModel model = myModels.get(file);
        Path path = pathOf(file);
        if (model == null || path == null || model.getDraftCount() == 0) {
            return;
        }
        AIChatContextManager.getInstance(myProject).revisePlan(path, model.takeFeedback(file.getName()));
    }

    @RequiredUIAccess
    void build(VirtualFile file) {
        Path path = pathOf(file);
        if (path != null) {
            AIChatContextManager.getInstance(myProject).buildPlan(path);
        }
    }

    @Override
    public void dispose() {
        myModels.clear();
    }
}
