package consulo.versionControlSystem.log.impl.internal.data;

import consulo.versionControlSystem.log.graph.GraphCommit;
import jakarta.annotation.Nonnull;

import java.util.*;

public class VcsLogMultiRepoJoiner<CommitId, Commit extends GraphCommit<CommitId>> {

  @Nonnull
  public List<Commit> join(@Nonnull Collection<List<Commit>> logsFromRepos) {
    if (logsFromRepos.size() == 1) {
      return logsFromRepos.iterator().next();
    }

    int size = 0;
    for (List<Commit> repo : logsFromRepos) {
      size += repo.size();
    }
    List<Commit> result = new ArrayList<>(size);

    Map<Commit, Iterator<Commit>> nextCommits = new HashMap<>();
    for (List<Commit> log : logsFromRepos) {
      Iterator<Commit> iterator = log.iterator();
      if (iterator.hasNext()) {
        nextCommits.put(iterator.next(), iterator);
      }
    }

    while (!nextCommits.isEmpty()) {
      Commit lastCommit = findLatestCommit(nextCommits.keySet());
      Iterator<Commit> iterator = nextCommits.get(lastCommit);
      result.add(lastCommit);
      nextCommits.remove(lastCommit);

      if (iterator.hasNext()) {
        nextCommits.put(iterator.next(), iterator);
      }
    }

    return result;
  }

  @Nonnull
  private Commit findLatestCommit(@Nonnull Set<Commit> commits) {
    long maxTimeStamp = Long.MIN_VALUE;
    Commit lastCommit = null;
    for (Commit commit : commits) {
      if (commit.getTimestamp() >= maxTimeStamp) {
        maxTimeStamp = commit.getTimestamp();
        lastCommit = commit;
      }
    }
    assert lastCommit != null;
    return lastCommit;
  }
}
