// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiModificationTracker;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.LinkedList;

/**
 * @author peter
 */
public class OffsetTranslator implements Disposable {
  static final Key<OffsetTranslator> RANGE_TRANSLATION = Key.create("completion.rangeTranslation");

  private final PsiFile myOriginalFile;
  private final Document myCopyDocument;
  private final LinkedList<DocumentEvent> myTranslation = new LinkedList<>();

  public OffsetTranslator(Document originalDocument, PsiFile originalFile, Document copyDocument, int start, int end, String replacement) {
    myOriginalFile = originalFile;
    myCopyDocument = copyDocument;
    myCopyDocument.putUserData(RANGE_TRANSLATION, this);
    myTranslation.addFirst(new DocumentEventImpl(copyDocument, start, originalDocument.getImmutableCharSequence().subSequence(start, end), replacement, 0, false));
    Disposer.register(originalFile.getProject(), this);

    final LinkedList<DocumentEvent> sinceCommit = new LinkedList<>();
    originalDocument.addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@Nonnull DocumentEvent e) {
        if (isUpToDate()) {
          DocumentEventImpl inverse = new DocumentEventImpl(originalDocument, e.getOffset(), e.getNewFragment(), e.getOldFragment(), 0, false);
          sinceCommit.addLast(inverse);
        }
      }
    }, this);

    originalFile.getProject().getMessageBus().connect(this).subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
      final long lastModCount = originalFile.getViewProvider().getModificationStamp();

      @Override
      public void modificationCountChanged() {
        if (isUpToDate() && lastModCount != originalFile.getViewProvider().getModificationStamp()) {
          myTranslation.addAll(sinceCommit);
          sinceCommit.clear();
        }
      }
    });

  }

  private boolean isUpToDate() {
    return this == myCopyDocument.getUserData(RANGE_TRANSLATION) && myOriginalFile.isValid();
  }

  @Override
  public void dispose() {
    if (isUpToDate()) {
      myCopyDocument.putUserData(RANGE_TRANSLATION, null);
    }
  }

  @Nullable
  Integer translateOffset(Integer offset) {
    for (DocumentEvent event : myTranslation) {
      offset = translateOffset(offset, event);
      if (offset == null) {
        return null;
      }
    }
    return offset;
  }

  @Nullable
  private static Integer translateOffset(int offset, DocumentEvent event) {
    if (event.getOffset() < offset && offset < event.getOffset() + event.getNewLength()) {
      if (event.getOldLength() == 0) {
        return event.getOffset();
      }

      return null;
    }

    return offset <= event.getOffset() ? offset : offset - event.getNewLength() + event.getOldLength();
  }

}
