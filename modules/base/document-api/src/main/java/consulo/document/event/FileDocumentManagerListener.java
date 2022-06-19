// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.document.event;

import consulo.annotation.component.Topic;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.EventListener;

/**
 * @see #TOPIC
 */
@Topic
public interface FileDocumentManagerListener extends EventListener {
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
