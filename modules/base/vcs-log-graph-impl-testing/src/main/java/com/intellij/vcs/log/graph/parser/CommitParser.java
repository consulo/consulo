package com.intellij.vcs.log.graph.parser;

import com.intellij.openapi.util.Pair;
import com.intellij.vcs.log.graph.GraphCommit;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author erokhins
 */
public class CommitParser {

  public static final String SEPARATOR = "|-";

  public static int nextSeparatorIndex(@Nonnull String line, int startIndex) {
    int nextIndex = line.indexOf(SEPARATOR, startIndex);
    if (nextIndex == -1) {
      throw new IllegalArgumentException("not found separator \"" + SEPARATOR + "\", with startIndex=" + startIndex +
                                         ", in line: " + line);
    }
    return nextIndex;
  }

  /**
   * @param line input format:
   *             ab123|-adada 193 352
   *             123|-             // no parent
   */
  @Nonnull
  public static Pair<String, String[]> parseCommitParents(@Nonnull String line) {
    int separatorIndex = nextSeparatorIndex(line, 0);
    String commitHashStr = line.substring(0, separatorIndex);

    String parentHashStr = line.substring(separatorIndex + 2, line.length());
    String[] parentsHashes = parentHashStr.split("\\s");
    return new Pair<String, String[]>(commitHashStr, parentsHashes);
  }

  @Nonnull
  public static GraphCommit<String> parseCommitParentsAsString(@Nonnull String line) {
    Pair<String, String[]> stringPair = parseCommitParents(line);
    return SimpleCommit.asStringCommit(stringPair.first, stringPair.second);
  }

  @Nonnull
  public static GraphCommit<Integer> parseCommitParentsAsInteger(@Nonnull String line) {
    Pair<String, String[]> stringPair = parseCommitParents(line);
    return SimpleCommit.asIntegerCommit(stringPair.first, stringPair.second);
  }

  public static int createHash(@Nonnull String s) {
    return Integer.parseInt(s, 16);
  }

  public static List<String> toLines(@Nonnull String in) {
    String[] split = in.split("\n");
    List<String> result = new ArrayList<String>();
    for (String line : split) {
      if (!line.isEmpty()) {
        result.add(line);
      }
    }
    return result;
  }

}
