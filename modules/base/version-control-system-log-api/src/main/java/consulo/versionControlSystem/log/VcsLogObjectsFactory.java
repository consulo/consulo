package consulo.versionControlSystem.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * Use this factory to create correct instances of such commonly used vcs-log-api objects as {@link Hash} or {@link VcsShortCommitDetails}.
 *
 * @author Kirill Likhodedov
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsLogObjectsFactory {

  @Nonnull
  Hash createHash(@Nonnull String stringHash);

  @Nonnull
  TimedVcsCommit createTimedCommit(@Nonnull Hash hash, @Nonnull List<Hash> parents, long timeStamp);

  @Nonnull
  VcsShortCommitDetails createShortDetails(@Nonnull Hash hash,
                                           @Nonnull List<Hash> parents,
                                           long commitTime,
                                           VirtualFile root,
                                           @Nonnull String subject,
                                           @Nonnull String authorName,
                                           String authorEmail,
                                           @Nonnull String committerName,
                                           @Nonnull String committerEmail,
                                           long authorTime);

  @Nonnull
  VcsCommitMetadata createCommitMetadata(@Nonnull Hash hash,
                                         @Nonnull List<Hash> parents,
                                         long commitTime,
                                         VirtualFile root,
                                         @Nonnull String subject,
                                         @Nonnull String authorName,
                                         @Nonnull String authorEmail,
                                         @Nonnull String message,
                                         @Nonnull String committerName,
                                         @Nonnull String committerEmail,
                                         long authorTime);

  @Nonnull
  VcsFullCommitDetails createFullDetails(@Nonnull Hash hash,
                                         @Nonnull List<Hash> parents,
                                         long commitTime,
                                         VirtualFile root,
                                         @Nonnull String subject,
                                         @Nonnull String authorName,
                                         @Nonnull String authorEmail,
                                         @Nonnull String message,
                                         @Nonnull String committerName,
                                         @Nonnull String committerEmail,
                                         long authorTime,
                                         @Nonnull ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter);

  @Nonnull
  VcsUser createUser(@Nonnull String name, @Nonnull String email);

  @Nonnull
  VcsRef createRef(@Nonnull Hash commitHash, @Nonnull String name, @Nonnull VcsRefType type, @Nonnull VirtualFile root);
}
