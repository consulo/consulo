package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.util.function.Computable;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsUser;
import consulo.versionControlSystem.log.base.VcsCommitMetadataImpl;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Fake {@link VcsCommitMetadataImpl} implementation that is used to indicate that details are not ready for the moment,
 * they are being retrieved from the VCS.
 *
 * @author Kirill Likhodedov
 */
public class LoadingDetails implements VcsFullCommitDetails {
  private static final VcsUserImpl STUB_USER = new VcsUserImpl("", "");
  private static final String LOADING = "Loading...";

  
  private final Computable<CommitId> myCommitIdComputable;
  private final long myLoadingTaskIndex;
  @Nullable private volatile CommitId myCommitId;

  public LoadingDetails(Computable<CommitId> commitIdComputable, long loadingTaskIndex) {
    myCommitIdComputable = commitIdComputable;
    myLoadingTaskIndex = loadingTaskIndex;
  }


  protected CommitId getCommitId() {
    if (myCommitId == null) {
      myCommitId = myCommitIdComputable.compute();
    }
    return myCommitId;
  }

  public long getLoadingTaskIndex() {
    return myLoadingTaskIndex;
  }

  
  @Override
  public Collection<Change> getChanges() {
    return List.of();
  }

  
  @Override
  public String getFullMessage() {
    return "";
  }

  
  @Override
  public VirtualFile getRoot() {
    return getCommitId().getRoot();
  }

  
  @Override
  public String getSubject() {
    return LOADING;
  }

  
  @Override
  public VcsUser getAuthor() {
    return STUB_USER;
  }

  
  @Override
  public VcsUser getCommitter() {
    return STUB_USER;
  }

  @Override
  public long getAuthorTime() {
    return -1;
  }

  @Override
  public long getCommitTime() {
    return -1;
  }

  
  @Override
  public Hash getId() {
    return getCommitId().getHash();
  }

  
  @Override
  public List<Hash> getParents() {
    return List.of();
  }

  @Override
  public long getTimestamp() {
    return -1;
  }
}
