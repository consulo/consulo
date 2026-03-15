package consulo.versionControlSystem.log.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.base.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;

@Singleton
@ServiceImpl
public class VcsLogObjectsFactoryImpl implements VcsLogObjectsFactory {

  
  private final VcsUserRegistry myUserRegistry;

  @Inject
  VcsLogObjectsFactoryImpl(VcsUserRegistry userRegistry) {
    myUserRegistry = userRegistry;
  }

  
  @Override
  public Hash createHash(String stringHash) {
    return HashImpl.build(stringHash);
  }

  
  @Override
  public TimedVcsCommit createTimedCommit(Hash hash, List<Hash> parents, long timeStamp) {
    return new TimedVcsCommitImpl(hash, parents, timeStamp);
  }

  
  @Override
  public VcsShortCommitDetails createShortDetails(Hash hash,
                                                  List<Hash> parents,
                                                  long commitTime,
                                                  VirtualFile root,
                                                  String subject,
                                                  String authorName,
                                                  String authorEmail,
                                                  String committerName,
                                                  String committerEmail,
                                                  long authorTime) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsShortCommitDetailsImpl(hash, parents, commitTime, root, subject, author, committer, authorTime);
  }

  
  @Override
  public VcsCommitMetadata createCommitMetadata(Hash hash,
                                                List<Hash> parents,
                                                long commitTime,
                                                VirtualFile root,
                                                String subject,
                                                String authorName,
                                                String authorEmail,
                                                String message,
                                                String committerName,
                                                String committerEmail,
                                                long authorTime) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsCommitMetadataImpl(hash, parents, commitTime, root, subject, author, message, committer, authorTime);
  }

  
  @Override
  public VcsFullCommitDetails createFullDetails(Hash hash,
                                                List<Hash> parents,
                                                long commitTime,
                                                VirtualFile root,
                                                String subject,
                                                String authorName,
                                                String authorEmail,
                                                String message,
                                                String committerName,
                                                String committerEmail,
                                                long authorTime,
                                                ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter) {
    VcsUser author = createUser(authorName, authorEmail);
    VcsUser committer = createUser(committerName, committerEmail);
    return new VcsChangesLazilyParsedDetails(hash, parents, commitTime, root, subject, author, message, committer, authorTime, changesGetter);
  }

  
  @Override
  public VcsUser createUser(String name, String email) {
    return myUserRegistry.createUser(name, email);
  }

  
  @Override
  public VcsRef createRef(Hash commitHash, String name, VcsRefType type, VirtualFile root) {
    return new VcsRefImpl(commitHash, name, type, root);
  }
}
