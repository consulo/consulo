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
package consulo.sandboxPlugin.lang;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IFileElementType;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.IStubFileElementType;
import consulo.language.version.LanguageVersionableParserDefinition;
import consulo.sandboxPlugin.lang.psi.SandFile;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
@ExtensionImpl
public class SandParsingDefinition extends LanguageVersionableParserDefinition {
  private static IFileElementType FILE = new IStubFileElementType<>(SandLanguage.INSTANCE) {
    @Override
    public int getStubVersion() {
      return 2;
    }
  };

  @Nonnull
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }

  @Nonnull
  @Override
  public IFileElementType getFileNodeType() {
    return FILE;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public PsiElement createElement(@Nonnull ASTNode node) {
    return new ASTWrapperPsiElement(node);
  }

  @Nonnull
  @Override
  public PsiFile createFile(@Nonnull FileViewProvider viewProvider) {
    return new SandFile(viewProvider);
  }
}
