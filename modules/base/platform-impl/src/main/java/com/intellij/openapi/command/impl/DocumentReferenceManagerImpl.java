/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.command.impl;

import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.reference.SoftReference;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.fs.FilePath;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class DocumentReferenceManagerImpl extends DocumentReferenceManager {
  private static final Key<List<VirtualFile>> DELETED_FILES = Key.create(DocumentReferenceManagerImpl.class.getName() + ".DELETED_FILES");

  private static final Key<Reference<DocumentReference>> FILE_TO_REF_KEY = Key.create("FILE_TO_REF_KEY");
  private static final Key<DocumentReference> FILE_TO_STRONG_REF_KEY = Key.create("FILE_TO_STRONG_REF_KEY");

  private final Map<Document, DocumentReference> myDocToRef = ContainerUtil.createConcurrentWeakKeyWeakValueMap();
  private final Map<FilePath, DocumentReference> myDeletedFilePathToRef = ContainerUtil.createConcurrentWeakValueMap();
  private final FileDocumentManager myFileDocumentManager;

  @Inject
  public DocumentReferenceManagerImpl(VirtualFileManager virtualFileManager, FileDocumentManager fileDocumentManager) {
    myFileDocumentManager = fileDocumentManager;
    virtualFileManager.addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileCreated(@Nonnull VirtualFileEvent event) {
        VirtualFile f = event.getFile();
        DocumentReference ref = myDeletedFilePathToRef.remove(new FilePath(f.getUrl()));
        if (ref != null) {
          f.putUserData(FILE_TO_REF_KEY, new WeakReference<>(ref));
          ((DocumentReferenceByVirtualFile)ref).update(f);
        }
      }

      @Override
      public void beforeFileDeletion(@Nonnull VirtualFileEvent event) {
        VirtualFile f = event.getFile();
        f.putUserData(DELETED_FILES, collectDeletedFiles(f, new ArrayList<>()));
      }

      @Override
      public void fileDeleted(@Nonnull VirtualFileEvent event) {
        VirtualFile f = event.getFile();
        List<VirtualFile> files = f.getUserData(DELETED_FILES);
        f.putUserData(DELETED_FILES, null);

        assert files != null : f;
        for (VirtualFile each : files) {
          DocumentReference ref = SoftReference.dereference(each.getUserData(FILE_TO_REF_KEY));
          each.putUserData(FILE_TO_REF_KEY, null);
          if (ref != null) {
            myDeletedFilePathToRef.put(new FilePath(each.getUrl()), ref);
          }
        }
      }
    });
  }

  private static List<VirtualFile> collectDeletedFiles(VirtualFile f, List<VirtualFile> files) {
    if (!(f instanceof NewVirtualFile)) return files;

    if (!f.isDirectory()) {
      files.add(f);
    }
    else {
      for (VirtualFile each : ((NewVirtualFile)f).iterInDbChildren()) {
        collectDeletedFiles(each, files);
      }
    }
    return files;
  }

  @Nonnull
  @Override
  public DocumentReference create(@Nonnull Document document) {
    checkLocking();

    VirtualFile file = myFileDocumentManager.getFile(document);
    return file == null ? createFromDocument(document) : create(file);
  }

  @Nonnull
  private DocumentReference createFromDocument(@Nonnull final Document document) {
    return myDocToRef.computeIfAbsent(document, DocumentReferenceByDocument::new);
  }

  @Nonnull
  @Override
  public DocumentReference create(@Nonnull VirtualFile file) {
    checkLocking();

    if (!file.isInLocalFileSystem()) { // we treat local files differently from non local because we can undo their deletion
      DocumentReference reference = file.getUserData(FILE_TO_STRONG_REF_KEY);
      if (reference == null) {
        file.putUserData(FILE_TO_STRONG_REF_KEY, reference = new DocumentReferenceByNonlocalVirtualFile(file, myFileDocumentManager));
      }
      return reference;
    }

    assert file.isValid() : "file is invalid: " + file;

    DocumentReference result = SoftReference.dereference(file.getUserData(FILE_TO_REF_KEY));
    if (result == null) {
      result = new DocumentReferenceByVirtualFile(file);
      file.putUserData(FILE_TO_REF_KEY, new WeakReference<>(result));
    }
    return result;
  }

  private static void checkLocking() {
    // before write own thread - assert dispatch thread
  }

  @TestOnly
  public void cleanupForNextTest() {
    myDeletedFilePathToRef.clear();
    myDocToRef.clear();
  }

}
