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

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * @author max
 */
public class VFileMoveEvent extends VFileEvent {
  private final VirtualFile myFile;
  private final VirtualFile myOldParent;
  private final VirtualFile myNewParent;

  public VFileMoveEvent(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) {
    super(requestor, false);
    myFile = file;
    myNewParent = newParent;
    myOldParent = file.getParent();
  }

  @Nonnull
  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  public VirtualFile getNewParent() {
    return myNewParent;
  }

  public VirtualFile getOldParent() {
    return myOldParent;
  }

  @Override
  public String toString() {
    return "VfsEvent[move " + myFile.getName() + " from " + myOldParent + " to " + myNewParent + "]";
  }

  @Nonnull
  @Override
  public String getPath() {
    return computePath();
  }

  @Nonnull
  @Override
  protected String computePath() {
    return myFile.getPath();
  }

  @Nonnull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myFile.getFileSystem();
  }

  @Override
  public boolean isValid() {
    return myFile.isValid() && Objects.equals(myFile.getParent(), myOldParent) && myOldParent.isValid();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VFileMoveEvent event = (VFileMoveEvent)o;

    if (!myFile.equals(event.myFile)) return false;
    if (!myNewParent.equals(event.myNewParent)) return false;
    if (!myOldParent.equals(event.myOldParent)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myFile.hashCode();
    result = 31 * result + myOldParent.hashCode();
    result = 31 * result + myNewParent.hashCode();
    return result;
  }

  @Nonnull
  public String getOldPath() {
    return myOldParent.getPath() + "/" + myFile.getName();
  }

  @Nonnull
  public String getNewPath() {
    return myNewParent.getPath() + "/" + myFile.getName();
  }
}
