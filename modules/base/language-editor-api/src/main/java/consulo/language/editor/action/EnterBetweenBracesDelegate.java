// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The class is responsible for recognizing the input context of the Enter key (new line) to support indentation.
 * The indentation procedure in the context can be delegated to a language-specific implementation.
 * The procedure can skip parsing during typing only if the language-specific implementation is inherited
 * from <code>{@link EnterBetweenBracesNoCommitDelegate}</code>.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class EnterBetweenBracesDelegate implements LanguageExtension {
  private static final Logger LOG = Logger.getInstance(EnterBetweenBracesDelegate.class);
  private static final ExtensionPointCacheKey<EnterBetweenBracesDelegate, ByLanguageValue<EnterBetweenBracesDelegate>> KEY =
    ExtensionPointCacheKey.create("EnterBetweenBracesDelegate", LanguageOneToOne.build());

  @Nullable
  public static EnterBetweenBracesDelegate forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(EnterBetweenBracesDelegate.class).getOrBuildCache(KEY).get(language);
  }

  public boolean isApplicable(@Nonnull PsiFile file,
                              @Nonnull Editor editor,
                              CharSequence documentText,
                              int caretOffset) {
    return true;
  }

  /**
   * Checks that the braces belong to the same syntax element, and whether there is a need to calculate indentation or it can be simplified.
   * Usually the implementation checks if both braces are within the same string literal or comment.
   *
   * @param file         The PSI file associated with the document.
   * @param editor       The editor.
   * @param lBraceOffset The left brace offset.
   * @param rBraceOffset The right brace offset.
   * @return <code>true</code> if the left and the right braces are within the same syntax element.
   */
  public boolean bracesAreInTheSameElement(@Nonnull PsiFile file, @Nonnull Editor editor, int lBraceOffset, int rBraceOffset) {
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    if (file.findElementAt(lBraceOffset) == file.findElementAt(rBraceOffset)) {
      return true;
    }
    return false;
  }

  /**
   * Reformats the line at the specified offset in the specified file, modifying only the line indent
   * and leaving all other whitespace intact. At the time of call, the document is in the uncommitted state.
   *
   * @param file   The PSI file to reformat.
   * @param offset The offset the line at which should be reformatted.
   */
  public void formatAtOffset(@Nonnull PsiFile file, @Nonnull Editor editor, int offset, @Nullable Language language) {
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    try {
      CodeStyleManager.getInstance(file.getProject()).adjustLineIndent(file, offset);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  /**
   * Detects if the offset in the file is within the comment.
   * Indentation inside the comment is delegated to the standard procedure.
   *
   * @param file   The PSI file associated with the document.
   * @param editor The editor.
   * @param offset The position in the editor.
   * @return <code>true</code> if you need to use the standard indentation procedure in comments.
   */
  public boolean isInComment(@Nonnull PsiFile file, @Nonnull Editor editor, int offset) {
    return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiComment.class) != null;
  }

  /**
   * @param lBrace The left brace offset.
   * @param rBrace The right brace offset.
   * @return <code>true</code>, if braces are pair for handling.
   */
  public boolean isBracePair(char lBrace, char rBrace) {
    return (lBrace == '(' && rBrace == ')') || (lBrace == '{' && rBrace == '}');
  }
}
