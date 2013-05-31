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
package com.intellij.lang.dtd;

import com.intellij.lang.*;
import com.intellij.lang.xml.XMLParserDefinition;
import com.intellij.lexer.DtdLexer;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.parsing.xml.DtdParsing;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlEntityDecl;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class DTDParserDefinition extends XMLParserDefinition {
  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return LanguageUtil.canStickTokensTogetherByLexer(left, right, new DtdLexer(false));
  }

  public PsiFile createFile(FileViewProvider viewProvider) {
    return new XmlFileImpl(viewProvider, XmlElementType.DTD_FILE);
  }

  @NotNull
  @Override
  public PsiParser createParser(@NotNull Project project, @NotNull LanguageVersion languageVersion) {
    return new PsiParser() {
      @NotNull
      @Override
      public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder, @NotNull LanguageVersion languageVersion) {
        return new DtdParsing(root, XmlEntityDecl.EntityContextType.GENERIC_XML, builder).parse();
      }
    };
  }

  @NotNull
  @Override
  public IFileElementType getFileNodeType() {
    return XmlElementType.DTD_FILE;
  }

  @NotNull
  @Override
  public Lexer createLexer(@NotNull Project project, @NotNull LanguageVersion languageVersion) {
    return new DtdLexer(false);
  }
}
