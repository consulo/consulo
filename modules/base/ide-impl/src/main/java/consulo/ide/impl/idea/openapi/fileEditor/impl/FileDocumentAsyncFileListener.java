/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.impl.DocumentImpl;
import consulo.virtualFileSystem.internal.PersistentFS;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl
final class FileDocumentAsyncFileListener implements AsyncFileListener {
  private final FileDocumentManagerImpl myFileDocumentManager = (FileDocumentManagerImpl)FileDocumentManager.getInstance();

  private final FileTypeRegistry myFileTypeRegistry;

  @Inject
  FileDocumentAsyncFileListener(FileTypeRegistry fileTypeRegistry) {
    myFileTypeRegistry = fileTypeRegistry;
  }

  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    List<VirtualFile> toRecompute = new ArrayList<>();
    Map<VirtualFile, Document> strongRefsToDocuments = new HashMap<>();
    List<VFileContentChangeEvent> contentChanges = ContainerUtil.findAll(events, VFileContentChangeEvent.class);
    for (VFileContentChangeEvent event : contentChanges) {
      ProgressManager.checkCanceled();
      VirtualFile virtualFile = event.getFile();

      // when an empty unknown file is written into, re-run file type detection
      long lastRecordedLength = PersistentFS.getInstance().getLastRecordedLength(virtualFile);
      if (lastRecordedLength == 0 && myFileTypeRegistry.isFileOfType(virtualFile, UnknownFileType.INSTANCE)) { // check file type last to avoid content detection running
        toRecompute.add(virtualFile);
      }

      prepareForRangeMarkerUpdate(strongRefsToDocuments, virtualFile);
    }

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        for (VFileContentChangeEvent event : contentChanges) {
          // new range markers could've appeared after "prepareChange" in some read action
          prepareForRangeMarkerUpdate(strongRefsToDocuments, event.getFile());
          if (FileDocumentManagerImpl.ourConflictsSolverEnabled) {
            myFileDocumentManager.myConflictResolver.beforeContentChange(event);
          }
        }

        for (VirtualFile file : toRecompute) {
          file.putUserData(FileDocumentManagerImpl.MUST_RECOMPUTE_FILE_TYPE, Boolean.TRUE);
        }
      }

      @Override
      public void afterVfsChange() {
        for (VFileEvent event : events) {
          if (event instanceof VFileContentChangeEvent vFileContentChangeEvent && event.getFile().isValid()) {
            myFileDocumentManager.contentsChanged(vFileContentChangeEvent);
          }
          else if (event instanceof VFileDeleteEvent vFileDeleteEvent && event.getFile().isValid()) {
            myFileDocumentManager.fileDeleted(vFileDeleteEvent);
          }
          else if (event instanceof VFilePropertyChangeEvent vFilePropertyChangeEvent && event.getFile().isValid()) {
            myFileDocumentManager.propertyChanged(vFilePropertyChangeEvent);
          }
        }
        ObjectUtil.reachabilityFence(strongRefsToDocuments);
      }
    };
  }

  private void prepareForRangeMarkerUpdate(Map<VirtualFile, Document> strongRefsToDocuments, VirtualFile virtualFile) {
    Document document = myFileDocumentManager.getCachedDocument(virtualFile);
    if (document == null && DocumentImpl.areRangeMarkersRetainedFor(virtualFile)) {
      // re-create document with the old contents prior to this event
      // then contentChanged() will diff the document with the new contents and update the markers
      document = myFileDocumentManager.getDocument(virtualFile);
    }
    // save document strongly to make it live until contentChanged()
    if (document != null) {
      strongRefsToDocuments.put(virtualFile, document);
    }
  }
}
