// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.completion;

import consulo.annotation.access.RequiredReadAction;
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
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.filter.TrueFilter;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.virtualFileSystem.fileType.FileType;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CompletionUtil {

  private static final CompletionData ourGenericCompletionData = new CompletionData() {
    {
      final CompletionVariant variant = new CompletionVariant(PsiElement.class, TrueFilter.INSTANCE);
      variant.addCompletionFilter(TrueFilter.INSTANCE, TailType.NONE);
      registerVariant(variant);
    }
  };
  public static final String DUMMY_IDENTIFIER = CompletionInitializationContext.DUMMY_IDENTIFIER;
  public static final String DUMMY_IDENTIFIER_TRIMMED = DUMMY_IDENTIFIER.trim();

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
    return CompletionUtilCore.shouldShowFeature(parameters, id);
  }

  public static boolean shouldShowFeature(Project project, @NonNls String id) {
    return CompletionUtilCore.shouldShowFeature(project, id);
  }

  public static String findJavaIdentifierPrefix(CompletionParameters parameters) {
    return CompletionUtilCore.findJavaIdentifierPrefix(parameters);
  }

  public static String findJavaIdentifierPrefix(final PsiElement insertedElement, final int offset) {
    return CompletionUtilCore.findJavaIdentifierPrefix(insertedElement, offset);
  }

  public static String findReferenceOrAlphanumericPrefix(CompletionParameters parameters) {
    return CompletionUtilCore.findReferenceOrAlphanumericPrefix(parameters);
  }

  public static String findAlphanumericPrefix(CompletionParameters parameters) {
    return CompletionUtilCore.findAlphanumericPrefix(parameters);
  }

  public static String findIdentifierPrefix(PsiElement insertedElement, int offset, ElementPattern<Character> idPart, ElementPattern<Character> idStart) {
    return CompletionUtilCore.findIdentifierPrefix(insertedElement, offset, idPart, idStart);
  }

  @SuppressWarnings("unused") // used in Rider
  public static String findIdentifierPrefix(@Nonnull Document document, int offset, ElementPattern<Character> idPart, ElementPattern<Character> idStart) {
    return CompletionUtilCore.findIdentifierPrefix(document, offset, idPart, idStart);
  }

  @Nullable
  @RequiredReadAction
  public static String findReferencePrefix(CompletionParameters parameters) {
    return CompletionUtilCore.findReferencePrefix(parameters);
  }


  public static InsertionContext emulateInsertion(InsertionContext oldContext, int newStart, final LookupElement item) {
    return CompletionUtilCore.emulateInsertion(oldContext, newStart, item);
  }

  private static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement) {
    final Editor editor = oldContext.getEditor();
    return new InsertionContext(new OffsetMap(editor.getDocument()), Lookup.AUTO_INSERT_SELECT_CHAR, new LookupElement[]{forElement}, oldContext.getFile(), editor,
                                oldContext.shouldAddCompletionChar());
  }

  public static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement, int startOffset, int tailOffset) {
    return CompletionUtilCore.newContext(oldContext, forElement, startOffset, tailOffset);
  }

  public static void emulateInsertion(LookupElement item, int offset, InsertionContext context) {
    CompletionUtilCore.emulateInsertion(item, offset, context);
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
    return CompletionUtilCore.getOriginalOrSelf(psi);
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
