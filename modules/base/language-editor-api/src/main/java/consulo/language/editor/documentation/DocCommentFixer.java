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
package consulo.language.editor.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiComment;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 9/20/12 8:37 PM
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DocCommentFixer extends LanguageExtension {
  ExtensionPointCacheKey<DocCommentFixer, ByLanguageValue<DocCommentFixer>> KEY = ExtensionPointCacheKey.create("DocCommentFixer", LanguageOneToOne.build());

  @Nullable
  static DocCommentFixer forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(DocCommentFixer.class).getOrBuildCache(KEY).get(language);
  }

  void fixComment(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiComment comment);
}
