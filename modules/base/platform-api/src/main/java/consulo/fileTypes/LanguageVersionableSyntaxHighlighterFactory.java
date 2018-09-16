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
package consulo.fileTypes;

import com.intellij.lang.Language;
import consulo.lang.LanguageVersion;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.lang.util.LanguageVersionUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 21:14/24.06.13
 *
 * This is need register in plugin.xml like
 * - <lang.syntaxHighlighterFactory key="LANGUAGE" implementationClass="CLASS"/>
 */
public abstract class LanguageVersionableSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  private final Map<LanguageVersion, SyntaxHighlighter> myHighlighters = new HashMap<LanguageVersion, SyntaxHighlighter>();
  private final Language myLanguage;

  protected LanguageVersionableSyntaxHighlighterFactory(Language language) {
    myLanguage = language;
  }

  @Nonnull
  public abstract SyntaxHighlighter getSyntaxHighlighter(@Nonnull LanguageVersion languageVersion);

  @Nonnull
  @Override
  public final SyntaxHighlighter getSyntaxHighlighter(@Nullable Project project, @Nullable VirtualFile virtualFile) {
    final LanguageVersion languageVersion = LanguageVersionUtil.findLanguageVersion(myLanguage, project, virtualFile);
    SyntaxHighlighter syntaxHighlighter = myHighlighters.get(languageVersion);
    if(syntaxHighlighter == null) {
      myHighlighters.put(languageVersion, syntaxHighlighter = getSyntaxHighlighter(languageVersion));
    }
    return syntaxHighlighter;
  }
}
