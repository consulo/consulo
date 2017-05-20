/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.intellij.lang.impl;

import com.intellij.lang.*;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import consulo.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class PsiBuilderFactoryImpl extends PsiBuilderFactory {
  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project, @NotNull final ASTNode chameleon, LanguageVersion languageVersion) {
    return createBuilder(project, chameleon, null, chameleon.getElementType().getLanguage(), languageVersion, chameleon.getChars());
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project, @NotNull final LighterLazyParseableNode chameleon, LanguageVersion languageVersion) {
    final Language language = chameleon.getTokenType().getLanguage();
    ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

    return new PsiBuilderImpl(project, parserDefinition, languageVersion, createLexer(languageVersion), chameleon, chameleon.getText());
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project,
                                  @NotNull final ASTNode chameleon,
                                  @Nullable final Lexer lexer,
                                  @NotNull final Language lang,
                                  @NotNull LanguageVersion languageVersion,
                                  @NotNull final CharSequence seq) {
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    return new PsiBuilderImpl(project, parserDefinition, languageVersion, lexer != null ? lexer : createLexer(languageVersion), chameleon, seq);
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final Project project,
                                  @NotNull final LighterLazyParseableNode chameleon,
                                  @Nullable final Lexer lexer,
                                  @NotNull final Language lang,
                                  @NotNull LanguageVersion languageVersion,
                                  @NotNull final CharSequence seq) {
    final Language language = chameleon.getTokenType().getLanguage();
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);
    return new PsiBuilderImpl(project, parserDefinition, languageVersion, lexer != null ? lexer : createLexer(languageVersion), chameleon, seq);
  }

  private static Lexer createLexer(LanguageVersion languageVersion) {
    Language lang = languageVersion.getLanguage();
    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    assert parserDefinition != null : "ParserDefinition absent for language: " + lang.getID();
    return parserDefinition.createLexer(languageVersion);
  }

  @NotNull
  @Override
  public PsiBuilder createBuilder(@NotNull final ParserDefinition parserDefinition,
                                  @NotNull final Lexer lexer,
                                  @NotNull LanguageVersion languageVersion,
                                  @NotNull final CharSequence seq) {
    return new PsiBuilderImpl(null, null, parserDefinition, lexer, languageVersion, null, seq, null, null);
  }
}
