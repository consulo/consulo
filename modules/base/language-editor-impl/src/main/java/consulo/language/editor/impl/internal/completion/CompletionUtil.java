// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.completion;

import consulo.application.statistic.FeatureUsageTracker;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.TailType;
import consulo.language.pattern.CharPattern;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.filter.TrueFilter;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CompletionUtil {

  private static final CompletionData ourGenericCompletionData = new CompletionData() {
    {
      final CompletionVariant variant = new CompletionVariant(PsiElement.class, TrueFilter.INSTANCE);
      variant.addCompletionFilter(TrueFilter.INSTANCE, TailType.NONE);
      registerVariant(variant);
    }
  };
  public static final
  @NonNls
  String DUMMY_IDENTIFIER = CompletionInitializationContext.DUMMY_IDENTIFIER;
  public static final
  @NonNls
  String DUMMY_IDENTIFIER_TRIMMED = DUMMY_IDENTIFIER.trim();

  @Nullable
  public static CompletionData getCompletionDataByElement(@Nullable final PsiElement position, @Nonnull PsiFile originalFile) {
    if (position == null) return null;

    PsiElement parent = position.getParent();
    Language language = parent == null ? position.getLanguage() : parent.getLanguage();
    final FileType fileType = language.getAssociatedFileType();
    if (fileType != null) {
      final CompletionData mainData = getCompletionDataByFileType(fileType);
      if (mainData != null) {
        return mainData;
      }
    }

    final CompletionData mainData = getCompletionDataByFileType(originalFile.getFileType());
    return mainData != null ? mainData : ourGenericCompletionData;
  }

  @Nullable
  private static CompletionData getCompletionDataByFileType(FileType fileType) {
    // FIXME [VISTALL] not supported anymore, just remove it later
    return null;
  }

  public static boolean shouldShowFeature(CompletionParameters parameters, @NonNls final String id) {
    return shouldShowFeature(parameters.getPosition().getProject(), id);
  }

  public static boolean shouldShowFeature(Project project, @NonNls String id) {
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

  public static String findAlphanumericPrefix(CompletionParameters parameters) {
    return findIdentifierPrefix(parameters.getPosition().getContainingFile(), parameters.getOffset(), CharPattern.letterOrDigitCharacter(), CharPattern.letterOrDigitCharacter());
  }

  public static String findIdentifierPrefix(PsiElement insertedElement, int offset, ElementPattern<Character> idPart, ElementPattern<Character> idStart) {
    if (insertedElement == null) return "";
    int startOffset = insertedElement.getTextRange().getStartOffset();
    return findInText(offset, startOffset, idPart, idStart, insertedElement.getNode().getChars());
  }

  @SuppressWarnings("unused") // used in Rider
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

  @Nullable
  public static String findReferencePrefix(CompletionParameters parameters) {
    return CompletionData.getReferencePrefix(parameters.getPosition(), parameters.getOffset());
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

  @Nullable
  public static PsiElement getTargetElement(LookupElement lookupElement) {
    return CompletionUtilCore.getTargetElement(lookupElement);
  }

  @Nullable
  public static <T extends PsiElement> T getOriginalElement(@Nonnull T psi) {
    return CompletionUtilCore.getOriginalElement(psi);
  }

  @Nonnull
  public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T psi) {
    final T element = getOriginalElement(psi);
    return element == null ? psi : element;
  }

  public static Iterable<String> iterateLookupStrings(@Nonnull final LookupElement element) {
    return CompletionUtilCore.iterateLookupStrings(element);
  }

  /**
   * @return String representation of action shortcut. Useful while advertising something
   * @see #advertise(CompletionParameters)
   */
  @Nonnull
  public static String getActionShortcut(@NonNls @Nonnull final String actionId) {
    return KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(actionId));
  }
}
