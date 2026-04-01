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
package consulo.virtualFileSystem.event;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author max
 */
public class BulkVirtualFileListenerAdapter implements BulkFileListener {
  private final VirtualFileListener myAdapted;
  private final @Nullable VirtualFileSystem myFileSystem;

  public BulkVirtualFileListenerAdapter(VirtualFileListener adapted) {
    this(adapted, null);
  }

  public BulkVirtualFileListenerAdapter(VirtualFileListener adapted, @Nullable VirtualFileSystem fileSystem) {
    myAdapted = adapted;
    myFileSystem = fileSystem;
  }

  @Override
  public void before(List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (myFileSystem == null || myFileSystem == event.getFileSystem()) {
        fireBefore(myAdapted, event);
      }
    }
  }

  @Override
  public void after(List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (myFileSystem == null || myFileSystem == event.getFileSystem()) {
        fireAfter(myAdapted, event);
      }
    }
  }

  public static void fireAfter(VirtualFileListener adapted, VFileEvent event) {
    if (event instanceof VFileContentChangeEvent ce) {
      VirtualFile file = ce.getRequiredFile();
      adapted.contentsChanged(new VirtualFileEvent(
          ce.getRequestor(),
          file,
          file.getParent(),
          ce.getOldModificationStamp(),
          ce.getModificationStamp()
      ));
    }
    else if (event instanceof VFileCopyEvent ce) {
      VirtualFile original = ce.getRequiredFile();
      VirtualFile copy = ce.findCreatedFile();
      if (copy != null) {
        adapted.fileCopied(new VirtualFileCopyEvent(ce.getRequestor(), original, copy));
      }
    }
    else if (event instanceof VFileCreateEvent ce) {
      VirtualFile newChild = ce.getFile();
      if (newChild != null) {
        adapted.fileCreated(new VirtualFileEvent(ce.getRequestor(), newChild, ce.getChildName(), ce.getParent()));
      }
    }
    else if (event instanceof VFileDeleteEvent de) {
      VirtualFile file = de.getRequiredFile();
      adapted.fileDeleted(new VirtualFileEvent(de.getRequestor(), file, file.getParent(), 0, 0));
    }
    else if (event instanceof VFileMoveEvent me) {
      adapted.fileMoved(new VirtualFileMoveEvent(me.getRequestor(), me.getRequiredFile(), me.getOldParent(), me.getNewParent()));
    }
    else if (event instanceof VFilePropertyChangeEvent pce) {
      adapted.propertyChanged(new VirtualFilePropertyEvent(
          pce.getRequestor(),
          pce.getRequiredFile(),
          pce.getPropertyName(),
          pce.getOldValue(),
          pce.getNewValue()
      ));
    }
  }

  public static void fireBefore(VirtualFileListener adapted, VFileEvent event) {
    if (event instanceof VFileContentChangeEvent ce) {
      VirtualFile file = ce.getRequiredFile();
      adapted.beforeContentsChange(new VirtualFileEvent(
          ce.getRequestor(),
          file,
          file.getParent(),
          ce.getOldModificationStamp(),
          ce.getModificationStamp()
      ));
    }
    else if (event instanceof VFileDeleteEvent de) {
      VirtualFile file = de.getRequiredFile();
      adapted.beforeFileDeletion(new VirtualFileEvent(de.getRequestor(), file, file.getParent(), 0, 0));
    }
    else if (event instanceof VFileMoveEvent me) {
      adapted.beforeFileMovement(new VirtualFileMoveEvent(me.getRequestor(), me.getRequiredFile(), me.getOldParent(), me.getNewParent()));
    }
    else if (event instanceof VFilePropertyChangeEvent pce) {
      adapted.beforePropertyChange(new VirtualFilePropertyEvent(
          pce.getRequestor(),
          pce.getRequiredFile(),
          pce.getPropertyName(),
          pce.getOldValue(),
          pce.getNewValue()
      ));
    }
  }
}