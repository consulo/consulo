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
package com.intellij.vcs.log.graph.api.permanent;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Set;

public interface PermanentCommitsInfo<CommitId> {
  @Nonnull
  CommitId getCommitId(int nodeId);

  long getTimestamp(int nodeId);

  int getNodeId(@Nonnull CommitId commitId);

  @Nonnull
  Set<Integer> convertToNodeIds(@Nonnull Collection<CommitId> heads);
}
