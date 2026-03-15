package consulo.versionControlSystem.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.util.function.ThrowableComputable;
import consulo.versionControlSystem.change.Change;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.List;

/**
 * Use this factory to create correct instances of such commonly used vcs-log-api objects as {@link Hash} or {@link VcsShortCommitDetails}.
 *
 * @author Kirill Likhodedov
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface VcsLogObjectsFactory {

  
  Hash createHash(String stringHash);

  
  TimedVcsCommit createTimedCommit(Hash hash, List<Hash> parents, long timeStamp);

  
  VcsShortCommitDetails createShortDetails(Hash hash,
                                           List<Hash> parents,
                                           long commitTime,
                                           VirtualFile root,
                                           String subject,
                                           String authorName,
                                           String authorEmail,
                                           String committerName,
                                           String committerEmail,
                                           long authorTime);

  
  VcsCommitMetadata createCommitMetadata(Hash hash,
                                         List<Hash> parents,
                                         long commitTime,
                                         VirtualFile root,
                                         String subject,
                                         String authorName,
                                         String authorEmail,
                                         String message,
                                         String committerName,
                                         String committerEmail,
                                         long authorTime);

  
  VcsFullCommitDetails createFullDetails(Hash hash,
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
                                         ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter);

  
  VcsUser createUser(String name, String email);

  
  VcsRef createRef(Hash commitHash, String name, VcsRefType type, VirtualFile root);
}
