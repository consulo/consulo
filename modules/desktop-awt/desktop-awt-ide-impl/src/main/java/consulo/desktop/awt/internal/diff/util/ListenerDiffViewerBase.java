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
package consulo.desktop.awt.internal.diff.util;

import consulo.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.event.VirtualFileAdapter;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ListenerDiffViewerBase extends DiffViewerBase {
  
  private final DocumentListener myDocumentListener;
  private final @Nullable VirtualFileListener myFileListener;

  public ListenerDiffViewerBase(DiffContext context, ContentDiffRequest request) {
    super(context, request);
    myDocumentListener = createDocumentListener();
    myFileListener = createFileListener(request);

    if (myFileListener != null) VirtualFileManager.getInstance().addVirtualFileListener(myFileListener);

    for (Document document : getDocuments()) {
      document.addDocumentListener(myDocumentListener);
    }
  }

  @Override
  protected void onDispose() {
    if (myFileListener != null) VirtualFileManager.getInstance().removeVirtualFileListener(myFileListener);

    for (Document document : getDocuments()) {
      document.removeDocumentListener(myDocumentListener);
    }
    super.onDispose();
  }

  
  protected DocumentListener createDocumentListener() {
    return new DocumentAdapter() {
      @Override
      public void beforeDocumentChange(DocumentEvent event) {
        onBeforeDocumentChange(event);
      }

      @Override
      public void documentChanged(DocumentEvent event) {
        onDocumentChange(event);
      }
    };
  }

  protected @Nullable VirtualFileListener createFileListener(ContentDiffRequest request) {
    final List<VirtualFile> files = new ArrayList<VirtualFile>(0);
    for (DiffContent content : request.getContents()) {
      if (content instanceof FileContent && !(content instanceof DocumentContent)) {
        files.add(((FileContent)content).getFile());
      }
    }

    if (files.isEmpty()) return null;

    return new VirtualFileAdapter() {
      @Override
      public void contentsChanged(VirtualFileEvent event) {
        if (files.contains(event.getFile())) {
          onFileChange(event);
        }
      }

      @Override
      public void propertyChanged(VirtualFilePropertyEvent event) {
        if (files.contains(event.getFile())) {
          onFileChange(event);
        }
      }
    };
  }

  //
  // Abstract
  //

  @RequiredUIAccess
  protected void onDocumentChange(DocumentEvent event) {
    scheduleRediff();
  }

  @RequiredUIAccess
  protected void onBeforeDocumentChange(DocumentEvent event) {
  }

  @RequiredUIAccess
  protected void onFileChange(VirtualFileEvent event) {
    scheduleRediff();
  }

  //
  // Helpers
  //

  
  private Set<Document> getDocuments() {
    Set<Document> documents = new HashSet<Document>();
    for (DiffContent content : myRequest.getContents()) {
      if (content instanceof DocumentContent) {
        documents.add(((DocumentContent)content).getDocument());
      }
    }
    return documents;
  }
}
