// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.TargetElementUtil;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static consulo.document.util.TextRange.EMPTY_RANGE;

/**
 * Provides information about complex language structures, like multi-block expressions ({@code if/elsif/else/end}).
 * <p/>
 * Used to:
 * <ul>
 * <li>highlight markers or keywords if one of them is under cursor</li>
 * <li>navigate to the begin/end of the block</li>
 * </ul>
 *
 * @see AbstractCodeBlockSupportHandler
 * @see BraceMatcher
 * @see consulo.language.editor.action.CodeBlockProvider
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CodeBlockSupportHandler extends LanguageExtension {
  ExtensionPointCacheKey<CodeBlockSupportHandler, ByLanguageValue<List<CodeBlockSupportHandler>>> KEY =
    ExtensionPointCacheKey.create("CodeBlockSupportHandler", LanguageOneToMany.build(false));

  static List<CodeBlockSupportHandler> forLanguage(Language language) {
    return Application.get().getExtensionPoint(CodeBlockSupportHandler.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Checks if code block marker (or keyword) is under cursor and collects related markers: e.g. finds {@code if}/{@code elsif}/{@code else}
   * if cursor is on the {@code else} from this compound statement.
   *
   * @return List of ranges of related structural markers or empty list if there is no structural marker under cursor or it should not be
   * handled (highlighted) for any reason.
   * @apiNote platform iterates handlers, until one returns a non-empty list of ranges
   */
  List<TextRange> getCodeBlockMarkerRanges(PsiElement elementAtCursor);

  /**
   * Checks if cursor is inside the block supported by this handler.
   *
   * @return range of the smallest code block cursor is in or empty range if not in code block.
   */
  TextRange getCodeBlockRange(PsiElement elementAtCursor);

  /**
   * Attempts to find code block range using extension points.
   */
  static TextRange findCodeBlockRange(Editor editor, PsiFile psiFile) {
    int offset = TargetElementUtil.adjustOffset(psiFile, editor.getDocument(), editor.getCaretModel().getOffset());
    PsiElement contextElement = psiFile.findElementAt(offset);
    if (contextElement == null) return EMPTY_RANGE;

    return forLanguage(contextElement.getLanguage()).stream()
      .map(handler -> handler.getCodeBlockRange(contextElement))
      .filter(codeBlockRange -> !codeBlockRange.isEmpty())
      .findFirst().orElse(EMPTY_RANGE);
  }

  /**
   * @return ranges of markers to highlight iff {@code markerPsiElement} is one of these markers.
   */
  static List<TextRange> findMarkersRanges(@Nullable PsiElement markerPsiElement) {
    if (markerPsiElement == null) {
      return Collections.emptyList();
    }
    for (CodeBlockSupportHandler handler : forLanguage(markerPsiElement.getLanguage())) {
      List<TextRange> rangesToHighlight = handler.getCodeBlockMarkerRanges(markerPsiElement);
      if (!rangesToHighlight.isEmpty()) {
        return rangesToHighlight;
      }
    }
    return Collections.emptyList();
  }

  /**
   * @param language language helps to use a proper psi subtree in case of multi-root psi trees
   * @return ranges of markers to highlight iff there is a marker in {@code psiFile} at {@code offset}.
   * @see #findMarkersRanges(PsiElement)
   */
  static List<TextRange> findMarkersRanges(@Nullable PsiFile psiFile, Language language, int offset) {
    if (psiFile == null) {
      return Collections.emptyList();
    }
    return findMarkersRanges(psiFile.getViewProvider().findElementAt(offset, language));
  }
}
