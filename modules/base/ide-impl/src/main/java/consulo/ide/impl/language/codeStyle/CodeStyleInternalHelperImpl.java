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
package consulo.ide.impl.language.codeStyle;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.psi.formatter.FormattingDocumentModelImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.FormattingDocumentModel;
import consulo.language.codeStyle.WhiteSpaceFormattingStrategy;
import consulo.language.codeStyle.WhiteSpaceFormattingStrategyFactory;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.*;
import consulo.language.impl.ast.Factory;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.CharTable;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 30-Apr-22
 */
@Singleton
public class CodeStyleInternalHelperImpl implements CodeStyleInternalHelper {
  @Override
  public void debugTreeToBuffer(@Nonnull Appendable buffer, @Nonnull ASTNode root, int indent, boolean skipWhiteSpaces, boolean showRanges, boolean showChildrenRanges, boolean usePsi) {
    DebugUtil.treeToBuffer(buffer, root, indent, skipWhiteSpaces, showRanges, showChildrenRanges, usePsi);
  }

  @Override
  public void replaceLastWhiteSpace(ASTNode astNode, String whiteSpace, TextRange textRange) {
    ASTNode lastWS = TreeUtil.findLastLeaf(astNode);
    if (lastWS == null) {
      return;
    }
    if (lastWS.getElementType() != TokenType.WHITE_SPACE) {
      lastWS = null;
    }
    if (lastWS != null && !lastWS.getTextRange().equals(textRange)) {
      return;
    }
    if (whiteSpace.isEmpty() && lastWS == null) {
      return;
    }
    if (lastWS != null && whiteSpace.isEmpty()) {
      lastWS.getTreeParent().removeRange(lastWS, null);
      return;
    }

    LeafElement whiteSpaceElement = ASTFactory.whitespace(whiteSpace);

    if (lastWS == null) {
      astNode.addChild(whiteSpaceElement, null);
    }
    else {
      ASTNode treeParent = lastWS.getTreeParent();
      treeParent.replaceChild(lastWS, whiteSpaceElement);
    }
  }

  @Override
  public void replaceWhiteSpace(String whiteSpace, ASTNode leafElement, IElementType whiteSpaceToken, @Nullable TextRange textRange) {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(leafElement);

    ASTNode treePrev = findPreviousWhiteSpace(leafElement, whiteSpaceToken);
    if (treePrev == null) {
      treePrev = getWsCandidate(leafElement);
    }

    if (treePrev != null && treePrev.getText().trim().isEmpty() && treePrev.getElementType() != whiteSpaceToken && treePrev.getTextLength() > 0 && !whiteSpace.isEmpty()) {
      LeafElement whiteSpaceElement = Factory.createSingleLeafElement(treePrev.getElementType(), whiteSpace, charTable, SharedImplUtil.getManagerByTree(leafElement));

      ASTNode treeParent = treePrev.getTreeParent();
      treeParent.replaceChild(treePrev, whiteSpaceElement);
    }
    else {
      LeafElement whiteSpaceElement = Factory.createSingleLeafElement(whiteSpaceToken, whiteSpace, charTable, SharedImplUtil.getManagerByTree(leafElement));

      if (treePrev == null) {
        if (!whiteSpace.isEmpty()) {
          addWhiteSpace(leafElement, whiteSpaceElement);
        }
      }
      else {
        if (!(treePrev.getElementType() == whiteSpaceToken)) {
          if (!whiteSpace.isEmpty()) {
            addWhiteSpace(treePrev, whiteSpaceElement);
          }
        }
        else {
          if (treePrev.getElementType() == whiteSpaceToken) {
            final CompositeElement treeParent = (CompositeElement)treePrev.getTreeParent();
            if (!whiteSpace.isEmpty()) {
              //          LOG.assertTrue(textRange == null || treeParent.getTextRange().equals(textRange));
              treeParent.replaceChild(treePrev, whiteSpaceElement);
            }
            else {
              treeParent.removeChild(treePrev);
            }

            // There is a possible case that more than one PSI element is matched by the target text range.
            // That is the case, for example, for Python's multi-line expression. It may looks like below:
            //     import contextlib,\
            //       math, decimal
            // Here single range contains two blocks: '\' & '\n  '. So, we may want to replace that range to another text, hence,
            // we replace last element located there with it ('\n  ') and want to remove any remaining elements ('\').
            ASTNode removeCandidate = findPreviousWhiteSpace(whiteSpaceElement, whiteSpaceToken);
            while (textRange != null && removeCandidate != null && removeCandidate.getStartOffset() >= textRange.getStartOffset()) {
              treePrev = findPreviousWhiteSpace(removeCandidate, whiteSpaceToken);
              removeCandidate.getTreeParent().removeChild(removeCandidate);
              removeCandidate = treePrev;
            }
            //treeParent.subtreeChanged();
          }
        }
      }
    }
  }

