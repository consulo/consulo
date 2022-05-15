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
package consulo.sandboxPlugin.lang.version;

import consulo.language.lexer.Lexer;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.sandboxPlugin.lang.lexer.SandLexer;
import consulo.sandboxPlugin.lang.psi.SandElements;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandLanguageVersion extends BaseSandLanguageVersion {
  public SandLanguageVersion() {
    super("DEFAULT");
  }

  @Override
  protected List<Pair<IElementType, IElementType>> createList() {
    List<Pair<IElementType, IElementType>> list = new ArrayList<Pair<IElementType, IElementType>>(1);
    list.add(new Pair<IElementType, IElementType>(SandTokens.CLASS_KEYWORD, SandElements.CLASS));
    return list;
  }

  @Override
  public FileType getFileType() {
    return SandFileType.INSTANCE;
  }

  @Nonnull
  @Override
  public Lexer createLexer() {
    return new SandLexer();
  }

  @Nonnull
  @Override
  public TokenSet getWhitespaceTokens() {
    return TokenSet.create(SandTokens.WHITE_SPACE);
  }

  @Nonnull
  @Override
  public TokenSet getCommentTokens() {
    return TokenSet.create(SandTokens.LINE_COMMENT);
  }

  @Nonnull
  @Override
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public boolean isMyElement(@javax.annotation.Nullable PsiElement element) {
    return true;
  }

  @Override
  public boolean isMyFile(@javax.annotation.Nullable Project project, @Nullable VirtualFile virtualFile) {
    return true;
  }
}
