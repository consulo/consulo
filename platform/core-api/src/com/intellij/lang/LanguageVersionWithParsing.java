/*
 * Copyright 2013 Consulo.org
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
package com.intellij.lang;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:51/24.06.13
 */
public interface LanguageVersionWithParsing extends LanguageVersion {
  @NotNull
  Lexer createLexer(@Nullable Project project);

  @NotNull
  PsiParser createParser(@Nullable Project project);

  @NotNull
  TokenSet getWhitespaceTokens();

  @NotNull
  TokenSet getCommentTokens();

  @NotNull
  TokenSet getStringLiteralElements();
}
