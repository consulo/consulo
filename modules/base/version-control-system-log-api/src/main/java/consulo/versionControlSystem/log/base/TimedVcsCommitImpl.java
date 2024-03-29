package consulo.versionControlSystem.log.base;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.TimedVcsCommit;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * <p>We suppose that the Hash is the unique identifier of the Commit,
 * i. e. it is the only value that should be checked in equals() and hashCode().</p>
 * <p>equals() and hashCode() are made final to ensure that any descendants of this class are considered equal
 * if and only if their hashes are equals.</p>
 * <p>It is highly recommended to use this standard implementation of the VcsCommit because of the above reasons.</p>
 *
 * @author erokhins
 * @author Kirill Likhodedov
 */
public class TimedVcsCommitImpl implements TimedVcsCommit {

  @Nonnull
  private final Hash myHash;
  @Nonnull
  private final List<Hash> myParents;
  private final long myTime;

  public TimedVcsCommitImpl(@Nonnull Hash hash, @Nonnull List<Hash> parents, long timeStamp) {
    myHash = hash;
    myParents = parents;
    myTime = timeStamp;
  }

  @Override
  @Nonnull
  public final Hash getId() {
    return myHash;
  }

  @Override
  @Nonnull
  public final List<Hash> getParents() {
    return myParents;
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TimedVcsCommitImpl)) {
      return false;
    }
    return myHash.equals(((TimedVcsCommitImpl)obj).myHash);
  }

  @Override
  public final int hashCode() {
    return myHash.hashCode();
  }

  @Override
  public String toString() {
    return myHash.toShortString() + "|-" + StringUtil.join(ContainerUtil.map(myParents, hash -> hash.toShortString()), ",") + ":" + myTime;
  }

  @Override
  public final long getTimestamp() {
    return myTime;
  }
}
