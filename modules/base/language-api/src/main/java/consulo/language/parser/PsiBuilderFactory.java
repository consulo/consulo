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

package consulo.language.parser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.LighterLazyParseableNode;
import consulo.language.lexer.Lexer;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PsiBuilderFactory {
  public static PsiBuilderFactory getInstance() {
    return Application.get().getInstance(PsiBuilderFactory.class);
  }

  @Nonnull
  public abstract PsiBuilder createBuilder(@Nonnull Project project, @Nonnull ASTNode chameleon, @Nonnull LanguageVersion languageVersion);

  @Nonnull
  public abstract PsiBuilder createBuilder(@Nonnull Project project, @Nonnull LighterLazyParseableNode chameleon, @Nonnull LanguageVersion languageVersion);

  @Nonnull
  public abstract PsiBuilder createBuilder(@Nonnull Project project,
                                           @Nonnull ASTNode chameleon,
                                           @Nullable Lexer lexer,
                                           @Nonnull Language lang,
                                           @Nonnull LanguageVersion languageVersion,
                                           @Nonnull CharSequence seq);

  @Nonnull
  public abstract PsiBuilder createBuilder(@Nonnull Project project,
                                           @Nonnull LighterLazyParseableNode chameleon,
                                           @Nullable Lexer lexer,
                                           @Nonnull Language lang,
                                           @Nonnull LanguageVersion languageVersion,
                                           @Nonnull CharSequence seq);

  @Nonnull
  public abstract PsiBuilder createBuilder(@Nonnull ParserDefinition parserDefinition,
                                           @Nonnull Lexer lexer,
                                           @Nonnull LanguageVersion languageVersion,
                                           @Nonnull CharSequence seq);
}
