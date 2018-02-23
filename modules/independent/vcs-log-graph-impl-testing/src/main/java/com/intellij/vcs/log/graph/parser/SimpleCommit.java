/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.vcs.log.graph.parser;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SimpleCommit<CommitId> implements com.intellij.vcs.log.graph.GraphCommit<CommitId> {
  @Nonnull
  public static SimpleCommit<Integer> asIntegerCommit(@Nonnull String commitHash, @Nonnull String[] parentsHashes) {
    int intCommitHash = CommitParser.createHash(commitHash);
    List<Integer> parents = new ArrayList<Integer>();
    for (String parentsHash : parentsHashes) {
      if (parentsHash.length() > 0) {
        parents.add(CommitParser.createHash(parentsHash));
      }
    }
    return new SimpleCommit<Integer>(intCommitHash, parents, intCommitHash);
  }

  @Nonnull
  public static SimpleCommit<String> asStringCommit(@Nonnull String commitHash, @Nonnull String[] parentsHashes) {
    int timestamp = CommitParser.createHash(commitHash);
    List<String> parents = new ArrayList<String>();
    for (String parentsHash : parentsHashes) {
      if (parentsHash.length() > 0) {
        parents.add(parentsHash);
      }
    }
    return new SimpleCommit<String>(commitHash, parents, timestamp);
  }

  @Nonnull
  private final CommitId myId;
  @Nonnull
  private final List<CommitId> myParents;
  private final long myTimestamp;

  public SimpleCommit(@Nonnull CommitId id, @Nonnull List<CommitId> parents, long timestamp) {
    myId = id;
    myParents = parents;
    myTimestamp = timestamp;
  }

  @Nonnull
  @Override
  public CommitId getId() {
    return myId;
  }

  @Nonnull
  @Override
  public List<CommitId> getParents() {
    return myParents;
  }

  @Override
  public long getTimestamp() {
    return myTimestamp;
  }
}
