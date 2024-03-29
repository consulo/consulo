/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public class CommitId {
  @Nonnull
  private final Hash myHash;
  @Nonnull
  private final VirtualFile myRoot;

  public CommitId(@Nonnull Hash hash, @Nonnull VirtualFile root) {
    myHash = hash;
    myRoot = root;
  }

  @Nonnull
  public Hash getHash() {
    return myHash;
  }

  @Nonnull
  public VirtualFile getRoot() {
    return myRoot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CommitId commitId = (CommitId)o;

    if (!myHash.equals(commitId.myHash)) return false;
    if (!myRoot.equals(commitId.myRoot)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myHash.hashCode();
    result = 31 * result + myRoot.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return myHash.asString() + "(" + myRoot + ")";
  }
}
