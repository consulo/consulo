package consulo.versionControlSystem.log.impl.internal.data;

import consulo.disposer.Disposable;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogStorage;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.impl.internal.data.index.VcsLogIndex;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class MiniDetailsGetter extends AbstractDataGetter<VcsShortCommitDetails> {

  
  private final TopCommitsCache myTopCommitsDetailsCache;

  MiniDetailsGetter(VcsLogStorage hashMap,
                    Map<VirtualFile, VcsLogProvider> logProviders,
                    TopCommitsCache topCommitsDetailsCache,
                    VcsLogIndex index,
                    Disposable parentDisposable) {
    super(hashMap, logProviders, new VcsCommitCache<>(), index, parentDisposable);
    myTopCommitsDetailsCache = topCommitsDetailsCache;
  }

  @Override
  protected @Nullable VcsShortCommitDetails getFromAdditionalCache(int commitId) {
    return myTopCommitsDetailsCache.get(commitId);
  }

  
  @Override
  protected List<? extends VcsShortCommitDetails> readDetails(VcsLogProvider logProvider, VirtualFile root,
                                                              List<String> hashes) throws VcsException {
    return logProvider.readShortDetails(root, hashes);
  }
}
