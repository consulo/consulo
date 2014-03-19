/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.sandLanguage.lang.version;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.sandLanguage.lang.Sand2FileType;
import org.mustbe.consulo.sandLanguage.lang.lexer._Sand2Lexer;
import org.mustbe.consulo.sandLanguage.lang.psi.Sand2Tokens;
import org.mustbe.consulo.sandLanguage.lang.psi.SandElements;
import org.mustbe.consulo.sandLanguage.lang.psi.SandTokens;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class Sand2LanguageVersion extends BaseSandLanguageVersion {
  public Sand2LanguageVersion() {
    super("SAND2");
  }

  @Override
  protected List<Pair<IElementType, IElementType>> createList() {
    val list = new ArrayList<Pair<IElementType, IElementType>>(2);
    list.add(new Pair<IElementType, IElementType>(SandTokens.CLASS_KEYWORD, SandElements.CLASS));
    list.add(new Pair<IElementType, IElementType>(Sand2Tokens.DEF_KEYWORD, SandElements.DEF));
    return list;
  }

  @Override
  public FileType getFileType() {
    return Sand2FileType.INSTANCE;
  }

  @NotNull
  @Override
  public Lexer createLexer(@Nullable Project project) {
    return new FlexAdapter(new _Sand2Lexer((Reader)null));
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens() {
    return TokenSet.create(SandTokens.WHITE_SPACE);
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return TokenSet.create(SandTokens.LINE_COMMENT);
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }
}
