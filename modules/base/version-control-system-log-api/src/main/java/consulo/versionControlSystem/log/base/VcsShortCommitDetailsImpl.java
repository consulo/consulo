package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.VcsUser;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;

public class VcsShortCommitDetailsImpl extends TimedVcsCommitImpl implements VcsShortCommitDetails {

  
  private final String mySubject;
  
  private final VcsUser myAuthor;
  
  private final VirtualFile myRoot;
  
  private final VcsUser myCommitter;
  private final long myAuthorTime;

  public VcsShortCommitDetailsImpl(Hash hash, List<Hash> parents, long commitTime, VirtualFile root,
                                   String subject, VcsUser author, VcsUser committer, long authorTime) {
    super(hash, parents, commitTime);
    myRoot = root;
    mySubject = subject;
    myAuthor = author;
    myCommitter = committer;
    myAuthorTime = authorTime;
  }

  
  @Override
  public VirtualFile getRoot() {
    return myRoot;
  }

  @Override
  
  public final String getSubject() {
    return mySubject;
  }

  @Override
  
  public final VcsUser getAuthor() {
    return myAuthor;
  }

  
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
