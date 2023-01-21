package consulo.ide.impl.idea.vcs.log.data;

import consulo.versionControlSystem.VcsException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.ide.impl.idea.vcs.log.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The CommitDetailsGetter is responsible for getting {@link VcsFullCommitDetails complete commit details} from the cache or from the VCS.
 */
public class CommitDetailsGetter extends AbstractDataGetter<VcsFullCommitDetails> {

  CommitDetailsGetter(@Nonnull VcsLogStorage hashMap,
                      @Nonnull Map<VirtualFile, VcsLogProvider> logProviders,
                      @Nonnull VcsLogIndex index,
                      @Nonnull Disposable parentDisposable) {
    super(hashMap, logProviders, new VcsCommitCache<>(), index, parentDisposable);
  }

  @Nullable
  @Override
  protected VcsFullCommitDetails getFromAdditionalCache(int commitId) {
    return null;
  }

  @Nonnull
  @Override
  protected List<? extends VcsFullCommitDetails> readDetails(@Nonnull VcsLogProvider logProvider, @Nonnull VirtualFile root,
                                                             @Nonnull List<String> hashes) throws VcsException {
    return VcsLogUtil.getDetails(logProvider, root, hashes);
  }
}
