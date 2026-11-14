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
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The review of the changes of an agent - comments on the lines of the changed files, drawn in the diff of each, which
 * go to the agent together.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class AIChangesReview {
    public static final Key<AIChangesReview> KEY = Key.create("AIChangesReview");
    public static final Key<String> PATH = Key.create("AIChangesReview.path");

    private final Project myProject;
    private final Consumer<String> myFeedback;
    private final Map<String, AIReviewModel> myModels = new LinkedHashMap<>();

    public AIChangesReview(Project project, Consumer<String> feedback) {
        myProject = project;
        myFeedback = feedback;
    }

    /**
     * The review of a changed file - kept while the diff goes to the other files.
     */
    AIReviewModel model(String path, Document document) {
        return myModels.computeIfAbsent(path, key -> new AIReviewModel(myProject, document, () -> {
        }));
    }

    int getDraftCount() {
        int count = 0;
        for (AIReviewModel model : myModels.values()) {
            count += model.getDraftCount();
        }
        return count;
    }

    /**
     * The comments of every file as one message - and none of them after it.
     */
    void send() {
        StringBuilder feedback = new StringBuilder(AIEditorLocalize.promptChangesFeedback().get());
        for (Map.Entry<String, AIReviewModel> entry : myModels.entrySet()) {
            List<String> lines = entry.getValue().takeFeedback();
            if (lines.isEmpty()) {
                continue;
            }
            feedback.append("\n\n").append(AIEditorLocalize.promptChangesFeedbackFile(entry.getKey()).get());
            for (String line : lines) {
                feedback.append("\n- ").append(line);
            }
        }
        myFeedback.accept(feedback.toString());
    }
}
