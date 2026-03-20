package consulo.versionControlSystem.log.impl.internal.data;

import consulo.disposer.Disposable;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The CommitDetailsGetter is responsible for getting {@link VcsFullCommitDetails complete commit details} from the cache or from the VCS.
 */
public class CommitDetailsGetter extends AbstractDataGetter<VcsFullCommitDetails> {

  CommitDetailsGetter(VcsLogStorage hashMap,
                      Map<VirtualFile, VcsLogProvider> logProviders,
                      VcsLogIndex index,
                      Disposable parentDisposable) {
    super(hashMap, logProviders, new VcsCommitCache<>(), index, parentDisposable);
  }

  @Override
  protected @Nullable VcsFullCommitDetails getFromAdditionalCache(int commitId) {
    return null;
  }

  
  @Override
  protected List<? extends VcsFullCommitDetails> readDetails(VcsLogProvider logProvider, VirtualFile root,
                                                             List<String> hashes) throws VcsException {
    return VcsLogUtil.getDetails(logProvider, root, hashes);
  }
}
