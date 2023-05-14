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
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.matcher.CompositeStringHolder;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupValueWithPsiElement;
import consulo.language.editor.internal.OffsetTranslator;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.pattern.CharPattern;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.project.Project;
import consulo.util.collection.UnmodifiableIterator;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * @author yole
 */
public class CompletionUtilCore {
  private static final Logger LOG = Logger.getInstance(CompletionUtilCore.class);

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

  @Nonnull
  public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T psi) {
    final T element = getOriginalElement(psi);
    return element == null ? psi : element;
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

  public static InsertionContext emulateInsertion(InsertionContext oldContext, int newStart, final LookupElement item) {
    final InsertionContext newContext = newContext(oldContext, item);
    emulateInsertion(item, newStart, newContext);
    return newContext;
  }

  private static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement) {
    final Editor editor = oldContext.getEditor();
    return new InsertionContext(new OffsetMap(editor.getDocument()), Lookup.AUTO_INSERT_SELECT_CHAR, new LookupElement[]{forElement}, oldContext.getFile(), editor,
                                oldContext.shouldAddCompletionChar());
  }

  public static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement, int startOffset, int tailOffset) {
    final InsertionContext context = newContext(oldContext, forElement);
    setOffsets(context, startOffset, tailOffset);
    return context;
  }

  public static void emulateInsertion(LookupElement item, int offset, InsertionContext context) {
    setOffsets(context, offset, offset);

    final Editor editor = context.getEditor();
    final Document document = editor.getDocument();
    final String lookupString = item.getLookupString();

    document.insertString(offset, lookupString);
    editor.getCaretModel().moveToOffset(context.getTailOffset());
    PsiDocumentManager.getInstance(context.getProject()).commitDocument(document);
    item.handleInsert(context);
    PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document);
  }

  private static void setOffsets(InsertionContext context, int offset, final int tailOffset) {
    final OffsetMap offsetMap = context.getOffsetMap();
    offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, offset);
    offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, tailOffset);
    offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, tailOffset);
    context.setTailOffset(tailOffset);
  }

  public static boolean shouldShowFeature(CompletionParameters parameters, final String id) {
    return shouldShowFeature(parameters.getPosition().getProject(), id);
  }

  public static boolean shouldShowFeature(Project project, String id) {
    if (FeatureUsageTracker.getInstance().isToBeAdvertisedInLookup(id, project)) {
      FeatureUsageTracker.getInstance().triggerFeatureShown(id);
      return true;
    }
    return false;
  }

  public static String findJavaIdentifierPrefix(CompletionParameters parameters) {
    return findJavaIdentifierPrefix(parameters.getPosition(), parameters.getOffset());
  }

  public static String findJavaIdentifierPrefix(final PsiElement insertedElement, final int offset) {
    return findIdentifierPrefix(insertedElement, offset, CharPattern.javaIdentifierPartCharacter(), CharPattern.javaIdentifierStartCharacter());
  }

  public static String findReferenceOrAlphanumericPrefix(CompletionParameters parameters) {
    String prefix = findReferencePrefix(parameters);
    return prefix == null ? findAlphanumericPrefix(parameters) : prefix;
  }

  @Nullable
  @RequiredReadAction
  public static String findReferencePrefix(CompletionParameters parameters) {
    PsiElement insertedElement = parameters.getPosition();
    int offsetInFile = parameters.getOffset();
    try {
      final PsiReference ref = insertedElement.getContainingFile().findReferenceAt(offsetInFile);
      if (ref != null) {
        final List<TextRange> ranges = ReferenceRange.getRanges(ref);
        final PsiElement element = ref.getElement();
        final int elementStart = element.getTextRange().getStartOffset();
        for (TextRange refRange : ranges) {
          if (refRange.contains(offsetInFile - elementStart)) {
            final int endIndex = offsetInFile - elementStart;
            final int beginIndex = refRange.getStartOffset();
            if (beginIndex > endIndex) {
              LOG.error("Inconsistent reference (found at offset not included in its range): ref=" + ref + " element=" + element + " text=" + element.getText());
            }
            if (beginIndex < 0) {
              LOG.error("Inconsistent reference (begin < 0): ref=" + ref + " element=" + element + "; begin=" + beginIndex + " text=" + element.getText());
            }
            LOG.assertTrue(endIndex >= 0);
            return element.getText().substring(beginIndex, endIndex);
          }
        }
      }
    }
    catch (IndexNotReadyException ignored) {
    }
    return null;
  }

  public static String findAlphanumericPrefix(CompletionParameters parameters) {
    return findIdentifierPrefix(parameters.getPosition().getContainingFile(), parameters.getOffset(), CharPattern.letterOrDigitCharacter(), CharPattern.letterOrDigitCharacter());
  }

  public static String findIdentifierPrefix(PsiElement insertedElement, int offset, ElementPattern<Character> idPart, ElementPattern<Character> idStart) {
    if (insertedElement == null) return "";
    int startOffset = insertedElement.getTextRange().getStartOffset();
    return findInText(offset, startOffset, idPart, idStart, insertedElement.getNode().getChars());
  }

  public static String findIdentifierPrefix(@Nonnull Document document, int offset, ElementPattern<Character> idPart, ElementPattern<Character> idStart) {
    final String text = document.getText();
    return findInText(offset, 0, idPart, idStart, text);
  }

  @Nonnull
  private static String findInText(int offset, int startOffset, ElementPattern<Character> idPart, ElementPattern<Character> idStart, CharSequence text) {
    final int offsetInElement = offset - startOffset;
    int start = offsetInElement - 1;
    while (start >= 0) {
      if (!idPart.accepts(text.charAt(start))) break;
      --start;
    }
    while (start + 1 < offsetInElement && !idStart.accepts(text.charAt(start + 1))) {
      start++;
    }

    return text.subSequence(start + 1, offsetInElement).toString().trim();
  }
}
