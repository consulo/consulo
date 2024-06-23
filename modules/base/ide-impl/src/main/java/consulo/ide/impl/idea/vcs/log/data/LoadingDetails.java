package consulo.ide.impl.idea.vcs.log.data;

import consulo.application.util.function.Computable;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.log.base.VcsCommitMetadataImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.VcsUser;
import consulo.versionControlSystem.impl.internal.VcsUserImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fake {@link VcsCommitMetadataImpl} implementation that is used to indicate that details are not ready for the moment,
 * they are being retrieved from the VCS.
 *
 * @author Kirill Likhodedov
 */
public class LoadingDetails implements VcsFullCommitDetails {
  private static final VcsUserImpl STUB_USER = new VcsUserImpl("", "");
  private static final String LOADING = "Loading...";

  @Nonnull
  private final Supplier<CommitId> myCommitIdComputable;
  private final long myLoadingTaskIndex;
  @Nullable private volatile CommitId myCommitId;

  public LoadingDetails(@Nonnull Supplier<CommitId> commitIdComputable, long loadingTaskIndex) {
    myCommitIdComputable = commitIdComputable;
    myLoadingTaskIndex = loadingTaskIndex;
  }

  protected CommitId getCommitId() {
    if (myCommitId == null) {
      myCommitId = myCommitIdComputable.get();
    }
    return myCommitId;
  }

  public long getLoadingTaskIndex() {
    return myLoadingTaskIndex;
  }

  @Nonnull
  @Override
  public Collection<Change> getChanges() {
    return List.of();
  }

  @Nonnull
  @Override
  public String getFullMessage() {
    return "";
  }

  @Nonnull
  @Override
  public VirtualFile getRoot() {
    return getCommitId().getRoot();
  }

  @Nonnull
  @Override
  public String getSubject() {
    return LOADING;
  }

  @Nonnull
  @Override
  public VcsUser getAuthor() {
    return STUB_USER;
  }

  @Nonnull
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

  @Nonnull
  @Override
  public Hash getId() {
    return getCommitId().getHash();
  }

  @Nonnull
  @Override
  public List<Hash> getParents() {
    return List.of();
  }

  @Override
  public long getTimestamp() {
    return -1;
  }
}
