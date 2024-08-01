/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.versionControlSystem.change;

import consulo.application.AccessRule;
import consulo.application.ApplicationManager;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author max
 */
public class ChangesUtil {
  private static final Key<Boolean> INTERNAL_OPERATION_KEY = Key.create("internal vcs operation");

  public static final HashingStrategy<FilePath> CASE_SENSITIVE_FILE_PATH_HASHING_STRATEGY = new HashingStrategy<>() {
    @Override
    public int hashCode(@Nullable FilePath path) {
      return path != null ? Objects.hash(path.getPath(), path.isDirectory()) : 0;
    }

    @Override
    public boolean equals(@Nullable FilePath path1, @Nullable FilePath path2) {
      if (path1 == path2) return true;
      if (path1 == null || path2 == null) return false;

      return path1.isDirectory() == path2.isDirectory() && path1.getPath().equals(path2.getPath());
    }
  };

  public static final Comparator<LocalChangeList> CHANGELIST_COMPARATOR =
    Comparator.<LocalChangeList>comparingInt(list -> list.isDefault() ? -1 : 0)
              .thenComparing(list -> list.getName(), String::compareToIgnoreCase);

  private ChangesUtil() {
  }

  @Nonnull
  public static FilePath getFilePath(@Nonnull Change change) {
    ContentRevision revision = change.getAfterRevision();
    if (revision == null) {
      revision = change.getBeforeRevision();
      assert revision != null;
    }

    return revision.getFile();
  }

  @Nullable
  public static FilePath getBeforePath(@Nonnull Change change) {
    ContentRevision revision = change.getBeforeRevision();
    return revision == null ? null : revision.getFile();
  }

  @Nullable
  public static FilePath getAfterPath(@Nonnull Change change) {
    ContentRevision revision = change.getAfterRevision();
    return revision == null ? null : revision.getFile();
  }

  @Nullable
  public static AbstractVcs getVcsForChange(@Nonnull Change change, @Nonnull Project project) {
    AbstractVcs result = ChangeListManager.getInstance(project).getVcsFor(change);

    return result != null ? result : ProjectLevelVcsManager.getInstance(project).getVcsFor(getFilePath(change));
  }

  @Nonnull
  public static Set<AbstractVcs> getAffectedVcses(@Nonnull Collection<Change> changes, @Nonnull Project project) {
    return ContainerUtil.map2SetNotNull(changes, change -> getVcsForChange(change, project));
  }

