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
package consulo.sandboxPlugin.lang.version;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import consulo.sandboxPlugin.lang.Sand2FileType;
import consulo.sandboxPlugin.lang.lexer.Sand2Lexer;
import consulo.sandboxPlugin.lang.psi.Sand2Tokens;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import org.jetbrains.annotations.NotNull;

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
    List<Pair<IElementType, IElementType>> list = new ArrayList<>(2);
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
  public Lexer createLexer() {
    return new Sand2Lexer();
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
