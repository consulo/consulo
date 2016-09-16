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
package consulo.lang.util;

import com.intellij.lang.Language;
import consulo.lang.LanguageVersion;
import consulo.lang.LanguageVersionResolvers;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 14:09/27.06.13
 */
@SuppressWarnings("unchecked")
public class LanguageVersionUtil {
  public static <T extends Language> LanguageVersion<T> findLanguageVersion(@NotNull T language, @Nullable Project project, @Nullable VirtualFile virtualFile) {

    final LanguageVersion<? extends Language> languageVersion = LanguageVersion.KEY.get(virtualFile);
    if (languageVersion != null) {
      return (LanguageVersion<T>)languageVersion;
    }
    else {
      return (LanguageVersion<T>)LanguageVersionResolvers.INSTANCE.forLanguage(language).getLanguageVersion(language, project, virtualFile);
    }
  }

  public static LanguageVersion<?> findLanguageVersion(@NotNull Language language, @NotNull PsiFile psiFile) {
    if (psiFile.getLanguage() == language) {
      return psiFile.getLanguageVersion();
    }

    FileViewProvider viewProvider = psiFile.getViewProvider();

    PsiFile psi = viewProvider.getPsi(language);
    if (psi == null) {
      return LanguageVersionResolvers.INSTANCE.forLanguage(language).getLanguageVersion(language, psiFile);
    }
    return psi.getLanguageVersion();
  }

  public static LanguageVersion<?> findLanguageVersion(@NotNull Language language, @NotNull PsiElement element) {
    if (element.getLanguage() == language) {
      return element.getLanguageVersion();
    }
    PsiFile containingFile = element.getContainingFile();
    if(containingFile == null) {
      return LanguageVersionResolvers.INSTANCE.forLanguage(language).getLanguageVersion(language, element);
    }
    return findLanguageVersion(language, containingFile);
  }

  public static <T extends Language> LanguageVersion<T> findDefaultVersion(@NotNull T language) {
    return (LanguageVersion<T>)LanguageVersionResolvers.INSTANCE.forLanguage(language).getLanguageVersion(language, null, null);
  }
}
