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
package consulo.task.issue;

import consulo.task.Comment;
import consulo.task.Task;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public interface IssueTracker {
    CompletableFuture<IssuePage> search(IssueQuery query, int offset, int limit);

    CompletableFuture<@Nullable Task> findIssue(String id);

    default CompletableFuture<IssueDetails> loadDetails(Task task) {
        List<IssueComment> comments = new ArrayList<>();
        Comment[] taskComments = task.getComments();
        for (int i = 0; i < taskComments.length; i++) {
            Comment comment = taskComments[i];
            comments.add(new IssueComment(String.valueOf(i), comment.getAuthor(), comment.getDate(), comment.getText()));
        }
        return CompletableFuture.completedFuture(new IssueDetails(task, List.of(), comments, List.of(), List.of(), List.of(), List.of()));
    }

    Set<IssueCapability> getCapabilities();

    default <T> @Nullable T getSupport(Class<T> supportClass) {
        return supportClass.isInstance(this) ? supportClass.cast(this) : null;
    }
}
