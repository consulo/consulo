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
package consulo.language.codeStyle.template;

import consulo.language.Language;
import consulo.language.codeStyle.*;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public abstract class SimpleTemplateLanguageFormattingModelBuilder implements FormattingModelBuilder {
  @Override
  @Nonnull
  public FormattingModel createModel(@Nonnull FormattingContext formattingContext) {
    PsiElement element = formattingContext.getPsiElement();
    CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
    
    if (element instanceof PsiFile) {
      final FileViewProvider viewProvider = ((PsiFile)element).getViewProvider();
      if (viewProvider instanceof TemplateLanguageFileViewProvider) {
        final Language templateDataLanguage = ((TemplateLanguageFileViewProvider)viewProvider).getTemplateDataLanguage();
        FormattingModelBuilder builder = ContainerUtil.getFirstItem(FormattingModelBuilder.forLanguage(templateDataLanguage));
        if (builder != null) {
          return builder.createModel(formattingContext.withPsiElement(viewProvider.getPsi(templateDataLanguage)));
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
}
