/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.impl.ast;

import consulo.application.ApplicationManager;
import consulo.language.ast.ASTNode;
import consulo.language.ast.FileASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.CharTable;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

//TODO: rename/regroup?

public class SharedImplUtil {
  private static final Logger LOG = Logger.getInstance(SharedImplUtil.class);
  private static final boolean CHECK_FOR_READ_ACTION = DebugUtil.DO_EXPENSIVE_CHECKS || ApplicationManager.getApplication().isInternal();

  private SharedImplUtil() {
  }

  public static PsiElement getParent(ASTNode thisElement) {
    return SourceTreeToPsiMap.treeElementToPsi(thisElement.getTreeParent());
  }

  public static PsiElement getFirstChild(ASTNode element) {
    return SourceTreeToPsiMap.treeElementToPsi(element.getFirstChildNode());
  }

  @Nullable
  public static PsiElement getLastChild(ASTNode element) {
    return SourceTreeToPsiMap.treeElementToPsi(element.getLastChildNode());
  }

  public static PsiElement getNextSibling(ASTNode thisElement) {
    return SourceTreeToPsiMap.treeElementToPsi(thisElement.getTreeNext());
  }

  public static PsiElement getPrevSibling(ASTNode thisElement) {
    return SourceTreeToPsiMap.treeElementToPsi(thisElement.getTreePrev());
  }

  public static PsiFile getContainingFile(ASTNode thisElement) {
    FileASTNode element = findFileElement(thisElement);
    PsiElement psiElement = element == null ? null : element.getPsi();
    if (psiElement == null) return null;
    return psiElement.getContainingFile();
  }

  public static boolean isValid(ASTNode thisElement) {
    LOG.assertTrue(thisElement instanceof PsiElement);
    PsiFile file = getContainingFile(thisElement);
    return file != null && file.isValid();
  }

  public static boolean isWritable(ASTNode thisElement) {
    PsiFile file = getContainingFile(thisElement);
    return file == null || file.isWritable();
  }

  public static FileASTNode findFileElement(@Nonnull ASTNode element) {
    ASTNode parent = element.getTreeParent();
    while (parent != null) {
      element = parent;
      parent = parent.getTreeParent();
    }

    if (element instanceof FileASTNode) {
      return (FileASTNode)element;
    }
    return null;
  }

  public static CharTable findCharTableByTree(ASTNode tree) {
    while (tree != null) {
      final CharTable userData = tree.getUserData(CharTable.CHAR_TABLE_KEY);
      if (userData != null) return userData;
      if (tree instanceof FileElement) return ((FileElement)tree).getCharTable();
      tree = tree.getTreeParent();
    }
    LOG.error("Invalid root element");
    return null;
  }

  public static PsiElement addRange(PsiElement thisElement,
                                    PsiElement first,
                                    PsiElement last,
                                    ASTNode anchor,
                                    Boolean before) throws IncorrectOperationException {
    CheckUtil.checkWritable(thisElement);
    final CharTable table = findCharTableByTree(SourceTreeToPsiMap.psiElementToTree(thisElement));

    TreeElement copyFirst = null;
    ASTNode copyLast = null;
    ASTNode next = SourceTreeToPsiMap.psiElementToTree(last).getTreeNext();
    ASTNode parent = null;
    for (ASTNode element = SourceTreeToPsiMap.psiElementToTree(first); element != next; element = element.getTreeNext()) {
      TreeElement elementCopy = ChangeUtil.copyElement((TreeElement)element, table);
      if (element == first.getNode()) {
        copyFirst = elementCopy;
      }
      if (element == last.getNode()) {
        copyLast = elementCopy;
      }
      if (parent == null) {
        parent = elementCopy.getTreeParent();
      }
      else {
        if(elementCopy.getElementType() == TokenType.WHITE_SPACE)
          CodeEditUtil.setNodeGenerated(elementCopy, true);
        parent.addChild(elementCopy, null);
      }
    }
    if (copyFirst == null) return null;
    copyFirst = ((CompositeElement)SourceTreeToPsiMap.psiElementToTree(thisElement)).addInternal(copyFirst, copyLast, anchor, before);
    for (TreeElement element = copyFirst; element != null; element = element.getTreeNext()) {
      element = ChangeUtil.decodeInformation(element);
      if (element.getTreePrev() == null) {
        copyFirst = element;
      }
    }
    return SourceTreeToPsiMap.treeElementToPsi(copyFirst);
  }

  public static PsiManager getManagerByTree(final ASTNode node) {
    if(node instanceof FileElement) return node.getPsi().getManager();
    return node.getTreeParent().getPsi().getManager();
  }

  public static ASTNode[] getChildrenOfType(ASTNode node, IElementType elementType) {
    int count = countChildrenOfType(node, elementType);
    if (count == 0) {
      return ASTNode.EMPTY_ARRAY;
    }
    final ASTNode[] result = new ASTNode[count];
    count = 0;
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == elementType) {
        result[count++] = child;
      }
    }
    return result;
  }

  private static int countChildrenOfType(@Nonnull ASTNode node, @Nonnull IElementType elementType) {
    // no lock is needed because all chameleons are expanded already
    int count = 0;
    for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() == elementType) {
        count++;
      }
    }

    return count;
  }

  public static void acceptChildren(PsiElementVisitor visitor, ASTNode root) {
    ASTNode childNode = root.getFirstChildNode();

    while (childNode != null) {
      final PsiElement psi;
      if (childNode instanceof PsiElement) {
        psi = (PsiElement)childNode;
      }
      else {
        psi = childNode.getPsi();
      }
      psi.accept(visitor);

      childNode = childNode.getTreeNext();
    }
  }

  public static PsiElement doReplace(PsiElement psiElement, TreeElement treeElement, PsiElement newElement) {
    CompositeElement treeParent = treeElement.getTreeParent();
    LOG.assertTrue(treeParent != null);
    CheckUtil.checkWritable(psiElement);
    TreeElement elementCopy = ChangeUtil.copyToElement(newElement);
    treeParent.replaceChildInternal(treeElement, elementCopy);
    elementCopy = ChangeUtil.decodeInformation(elementCopy);
    final PsiElement result = SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    treeElement.invalidate();
    return result;
  }
}
