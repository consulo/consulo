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

import consulo.application.Application;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.MultiMap;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsTaskHandler;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class DvcsTaskHandler<R extends Repository> extends VcsTaskHandler {
    
    private final AbstractRepositoryManager<R> myRepositoryManager;
    
    private final Project myProject;
    
    private final String myBranchType;

    protected DvcsTaskHandler(
        AbstractRepositoryManager<R> repositoryManager,
        Project project,
        String branchType
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
    @RequiredUIAccess
    public CompletableFuture<TaskInfo> startNewTask(String taskName) {
        List<R> repositories = myRepositoryManager.getRepositories();
        List<R> problems = ContainerUtil.filter(
            repositories,
            repository -> hasBranch(repository, new TaskInfo(taskName, Collections.emptyList()))
        );
        if (problems.isEmpty()) {
            return CompletableFuture.completedFuture(createTask(taskName, List.of(), repositories));
        }
        if (Application.get().isUnitTestMode()) {
            return CompletableFuture.completedFuture(checkoutExisting(taskName, problems, repositories));
        }

        UIAccess uiAccess = UIAccess.current();
        return MessageBoxes.yesNo()
            .asWarning()
            .richText()
            .title(DistributedVcsLocalize.dialogTitleAlreadyExists(StringUtil.capitalize(myBranchType)))
            .text(DistributedVcsLocalize.dialogMessageFollowingRepositoriesAlreadyHaveSpecified(
                myBranchType,
                taskName,
                StringUtil.join(problems, "<br>"),
                myBranchType
            ))
            .showAsync(myProject)
            .thenApplyAsync(
                yes -> Boolean.TRUE.equals(yes)
                    ? checkoutExisting(taskName, problems, repositories)
                    : createTask(taskName, List.of(), ContainerUtil.filter(repositories, r -> !problems.contains(r))),
                uiAccess
            );
    }

    @RequiredUIAccess
    private TaskInfo checkoutExisting(String taskName, List<R> problems, List<R> repositories) {
        checkout(taskName, problems, null);
        return createTask(taskName, problems, ContainerUtil.filter(repositories, r -> !problems.contains(r)));
    }

    @RequiredUIAccess
    private TaskInfo createTask(String taskName, List<R> existing, List<R> newBranches) {
        if (!newBranches.isEmpty()) {
            checkoutAsNewBranch(taskName, newBranches);
        }
        List<R> all = new ArrayList<>(existing);
        all.addAll(newBranches);
        return new TaskInfo(taskName, ContainerUtil.map(all, Repository::getPresentableUrl));
    }

    @Override
    public void switchToTask(TaskInfo taskInfo, @Nullable Runnable invokeAfter) {
        String branchName = taskInfo.getName();
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
    public void closeTask(TaskInfo taskInfo, TaskInfo original) {
        checkout(
            original.getName(),
            getRepositories(original.getRepositories()),
            () -> mergeAndClose(taskInfo.getName(), getRepositories(taskInfo.getRepositories()))
        );
    }

    @Override
    public boolean isSyncEnabled() {
        return myRepositoryManager.isSyncEnabled();
    }

    
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

    
    private List<R> getRepositories(Collection<String> urls) {
        List<R> repositories = myRepositoryManager.getRepositories();
        return ContainerUtil.mapNotNull(
            urls,
            s -> ContainerUtil.find(repositories, repository -> s.equals(repository.getPresentableUrl()))
        );
    }

    protected abstract void checkout(String taskName, List<R> repos, @Nullable Runnable callInAwtLater);

    protected abstract void checkoutAsNewBranch(String name, List<R> repositories);

    protected abstract @Nullable String getActiveBranch(R repository);

    
    protected abstract Iterable<TaskInfo> getAllBranches(R repository);

    protected abstract void mergeAndClose(String branch, List<R> repositories);

    protected abstract boolean hasBranch(R repository, TaskInfo name);
}
