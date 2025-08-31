/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.language.ast.IElementType;
import consulo.language.ast.IStrongWhitespaceHolderElementType;
import consulo.language.ast.TokenSet;
import consulo.language.impl.DebugUtil;
import consulo.language.lexer.Lexer;
import consulo.language.psi.OuterLanguageElement;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiWhiteSpace;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class TreeUtil {
  private static final Key<String> UNCLOSED_ELEMENT_PROPERTY = Key.create("UNCLOSED_ELEMENT_PROPERTY");

  private TreeUtil() {
  }

  public static void ensureParsed(ASTNode node) {
    if (node != null) {
      node.getFirstChildNode();
    }
  }

  public static boolean isCollapsedChameleon(ASTNode node) {
    return node instanceof LazyParseableElement && !((LazyParseableElement)node).isParsed();
  }

  @Nullable
  public static ASTNode findChildBackward(ASTNode parent, IElementType type) {
    if (DebugUtil.CHECK_INSIDE_ATOMIC_ACTION_ENABLED) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }
    for (ASTNode element = parent.getLastChildNode(); element != null; element = element.getTreePrev()) {
      if (element.getElementType() == type) return element;
    }
    return null;
  }

  @Nullable
  public static ASTNode skipElements(ASTNode element, TokenSet types) {
    while (true) {
      if (element == null) return null;
      if (!types.contains(element.getElementType())) break;
      element = element.getTreeNext();
    }
    return element;
  }

  @Nullable
  public static ASTNode skipElementsBack(@Nullable ASTNode element, TokenSet types) {
    if (element == null) return null;
    if (!types.contains(element.getElementType())) return element;

    ASTNode parent = element.getTreeParent();
    ASTNode prev = element;
    while (prev instanceof CompositeElement) {
      if (!types.contains(prev.getElementType())) return prev;
      prev = prev.getTreePrev();
    }
    if (prev == null) return null;
    ASTNode firstChildNode = parent.getFirstChildNode();
    ASTNode lastRelevant = null;
    while (firstChildNode != prev) {
      if (!types.contains(firstChildNode.getElementType())) lastRelevant = firstChildNode;
      firstChildNode = firstChildNode.getTreeNext();
    }
    return lastRelevant;
  }

  @Nullable
  public static ASTNode findParent(ASTNode element, IElementType type) {
    for (ASTNode parent = element.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
      if (parent.getElementType() == type) return parent;
    }
    return null;
  }

  @Nullable
  public static ASTNode findParent(ASTNode element, TokenSet types) {
    for (ASTNode parent = element.getTreeParent(); parent != null; parent = parent.getTreeParent()) {
      if (types.contains(parent.getElementType())) return parent;
    }
    return null;
  }

  @Nullable
  public static LeafElement findFirstLeaf(ASTNode element) {
    return (LeafElement)findFirstLeaf(element, true);
  }

  public static ASTNode findFirstLeaf(ASTNode element, boolean expandChameleons) {
    if (element instanceof LeafElement || !expandChameleons && isCollapsedChameleon(element)) {
      return element;
    }
    else {
      for (ASTNode child = element.getFirstChildNode(); child != null; child = child.getTreeNext()) {
        ASTNode leaf = findFirstLeaf(child, expandChameleons);
        if (leaf != null) return leaf;
      }
      return null;
    }
  }

  private static boolean isLeafOrCollapsedChameleon(ASTNode node) {
    return node instanceof LeafElement || isCollapsedChameleon(node);
  }

  @Nullable
  public static ASTNode findLastLeaf(ASTNode element) {
    return findLastLeaf(element, true);
  }

  public static ASTNode findLastLeaf(ASTNode element, boolean expandChameleons) {
    if (element instanceof LeafElement || !expandChameleons && isCollapsedChameleon(element)) {
      return element;
    }
    for (ASTNode child = element.getLastChildNode(); child != null; child = child.getTreePrev()) {
      ASTNode leaf = findLastLeaf(child);
      if (leaf != null) return leaf;
    }
    return null;
  }

  @Nullable
  public static ASTNode findSibling(ASTNode start, IElementType elementType) {
    ASTNode child = start;
    while (true) {
      if (child == null) return null;
      if (child.getElementType() == elementType) return child;
      child = child.getTreeNext();
    }
  }

  @Nullable
  public static ASTNode findSibling(ASTNode start, TokenSet types) {
    ASTNode child = start;
    while (true) {
      if (child == null) return null;
      if (types.contains(child.getElementType())) return child;
      child = child.getTreeNext();
    }
  }

  @Nullable
  public static ASTNode findSiblingBackward(ASTNode start, IElementType elementType) {
    ASTNode child = start;
    while (true) {
      if (child == null) return null;
      if (child.getElementType() == elementType) return child;
      child = child.getTreePrev();
    }
  }


  @Nullable
  public static ASTNode findSiblingBackward(ASTNode start, TokenSet types) {
    ASTNode child = start;
    while (true) {
      if (child == null) return null;
      if (types.contains(child.getElementType())) return child;
      child = child.getTreePrev();
    }
  }

  @Nullable
  public static ASTNode findCommonParent(ASTNode one, ASTNode two) {
    // optimization
    if (one == two) return one;
    Set<ASTNode> parents = new HashSet<>(20);
    while (one != null) {
      parents.add(one);
      one = one.getTreeParent();
    }
    while (two != null) {
      if (parents.contains(two)) return two;
      two = two.getTreeParent();
    }
    return null;
  }

  public static Couple<ASTNode> findTopmostSiblingParents(ASTNode one, ASTNode two) {
    if (one == two) return Couple.of(null, null);

    LinkedList<ASTNode> oneParents = new LinkedList<>();
    while (one != null) {
      oneParents.add(one);
      one = one.getTreeParent();
    }
    LinkedList<ASTNode> twoParents = new LinkedList<>();
    while (two != null) {
      twoParents.add(two);
      two = two.getTreeParent();
    }

    do {
      one = oneParents.pollLast();
      two = twoParents.pollLast();
    }
    while (one == two && one != null);

    return Couple.of(one, two);
  }

  public static void clearCaches(@Nonnull TreeElement tree) {
    tree.acceptTree(new RecursiveTreeElementWalkingVisitor(false) {
      @Override
      protected void visitNode(TreeElement element) {
        element.clearCaches();
        super.visitNode(element);
      }
    });
  }

  @Nullable
  public static LeafElement nextLeaf(@Nonnull LeafElement node) {
    return nextLeaf(node, null);
  }

  @Nullable
  public static ASTNode nextLeaf(@Nonnull ASTNode node) {
    return nextLeaf((TreeElement)node, null);
  }

  public static final Key<FileElement> CONTAINING_FILE_KEY_AFTER_REPARSE = Key.create("CONTAINING_FILE_KEY_AFTER_REPARSE");

  public static FileElement getFileElement(TreeElement element) {
    TreeElement parent = element;
    while (parent != null && !(parent instanceof FileElement)) {
      parent = parent.getTreeParent();
    }
    if (parent == null) {
      parent = element.getUserData(CONTAINING_FILE_KEY_AFTER_REPARSE);
    }
    return (FileElement)parent;
  }

  @Nullable
  public static ASTNode prevLeaf(ASTNode node) {
    return prevLeaf((TreeElement)node, null);
  }

  public static boolean isStrongWhitespaceHolder(IElementType type) {
    return type instanceof IStrongWhitespaceHolderElementType;
  }

  public static String getTokenText(Lexer lexer) {
    return lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
  }

  @Nullable
  public static LeafElement nextLeaf(@Nonnull TreeElement start, CommonParentState commonParent) {
    return (LeafElement)nextLeaf(start, commonParent, null, true);
  }

  @Nullable
  public static TreeElement nextLeaf(@Nonnull TreeElement start, CommonParentState commonParent, IElementType searchedType, boolean expandChameleons) {
    TreeElement element = start;
    while (element != null) {
      if (commonParent != null) {
        commonParent.startLeafBranchStart = element;
        initStrongWhitespaceHolder(commonParent, element, true);
      }
      TreeElement nextTree = element;
      TreeElement next = null;
      while (next == null && (nextTree = nextTree.getTreeNext()) != null) {
        if (nextTree.getElementType() == searchedType) {
          return nextTree;
        }
        next = findFirstLeafOrType(nextTree, searchedType, commonParent, expandChameleons);
      }
      if (next != null) {
        if (commonParent != null) commonParent.nextLeafBranchStart = nextTree;
        return next;
      }
      element = element.getTreeParent();
    }
    return null;
  }

  private static void initStrongWhitespaceHolder(CommonParentState commonParent, ASTNode start, boolean slopeSide) {
    if (start instanceof CompositeElement && (isStrongWhitespaceHolder(start.getElementType()) || slopeSide && start.getUserData(UNCLOSED_ELEMENT_PROPERTY) != null)) {
      commonParent.strongWhiteSpaceHolder = (CompositeElement)start;
      commonParent.isStrongElementOnRisingSlope = slopeSide;
    }
  }

  @Nullable
  private static TreeElement findFirstLeafOrType(@Nonnull TreeElement element, final IElementType searchedType, final CommonParentState commonParent, final boolean expandChameleons) {
    class MyVisitor extends RecursiveTreeElementWalkingVisitor {
      private TreeElement result;

      private MyVisitor(boolean doTransform) {
        super(doTransform);
      }

      @Override
      protected void visitNode(TreeElement node) {
        if (result != null) return;

        if (commonParent != null) {
          initStrongWhitespaceHolder(commonParent, node, false);
        }
        if (!expandChameleons && isCollapsedChameleon(node) || node instanceof LeafElement || node.getElementType() == searchedType) {
          result = node;
          return;
        }

        super.visitNode(node);
      }
    }

    MyVisitor visitor = new MyVisitor(expandChameleons);
    element.acceptTree(visitor);
    return visitor.result;
  }

  @Nullable
  public static ASTNode prevLeaf(TreeElement start, @Nullable CommonParentState commonParent) {
    while (true) {
      if (start == null) return null;
      if (commonParent != null) {
        if (commonParent.strongWhiteSpaceHolder != null && start.getUserData(UNCLOSED_ELEMENT_PROPERTY) != null) {
          commonParent.strongWhiteSpaceHolder = (CompositeElement)start;
        }
        commonParent.nextLeafBranchStart = start;
      }
      ASTNode prevTree = start;
      ASTNode prev = null;
      while (prev == null && (prevTree = prevTree.getTreePrev()) != null) {
        prev = findLastLeaf(prevTree);
      }
      if (prev != null) {
        if (commonParent != null) commonParent.startLeafBranchStart = (TreeElement)prevTree;
        return prev;
      }
      start = start.getTreeParent();
    }
  }

  @Nullable
  public static ASTNode nextLeaf(@Nullable ASTNode start, boolean expandChameleons) {
    while (start != null) {
      for (ASTNode each = start.getTreeNext(); each != null; each = each.getTreeNext()) {
        ASTNode leaf = findFirstLeaf(each, expandChameleons);
        if (leaf != null) return leaf;
      }
      start = start.getTreeParent();
    }
    return null;
  }

  @Nullable
  public static ASTNode prevLeaf(@Nullable ASTNode start, boolean expandChameleons) {
    while (start != null) {
      for (ASTNode each = start.getTreePrev(); each != null; each = each.getTreePrev()) {
        ASTNode leaf = findLastLeaf(each, expandChameleons);
        if (leaf != null) return leaf;
      }
      start = start.getTreeParent();
    }
    return null;
  }

  @Nullable
  public static ASTNode getLastChild(ASTNode element) {
    ASTNode child = element;
    while (child != null) {
      element = child;
      child = element.getLastChildNode();
    }
    return element;
  }

  public static final class CommonParentState {
    TreeElement startLeafBranchStart;
    public ASTNode nextLeafBranchStart;
    CompositeElement strongWhiteSpaceHolder;
    boolean isStrongElementOnRisingSlope = true;
  }

  public static boolean containsOuterLanguageElements(@Nonnull ASTNode node) {
    AtomicBoolean result = new AtomicBoolean(false);
    ((TreeElement)node).acceptTree(new RecursiveTreeElementWalkingVisitor() {
      @Override
      protected void visitNode(TreeElement element) {
        if (element instanceof OuterLanguageElement) {
          result.set(true);
          stopWalking();
          return;
        }
        super.visitNode(element);
      }
    });
    return result.get();
  }

  @Nullable
  public static ASTNode skipWhitespaceAndComments(ASTNode node, boolean forward) {
    return skipWhitespaceCommentsAndTokens(node, TokenSet.EMPTY, forward);
  }

  @Nullable
  public static ASTNode skipWhitespaceCommentsAndTokens(ASTNode node, @Nonnull TokenSet alsoSkip, boolean forward) {
    ASTNode element = node;
    while (true) {
      if (element == null) return null;
      if (!isWhitespaceOrComment(element) && !alsoSkip.contains(element.getElementType())) break;
      element = forward ? element.getTreeNext() : element.getTreePrev();
    }
    return element;
  }

  public static boolean isWhitespaceOrComment(ASTNode element) {
    return element.getPsi() instanceof PsiWhiteSpace || element.getPsi() instanceof PsiComment;
  }
}
