/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.psi.formatter.common;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.IndentRangesCalculator;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-May-17
 * <p>
 * from kotlin  platform\lang-impl\src\com\intellij\psi\formatter\common\NodeIndentRangesCalculator.kt
 */
public class NodeIndentRangesCalculator {
  private ASTNode node;

  public NodeIndentRangesCalculator(ASTNode node) {
    this.node = node;
  }

  @RequiredReadAction
  public List<TextRange> calculateExtraRanges() {
    Document document = retrieveDocument(node);
    if (document != null) {
      TextRange ranges = node.getTextRange();
      return new IndentRangesCalculator(document, ranges).calcIndentRanges();
    }
    return Collections.singletonList(node.getTextRange());
  }

  @RequiredReadAction
  @Nullable
  private Document retrieveDocument(ASTNode node) {
    PsiFile file = node.getPsi().getContainingFile();
    return PsiDocumentManager.getInstance(node.getPsi().getProject()).getDocument(file);
  }
}
