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
package consulo.language.version;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 14:09/27.06.13
 */
public class LanguageVersionUtil {
  @RequiredReadAction
  public static LanguageVersion findLanguageVersion(@Nonnull Language language, @Nullable Project project, @Nullable VirtualFile virtualFile) {
    final LanguageVersion languageVersion = LanguageVersion.KEY.get(virtualFile);
    if (languageVersion != null) {
      return languageVersion;
    }
    else {
      return LanguageVersionResolver.forLanguage(language).getLanguageVersion(language, project, virtualFile);
    }
  }

  @RequiredReadAction
  public static LanguageVersion findLanguageVersion(@Nonnull Language language, @Nonnull PsiFile psiFile) {
    if (psiFile.getLanguage() == language) {
      return psiFile.getLanguageVersion();
    }

    FileViewProvider viewProvider = psiFile.getViewProvider();

    PsiFile psi = viewProvider.getPsi(language);
    if (psi == null) {
      return LanguageVersionResolver.forLanguage(language).getLanguageVersion(language, psiFile);
    }
    return psi.getLanguageVersion();
  }

  @RequiredReadAction
  public static LanguageVersion findLanguageVersion(@Nonnull Language language, @Nonnull PsiElement element) {
    if (element.getLanguage() == language) {
      return element.getLanguageVersion();
    }
    PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) {
      return LanguageVersionResolver.forLanguage(language).getLanguageVersion(language, element);
    }
    return findLanguageVersion(language, containingFile);
  }

  @RequiredReadAction
  public static LanguageVersion findDefaultVersion(@Nonnull Language language) {
    return LanguageVersionResolver.forLanguage(language).getLanguageVersion(language, null, null);
  }
}
