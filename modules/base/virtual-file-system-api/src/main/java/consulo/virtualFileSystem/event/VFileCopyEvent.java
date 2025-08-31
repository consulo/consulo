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

/**
 * @author max
 */
public class VFileCopyEvent extends VFileEvent {
  private final VirtualFile myFile;
  private final VirtualFile myNewParent;
  private final String myNewChildName;

  public VFileCopyEvent(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent, @Nonnull String newChildName) {
    super(requestor, false);
    myFile = file;
    myNewParent = newParent;
    myNewChildName = newChildName;
  }

  @Override
  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  public VirtualFile getNewParent() {
    return myNewParent;
  }

  @Nonnull
  public String getNewChildName() {
    return myNewChildName;
  }

  @Override
  public String toString() {
    return "VfsEvent[copy " + myFile + " to " + myNewParent + " as " + myNewChildName + "]";
  }

  @Nonnull
  @Override
  protected String computePath() {
    return myNewParent.getPath() + "/" + myNewChildName;
  }

  @Nonnull
  @Override
  public VirtualFileSystem getFileSystem() {
    return myFile.getFileSystem();
  }

  @Override
  public boolean isValid() {
    return myFile.isValid() && myNewParent.findChild(myNewChildName) == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VFileCopyEvent event = (VFileCopyEvent)o;

    if (!myFile.equals(event.myFile)) return false;
    if (!myNewChildName.equals(event.myNewChildName)) return false;
    if (!myNewParent.equals(event.myNewParent)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myFile.hashCode();
    result = 31 * result + myNewParent.hashCode();
    result = 31 * result + myNewChildName.hashCode();
    return result;
  }
}
