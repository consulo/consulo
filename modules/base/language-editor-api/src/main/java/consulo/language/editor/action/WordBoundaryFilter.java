// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * Defines boundaries between language tokens which should be used as stops for next/prev-word caret movements.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class WordBoundaryFilter implements LanguageExtension {
  private static final ExtensionPointCacheKey<WordBoundaryFilter, ByLanguageValue<WordBoundaryFilter>> KEY =
          ExtensionPointCacheKey.create("WordBoundaryFilter", LanguageOneToOne.build(new WordBoundaryFilter() {
            @Nonnull
            @Override
            public Language getLanguage() {
              return Language.ANY;
            }
          }));

  @Nonnull
  public static WordBoundaryFilter forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(WordBoundaryFilter.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Given types of two distinct subsequent tokens returned by {@link HighlighterIterator#getTokenType()}, says whether a boundary
   * between them should be recognized by 'Move Caret to Prev/Next Word' actions.
   * <p>
   * Default implementation assumes a word boundary between any two distinct tokens. Override this method to adjust the logic to the
   * specifics of particular programming language.
   *
   * @see HighlighterIterator
   * @see EditorHighlighter
   */
  public boolean isWordBoundary(@Nonnull IElementType previousTokenType, @Nonnull IElementType tokenType) {
    return !Objects.equals(previousTokenType, tokenType);
  }
}
