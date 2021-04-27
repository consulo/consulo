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

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import consulo.annotation.access.RequiredReadAction;
import consulo.lang.LanguageVersionableParserDefinition;
import consulo.sandboxPlugin.lang.psi.SandFile;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandParsingDefinition extends LanguageVersionableParserDefinition {
  private static IFileElementType FILE = new IFileElementType(SandLanguage.INSTANCE);

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

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new SandFile(viewProvider);
  }
}
