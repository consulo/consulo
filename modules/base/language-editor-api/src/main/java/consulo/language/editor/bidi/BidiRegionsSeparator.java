/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.bidi;

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

/**
 * Defines boundaries between regions for which bidi layout should be performed independently. This is required e.g. to make sure that
 * programming language elements (e.g. identifiers) are not reordered visually even if they are named using RTL languages.
 * Default implementation assumes a border between any two tokens of different types.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class BidiRegionsSeparator implements LanguageExtension {
  private static final ExtensionPointCacheKey<BidiRegionsSeparator, ByLanguageValue<BidiRegionsSeparator>> KEY =
          ExtensionPointCacheKey.create("BidiRegionsSeparator", LanguageOneToOne.build(new BidiRegionsSeparator() {
            @Override
            public boolean createBorderBetweenTokens(@Nonnull IElementType previousTokenType, @Nonnull IElementType tokenType) {
              return false;
            }

            @Nonnull
            @Override
            public Language getLanguage() {
              return Language.ANY;
            }
          }));

  @Nonnull
  public static BidiRegionsSeparator forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(BidiRegionsSeparator.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Given types of two distinct subsequent tokens returned by {@link HighlighterIterator#getTokenType()}, says whether bidi layout
   * should be performed independently on both sides of the border between tokens.
   *
   * @see HighlighterIterator
   * @see EditorHighlighter
   */
  public abstract boolean createBorderBetweenTokens(@Nonnull IElementType previousTokenType, @Nonnull IElementType tokenType);
}
