package consulo.ide.impl.idea.vcs.log.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.change.Change;
import consulo.ide.impl.idea.vcs.log.*;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.base.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

@Singleton
@ServiceImpl
public class VcsLogObjectsFactoryImpl implements VcsLogObjectsFactory {

  @Nonnull
  private final VcsUserRegistry myUserRegistry;

  @Inject
  VcsLogObjectsFactoryImpl(@Nonnull VcsUserRegistry userRegistry) {
    myUserRegistry = userRegistry;
  }

  @Nonnull
  @Override
  public Hash createHash(@Nonnull String stringHash) {
    return HashImpl.build(stringHash);
  }

  @Nonnull
  @Override
  public TimedVcsCommit createTimedCommit(@Nonnull Hash hash, @Nonnull List<Hash> parents, long timeStamp) {
    return new TimedVcsCommitImpl(hash, parents, timeStamp);
  }

  @Nonnull
  @Override
  public VcsShortCommitDetails createShortDetails(@Nonnull Hash hash,
                                                  @Nonnull List<Hash> parents,
                                                  long commitTime,
                                                  @Nonnull VirtualFile root,
                                                  @Nonnull String subject,
                                                  @Nonnull String authorName,
                                                  String authorEmail,
                                                  @Nonnull String committerName,
                                                  @Nonnull String committerEmail,
                                                  long authorTime) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsShortCommitDetailsImpl(hash, parents, commitTime, root, subject, author, committer, authorTime);
  }

  @Nonnull
  @Override
  public VcsCommitMetadata createCommitMetadata(@Nonnull Hash hash,
                                                @Nonnull List<Hash> parents,
                                                long commitTime,
                                                @Nonnull VirtualFile root,
                                                @Nonnull String subject,
                                                @Nonnull String authorName,
                                                @Nonnull String authorEmail,
                                                @Nonnull String message,
                                                @Nonnull String committerName,
                                                @Nonnull String committerEmail,
                                                long authorTime) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsCommitMetadataImpl(hash, parents, commitTime, root, subject, author, message, committer, authorTime);
  }

  @Nonnull
  @Override
  public VcsFullCommitDetails createFullDetails(@Nonnull Hash hash,
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
                                                @Nonnull ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsChangesLazilyParsedDetails(hash, parents, commitTime, root, subject, author, message, committer, authorTime, changesGetter);
  }

  @Nonnull
  @Override
  public VcsUser createUser(@Nonnull String name, @Nonnull String email) {
    return myUserRegistry.createUser(name, email);
  }

  @Nonnull
  @Override
  public VcsRef createRef(@Nonnull Hash commitHash, @Nonnull String name, @Nonnull VcsRefType type, @Nonnull VirtualFile root) {
    return new VcsRefImpl(commitHash, name, type, root);
  }
}
