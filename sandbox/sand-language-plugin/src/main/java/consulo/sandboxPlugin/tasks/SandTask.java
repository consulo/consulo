/*
 * Copyright 2013-2021 consulo.io
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
package consulo.sandboxPlugin.tasks;

import consulo.task.Comment;
import consulo.task.SimpleComment;
import consulo.task.Task;
import consulo.task.TaskRepository;
import consulo.task.TaskState;
import consulo.task.TaskType;
import consulo.task.icon.TaskIconGroup;
import consulo.task.issue.IssueAttachment;
import consulo.task.issue.IssueComment;
import consulo.task.issue.IssueLink;
import consulo.task.issue.IssueWorkLog;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SandTask extends Task {
    private final String myId;
    private final String mySummary;
    private final TaskType myType;
    private TaskState myState;
    private final @Nullable String myDescription;
    private final @Nullable Date myCreated;
    private final @Nullable Date myUpdated;
    private String myPriority = "Medium";
    private @Nullable String myAssignee;
    private final String myReporter;
    private @Nullable TaskRepository myRepository;

    final List<IssueComment> comments = new ArrayList<>();
    final List<IssueWorkLog> workLogs = new ArrayList<>();
    final List<IssueAttachment> attachments = new ArrayList<>();
    final List<IssueLink> links = new ArrayList<>();
    final List<SandTask> subtasks = new ArrayList<>();

    public SandTask(String id, String summary, TaskType type, TaskState state, @Nullable String description, @Nullable Date updated, String reporter) {
        myId = id;
        mySummary = summary;
        myType = type;
        myState = state;
        myDescription = description;
        myUpdated = updated;
        myCreated = updated;
        myReporter = reporter;
    }

    public void setRepository(TaskRepository repository) {
        myRepository = repository;
    }

    @Override
    public @Nullable TaskRepository getRepository() {
        return myRepository;
    }

    public void setState(TaskState state) {
        myState = state;
    }

    public void setPriority(String priority) {
        myPriority = priority;
    }

    public void setAssignee(@Nullable String assignee) {
        myAssignee = assignee;
    }

    public String getReporter() {
        return myReporter;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getSummary() {
        return mySummary;
    }

    @Override
    public @Nullable String getDescription() {
        return myDescription;
    }

    @Override
    public Comment[] getComments() {
        Comment[] result = new Comment[comments.size()];
        for (int i = 0; i < comments.size(); i++) {
            IssueComment comment = comments.get(i);
            result[i] = new SimpleComment(comment.getDate(), comment.getAuthor(), comment.getText());
        }
        return result;
    }

    @Override
    public Image getIcon() {
        return TaskIconGroup.bug();
    }

    @Override
    public TaskType getType() {
        return myType;
    }

    @Override
    public TaskState getState() {
        return myState;
    }

    @Override
    public String getPriority() {
        return myPriority;
    }

    @Override
    public @Nullable String getAssignee() {
        return myAssignee;
    }

    @Override
    public @Nullable Date getUpdated() {
        return myUpdated;
    }

    @Override
    public @Nullable Date getCreated() {
        return myCreated;
    }

    @Override
    public boolean isClosed() {
        return myState == TaskState.RESOLVED;
    }

    @Override
    public boolean isIssue() {
        return true;
    }

    @Override
    public @Nullable String getIssueUrl() {
        return "https://tracker.sand.example/issue/" + myId;
    }
}
