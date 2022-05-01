package consulo.ide.impl.idea.vcs.log.data;

import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.vcs.VcsException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.vcs.log.VcsLogProvider;
import consulo.ide.impl.idea.vcs.log.VcsShortCommitDetails;
import consulo.ide.impl.idea.vcs.log.data.index.VcsLogIndex;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
