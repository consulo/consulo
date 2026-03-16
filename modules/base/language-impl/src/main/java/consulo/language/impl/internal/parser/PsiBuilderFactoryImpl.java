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

package consulo.language.impl.internal.parser;

import consulo.annotation.component.ServiceImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.LighterLazyParseableNode;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderFactory;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class PsiBuilderFactoryImpl extends PsiBuilderFactory {
  
  @Override
  public PsiBuilder createBuilder(
    Project project,
    ASTNode chameleon,
    LanguageVersion languageVersion
  ) {
    return createBuilder(project, chameleon, null, chameleon.getElementType().getLanguage(), languageVersion, chameleon.getChars());
  }

  
  @Override
  public PsiBuilder createBuilder(
    Project project,
    LighterLazyParseableNode chameleon,
    LanguageVersion languageVersion
  ) {
    Language language = chameleon.getTokenType().getLanguage();
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);

    return new PsiBuilderImpl(project, parserDefinition, languageVersion, createLexer(languageVersion), chameleon, chameleon.getText());
  }

  
  @Override
  public PsiBuilder createBuilder(Project project,
                                  ASTNode chameleon,
                                  @Nullable Lexer lexer,
                                  Language lang,
                                  LanguageVersion languageVersion,
                                  CharSequence seq) {
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(lang);
    return new PsiBuilderImpl(project, parserDefinition, languageVersion, lexer != null ? lexer : createLexer(languageVersion), chameleon, seq);
  }

  
  @Override
  public PsiBuilder createBuilder(Project project,
                                  LighterLazyParseableNode chameleon,
                                  @Nullable Lexer lexer,
                                  Language lang,
                                  LanguageVersion languageVersion,
                                  CharSequence seq) {
    Language language = chameleon.getTokenType().getLanguage();
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);
    return new PsiBuilderImpl(project, parserDefinition, languageVersion, lexer != null ? lexer : createLexer(languageVersion), chameleon, seq);
  }

  private static Lexer createLexer(LanguageVersion languageVersion) {
    Language lang = languageVersion.getLanguage();
    ParserDefinition parserDefinition = ParserDefinition.forLanguage(lang);
    assert parserDefinition != null : "ParserDefinition absent for language: " + lang.getID();
    return parserDefinition.createLexer(languageVersion);
  }

  
  @Override
  public PsiBuilder createBuilder(ParserDefinition parserDefinition,
                                  Lexer lexer,
                                  LanguageVersion languageVersion,
                                  CharSequence seq) {
    return new PsiBuilderImpl(null, null, parserDefinition, lexer, languageVersion, null, seq, null, null);
  }
}