  @Nullable
  public static AbstractVcs getVcsForFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    return ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
  }

  @Nullable
  public static AbstractVcs getVcsForFile(@Nonnull File file, @Nonnull Project project) {
    return ProjectLevelVcsManager.getInstance(project).getVcsFor(VcsUtil.getFilePath(file));
  }

  @Nonnull
  public static List<FilePath> getPaths(@Nonnull Collection<Change> changes) {
    return iteratePaths(changes).toList();
  }

  @Nonnull
  public static JBIterable<FilePath> iteratePaths(@Nonnull Iterable<? extends Change> changes) {
    return JBIterable.from(changes).flatMap(ChangesUtil::iteratePathsCaseSensitive);
  }

  public static @Nonnull JBIterable<FilePath> iteratePathsCaseSensitive(@Nonnull Change change) {
    FilePath beforePath = getBeforePath(change);
    FilePath afterPath = getAfterPath(change);

    if (equalsCaseSensitive(beforePath, afterPath)) {
      return JBIterable.of(beforePath);
    }
    else {
      return JBIterable.of(beforePath, afterPath).filterNotNull();
    }
  }

  public static boolean equalsCaseSensitive(@Nullable FilePath path1, @Nullable FilePath path2) {
    return CASE_SENSITIVE_FILE_PATH_HASHING_STRATEGY.equals(path1, path2);
  }

  @Nonnull
  public static List<File> getIoFilesFromChanges(@Nonnull Collection<Change> changes) {
    return getAllPaths(changes.stream()).map(FilePath::getIOFile).distinct().collect(toList());
  }

  @Nonnull
  public static Stream<FilePath> getAllPaths(@Nonnull Stream<Change> changes) {
    return changes.flatMap(change -> Stream.of(getBeforePath(change), getAfterPath(change))).filter(Objects::nonNull);
  }

  /**
   * @deprecated Use {@link ChangesUtil#getAfterRevisionsFiles(Stream)}.
   */
  @SuppressWarnings("unused") // Required for compatibility with external plugins.
  @Deprecated
  @Nonnull
  public static VirtualFile[] getFilesFromChanges(@Nonnull Collection<Change> changes) {
    return getAfterRevisionsFiles(changes.stream()).toArray(VirtualFile[]::new);
  }

  @Nonnull
  public static Stream<VirtualFile> getAfterRevisionsFiles(@Nonnull Stream<Change> changes) {
    return getAfterRevisionsFiles(changes, false);
  }

  @Nonnull
  public static Stream<VirtualFile> getAfterRevisionsFiles(@Nonnull Stream<Change> changes, boolean refresh) {
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();

    return changes.map(Change::getAfterRevision)
                  .filter(Objects::nonNull)
                  .map(ContentRevision::getFile)
                  .map(path -> refresh ? fileSystem.refreshAndFindFileByPath(path.getPath()) : path.getVirtualFile())
                  .filter(Objects::nonNull)
                  .filter(VirtualFile::isValid);
  }

  @Nonnull
  public static Navigatable[] getNavigatableArray(@Nonnull Project project, @Nonnull VirtualFile[] files) {
    return getNavigatableArray(project, Stream.of(files));
  }

  @Nonnull
  public static Navigatable[] getNavigatableArray(@Nonnull Project project, @Nonnull Stream<VirtualFile> files) {
    return files.filter(file -> !file.isDirectory())
                .map(file -> OpenFileDescriptorFactory.getInstance(project).builder(file).build())
                .toArray(Navigatable[]::new);
  }

  @Nullable
  public static ChangeList getChangeListIfOnlyOne(@Nonnull Project project, @Nullable Change[] changes) {
    ChangeListManager manager = ChangeListManager.getInstance(project);
    String changeListName = manager.getChangeListNameIfOnlyOne(changes);

    return changeListName == null ? null : manager.findChangeList(changeListName);
  }

  public static FilePath getCommittedPath(@Nonnull Project project, FilePath filePath) {
    // check if the file has just been renamed (IDEADEV-15494)
    Change change = ChangeListManager.getInstance(project).getChange(filePath);
    if (change != null) {
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();
      if (beforeRevision != null && afterRevision != null && !beforeRevision.getFile()
                                                                            .equals(afterRevision.getFile()) && afterRevision.getFile()
                                                                                                                             .equals(
                                                                                                                               filePath)) {
        filePath = beforeRevision.getFile();
      }
    }
    return filePath;
  }

  public static FilePath getLocalPath(@Nonnull Project project, FilePath filePath) {
    // check if the file has just been renamed (IDEADEV-15494)
    ThrowableComputable<Change, RuntimeException> action = () -> {
      if (project.isDisposed()) throw new ProcessCanceledException();
      return ChangeListManager.getInstance(project).getChange(filePath);
    };
    Change change = AccessRule.read(action);
    if (change != null) {
      ContentRevision beforeRevision = change.getBeforeRevision();
      ContentRevision afterRevision = change.getAfterRevision();
      if (beforeRevision != null && afterRevision != null && !beforeRevision.getFile()
                                                                            .equals(afterRevision.getFile()) && beforeRevision.getFile()
                                                                                                                              .equals(
                                                                                                                                filePath)) {
        return afterRevision.getFile();
      }
    }
    return filePath;
  }

  @Nullable
  public static VirtualFile findValidParentUnderReadAction(@Nonnull FilePath path) {
    VirtualFile file = path.getVirtualFile();
    return file != null ? file : getValidParentUnderReadAction(path);
  }

  @Nullable
  public static VirtualFile findValidParentAccurately(@Nonnull FilePath filePath) {
    VirtualFile result = filePath.getVirtualFile();

    if (result == null && !ApplicationManager.getApplication().isReadAccessAllowed()) {
      result = LocalFileSystem.getInstance().refreshAndFindFileByPath(filePath.getPath());
    }
    if (result == null) {
      result = getValidParentUnderReadAction(filePath);
    }

    return result;
  }

  @Nullable
  private static VirtualFile getValidParentUnderReadAction(@Nonnull FilePath filePath) {
    ThrowableComputable<VirtualFile, RuntimeException> action = () -> {
      VirtualFile result = null;
      FilePath parent = filePath;
      LocalFileSystem lfs = LocalFileSystem.getInstance();

      while (result == null && parent != null) {
        result = lfs.findFileByPath(parent.getPath());
        parent = parent.getParentPath();
      }

      return result;
    };
    return AccessRule.read(action);
  }

  @Nullable
  public static String getProjectRelativePath(@Nonnull Project project, @Nullable File fileName) {
    if (fileName == null) return null;
    VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return fileName.toString();
    String relativePath = FileUtil.getRelativePath(VirtualFileUtil.virtualToIoFile(baseDir), fileName);
    if (relativePath != null) return relativePath;
    return fileName.toString();
  }

  public static boolean isBinaryContentRevision(@Nullable ContentRevision revision) {
    return revision instanceof BinaryContentRevision && !revision.getFile().isDirectory();
  }

  public static boolean isBinaryChange(@Nonnull Change change) {
    return isBinaryContentRevision(change.getBeforeRevision()) || isBinaryContentRevision(change.getAfterRevision());
  }

  public static boolean isTextConflictingChange(@Nonnull Change change) {
    FileStatus status = change.getFileStatus();
    return FileStatus.MERGED_WITH_CONFLICTS.equals(status) || FileStatus.MERGED_WITH_BOTH_CONFLICTS.equals(status);
  }

  @FunctionalInterface
  public interface PerVcsProcessor<T> {
    void process(@Nonnull AbstractVcs vcs, @Nonnull List<T> items);
  }

  @FunctionalInterface
  public interface VcsSeparator<T> {
    @Nullable
    AbstractVcs getVcsFor(@Nonnull T item);
  }

  public static <T> void processItemsByVcs(@Nonnull Collection<T> items,
                                           @Nonnull VcsSeparator<T> separator,
                                           @Nonnull PerVcsProcessor<T> processor) {
    final Map<AbstractVcs, List<T>> changesByVcs = new HashMap<>();

    for (T item : items) {
      final AbstractVcs vcs = separator.getVcsFor(item);
      if (vcs != null) {
        List<T> vcsChanges = changesByVcs.get(vcs);
        if (vcsChanges == null) {
          vcsChanges = new ArrayList<>();
          changesByVcs.put(vcs, vcsChanges);
        }
        vcsChanges.add(item);
      }
    }

    for (Map.Entry<AbstractVcs, List<T>> entry : changesByVcs.entrySet()) {
      processor.process(entry.getKey(), entry.getValue());
    }
  }

  public static void processChangesByVcs(@Nonnull Project project,
                                         @Nonnull Collection<Change> changes,
                                         @Nonnull PerVcsProcessor<Change> processor) {
    processItemsByVcs(changes, change -> getVcsForChange(change, project), processor);
  }

  public static void processVirtualFilesByVcs(@Nonnull Project project,
                                              @Nonnull Collection<VirtualFile> files,
                                              @Nonnull PerVcsProcessor<VirtualFile> processor) {
    processItemsByVcs(files, file -> getVcsForFile(file, project), processor);
  }

  public static void processFilePathsByVcs(@Nonnull Project project,
                                           @Nonnull Collection<FilePath> files,
                                           @Nonnull PerVcsProcessor<FilePath> processor) {
    processItemsByVcs(files, filePath -> getVcsForFile(filePath.getIOFile(), project), processor);
  }

  @Nonnull
  public static List<File> filePathsToFiles(@Nonnull Collection<FilePath> filePaths) {
    return filePaths.stream().map(FilePath::getIOFile).collect(toList());
  }

  public static boolean hasFileChanges(@Nonnull Collection<Change> changes) {
    return changes.stream().map(ChangesUtil::getFilePath).anyMatch(path -> !path.isDirectory());
  }

  public static void markInternalOperation(@Nonnull Iterable<Change> changes, boolean set) {
    for (Change change : changes) {
      VirtualFile file = change.getVirtualFile();
      if (file != null) {
        markInternalOperation(file, set);
      }
    }
  }

  public static void markInternalOperation(@Nonnull VirtualFile file, boolean set) {
    file.putUserData(INTERNAL_OPERATION_KEY, set);
  }

  public static boolean isInternalOperation(@Nonnull VirtualFile file) {
    return Boolean.TRUE.equals(file.getUserData(INTERNAL_OPERATION_KEY));
  }

  /**
   * Find common ancestor for changes (included both before and after files)
   */
  @Nullable
  public static File findCommonAncestor(@Nonnull Collection<Change> changes) {
    File ancestor = null;
    for (Change change : changes) {
      File currentChangeAncestor = getCommonBeforeAfterAncestor(change);
      if (currentChangeAncestor == null) return null;
      if (ancestor == null) {
        ancestor = currentChangeAncestor;
      }
      else {
        ancestor = FileUtil.findAncestor(ancestor, currentChangeAncestor);
        if (ancestor == null) return null;
      }
    }
    return ancestor;
  }

  @Nullable
  private static File getCommonBeforeAfterAncestor(@Nonnull Change change) {
    FilePath before = getBeforePath(change);
    FilePath after = getAfterPath(change);
    return before == null ? ObjectUtil.assertNotNull(after)
                                      .getIOFile() : after == null ? before.getIOFile() : FileUtil.findAncestor(before.getIOFile(),
                                                                                                                after.getIOFile());
  }

  @Nonnull
  public static JBIterable<VirtualFile> iterateAfterRevisionFiles(@Nonnull Iterable<? extends Change> changes) {
    return JBIterable.from(changes)
                     .map(ChangesUtil::getAfterPath)
                     .filter(Objects::nonNull)
                     .map(FilePath::getVirtualFile)
                     .filter(Objects::nonNull);
  }

  public static boolean hasMeaningfulChangelists(@Nonnull Project project) {
    ChangeListManager changeListManager = ChangeListManager.getInstance(project);
    if (!changeListManager.areChangeListsEnabled()) return false;

    if (VcsApplicationSettings.getInstance().CREATE_CHANGELISTS_AUTOMATICALLY) return true;

    List<LocalChangeList> changeLists = changeListManager.getChangeLists();
    if (changeLists.size() != 1) return true;
    if (!changeLists.get(0).isBlank()) return true;

    return false;
  }
}
