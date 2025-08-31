// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.util.SystemInfo;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.VcsDirtyScopeBuilder;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.util.VcsRootIterator;
import consulo.versionControlSystem.impl.internal.util.RecursiveFilePathSet;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsFileUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

public final class VcsDirtyScopeImpl extends VcsModifiableDirtyScope implements VcsDirtyScopeBuilder {
  private final Map<VirtualFile, Set<FilePath>> myDirtyFiles = new HashMap<>();
  private final Map<VirtualFile, RecursiveFilePathSet> myDirtyDirectoriesRecursively = new HashMap<>();
  private final Set<FilePath> myAllVcsRoots = new HashSet<>();
  private final Set<VirtualFile> myAffectedVcsRoots = new HashSet<>();
  @Nonnull
  private final Project myProject;
  private final ProjectLevelVcsManager myVcsManager;
  @Nonnull
  private final AbstractVcs myVcs;
  private boolean myWasEverythingDirty;

  private final @Nullable HashingStrategy<FilePath> myHashingStrategy;
  private final boolean myCaseSensitive;

  public VcsDirtyScopeImpl(@Nonnull AbstractVcs vcs) {
    myVcs = vcs;
    myProject = vcs.getProject();
    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    myHashingStrategy = VcsDirtyScopeManagerImpl.getDirtyScopeHashingStrategy(myVcs);
    myCaseSensitive = myVcs.needsCaseSensitiveDirtyScope() || SystemInfo.isFileSystemCaseSensitive;
    myAllVcsRoots.addAll(ContainerUtil.map(myVcsManager.getRootsUnderVcsWithoutFiltering(myVcs), root -> VcsUtil.getFilePath(root)));
  }

