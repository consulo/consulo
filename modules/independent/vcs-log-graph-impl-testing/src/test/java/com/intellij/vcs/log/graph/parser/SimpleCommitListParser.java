package com.intellij.vcs.log.graph.parser;

import com.intellij.vcs.log.graph.GraphCommit;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.vcs.log.graph.parser.CommitParser.toLines;

/**
 * @author erokhins
 */
public class SimpleCommitListParser {

  @Nonnull
  public static List<GraphCommit<String>> parseStringCommitList(@Nonnull String input) {
    List<GraphCommit<String>> vcsCommitParentses = new ArrayList<GraphCommit<String>>();
    for(String line : toLines(input)) {
      vcsCommitParentses.add(CommitParser.parseCommitParentsAsString(line));
    }
    return vcsCommitParentses;
  }

  @Nonnull
  public static List<GraphCommit<Integer>> parseIntegerCommitList(@Nonnull String input) {
    List<GraphCommit<Integer>> vcsCommitParentses = new ArrayList<GraphCommit<Integer>>();
    for(String line : toLines(input)) {
      vcsCommitParentses.add(CommitParser.parseCommitParentsAsInteger(line));
    }
    return vcsCommitParentses;
  }

}
