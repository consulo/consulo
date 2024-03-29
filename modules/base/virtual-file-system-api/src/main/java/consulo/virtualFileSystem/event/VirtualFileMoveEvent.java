/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides data for event which is fired when a virtual file is moved.
 *
 * @see VirtualFileListener#beforePropertyChange(VirtualFilePropertyEvent)
 * @see VirtualFileListener#propertyChanged(VirtualFilePropertyEvent)
 */
public class VirtualFileMoveEvent extends VirtualFileEvent {
  private final VirtualFile myOldParent;
  private final VirtualFile myNewParent;

  public VirtualFileMoveEvent(@Nullable Object requestor,
                              @Nonnull VirtualFile file,
                              @Nonnull VirtualFile oldParent,
                              @Nonnull VirtualFile newParent) {
    super(requestor, file, file.getName(), file.getParent());
    myOldParent = oldParent;
    myNewParent = newParent;
  }

  /**
   * Returns the parent of the file before the move.
   *
   * @return the parent of the file before the move.
   */
  @Nonnull
  public VirtualFile getOldParent() {
    return myOldParent;
  }

  /**
   * Returns the parent of the file after the move.
   *
   * @return the parent of the file after the move.
   */
  @Nonnull
  public VirtualFile getNewParent() {
    return myNewParent;
  }
}
