/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.task;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountManager;
import consulo.project.Project;
import consulo.task.issue.IssueTracker;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class describes bug-tracking server.
 *
 * @author Dmitry Avdeev
 * @see TaskRepositoryType
 * @see TaskRepositoryOptions
 */
public abstract class TaskRepository {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z]+\\-\\d+");

    private final TaskRepositoryType<?> myType;
    private final Project myProject;
    private final TaskRepositoryOptions myOptions;

    protected TaskRepository(TaskRepositoryType<?> type, Project project, TaskRepositoryOptions options) {
        myType = type;
        myProject = project;
        myOptions = options;
    }

    public TaskRepositoryOptions getOptions() {
        return myOptions;
    }

    public final TaskRepositoryType<?> getRepositoryType() {
        return myType;
    }

    public final Project getProject() {
        return myProject;
    }

    public abstract IssueTracker getIssueTracker();

    public @Nullable Account getAccount() {
        String accountId = myOptions.accountId;
        if (accountId == null) {
            return null;
        }
        return myProject.getApplication().getInstance(AccountManager.class).findAccount(accountId, myProject);
    }

    public CompletableFuture<AccountConnection> connect() {
        Account account = getAccount();
        if (account == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not logged in to " + getServerUrl()));
        }
        return myProject.getApplication().getInstance(AccountManager.class).connect(account);
    }

    public String getServerUrl() {
        return myOptions.serverUrl;
    }

    public boolean isConfigured() {
        return getAccount() != null;
    }

    public String getPresentableName() {
        String serverUrl = getServerUrl();
        Account account = getAccount();
        String server = StringUtil.isEmpty(serverUrl) ? myType.getPresentableName().get() : serverUrl;
        return account == null ? server : account.getName() + " @ " + server;
    }

    public Image getIcon() {
        return myType.getIcon();
    }

    public @Nullable String extractId(String taskName) {
        Matcher matcher = ID_PATTERN.matcher(taskName);
        return matcher.find() ? matcher.group() : null;
    }

    public @Nullable String getTaskComment(Task task) {
        if (!myOptions.shouldFormatCommitMessage) {
            return null;
        }
        return myOptions.commitMessageFormat.replace("{id}", task.getPresentableId()).replace("{summary}", task.getSummary());
    }

    public String getComment() {
        return "{id} (e.g. FOO-001), {summary}, {number} (e.g. 001), {project} (e.g. FOO)";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + myType.getId() + ", " + getServerUrl() + ")";
    }
}
