/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.dvcs.repo;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.impl.VcsInitObject;
import com.intellij.openapi.vcs.impl.VcsStartupActivity;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import consulo.disposer.Disposable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * VcsRepositoryManager creates,stores and updates all Repositories information using registered {@link VcsRepositoryCreator}
 * extension point in a thread safe way.
 */
@Singleton
public class VcsRepositoryManager implements Disposable {
  static final class MyStartupActivity implements VcsStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project) {
      getInstance(project).checkAndUpdateRepositoriesCollection(null);
    }

    @Override
    public int getOrder() {
      return VcsInitObject.OTHER_INITIALIZATION.getOrder();
    }
  }

  @Nonnull
  public static VcsRepositoryManager getInstance(@Nonnull Project project) {
    return project.getInstance(VcsRepositoryManager.class);
  }

  public static final Topic<VcsRepositoryMappingListener> VCS_REPOSITORY_MAPPING_UPDATED = Topic.create("VCS repository mapping updated", VcsRepositoryMappingListener.class);

  @Nonnull
  private final ProjectLevelVcsManager myVcsManager;

  @Nonnull
  private final ReentrantReadWriteLock REPO_LOCK = new ReentrantReadWriteLock();
  @Nonnull
  private final ReentrantReadWriteLock.WriteLock MODIFY_LOCK = new ReentrantReadWriteLock().writeLock();

  @Nonnull
  private final Map<VirtualFile, Repository> myRepositories = ContainerUtil.newHashMap();
  @Nonnull
  private final Map<VirtualFile, Repository> myExternalRepositories = ContainerUtil.newHashMap();

  private volatile boolean myDisposed;

  private final Project myProject;

  private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

  @Inject
  public VcsRepositoryManager(@Nonnull Project project, @Nonnull ProjectLevelVcsManager vcsManager) {
    myProject = project;
    myVcsManager = vcsManager;
    project.getMessageBus().connect(this).subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, this::scheduleUpdate);
  }

  @Override
  public void dispose() {
    myDisposed = true;
    try {
      REPO_LOCK.writeLock().lock();
      myRepositories.clear();
    }
    finally {
      REPO_LOCK.writeLock().unlock();
    }
  }

  private void scheduleUpdate() {
    if (myUpdateAlarm.isDisposed()) return;
    myUpdateAlarm.cancelAllRequests();
    myUpdateAlarm.addRequest(() -> checkAndUpdateRepositoriesCollection(null), 0);
  }

  @Nullable
  public Repository getRepositoryForFile(@Nonnull VirtualFile file) {
    return getRepositoryForFile(file, false);
  }

  @Nullable
  public Repository getRepositoryForFileQuick(@Nonnull VirtualFile file) {
    return getRepositoryForFile(file, true);
  }

  @Nullable
  public Repository getExternalRepositoryForFile(@Nonnull VirtualFile file) {
    Map<VirtualFile, Repository> repositories = getExternalRepositories();
    for (Map.Entry<VirtualFile, Repository> entry : repositories.entrySet()) {
      if (entry.getKey().isValid() && VfsUtilCore.isAncestor(entry.getKey(), file, false)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Nullable
  public Repository getExternalRepositoryForFile(@Nonnull FilePath file) {
    Map<VirtualFile, Repository> repositories = getExternalRepositories();
    for (Map.Entry<VirtualFile, Repository> entry : repositories.entrySet()) {
      if (entry.getKey().isValid() && FileUtil.isAncestor(entry.getKey().getPath(), file.getPath(), false)) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Nullable
  public Repository getRepositoryForFile(@Nonnull VirtualFile file, boolean quick) {
    final VcsRoot vcsRoot = myVcsManager.getVcsRootObjectFor(file);
    if (vcsRoot == null) {
      return getExternalRepositoryForFile(file);
    }
    return quick ? getRepositoryForRootQuick(vcsRoot.getPath()) : getRepositoryForRoot(vcsRoot.getPath());
  }

  @Nullable
  public Repository getRepositoryForRootQuick(@Nullable VirtualFile root) {
    return getRepositoryForRoot(root, false);
  }

  @Nullable
  public Repository getRepositoryForRoot(@Nullable VirtualFile root) {
    return getRepositoryForRoot(root, true);
  }

  @Nullable
  private Repository getRepositoryForRoot(@Nullable VirtualFile root, boolean updateIfNeeded) {
    if (root == null) return null;
    Repository result;
    try {
      REPO_LOCK.readLock().lock();
      if (myDisposed) {
        throw new ProcessCanceledException();
      }
      Repository repo = myRepositories.get(root);
      result = repo != null ? repo : myExternalRepositories.get(root);
    }
    finally {
      REPO_LOCK.readLock().unlock();
    }
    // if we didn't find appropriate repository, request update mappings if needed and try again
    // may be this should not be called  from several places (for example: branch widget updating from edt).
    if (updateIfNeeded && result == null && ArrayUtil.contains(root, myVcsManager.getAllVersionedRoots())) {
      checkAndUpdateRepositoriesCollection(root);
      try {
        REPO_LOCK.readLock().lock();
        return myRepositories.get(root);
      }
      finally {
        REPO_LOCK.readLock().unlock();
      }
    }
    else {
      return result;
    }
  }

  public void addExternalRepository(@Nonnull VirtualFile root, @Nonnull Repository repository) {
    REPO_LOCK.writeLock().lock();
    try {
      myExternalRepositories.put(root, repository);
    }
    finally {
      REPO_LOCK.writeLock().unlock();
    }
  }

  public void removeExternalRepository(@Nonnull VirtualFile root) {
    REPO_LOCK.writeLock().lock();
    try {
      myExternalRepositories.remove(root);
    }
    finally {
      REPO_LOCK.writeLock().unlock();
    }
  }

  public boolean isExternal(@Nonnull Repository repository) {
    try {
      REPO_LOCK.readLock().lock();
      return !myRepositories.containsValue(repository) && myExternalRepositories.containsValue(repository);
    }
    finally {
      REPO_LOCK.readLock().unlock();
    }
  }

  @Nonnull
  public Collection<Repository> getRepositories() {
    try {
      REPO_LOCK.readLock().lock();
      return Collections.unmodifiableCollection(myRepositories.values());
    }
    finally {
      REPO_LOCK.readLock().unlock();
    }
  }

  @Nonnull
  private Map<VirtualFile, Repository> getExternalRepositories() {
    REPO_LOCK.readLock().lock();
    try {
      return new HashMap<>(myExternalRepositories);
    }
    finally {
      REPO_LOCK.readLock().unlock();
    }
  }

  // note: we are not calling this method during the project startup - it is called anyway by f.e the GitRootTracker
  private void checkAndUpdateRepositoriesCollection(@Nullable VirtualFile checkedRoot) {
    Map<VirtualFile, Repository> repositories;
    try {
      MODIFY_LOCK.lock();
      try {
        REPO_LOCK.readLock().lock();
        if (myRepositories.containsKey(checkedRoot)) return;
        repositories = ContainerUtil.newHashMap(myRepositories);
      }
      finally {
        REPO_LOCK.readLock().unlock();
      }

      Collection<VirtualFile> invalidRoots = findInvalidRoots(repositories.keySet());
      repositories.keySet().removeAll(invalidRoots);
      Map<VirtualFile, Repository> newRoots = findNewRoots(repositories.keySet());
      repositories.putAll(newRoots);

      REPO_LOCK.writeLock().lock();
      try {
        if (!myDisposed) {
          myRepositories.clear();
          myRepositories.putAll(repositories);
        }
      }
      finally {
        REPO_LOCK.writeLock().unlock();
      }
      BackgroundTaskUtil.syncPublisher(myProject, VCS_REPOSITORY_MAPPING_UPDATED).mappingChanged();
    }
    finally {
      MODIFY_LOCK.unlock();
    }
  }

  @Nonnull
  private Map<VirtualFile, Repository> findNewRoots(@Nonnull Set<VirtualFile> knownRoots) {
    Map<VirtualFile, Repository> newRootsMap = ContainerUtil.newHashMap();
    for (VcsRoot root : myVcsManager.getAllVcsRoots()) {
      VirtualFile rootPath = root.getPath();
      if (rootPath != null && !knownRoots.contains(rootPath)) {
        AbstractVcs vcs = root.getVcs();
        VcsRepositoryCreator repositoryCreator = getRepositoryCreator(vcs);
        if (repositoryCreator == null) continue;
        Repository repository = repositoryCreator.createRepositoryIfValid(rootPath);
        if (repository != null) {
          newRootsMap.put(rootPath, repository);
        }
      }
    }
    return newRootsMap;
  }

  @Nonnull
  private Collection<VirtualFile> findInvalidRoots(@Nonnull final Collection<VirtualFile> roots) {
    final VirtualFile[] validRoots = myVcsManager.getAllVersionedRoots();
    return ContainerUtil.filter(roots, file -> !ArrayUtil.contains(file, validRoots));
  }

  @Nullable
  private VcsRepositoryCreator getRepositoryCreator(@Nullable final AbstractVcs vcs) {
    if (vcs == null) return null;
    return ContainerUtil.find(VcsRepositoryCreator.EXTENSION_POINT_NAME.getExtensionList(myProject), creator -> creator.getVcsKey().equals(vcs.getKeyInstanceMethod()));
  }

  @Nonnull
  public String toString() {
    return "RepositoryManager{myRepositories: " + myRepositories + '}';
  }
}
