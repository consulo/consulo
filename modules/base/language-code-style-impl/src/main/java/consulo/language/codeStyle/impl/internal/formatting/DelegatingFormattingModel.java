/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.impl.internal.formatting;

import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.FormattingDocumentModel;
import consulo.language.codeStyle.FormattingModel;
import consulo.language.codeStyle.FormattingModelEx;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class DelegatingFormattingModel implements FormattingModelEx {
  private final FormattingModel myBaseModel;
  private final Block myRootBlock;

  public DelegatingFormattingModel(FormattingModel model, Block block) {
    myBaseModel = model;
    myRootBlock = block;
  }

  @Nonnull
  @Override
  public Block getRootBlock() {
    return myRootBlock;
  }

  @Nonnull
  @Override
  public FormattingDocumentModel getDocumentModel() {
    return myBaseModel.getDocumentModel();
  }

  @Override
  public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
    return myBaseModel.replaceWhiteSpace(textRange, whiteSpace);
  }

  @Override
  public TextRange replaceWhiteSpace(TextRange textRange, ASTNode nodeAfter, String whiteSpace) {
    if (myBaseModel instanceof FormattingModelEx) {
      return ((FormattingModelEx) myBaseModel).replaceWhiteSpace(textRange, nodeAfter, whiteSpace);
    }
    return myBaseModel.replaceWhiteSpace(textRange, whiteSpace);
  }

  @Override
  public TextRange shiftIndentInsideRange(ASTNode node, TextRange range, int indent) {
    return myBaseModel.shiftIndentInsideRange(node, range, indent);
  }

  @Override
  public void commitChanges() {
    myBaseModel.commitChanges();
  }
}
