/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Defines whether or not some elements can be indented when a user selects a fragment of text and invokes "indent" action (normally by
 * pressing [TAB]). The elements which are said to be unmovable (canIndent() returns false) do not change their indentation. This may be
 * useful for cases like HEREDOC text handling.
 *
 * @author Rustam Vishnyakov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface IndentStrategy extends LanguageExtension {
  ExtensionPointCacheKey<IndentStrategy, ByLanguageValue<IndentStrategy>> KEY = ExtensionPointCacheKey.create("IndentStrategy", LanguageOneToOne.build(new IndentStrategy() {
    @Nonnull
    @Override
    public Language getLanguage() {
      return Language.ANY;
    }

    @Override
    public boolean canIndent(PsiElement element) {
      return true;
    }
  }));

  @Nonnull
  static IndentStrategy forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(IndentStrategy.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nonnull
  static IndentStrategy forFile(@Nullable PsiFile file) {
    if (file != null) {
      Language language = file.getLanguage();
      return forLanguage(language);
    }
    return forLanguage(Language.ANY);
  }

  /**
   * Checks if an element can be indented.
   *
   * @param element The element to check.
   * @return True if the element can change its indentation, false if the indentation must be preserved.
   */
  boolean canIndent(PsiElement element);
}
