// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.stubs;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author dmitrylomov
 */
public final class StubProcessingHelper extends StubProcessingHelperBase {
  private final ThreadLocal<Set<VirtualFile>> myFilesHavingProblems = new ThreadLocal<>();

  @Override
  protected void onInternalError(final VirtualFile file) {
    Set<VirtualFile> set = myFilesHavingProblems.get();
    if (set == null) myFilesHavingProblems.set(set = new HashSet<>());
    set.add(file);
    // requestReindex() may want to acquire write lock (for indices not requiring content loading)
    // thus, because here we are under read lock, need to use invoke later
    Application.get().invokeLater(() -> FileBasedIndex.getInstance().requestReindex(file), IdeaModalityState.nonModal());
  }

  @Nullable
  Set<VirtualFile> takeAccumulatedFilesWithIndexProblems() {
    Set<VirtualFile> filesWithProblems = myFilesHavingProblems.get();
    if (filesWithProblems != null) myFilesHavingProblems.set(null);
    return filesWithProblems;
  }
}
