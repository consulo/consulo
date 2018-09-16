/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.vcs.log.graph.impl;

import com.intellij.util.Function;
import com.intellij.vcs.log.graph.GraphCommit;
import com.intellij.vcs.log.graph.parser.SimpleCommitListParser;
import javax.annotation.Nonnull;

import java.util.List;

public abstract class CommitIdManager<CommitId> {
  public static final CommitIdManager<String> STRING_COMMIT_ID_MANAGER = new CommitIdManager<String>() {
    @Nonnull
    @Override
    public List<GraphCommit<String>> parseCommitList(@Nonnull String in) {
      return SimpleCommitListParser.parseStringCommitList(in);
    }

    @Nonnull
    @Override
    public String toStr(String commit) {
      return commit;
    }
  };

  public static final CommitIdManager<Integer> INTEGER_COMMIT_ID_MANAGER = new CommitIdManager<Integer>() {
    @Nonnull
    @Override
    public List<GraphCommit<Integer>> parseCommitList(@Nonnull String in) {
      return SimpleCommitListParser.parseIntegerCommitList(in);
    }

    @Nonnull
    @Override
    public String toStr(Integer commit) {
      return Integer.toHexString(commit);
    }
  };

  @Nonnull
  public abstract List<GraphCommit<CommitId>> parseCommitList(@Nonnull String in);

  @Nonnull
  public abstract String toStr(CommitId commit);

  @Nonnull
  public Function<CommitId, String> getToStrFunction() {
    return new Function<CommitId, String>() {
      @Override
      public String fun(CommitId commitId) {
        return toStr(commitId);
      }
    };
  }
}
