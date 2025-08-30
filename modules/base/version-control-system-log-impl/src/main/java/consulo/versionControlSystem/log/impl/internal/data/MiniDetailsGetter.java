package consulo.versionControlSystem.log.impl.internal.data;

import consulo.disposer.Disposable;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class MiniDetailsGetter extends AbstractDataGetter<VcsShortCommitDetails> {

  @Nonnull
  private final TopCommitsCache myTopCommitsDetailsCache;

  MiniDetailsGetter(@Nonnull VcsLogStorage hashMap,
                    @Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                    @Nonnull TopCommitsCache topCommitsDetailsCache,
                    @Nonnull VcsLogIndex index,
                    @Nonnull Disposable parentDisposable) {
    super(hashMap, logProviders, new VcsCommitCache<>(), index, parentDisposable);
    myTopCommitsDetailsCache = topCommitsDetailsCache;
  }

  @Nullable
  @Override
  protected VcsShortCommitDetails getFromAdditionalCache(int commitId) {
    return myTopCommitsDetailsCache.get(commitId);
  }

  @Nonnull
  @Override
  protected List<? extends VcsShortCommitDetails> readDetails(@Nonnull VcsLogProvider logProvider, @Nonnull VirtualFile root,
                                                              @Nonnull List<String> hashes) throws VcsException {
    return logProvider.readShortDetails(root, hashes);
  }
}
