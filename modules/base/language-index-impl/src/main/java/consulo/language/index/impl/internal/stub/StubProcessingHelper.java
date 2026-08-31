// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.stub;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.index.io.StorageException;
import consulo.language.index.impl.internal.UpdatableIndex;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.StubIndexKey;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author dmitrylomov
 */
public final class StubProcessingHelper extends StubProcessingHelperBase {
  private static final Logger LOG = Logger.getInstance(StubProcessingHelper.class);

  private final ThreadLocal<Set<VirtualFile>> myFilesHavingProblems = new ThreadLocal<>();

  public @Nullable <Key, Psi extends PsiElement> StubIdList retrieveStubIdList(
    StubIndexKey<Key, Psi> indexKey,
    Key key,
    VirtualFile file,
    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex,
    boolean failOnMissedKeys
  ) {
    int id = FileBasedIndex.getFileId(file);
    try {
      Map<Integer, SerializedStubTree> data = stubUpdatingIndex.getIndexedFileData(id);
      if (data.size() != 1) {
        if (failOnMissedKeys) {
          LOG.error("Stub index points to a file (" + file.getPath() + ") without indexed stub tree; actual stub count = " + data.size());
          onInternalError(file);
        }
        return null;
      }
      SerializedStubTree tree = data.values().iterator().next();
      StubIdList stubIdList =
        tree.restoreIndexedStubs(StubForwardIndexExternalizer.IdeStubForwardIndexesExternalizer.INSTANCE, indexKey, key);
      if (stubIdList == null && failOnMissedKeys) {
        LOG.error("Stub ids not found for key in index = " + indexKey.getName() + ", file " + file.getPath());
        onInternalError(file);
      }
      return stubIdList;
    }
    catch (StorageException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void onInternalError(VirtualFile file) {
    Set<VirtualFile> set = myFilesHavingProblems.get();
    if (set == null) myFilesHavingProblems.set(set = new HashSet<>());
    set.add(file);
    // requestReindex() may want to acquire write lock (for indices not requiring content loading)
    // thus, because here we are under read lock, need to use invoke later
    Application.get().invokeLater(() -> FileBasedIndex.getInstance().requestReindex(file), IdeaModalityState.nonModal());
  }

  @Nullable Set<VirtualFile> takeAccumulatedFilesWithIndexProblems() {
    Set<VirtualFile> filesWithProblems = myFilesHavingProblems.get();
    if (filesWithProblems != null) myFilesHavingProblems.set(null);
    return filesWithProblems;
  }
}