  @Override
  public Collection<VirtualFile> getAffectedContentRoots() {
    return myAffectedVcsRoots;
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  @Override
  public AbstractVcs getVcs() {
    return myVcs;
  }

  @Override
  public Set<FilePath> getDirtyFiles() {
    Set<FilePath> result = newFilePathsSet();
    for (Set<FilePath> paths : myDirtyFiles.values()) {
      result.addAll(paths);
    }
    for (Set<FilePath> paths : myDirtyFiles.values()) {
      for (FilePath filePath : paths) {
        VirtualFile vFile = filePath.getVirtualFile();
        if (vFile != null && vFile.isValid() && vFile.isDirectory()) {
          for (VirtualFile child : vFile.getChildren()) {
            result.add(VcsUtil.getFilePath(child));
          }
        }
      }
    }
    return result;
  }

  @Override
  public Set<FilePath> getDirtyFilesNoExpand() {
    Set<FilePath> paths = newFilePathsSet();
    for (Set<FilePath> filePaths : myDirtyFiles.values()) {
      paths.addAll(filePaths);
    }
    return paths;
  }

  @Override
  public Set<FilePath> getRecursivelyDirtyDirectories() {
    Set<FilePath> result = newFilePathsSet();
    for (RecursiveFilePathSet dirsByRoot : myDirtyDirectoriesRecursively.values()) {
      result.addAll(dirsByRoot.filePaths());
    }
    return result;
  }

  /**
   * Add file path into the sets, without removing potential duplicates.
   * See {@link #pack()}, that will be called later to perform this optimization.
   * <p>
   * Use {@link #addDirtyFile} / {@link #addDirtyDirRecursively} to add file path and remove duplicates.
   */
  @Override
  public void addDirtyPathFast(@Nonnull VirtualFile vcsRoot, @Nonnull FilePath filePath, boolean recursively) {
    myAffectedVcsRoots.add(vcsRoot);

    RecursiveFilePathSet dirsByRoot = myDirtyDirectoriesRecursively.get(vcsRoot);
    if (dirsByRoot != null && dirsByRoot.hasAncestor(filePath)) return;

    if (recursively) {
      if (dirsByRoot == null) {
        dirsByRoot = newRecursiveFilePathSet();
        myDirtyDirectoriesRecursively.put(vcsRoot, dirsByRoot);
      }
      dirsByRoot.add(filePath);
    }
    else {
      Set<FilePath> dirtyFiles = myDirtyFiles.get(vcsRoot);
      if (dirtyFiles == null) {
        dirtyFiles = newFilePathsSet();
        myDirtyFiles.put(vcsRoot, dirtyFiles);
      }
      dirtyFiles.add(filePath);
    }
  }

  @Override
  public void markEverythingDirty() {
    myWasEverythingDirty = true;
  }

  /**
   * @return VcsDirtyScope with trimmed duplicated paths from the sets.
   */
  @Override
  @Nonnull
  public VcsDirtyScopeImpl pack() {
    VcsDirtyScopeImpl copy = new VcsDirtyScopeImpl(myVcs);
    copy.myWasEverythingDirty = myWasEverythingDirty;
    for (VirtualFile root : myAffectedVcsRoots) {
      RecursiveFilePathSet rootDirs = myDirtyDirectoriesRecursively.get(root);
      Set<FilePath> rootFiles = Sets.notNullize(myDirtyFiles.get(root));

      RecursiveFilePathSet filteredDirs = removeAncestorsRecursive(rootDirs);
      Set<FilePath> filteredFiles = removeAncestorsNonRecursive(filteredDirs, rootFiles);

      copy.myAffectedVcsRoots.add(root);
      copy.myDirtyDirectoriesRecursively.put(root, filteredDirs);
      copy.myDirtyFiles.put(root, filteredFiles);
    }
    return copy;
  }

  @Nonnull
  private RecursiveFilePathSet removeAncestorsRecursive(@Nullable RecursiveFilePathSet dirs) {
    RecursiveFilePathSet result = newRecursiveFilePathSet();
    if (dirs == null) return result;

    List<FilePath> paths = ContainerUtil.sorted(dirs.filePaths(), Comparator.comparingInt(it -> it.getPath().length()));
    for (FilePath path : paths) {
      if (result.hasAncestor(path)) continue;
      result.add(path);
    }
    return result;
  }

  private @Nonnull Set<FilePath> removeAncestorsNonRecursive(@Nonnull RecursiveFilePathSet dirs,
                                                             @Nonnull Set<? extends FilePath> files) {
    Set<FilePath> result = newFilePathsSet();
    for (FilePath file : files) {
      if (dirs.hasAncestor(file)) continue;
      // if dir non-recursively + non-recursive file child -> can be truncated to dir only
      if (!file.isDirectory() && files.contains(file.getParentPath())) continue;
      result.add(file);
    }
    return result;
  }

  @Nonnull
  private Set<FilePath> newFilePathsSet() {
    return myHashingStrategy == null ? new HashSet<>() : Sets.newHashSet(myHashingStrategy);
  }

  @Nonnull
  private RecursiveFilePathSet newRecursiveFilePathSet() {
    return new RecursiveFilePathSet(myCaseSensitive);
  }

  /**
   * Add dirty directory recursively. If there are already dirty entries
   * that are descendants or ancestors for the added directory, the contained
   * entries are dropped from scope.
   *
   * @param newcomer a new directory to add
   */
  @Override
  public void addDirtyDirRecursively(FilePath newcomer) {
    VirtualFile vcsRoot = myVcsManager.getVcsRootFor(newcomer);
    if (vcsRoot == null) return;
    myAffectedVcsRoots.add(vcsRoot);

    for (Map.Entry<VirtualFile, Set<FilePath>> entry : myDirtyFiles.entrySet()) {
      VirtualFile groupRoot = entry.getKey();
      if (groupRoot != null && VirtualFileUtil.isAncestor(vcsRoot, groupRoot, false)) {
        Set<FilePath> files = entry.getValue();
        if (files != null) {
          for (Iterator<FilePath> it = files.iterator(); it.hasNext(); ) {
            FilePath oldBoy = it.next();
            if (VcsFileUtil.isAncestor(newcomer, oldBoy, false)) {
              it.remove();
            }
          }
        }
      }
    }

    RecursiveFilePathSet dirsByRoot = myDirtyDirectoriesRecursively.get(vcsRoot);
    if (dirsByRoot == null) {
      dirsByRoot = newRecursiveFilePathSet();
      myDirtyDirectoriesRecursively.put(vcsRoot, dirsByRoot);
    }
    else {
      if (dirsByRoot.hasAncestor(newcomer)) return;

      List<FilePath> toRemove = ContainerUtil.filter(dirsByRoot.filePaths(),
                                                     oldBoy -> VcsFileUtil.isAncestor(newcomer, oldBoy, false));
      for (FilePath path : toRemove) {
        dirsByRoot.remove(path);
      }
    }

    dirsByRoot.add(newcomer);
  }

  /**
   * Add dirty file to the scope. Note that file is not added if its ancestor was added as dirty recursively or if its parent is in already
   * in the dirty scope. Also immediate non-directory children are removed from the set of dirty files.
   *
   * @param newcomer a file or directory added to the dirty scope.
   */
  @Override
  public void addDirtyFile(FilePath newcomer) {
    VirtualFile vcsRoot = myVcsManager.getVcsRootFor(newcomer);
    if (vcsRoot == null) return;
    myAffectedVcsRoots.add(vcsRoot);

    RecursiveFilePathSet dirsByRoot = myDirtyDirectoriesRecursively.get(vcsRoot);
    if (dirsByRoot != null && dirsByRoot.hasAncestor(newcomer)) {
      return;
    }

    Set<FilePath> dirtyFiles = myDirtyFiles.get(vcsRoot);
    if (dirtyFiles == null) {
      dirtyFiles = newFilePathsSet();
      myDirtyFiles.put(vcsRoot, dirtyFiles);
    }
    else {
      if (newcomer.isDirectory()) {
        for (Iterator<FilePath> iterator = dirtyFiles.iterator(); iterator.hasNext(); ) {
          FilePath oldBoy = iterator.next();
          if (!oldBoy.isDirectory() &&
            (myHashingStrategy == null
              ? Objects.equals(oldBoy.getParentPath(), newcomer)
              : myHashingStrategy.equals(oldBoy.getParentPath(), newcomer))) {
            iterator.remove();
          }
        }
      }
      else if (!dirtyFiles.isEmpty()) {
        FilePath parent = newcomer.getParentPath();
        if (parent != null && dirtyFiles.contains(parent)) {
          return;
        }
      }
    }

    dirtyFiles.add(newcomer);
  }

  @Override
  public void iterate(Predicate<? super FilePath> iterator) {
    VcsRootIterator.iterate(this, iterator);
  }

  @Override
  public void iterateExistingInsideScope(Predicate<? super VirtualFile> iterator) {
    VcsRootIterator.iterateExistingInsideScope(this, iterator);
  }

  @Override
  public boolean isEmpty() {
    return myDirtyDirectoriesRecursively.isEmpty() && myDirtyFiles.isEmpty();
  }

  @Override
  public boolean belongsTo(FilePath path) {
    if (myProject.isDisposed()) return false;
    VcsRoot rootObject = myVcsManager.getVcsRootObjectFor(path);
    if (rootObject == null || rootObject.getVcs() != myVcs) {
      return false;
    }

    VirtualFile vcsRoot = rootObject.getPath();
    boolean pathIsRoot = myAllVcsRoots.contains(path);
    for (VirtualFile otherRoot : myDirtyDirectoriesRecursively.keySet()) {
      // since we don't know exact dirty mechanics, maybe we have 3 nested mappings like:
      // /root -> vcs1, /root/child -> vcs2, /root/child/inner -> vcs1, and we have file /root/child/inner/file,
      // mapping is detected as vcs1 with root /root/child/inner, but we could possibly have in scope
      // "affected root" -> /root with scope = /root recursively
      boolean strict = pathIsRoot && !myVcs.areDirectoriesVersionedItems();
      if (VirtualFileUtil.isAncestor(otherRoot, vcsRoot, strict)) {
        RecursiveFilePathSet dirsByRoot = myDirtyDirectoriesRecursively.get(otherRoot);
        if (dirsByRoot.hasAncestor(path)) {
          return true;
        }
      }
    }

    if (!myDirtyFiles.isEmpty()) {
      if (isInDirtyFiles(path, vcsRoot)) return true;

      FilePath parent = path.getParentPath();
      if (parent != null) {
        if (pathIsRoot) {
          if (isInDirtyFiles(parent)) return true;
        }
        else {
          if (isInDirtyFiles(parent, vcsRoot)) return true;
        }
      }
    }

    return false;
  }

  private boolean isInDirtyFiles(@Nonnull FilePath path) {
    VcsRoot rootObject = myVcsManager.getVcsRootObjectFor(path);
    if (rootObject == null || !myVcs.equals(rootObject.getVcs())) return false;
    return isInDirtyFiles(path, rootObject.getPath());
  }

  private boolean isInDirtyFiles(@Nonnull FilePath path, @Nonnull VirtualFile vcsRoot) {
    Set<FilePath> files = myDirtyFiles.get(vcsRoot);
    return files != null && files.contains(path);
  }

  @Override
  @NonNls
  public String toString() {
    @NonNls StringBuilder result = new StringBuilder("VcsDirtyScope[");
    if (!myDirtyFiles.isEmpty()) {
      result.append(" files: ");
      for (Set<FilePath> paths : myDirtyFiles.values()) {
        for (FilePath file : paths) {
          result.append(file).append(" ");
        }
      }
    }
    if (!myDirtyDirectoriesRecursively.isEmpty()) {
      result.append("\ndirs: ");
      for (RecursiveFilePathSet dirsByRoot : myDirtyDirectoriesRecursively.values()) {
        for (FilePath file : dirsByRoot.filePaths()) {
          result.append(file).append(" ");
        }
      }
    }
    result.append("\naffected roots: ");
    for (VirtualFile root : myAffectedVcsRoots) {
      result.append(root.getPath()).append(" ");
    }
    result.append("]");
    return result.toString();
  }

  @Override
  public boolean wasEveryThingDirty() {
    return myWasEverythingDirty;
  }
}
