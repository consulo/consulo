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

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Provides data for event which is fired when a virtual file is moved.
 *
 * @see VirtualFileListener#beforePropertyChange(VirtualFilePropertyEvent)
 * @see VirtualFileListener#propertyChanged(VirtualFilePropertyEvent)
 */
public class VirtualFileMoveEvent extends VirtualFileEvent {
  private final @Nullable VirtualFile myOldParent;
  private final @Nullable VirtualFile myNewParent;

  public VirtualFileMoveEvent(@Nullable Object requestor,
                              VirtualFile file,
                              @Nullable VirtualFile oldParent,
                              @Nullable VirtualFile newParent) {
    super(requestor, file, file.getName(), file.getParent());
    myOldParent = oldParent;
    myNewParent = newParent;
  }

  /**
   * Returns the parent of the file before the move.
   *
   * @return the parent of the file before the move.
   */
  public @Nullable VirtualFile getOldParent() {
    return myOldParent;
  }

  /**
   * Returns the parent of the file before the move or throws NPE if old parent is null.
   *
   * @return the parent of the file before the move.
   */
  public VirtualFile getRequiredOldParent() {
    return Objects.requireNonNull(myOldParent);
  }

  /**
   * Returns the parent of the file after the move.
   *
   * @return the parent of the file after the move.
   */
  public @Nullable VirtualFile getNewParent() {
    return myNewParent;
  }

  /**
   * Returns the parent of the file after the move or throws NPE if old parent is null.
   *
   * @return the parent of the file after the move.
   */
  public VirtualFile getRequiredNewParent() {
    return Objects.requireNonNull(myNewParent);
  }
}
