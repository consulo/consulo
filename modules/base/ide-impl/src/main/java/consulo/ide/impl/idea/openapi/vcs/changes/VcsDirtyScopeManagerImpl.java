/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.impl.DefaultVcsRootPolicy;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.change.VcsInvalidated;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author max
 */
@Singleton
@ServiceImpl
public class VcsDirtyScopeManagerImpl extends VcsDirtyScopeManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(VcsDirtyScopeManagerImpl.class);

  private final Project myProject;
  private final ChangeListManager myChangeListManager;
  private final ProjectLevelVcsManagerImpl myVcsManager;
  private final VcsGuess myGuess;

  private final DirtBuilder myDirtBuilder;
  @Nullable
  private DirtBuilder myDirtInProgress;

  private boolean myReady;
  private final Object LOCK = new Object();

  @Inject
  public VcsDirtyScopeManagerImpl(Project project, ChangeListManager changeListManager, ProjectLevelVcsManager vcsManager) {
    myProject = project;
    myChangeListManager = changeListManager;
    myVcsManager = (ProjectLevelVcsManagerImpl)vcsManager;

    myGuess = new VcsGuess(myProject);
    myDirtBuilder = new DirtBuilder(myGuess);
  }

  void startListenForChanges() {
    ReadAction.run(() -> {
      boolean ready = false;
      synchronized (LOCK) {
        if (!myProject.isDisposed()) {
          myReady = ready = true;
        }
      }
      if (ready) {
        VcsDirtyScopeVfsListener.install(myProject);
        markEverythingDirty();
      }
    });
  }

  @Override
  public void markEverythingDirty() {
    if ((!myProject.isOpen()) || myProject.isDisposed() || myVcsManager.getAllActiveVcss().length == 0) return;

    if (LOG.isDebugEnabled()) {
      LOG.debug("everything dirty: " + findFirstInterestingCallerClass());
    }

    synchronized (LOCK) {
      if (myReady) {
        myDirtBuilder.everythingDirty();
      }
    }

    myChangeListManager.scheduleUpdate();
  }

  @Override
  public void dispose() {
    synchronized (LOCK) {
      myReady = false;
      myDirtBuilder.reset();
      myDirtInProgress = null;
    }
  }

  @Nonnull
  private MultiMap<AbstractVcs, FilePath> groupByVcs(@Nullable final Collection<FilePath> from) {
    if (from == null) return MultiMap.empty();
    MultiMap<AbstractVcs, FilePath> map = MultiMap.createSet();
    for (FilePath path : from) {
      AbstractVcs vcs = myGuess.getVcsForDirty(path);
      if (vcs != null) {
        map.putValue(vcs, path);
      }
    }
    return map;
  }

  @Override
  public void filePathsDirty(@Nullable final Collection<FilePath> filesDirty, @Nullable final Collection<FilePath> dirsRecursivelyDirty) {
    try {
      final MultiMap<AbstractVcs, FilePath> filesConverted = groupByVcs(filesDirty);
      final MultiMap<AbstractVcs, FilePath> dirsConverted = groupByVcs(dirsRecursivelyDirty);
      if (filesConverted.isEmpty() && dirsConverted.isEmpty()) return;

      if (LOG.isDebugEnabled()) {
        LOG.debug("dirty files: " + toString(filesConverted) + "; dirty dirs: " + toString(dirsConverted) + "; " + findFirstInterestingCallerClass());
      }

      boolean hasSomethingDirty;
      synchronized (LOCK) {
        if (!myReady) return;
        markDirty(myDirtBuilder, filesConverted, false);
        markDirty(myDirtBuilder, dirsConverted, true);
        hasSomethingDirty = !myDirtBuilder.isEmpty();
      }

      if (hasSomethingDirty) {
        myChangeListManager.scheduleUpdate();
      }
    }
    catch (ProcessCanceledException ignore) {
    }
  }

  private static void markDirty(@Nonnull DirtBuilder dirtBuilder, @Nonnull MultiMap<AbstractVcs, FilePath> filesOrDirs, boolean recursively) {
    for (AbstractVcs vcs : filesOrDirs.keySet()) {
      for (FilePath path : filesOrDirs.get(vcs)) {
        if (recursively) {
          dirtBuilder.addDirtyDirRecursively(vcs, path);
        }
        else {
          dirtBuilder.addDirtyFile(vcs, path);
        }
      }
    }
  }

  @Override
  public void filesDirty(@Nullable final Collection<VirtualFile> filesDirty, @Nullable final Collection<VirtualFile> dirsRecursivelyDirty) {
    filePathsDirty(toFilePaths(filesDirty), toFilePaths(dirsRecursivelyDirty));
  }

  @Nonnull
  private static Collection<FilePath> toFilePaths(@Nullable Collection<VirtualFile> files) {
    if (files == null) return Collections.emptyList();
    return ContainerUtil.map(files, virtualFile -> VcsUtil.getFilePath(virtualFile));
  }

  @Override
  public void fileDirty(@Nonnull final VirtualFile file) {
    fileDirty(VcsUtil.getFilePath(file));
  }

  @Override
  public void fileDirty(@Nonnull final FilePath file) {
    filePathsDirty(Collections.singleton(file), null);
  }

  @Override
  public void dirDirtyRecursively(final VirtualFile dir, final boolean scheduleUpdate) {
    dirDirtyRecursively(dir);
  }

  @Override
  public void dirDirtyRecursively(final VirtualFile dir) {
    dirDirtyRecursively(VcsUtil.getFilePath(dir));
  }

  @Override
  public void dirDirtyRecursively(final FilePath path) {
    filePathsDirty(null, Collections.singleton(path));
  }

  @Override
  @Nullable
  public VcsInvalidated retrieveScopes() {
    DirtBuilder dirtBuilder;
    synchronized (LOCK) {
      if (!myReady) return null;
      dirtBuilder = new DirtBuilder(myDirtBuilder);
      myDirtInProgress = dirtBuilder;
      myDirtBuilder.reset();
    }
    return calculateInvalidated(dirtBuilder);
  }

  @Nonnull
  private VcsInvalidated calculateInvalidated(@Nonnull DirtBuilder dirt) {
    MultiMap<AbstractVcs, FilePath> files = dirt.getFilesForVcs();
    MultiMap<AbstractVcs, FilePath> dirs = dirt.getDirsForVcs();
    if (dirt.isEverythingDirty()) {
      dirs.putAllValues(getEverythingDirtyRoots());
    }
    Set<AbstractVcs> keys = ContainerUtil.union(files.keySet(), dirs.keySet());

    Map<AbstractVcs, VcsDirtyScopeImpl> scopes = ContainerUtil.newHashMap();
    for (AbstractVcs key : keys) {
      VcsDirtyScopeImpl scope = new VcsDirtyScopeImpl(key, myProject);
      scopes.put(key, scope);
      scope.addDirtyData(dirs.get(key), files.get(key));
    }

    return new VcsInvalidated(new ArrayList<>(scopes.values()), dirt.isEverythingDirty());
  }

  @Nonnull
  private MultiMap<AbstractVcs, FilePath> getEverythingDirtyRoots() {
    MultiMap<AbstractVcs, FilePath> dirtyRoots = MultiMap.createSet();
    dirtyRoots.putAllValues(groupByVcs(toFilePaths(DefaultVcsRootPolicy.getInstance(myProject).getDirtyRoots())));

    List<VcsDirectoryMapping> mappings = myVcsManager.getDirectoryMappings();
    for (VcsDirectoryMapping mapping : mappings) {
      if (!mapping.isDefaultMapping() && mapping.getVcs() != null) {
        AbstractVcs vcs = myVcsManager.findVcsByName(mapping.getVcs());
        if (vcs != null) {
          dirtyRoots.putValue(vcs, VcsUtil.getFilePath(mapping.getDirectory(), true));
        }
      }
    }
    return dirtyRoots;
  }

  @Override
  public void changesProcessed() {
    synchronized (LOCK) {
      myDirtInProgress = null;
    }
  }

  @Nonnull
  @Override
  public Collection<FilePath> whatFilesDirty(@Nonnull final Collection<FilePath> files) {
    DirtBuilder dirtBuilder;
    DirtBuilder dirtBuilderInProgress;
    synchronized (LOCK) {
      if (!myReady) return Collections.emptyList();
      dirtBuilder = new DirtBuilder(myDirtBuilder);
      dirtBuilderInProgress = myDirtInProgress != null ? new DirtBuilder(myDirtInProgress) : new DirtBuilder(myGuess);
    }

    VcsInvalidated invalidated = calculateInvalidated(dirtBuilder);
    VcsInvalidated inProgress = calculateInvalidated(dirtBuilderInProgress);
    Collection<FilePath> result = ContainerUtil.newArrayList();
    for (FilePath fp : files) {
      if (invalidated.isFileDirty(fp) || inProgress.isFileDirty(fp)) {
        result.add(fp);
      }
    }
    return result;
  }

  @Nonnull
  private static String toString(@Nonnull final MultiMap<AbstractVcs, FilePath> filesByVcs) {
    return StringUtil.join(filesByVcs.keySet(), vcs -> vcs.getName() + ": " + StringUtil.join(filesByVcs.get(vcs), FilePath::getPath, "\n"), "\n");
  }

  @Nullable
  private static Class findFirstInterestingCallerClass() {
    for (int i = 1; i <= 5; i++) {
      Class clazz = ReflectionUtil.findCallerClass(i);
      if (clazz == null || !clazz.getName().contains(VcsDirtyScopeManagerImpl.class.getName())) return clazz;
    }
    return null;
  }
}
