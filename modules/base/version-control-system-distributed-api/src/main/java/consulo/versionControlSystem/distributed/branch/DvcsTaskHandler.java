/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.branch;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.VcsTaskHandler;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public abstract class DvcsTaskHandler<R extends Repository> extends VcsTaskHandler {
    @Nonnull
    private final AbstractRepositoryManager<R> myRepositoryManager;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final String myBranchType;

    protected DvcsTaskHandler(
        @Nonnull AbstractRepositoryManager<R> repositoryManager,
        @Nonnull Project project,
        @Nonnull String branchType
    ) {
        myRepositoryManager = repositoryManager;
        myProject = project;
        myBranchType = branchType;
    }

    @Override
    public boolean isEnabled() {
        return !myRepositoryManager.getRepositories().isEmpty();
    }

    @Override
    public TaskInfo startNewTask(@Nonnull final String taskName) {
        List<R> repositories = myRepositoryManager.getRepositories();
        List<R> problems = ContainerUtil.filter(repositories, new Condition<R>() {
            @Override
            public boolean value(R repository) {
                return hasBranch(repository, new TaskInfo(taskName, Collections.emptyList()));
            }
        });
        List<R> map = new ArrayList<>();
        if (!problems.isEmpty()) {
            if (ApplicationManager.getApplication().isUnitTestMode() ||
                Messages.showDialog(myProject,
                    "<html>The following repositories already have specified " + myBranchType + "<b>" + taskName + "</b>:<br>" +
                        StringUtil.join(problems, "<br>") + ".<br>" +
                        "Do you want to checkout existing " + myBranchType + "?", StringUtil.capitalize(myBranchType) + " Already Exists",
                    new String[]{Messages.YES_BUTTON, Messages.NO_BUTTON}, 0,
                    Messages.getWarningIcon()
                ) == 0) {
                checkout(taskName, problems, null);
                map.addAll(problems);
            }
        }
        repositories.removeAll(problems);
        if (!repositories.isEmpty()) {
            checkoutAsNewBranch(taskName, repositories);
        }

        map.addAll(repositories);
        return new TaskInfo(taskName, ContainerUtil.map(map, r -> r.getPresentableUrl()));
    }

    @Override
    public void switchToTask(@Nonnull TaskInfo taskInfo, @Nullable Runnable invokeAfter) {
        final String branchName = taskInfo.getName();
        List<R> repositories = new ArrayList<>(getRepositories(taskInfo.getRepositories()));
        List<R> notFound = ContainerUtil.filter(repositories, repository -> !hasBranch(repository, taskInfo));
        if (!notFound.isEmpty()) {
            checkoutAsNewBranch(branchName, notFound);
        }
        repositories.removeAll(notFound);
        if (!repositories.isEmpty()) {
            checkout(branchName, repositories, invokeAfter);
        }
    }

    @Override
    public void closeTask(@Nonnull final TaskInfo taskInfo, @Nonnull TaskInfo original) {
        checkout(original.getName(), getRepositories(original.getRepositories()), new Runnable() {
            @Override
            public void run() {
                mergeAndClose(taskInfo.getName(), getRepositories(taskInfo.getRepositories()));
            }
        });
    }

    @Override
    public boolean isSyncEnabled() {
        return myRepositoryManager.isSyncEnabled();
    }

    @Nonnull
    @Override
    public TaskInfo[] getCurrentTasks() {
        List<R> repositories = myRepositoryManager.getRepositories();
        Map<String, TaskInfo> tasks = FactoryMap.create(key -> new TaskInfo(key, new ArrayList<>()));
        for (R repository : repositories) {
            String branch = getActiveBranch(repository);
            if (branch != null) {
                tasks.get(branch).getRepositories().add(repository.getPresentableUrl());
            }
        }
        if (tasks.size() == 0) {
            return new TaskInfo[0];
        }
        if (isSyncEnabled()) {
            return new TaskInfo[]{tasks.values().iterator().next()};
        }
        else {
            return tasks.values().toArray(new TaskInfo[tasks.values().size()]);
        }
    }

    @Override
    public TaskInfo[] getAllExistingTasks() {
        List<R> repositories = myRepositoryManager.getRepositories();
        MultiMap<String, TaskInfo> tasks = new MultiMap<>();
        for (R repository : repositories) {
            for (TaskInfo branch : getAllBranches(repository)) {
                tasks.putValue(branch.getName(), branch);
            }
        }
        return ContainerUtil.map2Array(tasks.entrySet(), TaskInfo.class, entry -> {
            Set<String> repositories1 = new HashSet<>();
            boolean remote = false;
            for (TaskInfo info : entry.getValue()) {
                remote |= info.isRemote();
                repositories1.addAll(info.getRepositories());
            }
            return new TaskInfo(entry.getKey(), repositories1, remote);
        });
    }

    @Nonnull
    private List<R> getRepositories(@Nonnull Collection<String> urls) {
        final List<R> repositories = myRepositoryManager.getRepositories();
        return ContainerUtil.mapNotNull(
            urls,
            s -> ContainerUtil.find(repositories, repository -> s.equals(repository.getPresentableUrl()))
        );
    }

    protected abstract void checkout(@Nonnull String taskName, @Nonnull List<R> repos, @Nullable Runnable callInAwtLater);

    protected abstract void checkoutAsNewBranch(@Nonnull String name, @Nonnull List<R> repositories);

    @Nullable
    protected abstract String getActiveBranch(R repository);

    @Nonnull
    protected abstract Iterable<TaskInfo> getAllBranches(@Nonnull R repository);

    protected abstract void mergeAndClose(@Nonnull String branch, @Nonnull List<R> repositories);

    protected abstract boolean hasBranch(@Nonnull R repository, @Nonnull TaskInfo name);
}
