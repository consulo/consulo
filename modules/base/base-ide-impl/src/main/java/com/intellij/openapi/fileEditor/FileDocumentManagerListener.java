// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor;

import com.intellij.AppTopics;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.util.EventListener;

/**
 * @see AppTopics#FILE_DOCUMENT_SYNC
 */
public interface FileDocumentManagerListener extends EventListener {

  /**
   * There is a possible case that callback that listens for the events implied by the current interface needs to modify document
   * contents (e.g. strip trailing spaces before saving a document). It's too dangerous to do that from message bus callback
   * because that may cause unexpected 'nested modification' (see IDEA-71701 for more details).
   * <p/>
   * That's why this interface is exposed via extension point as well - it's possible to modify document content from
   * the extension callback.
   */
  ExtensionPointName<FileDocumentManagerListener> EP_NAME = ExtensionPointName.create("com.intellij.fileDocumentManagerListener");

  /**
   * Fired before processing FileDocumentManager.saveAllDocuments(). Can be used by plugins
   * which need to perform additional save operations when documents, rather than settings,
   * are saved.
   */
  default void beforeAllDocumentsSaving() {
  }

  /**
   * NOTE: Vetoing facility is deprecated in this listener implement {@link FileDocumentSynchronizationVetoer} instead.
   */
  default void beforeDocumentSaving(@Nonnull Document document) {
  }

  /**
   * NOTE: Vetoing facility is deprecated in this listener implement {@link FileDocumentSynchronizationVetoer} instead.
   */
  default void beforeFileContentReload(@Nonnull VirtualFile file, @Nonnull Document document) {
  }

  default void fileWithNoDocumentChanged(@Nonnull VirtualFile file) {
  }

  default void fileContentReloaded(@Nonnull VirtualFile file, @Nonnull Document document) {
  }

  default void fileContentLoaded(@Nonnull VirtualFile file, @Nonnull Document document) {
  }

  default void unsavedDocumentsDropped() {
  }
}
