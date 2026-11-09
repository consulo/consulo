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

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.CustomTaskState;
import consulo.task.LocalTask;
import consulo.task.Task;
import consulo.task.TaskRepository;
import consulo.task.TaskState;
import consulo.task.TaskType;
import consulo.task.issue.IssueAssigneeSupport;
import consulo.task.issue.IssueAttachment;
import consulo.task.issue.IssueAttachmentSupport;
import consulo.task.issue.IssueCapability;
import consulo.task.issue.IssueComment;
import consulo.task.issue.IssueCommentSupport;
import consulo.task.issue.IssueDetails;
import consulo.task.issue.IssueField;
import consulo.task.issue.IssueLink;
import consulo.task.issue.IssueLinkSupport;
import consulo.task.issue.IssuePage;
import consulo.task.issue.IssuePrioritySupport;
import consulo.task.issue.IssueQuery;
import consulo.task.issue.IssueStateSupport;
import consulo.task.issue.IssueTracker;
import consulo.task.issue.IssueWorkLog;
import consulo.task.issue.IssueWorkLogSupport;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

public class SandTaskRepository extends TaskRepository {
    private static final List<String> USERS = List.of("alice", "bob", "carol", "dave");
    private static final List<String> PRIORITIES = List.of("Highest", "High", "Medium", "Low", "Lowest");
    private static final List<String> LINK_TYPES = List.of("blocks", "is blocked by", "relates to", "duplicates");

    private final Map<String, SandTask> myTasks = new LinkedHashMap<>();
    private final Tracker myTracker = new Tracker();

    public SandTaskRepository(SandRepositoryType type, Project project, SandRepositoryOptions options) {
        super(type, project, options);
        String key = StringUtil.isEmpty(options.projectKey) ? "SAND" : options.projectKey;
        long now = System.currentTimeMillis();
        long hour = 60L * 60L * 1000L;

        SandTask first = add(new SandTask(key + "-1", "Test sand", TaskType.BUG, TaskState.OPEN,
            "Check that the sand tracker lists issues.\nSecond line of the description.", new Date(now - hour), "alice"));
        first.setAssignee("bob");
        first.setPriority("High");
        first.comments.add(new IssueComment("c1", "alice", new Date(now - 2 * hour), "Reproduced on the latest build."));
        first.comments.add(new IssueComment("c2", "bob", new Date(now - hour), "Fix is in review."));
        first.workLogs.add(new IssueWorkLog("w1", "bob", new Date(now - hour), "1h 30m", "Investigation"));
        first.attachments.add(new IssueAttachment("a1", "screenshot.png", 48213, "https://tracker.sand.example/attachment/a1"));
        first.attachments.add(new IssueAttachment("a2", "build.log", 2048, "https://tracker.sand.example/attachment/a2"));

        SandTask hello = add(new SandTask(key + "-100", "Hello", TaskType.FEATURE, TaskState.IN_PROGRESS,
            "Issue that is being worked on.", new Date(now - 3 * hour), "carol"));
        hello.setAssignee("alice");
        hello.links.add(new IssueLink("l1", "blocks", first.getId(), first.getSummary()));
        SandTask subtask = new SandTask(key + "-100.1", "Write tests for Hello", TaskType.FEATURE, TaskState.OPEN, null, new Date(now - 2 * hour), "carol");
        hello.subtasks.add(subtask);
        add(subtask);

        add(new SandTask(key + "-2", "Closed", TaskType.BUG, TaskState.RESOLVED,
            "Resolved issue, shown only with closed issues included.", new Date(now - 30 * hour), "dave"));
        if (!options.assignedToMeOnly) {
            add(new SandTask(key + "-101", "Somebody else's task", TaskType.OTHER, TaskState.OPEN, null, new Date(now - 50 * hour), "dave"));
        }
        for (int i = 200; i < 330; i++) {
            TaskType type1 = i % 3 == 0 ? TaskType.FEATURE : TaskType.BUG;
            add(new SandTask(key + "-" + i, "Generated issue " + i, type1, i % 7 == 0 ? TaskState.RESOLVED : TaskState.OPEN,
                "Generated description " + i, new Date(now - i * hour), USERS.get(i % USERS.size())));
        }
    }

