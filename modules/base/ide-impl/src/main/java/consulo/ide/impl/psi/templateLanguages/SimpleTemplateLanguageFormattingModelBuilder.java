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
package consulo.ide.impl.psi.templateLanguages;

import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.codeStyle.LanguageFormatting;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.file.FileViewProvider;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.ide.impl.psi.formatter.DocumentBasedFormattingModel;
import consulo.language.codeStyle.AbstractBlock;
import consulo.language.template.TemplateLanguageFileViewProvider;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class SimpleTemplateLanguageFormattingModelBuilder implements FormattingModelBuilder {
  @Override
  @Nonnull
  public FormattingModel createModel(final PsiElement element, final CodeStyleSettings settings) {
    if (element instanceof PsiFile) {
      final FileViewProvider viewProvider = ((PsiFile)element).getViewProvider();
      if (viewProvider instanceof TemplateLanguageFileViewProvider) {
        final Language language = ((TemplateLanguageFileViewProvider)viewProvider).getTemplateDataLanguage();
        FormattingModelBuilder builder = LanguageFormatting.INSTANCE.forLanguage(language);
        if (builder != null) {
          return builder.createModel(viewProvider.getPsi(language), settings);
        }
      }
    }

    final PsiFile file = element.getContainingFile();
    return new DocumentBasedFormattingModel(new AbstractBlock(element.getNode(), Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment()) {
      @Override
      protected List<Block> buildChildren() {
        return Collections.emptyList();
      }

      @Override
      public Spacing getSpacing(final Block child1, @Nonnull final Block child2) {
        return Spacing.getReadOnlySpacing();
      }

      @Override
      public boolean isLeaf() {
        return true;
      }
    }, element.getProject(), settings, file.getFileType(), file);
  }

  @Override
  public TextRange getRangeAffectingIndent(final PsiFile file, final int offset, final ASTNode elementAtOffset) {
    return null;
  }
}
