/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.language.editor.highlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.internal.DefaultSyntaxHighlighterFactory;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageGroupByFactory;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class SyntaxHighlighterFactory implements LanguageExtension {
  private static final ExtensionPointCacheKey<SyntaxHighlighterFactory, ByLanguageValue<SyntaxHighlighterFactory>> KEY =
          ExtensionPointCacheKey.create("SyntaxHighlighterFactory", LanguageGroupByFactory.build(new DefaultSyntaxHighlighterFactory()));

  /**
   * Returns syntax highlighter for the given language.
   *
   * @param language        a {@code Language} to get highlighter for
   * @param project     might be necessary to gather various project settings from
   * @param virtualFile might be necessary to collect file specific settings
   * @return {@code SyntaxHighlighter} interface implementation for the given file type
   */
  public static SyntaxHighlighter getSyntaxHighlighter(@Nonnull Language language, @Nullable Project project, @Nullable final VirtualFile virtualFile) {
    SyntaxHighlighterFactory highlighterFactory = Application.get().getExtensionPoint(SyntaxHighlighterFactory.class).getOrBuildCache(KEY).get(language);
    assert highlighterFactory != null;
    return highlighterFactory.getSyntaxHighlighter(project, virtualFile);
  }

  /**
   * Returns syntax highlighter for the given file type.
   * Note: it is recommended to use {@link #getSyntaxHighlighter(Language, Project, VirtualFile)} in most cases,
   * and use this method only when you are do not know the language you use.
   *
   * @param fileType    a file type to use to select appropriate highlighter
   * @param project     might be necessary to gather various project settings from
   * @param virtualFile might be necessary to collect file specific settings
   * @return {@code SyntaxHighlighter} interface implementation for the given file type
   */
  @Nullable
  public static SyntaxHighlighter getSyntaxHighlighter(final FileType fileType, final @Nullable Project project, final @Nullable VirtualFile virtualFile) {
    for (SyntaxHighlighterProvider provider : Application.get().getExtensionPoint(SyntaxHighlighterProvider.class).getExtensionList()) {
      SyntaxHighlighter highlighter = provider.create(fileType, project, virtualFile);
      if (highlighter != null) {
        return highlighter;
      }
    }
    return null;
  }

  /**
   * Override this method to provide syntax highlighting (coloring) capabilities for your language implementation.
   * By syntax highlighting we mean highlighting of keywords, comments, braces etc. where lexing the file content is enough
   * to identify proper highlighting attributes.
   * <p/>
   * Default implementation doesn't highlight anything.
   *
   * @param project     might be necessary to gather various project settings from.
   * @param virtualFile might be necessary to collect file specific settings
   * @return <code>SyntaxHighlighter</code> interface implementation for this particular language.
   */
  @Nonnull
  public abstract SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile);

  /**
   * Target language for this highlighter
   */
  @Override
  @Nonnull
  public abstract Language getLanguage();
}