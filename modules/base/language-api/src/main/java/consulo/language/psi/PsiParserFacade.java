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

package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.project.Project;
import consulo.language.util.IncorrectOperationException;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PsiParserFacade {
  @Nonnull
  static PsiParserFacade getInstance(@Nonnull Project project) {
    return project.getInstance(PsiParserFacade.class);
  }
  
  /**
   * Creates an PsiWhiteSpace with the specified text.
   *
   * @param s the text of whitespace
   * @return the created whitespace instance.
   * @throws IncorrectOperationException if the text does not specify a valid whitespace.
   */
  @Nonnull
  PsiElement createWhiteSpaceFromText(@Nonnull String s) throws IncorrectOperationException;

  /**
   * Creates a line comment for the specified language.
   */
  @Nonnull
  PsiComment createLineCommentFromText(@Nonnull LanguageFileType fileType, @Nonnull String text) throws IncorrectOperationException;

  /**
   * Creates a line comment for the specified language.
   */
  @Nonnull
  PsiComment createBlockCommentFromText(@Nonnull Language language, @Nonnull String text) throws IncorrectOperationException;

  /**
   * Creates a line comment for the specified language or block comment if language doesn't support line ones
   */
  @Nonnull
  PsiComment createLineOrBlockCommentFromText(@Nonnull Language lang, @Nonnull String text) throws IncorrectOperationException;

  @Deprecated
  class SERVICE {
    private SERVICE() {
    }

    public static PsiParserFacade getInstance(Project project) {
      return PsiParserFacade.getInstance(project);
    }
  }
}
