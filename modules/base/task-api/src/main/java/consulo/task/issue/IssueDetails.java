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

import consulo.task.Task;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssueDetails {
    private final Task myTask;
    private final List<IssueField> myFields;
    private final List<IssueComment> myComments;
    private final List<IssueWorkLog> myWorkLogs;
    private final List<IssueAttachment> myAttachments;
    private final List<IssueLink> myLinks;
    private final List<Task> mySubtasks;

    public IssueDetails(
        Task task,
        List<IssueField> fields,
        List<IssueComment> comments,
        List<IssueWorkLog> workLogs,
        List<IssueAttachment> attachments,
        List<IssueLink> links,
        List<Task> subtasks
    ) {
        myTask = task;
        myFields = List.copyOf(fields);
        myComments = List.copyOf(comments);
        myWorkLogs = List.copyOf(workLogs);
        myAttachments = List.copyOf(attachments);
        myLinks = List.copyOf(links);
        mySubtasks = List.copyOf(subtasks);
    }

    public Task getTask() {
        return myTask;
    }

    public List<IssueField> getFields() {
        return myFields;
    }

    public List<IssueComment> getComments() {
        return myComments;
    }

    public List<IssueWorkLog> getWorkLogs() {
        return myWorkLogs;
    }

    public List<IssueAttachment> getAttachments() {
        return myAttachments;
    }

    public List<IssueLink> getLinks() {
        return myLinks;
    }

    public List<Task> getSubtasks() {
        return mySubtasks;
    }
}
