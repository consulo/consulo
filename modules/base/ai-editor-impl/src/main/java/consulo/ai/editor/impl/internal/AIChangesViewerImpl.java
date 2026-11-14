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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChangesViewer;
import consulo.ai.AIToolCallContent;
import consulo.ai.editor.impl.internal.review.AIChangesReview;
import consulo.ai.editor.impl.internal.review.AIChangesReviewSendAction;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ServiceImpl;
import consulo.diff.DiffContentFactory;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffManager;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.SimpleDiffRequestChain;
import consulo.diff.content.DiffContent;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.SimpleDiffRequest;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The changes of an agent as a chain of diffs - what each file was before the agent touched it against what it made
 * of it - stepped through like the changes of a commit.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceImpl
@Singleton
public class AIChangesViewerImpl implements AIChangesViewer {
    @RequiredUIAccess
    @Override
    public void show(Project project, List<AIToolCallContent.Diff> changes, int selected, @Nullable Consumer<String> feedback) {
        AIChangesReview review = feedback == null ? null : new AIChangesReview(project, feedback);
        if (changes.isEmpty()) {
            return;
        }
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        List<DiffRequest> requests = new ArrayList<>();
        for (AIToolCallContent.Diff change : changes) {
            FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName(change.path()));
            String oldText = change.oldText();
            DiffContent before = oldText == null ? contentFactory.createEmpty() : contentFactory.create(project, oldText, fileType);
            DiffContent after = contentFactory.create(project, change.newText(), fileType);
            SimpleDiffRequest request = new SimpleDiffRequest(
                relative(project, change.path()),
                before,
                after,
                oldText == null ? AIEditorLocalize.diffCreated().get() : AIEditorLocalize.diffBefore().get(),
                AIEditorLocalize.diffAfter().get()
            );
            // the changes take review comments, sent to the agent from the toolbar of the diff
            if (review != null) {
                request.putUserData(AIChangesReview.KEY, review);
                request.putUserData(AIChangesReview.PATH, relative(project, change.path()));
                request.putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, List.of(new AIChangesReviewSendAction(review)));
            }
            requests.add(request);
        }
        SimpleDiffRequestChain chain = new SimpleDiffRequestChain(requests);
        chain.setIndex(Math.max(0, Math.min(selected, requests.size() - 1)));
        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
    }

    private static String fileName(String path) {
        Path name = Path.of(path).getFileName();
        return name == null ? path : name.toString();
    }

    private static String relative(Project project, String path) {
        String basePath = project.getBasePath();
        return basePath != null && path.startsWith(basePath + "/") ? path.substring(basePath.length() + 1) : path;
    }
}
