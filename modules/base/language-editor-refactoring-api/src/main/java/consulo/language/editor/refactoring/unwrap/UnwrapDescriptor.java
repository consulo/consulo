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

package consulo.language.editor.refactoring.unwrap;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UnwrapDescriptor extends LanguageExtension  {
  ExtensionPointCacheKey<UnwrapDescriptor, ByLanguageValue<List<UnwrapDescriptor>>> KEY = ExtensionPointCacheKey.create("UnwrapDescriptor", LanguageOneToMany.build(false));

  @Nonnull
  public static List<UnwrapDescriptor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(UnwrapDescriptor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  List<Pair<PsiElement, Unwrapper>> collectUnwrappers(Project project, Editor editor, PsiFile file);

  boolean showOptionsDialog();

  boolean shouldTryToRestoreCaretPosition();
}