    private SandTask add(SandTask task) {
        task.setRepository(this);
        myTasks.put(task.getId(), task);
        return task;
    }

    @Override
    public SandRepositoryOptions getOptions() {
        return (SandRepositoryOptions) super.getOptions();
    }

    @Override
    public IssueTracker getIssueTracker() {
        return myTracker;
    }

    private static <T> CompletableFuture<T> later(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS));
    }

    private SandTask find(Task task) {
        SandTask sandTask = myTasks.get(task.getId());
        if (sandTask == null) {
            throw new IllegalStateException("Unknown issue " + task.getId());
        }
        return sandTask;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private class Tracker implements IssueTracker, IssueStateSupport, IssueWorkLogSupport, IssueCommentSupport,
        IssueAttachmentSupport, IssueLinkSupport, IssueAssigneeSupport, IssuePrioritySupport {
        @Override
        public CompletableFuture<IssuePage> search(IssueQuery query, int offset, int limit) {
            return later(() -> {
                if (getAccount() == null) {
                    throw new IllegalStateException("Not logged in to " + getServerUrl());
                }
                String text = StringUtil.notNullize(query.getText()).toLowerCase(Locale.ROOT);
                List<Task> found = new ArrayList<>();
                for (SandTask task : myTasks.values()) {
                    if (task.getId().contains(".")) {
                        continue;
                    }
                    if (!query.isIncludeClosed() && task.isClosed()) {
                        continue;
                    }
                    if (!text.isEmpty() && !task.getId().toLowerCase(Locale.ROOT).contains(text) && !task.getSummary().toLowerCase(Locale.ROOT).contains(text)) {
                        continue;
                    }
                    found.add(task);
                }
                int from = Math.min(offset, found.size());
                int to = Math.min(from + limit, found.size());
                return new IssuePage(found.subList(from, to), to < found.size());
            });
        }

        @Override
        public CompletableFuture<@Nullable Task> findIssue(String id) {
            return later(() -> myTasks.get(id));
        }

        @Override
        public CompletableFuture<IssueDetails> loadDetails(Task task) {
            return later(() -> {
                SandTask sandTask = find(task);
                List<IssueField> fields = new ArrayList<>();
                fields.add(new IssueField(LocalizeValue.of("Reporter"), sandTask.getReporter()));
                fields.add(new IssueField(LocalizeValue.of("Priority"), sandTask.getPriority()));
                fields.add(new IssueField(LocalizeValue.of("Assignee"), StringUtil.notNullize(sandTask.getAssignee(), "Unassigned")));
                fields.add(new IssueField(LocalizeValue.of("Labels"), "sand, demo"));
                return new IssueDetails(sandTask, fields, sandTask.comments, sandTask.workLogs, sandTask.attachments, sandTask.links,
                    new ArrayList<>(sandTask.subtasks));
            });
        }

        @Override
        public Set<IssueCapability> getCapabilities() {
            return EnumSet.allOf(IssueCapability.class);
        }

        @Override
        public CompletableFuture<Set<CustomTaskState>> getAvailableStates(Task task) {
            return later(() -> {
                Set<CustomTaskState> states = new LinkedHashSet<>();
                for (TaskState state : getRepositoryType().getPossibleTaskStates()) {
                    states.add(CustomTaskState.fromPredefined(state));
                }
                return states;
            });
        }

        @Override
        public CompletableFuture<?> setState(Task task, CustomTaskState state) {
            return later(() -> {
                TaskState predefined = state.asPredefined();
                if (predefined != null) {
                    find(task).setState(predefined);
                }
                return state;
            });
        }

        @Override
        public CompletableFuture<?> logWork(LocalTask task, String timeSpent, String comment) {
            return addWorkLog(task, timeSpent, comment);
        }

        @Override
        public CompletableFuture<?> addWorkLog(Task task, String timeSpent, String comment) {
            return later(() -> find(task).workLogs.add(new IssueWorkLog(newId(), "me", new Date(), timeSpent, comment)));
        }

        @Override
        public CompletableFuture<?> editWorkLog(Task task, IssueWorkLog workLog, String timeSpent, String comment) {
            return later(() -> {
                List<IssueWorkLog> logs = find(task).workLogs;
                int index = indexOf(logs, workLog.getId(), IssueWorkLog::getId);
                logs.set(index, new IssueWorkLog(workLog.getId(), workLog.getAuthor(), workLog.getDate(), timeSpent, comment));
                return workLog;
            });
        }

        @Override
        public CompletableFuture<?> deleteWorkLog(Task task, IssueWorkLog workLog) {
            return later(() -> find(task).workLogs.removeIf(log -> log.getId().equals(workLog.getId())));
        }

        @Override
        public CompletableFuture<?> addComment(Task task, String text) {
            return later(() -> find(task).comments.add(new IssueComment(newId(), "me", new Date(), text)));
        }

        @Override
        public CompletableFuture<?> editComment(Task task, IssueComment comment, String text) {
            return later(() -> {
                List<IssueComment> comments = find(task).comments;
                int index = indexOf(comments, comment.getId(), IssueComment::getId);
                comments.set(index, new IssueComment(comment.getId(), comment.getAuthor(), comment.getDate(), text));
                return comment;
            });
        }

        @Override
        public CompletableFuture<?> deleteComment(Task task, IssueComment comment) {
            return later(() -> find(task).comments.removeIf(item -> item.getId().equals(comment.getId())));
        }

        @Override
        public CompletableFuture<?> addAttachment(Task task, Path file) {
            return later(() -> {
                long size;
                try {
                    size = Files.size(file);
                }
                catch (Exception e) {
                    size = 0;
                }
                return find(task).attachments.add(new IssueAttachment(newId(), file.getFileName().toString(), size, null));
            });
        }

        @Override
        public CompletableFuture<?> deleteAttachment(Task task, IssueAttachment attachment) {
            return later(() -> find(task).attachments.removeIf(item -> item.getId().equals(attachment.getId())));
        }

        @Override
        public CompletableFuture<List<String>> getLinkTypes(Task task) {
            return later(() -> LINK_TYPES);
        }

        @Override
        public CompletableFuture<?> addLink(Task task, String type, String issueId) {
            return later(() -> {
                SandTask target = myTasks.get(issueId);
                if (target == null) {
                    throw new IllegalArgumentException("Issue " + issueId + " does not exist");
                }
                return find(task).links.add(new IssueLink(newId(), type, target.getId(), target.getSummary()));
            });
        }

        @Override
        public CompletableFuture<?> deleteLink(Task task, IssueLink link) {
            return later(() -> find(task).links.removeIf(item -> item.getId().equals(link.getId())));
        }

        @Override
        public CompletableFuture<List<String>> getAssignableUsers(Task task) {
            return later(() -> USERS);
        }

        @Override
        public CompletableFuture<?> assign(Task task, @Nullable String user) {
            return later(() -> {
                find(task).setAssignee(user);
                return task;
            });
        }

        @Override
        public CompletableFuture<List<String>> getPriorities(Task task) {
            return later(() -> PRIORITIES);
        }

        @Override
        public CompletableFuture<?> setPriority(Task task, String priority) {
            return later(() -> {
                find(task).setPriority(priority);
                return task;
            });
        }
    }

    private static <T> int indexOf(List<T> list, String id, Function<T, String> idGetter) {
        for (int i = 0; i < list.size(); i++) {
            if (idGetter.apply(list.get(i)).equals(id)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown id " + id);
    }
}
