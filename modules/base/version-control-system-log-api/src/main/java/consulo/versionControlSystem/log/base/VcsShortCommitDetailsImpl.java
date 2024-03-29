package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.VcsUser;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.List;

public class VcsShortCommitDetailsImpl extends TimedVcsCommitImpl implements VcsShortCommitDetails {

  @Nonnull
  private final String mySubject;
  @Nonnull
  private final VcsUser myAuthor;
  @Nonnull
  private final VirtualFile myRoot;
  @Nonnull
  private final VcsUser myCommitter;
  private final long myAuthorTime;

  public VcsShortCommitDetailsImpl(@Nonnull Hash hash, @Nonnull List<Hash> parents, long commitTime, @Nonnull VirtualFile root,
                                   @Nonnull String subject, @Nonnull VcsUser author, @Nonnull VcsUser committer, long authorTime) {
    super(hash, parents, commitTime);
    myRoot = root;
    mySubject = subject;
    myAuthor = author;
    myCommitter = committer;
    myAuthorTime = authorTime;
  }

  @Nonnull
  @Override
  public VirtualFile getRoot() {
    return myRoot;
  }

  @Override
  @Nonnull
  public final String getSubject() {
    return mySubject;
  }

  @Override
  @Nonnull
  public final VcsUser getAuthor() {
    return myAuthor;
  }

  @Nonnull
  @Override
  public VcsUser getCommitter() {
    return myCommitter;
  }

  @Override
  public long getAuthorTime() {
    return myAuthorTime;
  }

  @Override
  public long getCommitTime() {
    return getTimestamp();
  }

  @Override
  public String toString() {
    return getId().toShortString() + "(" + getSubject() + ")";
  }
}
