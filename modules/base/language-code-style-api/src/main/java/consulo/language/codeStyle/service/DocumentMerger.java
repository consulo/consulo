// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.service;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.document.Document;

/**
 * Merges the current document with the new text content.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DocumentMerger {
    /**
     * Merge the current document with the new content.
     *
     * @param document The document to be updated.
     * @param newText  The new text to be merged with the document.
     * @return True on success, false if the changes can't be merged.
     */
    boolean updateDocument(Document document, String newText);
}
