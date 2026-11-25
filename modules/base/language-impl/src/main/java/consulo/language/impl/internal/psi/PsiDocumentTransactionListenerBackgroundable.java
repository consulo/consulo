// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.impl.internal.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.document.Document;
import consulo.language.psi.PsiFile;

/**
 * Same as {@link PsiDocumentTransactionListener}, but can be executed on a background thread.
 * This can improve performance and responsiveness of the IDE.
 */
@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface PsiDocumentTransactionListenerBackgroundable extends PsiDocumentTransactionListener {
    @Override
    void transactionStarted(Document document, PsiFile file);

    @Override
    default void transactionCompleted(Document document, PsiFile file) {
    }
}
