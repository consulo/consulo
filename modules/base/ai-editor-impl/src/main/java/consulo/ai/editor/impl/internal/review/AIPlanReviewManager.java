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

import consulo.ai.AIChatContextManager;
import consulo.ai.editor.impl.internal.review.state.AIPlanReviewDraftEntry;
import consulo.ai.editor.impl.internal.review.state.AIPlanReviewFileEntry;
import consulo.ai.editor.impl.internal.review.state.AIPlanReviewState;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
@State(name = "AIPlanReviews", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class AIPlanReviewManager implements Disposable, PersistentStateComponent<AIPlanReviewState> {
    private final Project myProject;
    private final Map<VirtualFile, AIReviewModel> myModels = new HashMap<>();
    /**
     * The drafts by the path of their plan - the ones of the plans open now are taken from their models when saved.
     */
    private AIPlanReviewState myState = new AIPlanReviewState();

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
        AIReviewModel model = myModels.computeIfAbsent(file, key -> {
            AIReviewModel[] created = new AIReviewModel[1];
            created[0] = new AIReviewModel(myProject, editor.getDocument(), () -> {
                saveDrafts(key, created[0]);
                EditorNotifications.getInstance(myProject).updateNotifications(key);
            });
            Disposer.register(this, created[0]);
            AIPlanReviewFileEntry saved = findEntry(key.getPath());
            if (saved != null) {
                created[0].restoreDrafts(new ArrayList<>(saved.DRAFTS));
            }
            return created[0];
        });
        myProject.getApplication().getInstance(CodeReviewEditorRenderer.class).setupIn(editorDisposable, editor, model);
    }

    /**
     * Opens the input of a new comment on the lines - the ones a selection spans.
     */
    @RequiredUIAccess
    void requestComment(VirtualFile file, CodeReviewLineRange lines) {
        AIReviewModel model = myModels.get(file);
        if (model != null) {
            model.requestNewComment(lines);
        }
    }

    boolean hasReview(VirtualFile file) {
        return myModels.containsKey(file);
    }

    int getDraftCount(VirtualFile file) {
        AIReviewModel model = myModels.get(file);
        return model == null ? 0 : model.getDraftCount();
    }

    /**
     * The comments go to the chat which made the plan, which revises it.
     */
    @RequiredUIAccess
    void sendFeedback(VirtualFile file) {
        AIReviewModel model = myModels.get(file);
        Path path = pathOf(file);
        if (model == null || path == null || model.getDraftCount() == 0) {
            return;
        }
        StringBuilder feedback = new StringBuilder(AIEditorLocalize.promptPlanFeedback(file.getName()).get());
        for (String line : model.takeFeedback()) {
            feedback.append("\n- ").append(line);
        }
        AIChatContextManager.getInstance(myProject).revisePlan(path, feedback.toString());
    }

    @RequiredUIAccess
    void build(VirtualFile file) {
        Path path = pathOf(file);
        if (path != null) {
            AIChatContextManager.getInstance(myProject).buildPlan(path);
        }
    }

    private void saveDrafts(VirtualFile file, @Nullable AIReviewModel model) {
        if (model == null) {
            return;
        }
        AIPlanReviewFileEntry entry = findEntry(file.getPath());
        List<AIPlanReviewDraftEntry> drafts = model.saveDrafts();
        if (drafts.isEmpty()) {
            if (entry != null) {
                myState.FILES.remove(entry);
            }
            return;
        }
        if (entry == null) {
            entry = new AIPlanReviewFileEntry();
            entry.PATH = file.getPath();
            myState.FILES.add(entry);
        }
        entry.DRAFTS = drafts;
    }

    private @Nullable AIPlanReviewFileEntry findEntry(String path) {
        for (AIPlanReviewFileEntry entry : myState.FILES) {
            if (path.equals(entry.PATH)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public AIPlanReviewState getState() {
        return myState;
    }

    @Override
    public void loadState(AIPlanReviewState state) {
        myState = state;
    }

    @Override
    public void dispose() {
        myModels.clear();
    }
}
