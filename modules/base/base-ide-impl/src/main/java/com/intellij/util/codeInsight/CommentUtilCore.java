/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.util.codeInsight;

import com.intellij.lang.*;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import consulo.lang.LanguageVersion;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.annotation.access.RequiredReadAction;

public class CommentUtilCore {
  @RequiredReadAction
  public static boolean isComment(@Nullable final PsiElement element) {
    return element != null && isCommentToken(element.getNode().getElementType(), element.getLanguageVersion());
  }

  @RequiredReadAction
  public static boolean isComment(@Nullable final ASTNode node) {
    if (node == null) {
      return false;
    }
    final PsiElement psi = node.getPsi();
    return psi != null && isComment(psi);
  }

  @RequiredReadAction
  public static boolean isCommentTextElement(final PsiElement element) {
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(element.getLanguage());
    if (commenter instanceof CodeDocumentationAwareCommenterEx) {
      final CodeDocumentationAwareCommenterEx commenterEx = (CodeDocumentationAwareCommenterEx)commenter;
      if (commenterEx.isDocumentationCommentText(element)) return true;
      if (element instanceof PsiComment && commenterEx.isDocumentationComment((PsiComment)element)) return false;
    }

    return isComment(element);
  }

  public static boolean isCommentToken(@Nonnull IElementType tokenType, @Nonnull LanguageVersion languageVersion) {
    final Language language = tokenType.getLanguage();
    if(language != languageVersion.getLanguage()) {
      return false;
    }

    final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(language);

    if (parserDefinition != null) {
      final TokenSet commentTokens = parserDefinition.getCommentTokens(languageVersion);

      if (commentTokens.contains(tokenType)) {
        return true;
      }
    }
    return false;
  }
}
