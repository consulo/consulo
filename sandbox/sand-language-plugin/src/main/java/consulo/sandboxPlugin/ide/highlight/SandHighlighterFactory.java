/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.LanguageVersionableSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.version.LanguageVersion;
import consulo.sandboxPlugin.lang.SandLanguage;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
@ExtensionImpl
public class SandHighlighterFactory extends LanguageVersionableSyntaxHighlighterFactory {
  @Nonnull
  @Override
  public SyntaxHighlighter getSyntaxHighlighter(@Nonnull LanguageVersion languageVersion) {
    return new SandHighlighter(languageVersion);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }
}
