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
package com.intellij.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author peter
 */
public class FileContentUtilCore {
  @NonNls public static final String FORCE_RELOAD_REQUESTOR = "FileContentUtilCore.saveOrReload";

  @RequiredUIAccess
  public static void reparseFiles(@Nonnull VirtualFile... files) {
    reparseFiles(Arrays.asList(files));
  }

  @RequiredUIAccess
  public static void reparseFiles(@Nonnull final Collection<? extends VirtualFile> files) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        // files must be processed under one write action to prevent firing event for invalid files.
        final Set<VFilePropertyChangeEvent> events = new HashSet<VFilePropertyChangeEvent>();
        for (VirtualFile file : files) {
          saveOrReload(file, events);
        }

        BulkFileListener publisher = ApplicationManager.getApplication().getMessageBus().syncPublisher(VirtualFileManager.VFS_CHANGES);
        List<VFileEvent> eventList = new ArrayList<VFileEvent>(events);
        publisher.before(eventList);
        publisher.after(eventList);
      }
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