  @Nullable
  private static ASTNode findPreviousWhiteSpace(final ASTNode leafElement, final IElementType whiteSpaceTokenType) {
    final int offset = leafElement.getTextRange().getStartOffset() - 1;
    if (offset < 0) return null;
    final PsiElement psiElement = leafElement.getPsi();
    if (psiElement == null) {
      return null;
    }
    final PsiElement found = psiElement.getContainingFile().findElementAt(offset);
    if (found == null) return null;
    final ASTNode treeElement = found.getNode();
    if (treeElement != null && treeElement.getElementType() == whiteSpaceTokenType) return treeElement;
    return null;
  }

  @Nullable
  private static ASTNode getWsCandidate(@Nullable ASTNode node) {
    if (node == null) return null;
    ASTNode treePrev = node.getTreePrev();
    if (treePrev != null) {
      if (treePrev.getElementType() == TokenType.WHITE_SPACE) {
        return treePrev;
      }
      else if (treePrev.getTextLength() == 0) {
        return getWsCandidate(treePrev);
      }
      else {
        return node;
      }
    }
    final ASTNode treeParent = node.getTreeParent();

    if (treeParent == null || treeParent.getTreeParent() == null) {
      return node;
    }
    else {
      return getWsCandidate(treeParent);
    }
  }

  private static StringBuilder createNewLeafChars(final ASTNode leafElement, final TextRange textRange, final String whiteSpace) {
    final TextRange elementRange = leafElement.getTextRange();
    final String elementText = leafElement.getText();

    final StringBuilder result = new StringBuilder();

    if (elementRange.getStartOffset() < textRange.getStartOffset()) {
      result.append(elementText.substring(0, textRange.getStartOffset() - elementRange.getStartOffset()));
    }

    result.append(whiteSpace);

    if (elementRange.getEndOffset() > textRange.getEndOffset()) {
      result.append(elementText.substring(textRange.getEndOffset() - elementRange.getStartOffset()));
    }

    return result;
  }

  private static void addWhiteSpace(final ASTNode treePrev, final ASTNode whiteSpaceElement) {
    for (WhiteSpaceFormattingStrategy strategy : WhiteSpaceFormattingStrategyFactory.getAllStrategies()) {
      if (strategy.addWhitespace(treePrev, whiteSpaceElement)) {
        return;
      }
    }

    final ASTNode treeParent = treePrev.getTreeParent();
    treeParent.addChild(whiteSpaceElement, treePrev);
  }

  @Override
  public void replaceInnerWhiteSpace(@Nonnull String newWhiteSpaceText, @Nonnull ASTNode holder, @Nonnull TextRange whiteSpaceRange) {
    final CharTable charTable = SharedImplUtil.findCharTableByTree(holder);
    StringBuilder newText = createNewLeafChars(holder, whiteSpaceRange, newWhiteSpaceText);
    LeafElement newElement = Factory.createSingleLeafElement(holder.getElementType(), newText, charTable, holder.getPsi().getManager());

    holder.getTreeParent().replaceChild(holder, newElement);
  }

  @Override
  public boolean containsWhiteSpacesOnly(@Nullable ASTNode node) {
    if (node == null) return false;

    ArrayDeque<ASTNode> queue = new ArrayDeque<>();
    queue.offer(node);
    while (!queue.isEmpty()) {
      TreeElement each = (TreeElement)queue.poll();
      if (each instanceof CompositeElement && spacesOnly(each)) {
        continue;
      }

      if (each instanceof LeafElement && !spacesOnly(each)) {
        return false;
      }

      Collections.addAll(queue, each.getChildren(null));
    }
    return true;
  }


  private static boolean spacesOnly(@Nullable ASTNode node) {
    if (node == null) return false;

    if (FormatterUtil.isWhitespaceOrEmpty(node)) return true;
    PsiElement psi = node.getPsi();
    if (psi == null) {
      return false;
    }
    Language language = psi.getLanguage();
    return WhiteSpaceFormattingStrategyFactory.getStrategy(language).containsWhitespacesOnly(node);
  }

  @Nullable
  @Override
  public ASTNode getPreviousNonWhitespaceLeaf(@Nullable ASTNode node) {
    if (node == null) return null;
    ASTNode treePrev = node.getTreePrev();
    if (treePrev != null) {
      ASTNode candidate = TreeUtil.getLastChild(treePrev);
      if (candidate != null && !FormatterUtil.isWhitespaceOrEmpty(candidate)) {
        return candidate;
      }
      else {
        return getPreviousNonWhitespaceLeaf(candidate);
      }
    }
    final ASTNode treeParent = node.getTreeParent();

    if (treeParent == null || treeParent.getTreeParent() == null) {
      return null;
    }
    else {
      return getPreviousNonWhitespaceLeaf(treeParent);
    }
  }

  @Override
  public void allowToMarkNodesForPostponedFormatting(boolean value) {
    CodeEditUtil.allowToMarkNodesForPostponedFormatting(value);
  }

  @Override
  public FormattingDocumentModel createFormattingDocumentModel(PsiFile file) {
    return FormattingDocumentModel.create(file);
  }

  @Override
  public FormattingDocumentModel createFormattingDocumentModel(@Nonnull Document document, @Nullable PsiFile file) {
    return new FormattingDocumentModelImpl(document, file);
  }
}
