/*
 * Copyright 2013-2022 consulo.io
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
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.folding.CustomFoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandTokens;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jul-22
 */
@ExtensionImpl
public class SandFoldingBuilder extends CustomFoldingBuilder {
  @RequiredReadAction
  @Override
  protected void buildLanguageFoldRegions(@Nonnull List<FoldingDescriptor> descriptors, @Nonnull PsiElement root, @Nonnull Document document, boolean quick) {
    root.accept(new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof SandClass) {
          ASTNode lbrace = element.getNode().findChildByType(SandTokens.LBRACE);
          ASTNode rbrace = element.getNode().findChildByType(SandTokens.RBRACE);

          if (lbrace != null && rbrace != null) {
            descriptors.add(new FoldingDescriptor(element, new TextRange(lbrace.getStartOffset(), rbrace.getTextRange().getEndOffset())));
          }
        }
        super.visitElement(element);
      }
    });
  }

  @Override
  protected String getLanguagePlaceholderText(@Nonnull ASTNode node, @Nonnull TextRange range) {
    return "test";
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@Nonnull ASTNode node) {
    return false;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }
}
