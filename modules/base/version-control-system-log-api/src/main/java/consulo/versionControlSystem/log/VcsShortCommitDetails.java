package consulo.versionControlSystem.log;

import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Returns the basic level of commit meta-data: author, time, subject. These details will be displayed in the log table.
 * <p/>
 * An instance of this object can be obtained via
 * {@link VcsLogObjectsFactory#createShortDetails(Hash, List, long, VirtualFile, String, String, String)
 * VcsLogObjectsFactory#createShortDetails}.
 * <p/>
 * It is not recommended to create a custom implementation of this interface, but if you need it, <b>make sure to implement {@code equals()}
 * and {@code hashcode()} so that they consider only the Hash</b>, i.e. two VcsShortCommitDetails are equal if and only if they have equal
 * hash codes. The VCS Log framework heavily relies on this fact.
 *
 * @see VcsCommitMetadata
 * @see VcsFullCommitDetails
 */
public interface VcsShortCommitDetails extends TimedVcsCommit {

  @Nonnull
  VirtualFile getRoot();

  @Nonnull
  String getSubject();

  @Nonnull
  VcsUser getAuthor();

  @Nonnull
  VcsUser getCommitter();

  long getAuthorTime();

  long getCommitTime();
}
