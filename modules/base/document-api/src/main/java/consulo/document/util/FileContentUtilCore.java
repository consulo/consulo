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
package consulo.document.util;

import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author peter
 */
public class FileContentUtilCore {
  public static final String FORCE_RELOAD_REQUESTOR = consulo.virtualFileSystem.internal.FileContentUtilCore.FORCE_RELOAD_REQUESTOR;

  @RequiredUIAccess
  public static void reparseFiles(@Nonnull VirtualFile... files) {
    reparseFiles(Arrays.asList(files));
  }

  @RequiredUIAccess
  public static void reparseFiles(@Nonnull Collection<? extends VirtualFile> files) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      // files must be processed under one write action to prevent firing event for invalid files.
      Set<VFilePropertyChangeEvent> events = new HashSet<>();
      for (VirtualFile file : files) {
        saveOrReload(file, events);
      }

      BulkFileListener publisher = ApplicationManager.getApplication().getMessageBus().syncPublisher(BulkFileListener.class);
      List<VFileEvent> eventList = new ArrayList<VFileEvent>(events);
      publisher.before(eventList);
      publisher.after(eventList);
    });
  }

  @RequiredWriteAction
  private static void saveOrReload(VirtualFile file, @Nonnull Collection<VFilePropertyChangeEvent> events) {
    if (file == null || file.isDirectory() || !file.isValid()) {
      return;
    }

    FileDocumentManager documentManager = FileDocumentManager.getInstance();
    if (documentManager.isFileModified(file)) {
      Document document = documentManager.getDocument(file);
      if (document != null) {
        documentManager.saveDocument(document);
      }
    }

    events.add(new VFilePropertyChangeEvent(FORCE_RELOAD_REQUESTOR, file, VirtualFile.PROP_NAME, file.getName(), file.getName(), false));
  }
}
