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
package consulo.versionControlSystem.log;

import consulo.annotation.UsedInPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@UsedInPlugin
public class LogDataImpl implements VcsLogProvider.DetailedLogData, VcsLogProvider.LogData {

  private static final LogDataImpl EMPTY = new LogDataImpl(Collections.<VcsRef>emptySet(),
                                                           Collections.<VcsUser>emptySet(),
                                                           Collections.<VcsCommitMetadata>emptyList());

  
  private final List<VcsCommitMetadata> myCommits;
  
  private final Set<VcsRef> myRefs;
  
  private final Set<VcsUser> myUsers;

  
  public static LogDataImpl empty() {
    return EMPTY;
  }

  public LogDataImpl(Set<VcsRef> refs, Set<VcsUser> users) {
    this(refs, users, Collections.<VcsCommitMetadata>emptyList());
  }

  public LogDataImpl(Set<VcsRef> refs, List<VcsCommitMetadata> metadatas) {
    this(refs, Collections.<VcsUser>emptySet(), metadatas);
  }

  private LogDataImpl(Set<VcsRef> refs, Set<VcsUser> users, List<VcsCommitMetadata> commits) {
    myRefs = refs;
    myUsers = users;
    myCommits = commits;
  }

  
  @Override
  public List<VcsCommitMetadata> getCommits() {
    return myCommits;
  }

  @Override
  
  public Set<VcsRef> getRefs() {
    return myRefs;
  }

  
  @Override
  public Set<VcsUser> getUsers() {
    return myUsers;
  }
}
