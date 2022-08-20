/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.matcher.CompositeStringHolder;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.OffsetTranslator;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupValueWithPsiElement;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.collection.UnmodifiableIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * @author yole
 */
public class CompletionUtilCore {
  /**
   * A default string that is inserted to the file before completion to guarantee that there'll always be some non-empty element there
   */
  public static final String DUMMY_IDENTIFIER = "IntellijIdeaRulezzz ";
  public static final String DUMMY_IDENTIFIER_TRIMMED = "IntellijIdeaRulezzz";

  @Nullable
  public static PsiElement getTargetElement(LookupElement lookupElement) {
    PsiElement psiElement = lookupElement.getPsiElement();
    if (psiElement != null && psiElement.isValid()) {
      return getOriginalElement(psiElement);
    }

    Object object = lookupElement.getObject();
    if (object instanceof LookupValueWithPsiElement) {
      final PsiElement element = ((LookupValueWithPsiElement)object).getElement();
      if (element != null && element.isValid()) return getOriginalElement(element);
    }

    return null;
  }

  @Nullable
  public static <T extends PsiElement> T getOriginalElement(@Nonnull T psi) {
    final PsiFile file = psi.getContainingFile();
    return getOriginalElement(psi, file);
  }

  @RequiredReadAction
  public static <T extends PsiElement> T getOriginalElement(@Nonnull T psi, PsiFile containingFile) {
    if (containingFile != null && containingFile != containingFile.getOriginalFile() && psi.getTextRange() != null) {
      TextRange range = psi.getTextRange();
      Integer start = range.getStartOffset();
      Integer end = range.getEndOffset();
      final Document document = containingFile.getViewProvider().getDocument();
      if (document != null) {
        Document hostDocument = document instanceof DocumentWindow ? ((DocumentWindow)document).getDelegate() : document;
        OffsetTranslator translator = hostDocument.getUserData(OffsetTranslator.RANGE_TRANSLATION);
        if (translator != null) {
          if (document instanceof DocumentWindow) {
            TextRange translated = ((DocumentWindow)document).injectedToHost(new TextRange(start, end));
            start = translated.getStartOffset();
            end = translated.getEndOffset();
          }

          start = translator.translateOffset(start);
          end = translator.translateOffset(end);
          if (start == null || end == null) {
            return null;
          }

          if (document instanceof DocumentWindow) {
            start = ((DocumentWindow)document).hostToInjected(start);
            end = ((DocumentWindow)document).hostToInjected(end);
          }
        }
      }
      //noinspection unchecked
      return (T)PsiTreeUtil.findElementOfClassAtRange(containingFile.getOriginalFile(), start, end, psi.getClass());
    }

    return psi;
  }

  public static Iterable<String> iterateLookupStrings(@Nonnull final CompositeStringHolder element) {
    return new Iterable<String>() {
      @Nonnull
      @Override
      public Iterator<String> iterator() {
        final Iterator<String> original = element.getAllStrings().iterator();
        return new UnmodifiableIterator<>(original) {
          @Override
          public boolean hasNext() {
            try {
              return super.hasNext();
            }
            catch (ConcurrentModificationException e) {
              throw handleCME(e);
            }
          }

          @Override
          public String next() {
            try {
              return super.next();
            }
            catch (ConcurrentModificationException e) {
              throw handleCME(e);
            }
          }

          private RuntimeException handleCME(ConcurrentModificationException cme) {
            RuntimeExceptionWithAttachments ewa = new RuntimeExceptionWithAttachments("Error while traversing lookup strings of " + element + " of " + element.getClass(), (String)null,
                                                                                      AttachmentFactory.get().create("threadDump.txt", ThreadDumper.dumpThreadsToString()));
            ewa.initCause(cme);
            return ewa;
          }
        };
      }
    };
  }
}
