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
package consulo.virtualFileSystem.event;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author max
 */
public class VFileMoveEvent extends VFileEvent {
  private final VirtualFile myFile;
  private final @Nullable VirtualFile myOldParent;
  private final @Nullable VirtualFile myNewParent;

  public VFileMoveEvent(Object requestor, VirtualFile file, @Nullable VirtualFile newParent) {
    super(requestor, false);
    myFile = file;
    myNewParent = newParent;
    myOldParent = file.getParent();
  }

  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  public @Nullable VirtualFile getNewParent() {
    return myNewParent;
  }

  public @Nullable VirtualFile getOldParent() {
    return myOldParent;
  }

  @Override
  public String toString() {
    return "VfsEvent[move " + myFile.getName() + " from " + myOldParent + " to " + myNewParent + "]";
  }

  @Override
  public String getPath() {
    return computePath();
  }

  @Override
  protected String computePath() {
    return myFile.getPath();
  }

  @Override
  public VirtualFileSystem getFileSystem() {
    return myFile.getFileSystem();
  }

  @Override
  public boolean isValid() {
    return myFile.isValid()
        && Objects.equals(myFile.getParent(), myOldParent)
        && (myOldParent == null || myOldParent.isValid());
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VFileMoveEvent that = (VFileMoveEvent)o;

    return myFile.equals(that.myFile)
        && Objects.equals(myNewParent, that.myNewParent)
        && Objects.equals(myOldParent, that.myOldParent);
  }

  @Override
  public int hashCode() {
    int result = myFile.hashCode();
    result = 31 * result + Objects.hashCode(myOldParent);
    result = 31 * result + Objects.hashCode(myNewParent);
    return result;
  }

  public String getOldPath() {
    return (myOldParent == null ? "" : myOldParent.getPath()) + "/" + myFile.getName();
  }

  public String getNewPath() {
    return (myNewParent == null ? "" : myNewParent.getPath()) + "/" + myFile.getName();
  }
}
