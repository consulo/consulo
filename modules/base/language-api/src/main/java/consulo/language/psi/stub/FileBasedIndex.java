// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ReadAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.PerApplicationInstance;
import consulo.application.util.function.Processor;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.content.ContentIterator;
import consulo.content.scope.SearchScope;
import consulo.index.io.ID;
import consulo.language.util.IncorrectOperationException;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author dmitrylomov
 * @see FileBasedIndexExtension
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class FileBasedIndex {
  private static final PerApplicationInstance<FileBasedIndex> ourInstance = PerApplicationInstance.of(FileBasedIndex.class);

  @Nonnull
  public static FileBasedIndex getInstance() {
    return ourInstance.get();
  }

  public abstract void iterateIndexableFiles(@Nonnull ContentIterator processor, @Nonnull Project project, ProgressIndicator indicator);

  public void iterateIndexableFilesConcurrently(
    @Nonnull ContentIterator processor,
    @Nonnull Project project,
    @Nonnull ProgressIndicator indicator
  ) {
    iterateIndexableFiles(processor, project, indicator);
  }

  /**
   * @return the file which the current thread is indexing right now, or {@code null} if current thread isn't indexing.
   */
  @Nullable
  public abstract VirtualFile getFileBeingCurrentlyIndexed();

  @Nullable
  public DumbModeAccessType getCurrentDumbModeAccessType() {
    throw new UnsupportedOperationException();
  }

  public abstract void registerIndexableSet(@Nonnull IndexableFileSet set, @Nullable Project project);

  public abstract void removeIndexableSet(@Nonnull IndexableFileSet set);

  public static int getFileId(@Nonnull final VirtualFile file) {
    if (file instanceof VirtualFileWithId) {
      return ((VirtualFileWithId)file).getId();
    }

    throw new IllegalArgumentException(
      "Virtual file doesn't support id: " + file + ", implementation class: " + file.getClass().getName()
    );
  }

  // note: upsource implementation requires access to Project here, please don't remove
  public abstract VirtualFile findFileById(Project project, int id);

  public void requestRebuild(@Nonnull ID<?, ?> indexId) {
    requestRebuild(indexId, new Throwable());
  }

  @Nonnull
  public abstract <K, V> List<V> getValues(@Nonnull ID<K, V> indexId, @Nonnull K dataKey, @Nonnull SearchScope filter);

  @Nonnull
  public abstract <K, V> Collection<VirtualFile> getContainingFiles(
    @Nonnull ID<K, V> indexId,
    @Nonnull K dataKey,
    @Nonnull SearchScope filter
  );

  /**
   * @return {@code false} if ValueProcessor.process() returned {@code false};
   * {@code true} otherwise or if ValueProcessor was not called at all
   */
  public abstract <K, V> boolean processValues(
    @Nonnull ID<K, V> indexId,
    @Nonnull K dataKey,
    @Nullable VirtualFile inFile,
    @Nonnull ValueProcessor<? super V> processor,
    @Nonnull SearchScope filter
  );

  /**
   * @return {@code false} if ValueProcessor.process() returned {@code false};
   * {@code true} otherwise or if ValueProcessor was not called at all
   */
  public <K, V> boolean processValues(
    @Nonnull ID<K, V> indexId,
    @Nonnull K dataKey,
    @Nullable VirtualFile inFile,
    @Nonnull ValueProcessor<? super V> processor,
    @Nonnull SearchScope filter,
    @Nullable IdFilter idFilter
  ) {
    return processValues(indexId, dataKey, inFile, processor, filter);
  }

  public abstract <K, V> long getIndexModificationStamp(@Nonnull ID<K, V> indexId, @Nonnull Project project);

  public abstract <K, V> boolean processFilesContainingAllKeys(
    @Nonnull ID<K, V> indexId,
    @Nonnull Collection<? extends K> dataKeys,
    @Nonnull SearchScope filter,
    @Nullable Predicate<? super V> valueChecker,
    @Nonnull Predicate<? super VirtualFile> processor
  );

  /**
   * It is guaranteed to return data which is up-to-date within the given project.
   * Keys obtained from the files which do not belong to the project specified may not be up-to-date or even exist.
   */
  @Nonnull
  public abstract <K> Collection<K> getAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Project project);

  /**
   * DO NOT CALL DIRECTLY IN CLIENT CODE
   * The method is internal to indexing engine end is called internally. The method is public due to implementation details
   */
  //@ApiStatus.Internal
  public abstract <K> void ensureUpToDate(@Nonnull ID<K, ?> indexId, @Nullable Project project, @Nullable SearchScope filter);

  public abstract void requestRebuild(@Nonnull ID<?, ?> indexId, Throwable throwable);

  public abstract <K> void scheduleRebuild(@Nonnull ID<K, ?> indexId, @Nonnull Throwable e);

  public abstract void requestReindex(@Nonnull VirtualFile file);

  public abstract <K, V> boolean getFilesWithKey(
    @Nonnull ID<K, V> indexId,
    @Nonnull Set<? extends K> dataKeys,
    @Nonnull Processor<? super VirtualFile> processor,
    @Nonnull SearchScope filter
  );

  /**
   * Executes command and allow its to have an index access in dumb mode.
   * Inside the command it's safe to call index related stuff and
   * {@link IndexNotReadyException} are not expected to be happen here.
   *
   * <p> In smart mode, the behavior is similar to direct command execution
   *
   * @param command            - a command to execute
   * @param dumbModeAccessType - defines in which manner command should be executed. Does a client expect only reliable data
   */
  public void ignoreDumbMode(@Nonnull Runnable command, @Nonnull DumbModeAccessType dumbModeAccessType) {
    ignoreDumbMode(dumbModeAccessType, () -> {
      command.run();
      return null;
    });
  }

  public <T, E extends Throwable> T ignoreDumbMode(
    @Nonnull DumbModeAccessType dumbModeAccessType,
    @Nonnull ThrowableComputable<T, E> computable
  ) throws E {
    throw new UnsupportedOperationException();
  }

  /**
   * It is guaranteed to return data which is up-to-date within the given project.
   */
  public abstract <K> boolean processAllKeys(@Nonnull ID<K, ?> indexId, @Nonnull Processor<? super K> processor, @Nullable Project project);

  public <K> boolean processAllKeys(
    @Nonnull ID<K, ?> indexId,
    @Nonnull Processor<? super K> processor,
    @Nonnull SearchScope scope,
    @Nullable IdFilter idFilter
  ) {
    return processAllKeys(indexId, processor, ((ProjectAwareSearchScope)scope).getProject());
  }

  //@ApiStatus.Experimental
  @Nonnull
  public abstract <K, V> Map<K, V> getFileData(@Nonnull ID<K, V> id, @Nonnull VirtualFile virtualFile, @Nonnull Project project);

  public static void iterateRecursively(
    @Nullable final VirtualFile root,
    @Nonnull final ContentIterator processor,
    @Nullable final ProgressIndicator indicator,
    @Nullable final Set<? super VirtualFile> visitedRoots,
    @Nullable final ProjectFileIndex projectFileIndex
  ) {
    if (root == null) {
      return;
    }

    VirtualFileUtil.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@Nonnull VirtualFile file) {
        if (!acceptsFile(file)) return false;
        if (file.is(VFileProperty.SYMLINK)) {
          if (!Registry.is("indexer.follows.symlinks")) return false;
          VirtualFile canonicalFile = file.getCanonicalFile();

          if (canonicalFile != null) {
            if (!acceptsFile(canonicalFile)) return false;
          }
        }
        if (indicator != null) indicator.checkCanceled();

        processor.processFile(file);
        return true;
      }

      private boolean acceptsFile(@Nonnull VirtualFile file) {
        if (visitedRoots != null && !root.equals(file) && file.isDirectory() && !visitedRoots.add(file)) {
          return false;
        }
        return projectFileIndex == null || !ReadAction.compute(() -> projectFileIndex.isExcluded(file));
      }
    });
  }

  public void invalidateCaches() {
    throw new IncorrectOperationException();
  }

  @Nullable
  public abstract IdFilter createProjectIndexableFiles(@Nullable Project project);

  @FunctionalInterface
  public interface ValueProcessor<V> {
    /**
     * @param value a value to process
     * @param file  the file the value came from
     * @return {@code false} if no further processing is needed, {@code true} otherwise
     */
    boolean process(@Nonnull VirtualFile file, V value);
  }

  @FunctionalInterface
  public interface InputFilter {
    boolean acceptInput(@Nullable Project project, @Nonnull VirtualFile file);
  }

  /**
   * @see DefaultFileTypeSpecificInputFilter
   */
  public interface FileTypeSpecificInputFilter extends InputFilter {
    void registerFileTypesUsedForIndexing(@Nonnull Consumer<FileType> fileTypeSink);
  }

  /**
   * @deprecated inline true
   */
  @Deprecated
  public static final boolean ourEnableTracingOfKeyHashToVirtualFileMapping = true;

  private static final boolean ourDisableIndexAccessDuringDumbMode =
    SystemProperties.getBooleanProperty("idea.disable.index.access.during.dumb.mode", false);

  public static boolean isIndexAccessDuringDumbModeEnabled() {
    return !ourDisableIndexAccessDuringDumbMode;
  }
}
