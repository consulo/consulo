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
package consulo.document.event;

import consulo.document.Document;
import consulo.util.collection.ArrayFactory;

import jakarta.annotation.Nonnull;
import java.util.EventListener;

/**
 * Allows to receive notifications about changes in edited documents.
 * Implementations shouldn't modify the document, for which event is emitted, in listener methods.
 *
 * @see Document#addDocumentListener(DocumentListener)
 * @see EditorEventMulticaster#addDocumentListener(DocumentListener)
 */
public interface DocumentListener extends EventListener {
  DocumentListener[] EMPTY_ARRAY = new DocumentListener[0];
  ArrayFactory<DocumentListener> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new DocumentListener[count];

  /**
   * Called before the text of the document is changed.
   *
   * @param event the event containing the information about the change.
   */
  default void beforeDocumentChange(DocumentEvent event) {
  }

  /**
   * Called after the text of the document has been changed.
   *
   * @param event the event containing the information about the change.
   */
  default void documentChanged(DocumentEvent event) {
  }

  /**
   * Notifies about {@link Document#setInBulkUpdate(boolean) bulk mode} being enabled.
   */
  default void bulkUpdateStarting(@Nonnull Document document) {
  }

  /**
   * Notifies about {@link Document#setInBulkUpdate(boolean) bulk mode} being disabled.
   */
  default void bulkUpdateFinished(@Nonnull Document document) {
  }
}
