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
package git4idea;

import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a Git commit with its meta information (hash, author, message, etc.), its parents and the {@link Change changes}.
 *
 * @note [VISTALL] class sometimes throw NPE while compiling(last two arguments store safe names) - that why was added Builder
 * @see http://bugs.sun.com/view_bug.do?bug_id=6889255
 * @author Kirill Likhodedov
 */
public final class GitCommit {

  public static class Builder {
    private Hash myHash;
    private String myAuthorName;
    private String myAuthorEmail;
    private long myAuthorTime;
    private String myCommitterName;
    private String myCommitterEmail;
    private long myCommitTime;
    private String mySubject;
    private String myMessage;
    private List<Hash> myParents = Collections.emptyList();
    private List<Change> myChanges = Collections.emptyList();

    public Builder setHash(Hash hash) {
      myHash = hash;
      return this;
    }

    public Builder setAuthorName(@NotNull String authorName) {
      myAuthorName = authorName;
      return this;
    }

    public Builder setAuthorEmail(@NotNull String authorEmail) {
      myAuthorEmail = authorEmail;
      return this;
    }

    public Builder setAuthorTime(long authorTime) {
      myAuthorTime = authorTime;
      return this;
    }

    public Builder setCommitterName(@NotNull String committerName) {
      myCommitterName = committerName;
      return this;
    }

    public Builder setCommitterEmail(@NotNull String committerEmail) {
      myCommitterEmail = committerEmail;
      return this;
    }

    public Builder setCommitTime(long commitTime) {
      myCommitTime = commitTime;
      return this;
    }

    public Builder setSubject(@NotNull String subject) {
      mySubject = subject;
      return this;
    }

    public Builder setMessage(@NotNull String message) {
      myMessage = message;
      return this;
    }

    public Builder setParents(@NotNull List<Hash> parents) {
      myParents = parents;
      return this;
    }

    public Builder setChanges(@NotNull List<Change> changes) {
      myChanges = changes;
      return this;
    }

    public GitCommit build() {
      return new GitCommit(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @NotNull private Hash myHash; // full (long) hash

  @NotNull
  private String myAuthorName;
  @NotNull
  private String myAuthorEmail;
  private long myAuthorTime;

  @NotNull
  private String myCommitterName;
  @NotNull
  private String myCommitterEmail;
  private long myCommitTime;

  @NotNull
  private String mySubject;
  @NotNull
  private String myFullMessage;

  @NotNull
  private List<Hash> myParents;
  @NotNull
  private List<Change> myChanges;

  private GitCommit(Builder builder) {
    myHash = builder.myHash;
    myAuthorName = builder.myAuthorName;
    myAuthorEmail = builder.myAuthorEmail;
    myAuthorTime = builder.myAuthorTime;
    myCommitterName = builder.myCommitterName;
    myCommitterEmail = builder.myCommitterEmail;
    myCommitTime = builder.myCommitTime;
    mySubject = builder.mySubject;
    myFullMessage = builder.myMessage;
    myParents = builder.myParents;
    myChanges = builder.myChanges;
  }

  @NotNull
  public Hash getHash() {
    return myHash;
  }

  @NotNull
  public String getAuthorName() {
    return myAuthorName;
  }

  @NotNull
  public String getAuthorEmail() {
    return myAuthorEmail;
  }

  public long getAuthorTime() {
    return myAuthorTime;
  }

  @NotNull
  public String getCommitterName() {
    return myCommitterName;
  }

  @NotNull
  public String getCommitterEmail() {
    return myCommitterEmail;
  }

  public long getCommitTime() {
    return myCommitTime;
  }

  @NotNull
  public String getSubject() {
    return mySubject;
  }

  @NotNull
  public String getFullMessage() {
    return myFullMessage;
  }

  @NotNull
  public List<Hash> getParents() {
    return myParents;
  }

  @NotNull
  public List<Change> getChanges() {
    return myChanges;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GitCommit commit = (GitCommit)o;

    // a commit is fully identified by its hash
    if (!myHash.equals(commit.myHash)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myHash.hashCode();
  }

}
