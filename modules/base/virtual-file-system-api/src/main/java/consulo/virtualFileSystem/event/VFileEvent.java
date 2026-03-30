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

import consulo.virtualFileSystem.SavingRequestor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import org.jspecify.annotations.Nullable;

/**
 * @author max
 */
public abstract class VFileEvent {
  private final boolean myIsFromRefresh;
  private final @Nullable Object myRequestor;
  private @Nullable String myCachedPath = null;

  public VFileEvent(@Nullable Object requestor, boolean isFromRefresh) {
    myRequestor = requestor;
    myIsFromRefresh = isFromRefresh;
  }

  public boolean isFromRefresh() {
    return myIsFromRefresh;
  }

  /**
   * Returns {@code true} if the VFS change described by the event is the save of a document.
   */
  public boolean isFromSave() {
    return myRequestor instanceof SavingRequestor;
  }

  public @Nullable Object getRequestor() {
    return myRequestor;
  }

  /**
   * Returns the file path (in system independent format) affected by this event.<br/><br/>
   * <p>
   * Note that the path might be cached, thus can become out-of-date if requested later,
   * asynchronously from the event dispatching procedure
   * (e.g. {@code event.getPath()} can become not equal to {@code event.getFile().getPath()}).
   */
  public String getPath() {
    String path = myCachedPath;
    if (path == null) {
      myCachedPath = path = computePath();
    }
    return path;
  }

  protected abstract String computePath();

  /**
   * Returns the VirtualFile which this event belongs to.
   * In some cases it may be fake file - it is not guaranteed that there is such a file.
   * <p/>
   * NB: Use this method with caution, because {@link VFileCreateEvent#getFile()} needs
   * {@link VirtualFile#findChild(String)} which may be a performance leak.
   */
  public abstract VirtualFile getFile();

  public abstract VirtualFileSystem getFileSystem();

  public abstract boolean isValid();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object o);
}
