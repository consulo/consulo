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
package consulo.language.codeStyle;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

public class FormatterUtil {
  // TODO [VISTALL] change it. make it more localize friendly
  public static final String REFORMAT_COMMAND_NAME = "Reformat Code";
  public static final String REFORMAT_BEFORE_COMMIT_COMMAND_NAME = "Reformat Code Before Commit";

  public static final Set<String> FORMATTER_ACTION_NAMES = Set.of(REFORMAT_COMMAND_NAME, REFORMAT_BEFORE_COMMIT_COMMAND_NAME);

  private FormatterUtil() {
  }

  public static boolean isWhitespaceOrEmpty(@Nullable ASTNode node) {
    if (node == null) return false;
    IElementType type = node.getElementType();
    return type == TokenType.WHITE_SPACE || (type != TokenType.ERROR_ELEMENT && node.getTextLength() == 0);
  }

  public static boolean isOneOf(@Nullable ASTNode node, @Nonnull IElementType... types) {
    if (node == null) return false;
    IElementType elementType = node.getElementType();
    for (IElementType each : types) {
      if (elementType == each) return true;
    }
    return false;
  }

  @Nullable
  public static ASTNode getPrevious(@Nullable ASTNode node, @Nonnull IElementType... typesToIgnore) {
    return getNextOrPrevious(node, false, typesToIgnore);
  }

  @Nullable
  public static ASTNode getNext(@Nullable ASTNode node, @Nonnull IElementType... typesToIgnore) {
    return getNextOrPrevious(node, true, typesToIgnore);
  }

  @Nullable
  private static ASTNode getNextOrPrevious(@Nullable ASTNode node, boolean isNext, @Nonnull IElementType... typesToIgnore) {
    if (node == null) return null;

    ASTNode each = isNext ? node.getTreeNext() : node.getTreePrev();
    ASTNode parent = node.getTreeParent();
    while (each == null && parent != null) {
      each = isNext ? parent.getTreeNext() : parent.getTreePrev();
      parent = parent.getTreeParent();
    }

    if (each == null) {
      return null;
    }

    for (IElementType type : typesToIgnore) {
      if (each.getElementType() == type) {
        return getNextOrPrevious(each, isNext, typesToIgnore);
      }
    }

    return each;
  }

  @Nullable
  public static ASTNode getPreviousLeaf(@Nullable ASTNode node, @Nonnull IElementType... typesToIgnore) {
    ASTNode prev = getPrevious(node, typesToIgnore);
    if (prev == null) {
      return null;
    }

    ASTNode result = prev;
    ASTNode lastChild = prev.getLastChildNode();
    while (lastChild != null) {
      result = lastChild;
      lastChild = lastChild.getLastChildNode();
    }

    for (IElementType type : typesToIgnore) {
      if (result.getElementType() == type) {
        return getPreviousLeaf(result, typesToIgnore);
      }
    }
    return result;
  }

  @Nullable
  public static ASTNode getPreviousNonWhitespaceLeaf(@Nullable ASTNode node) {
    return CodeStyleInternalHelper.getInstance().getPreviousNonWhitespaceLeaf(node);
  }

  @Nullable
  public static ASTNode getPreviousNonWhitespaceSibling(@Nullable ASTNode node) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && isWhitespaceOrEmpty(prevNode)) {
      prevNode = prevNode.getTreePrev();
    }
    return prevNode;
  }

  @Nullable
  public static ASTNode getNextNonWhitespaceSibling(@Nullable ASTNode node) {
    ASTNode next = node == null ? null : node.getTreeNext();
    while (next != null && isWhitespaceOrEmpty(next)) {
      next = next.getTreeNext();
    }
    return next;
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType) {
    return isPrecededBy(node, expectedType, IElementType.EMPTY_ARRAY);
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType, TokenSet skipTypes) {
    return isPrecededBy(node, expectedType, skipTypes.getTypes());
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, IElementType expectedType, IElementType... skipTypes) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes))) {
      prevNode = prevNode.getTreePrev();
    }
    if (prevNode == null) return false;
    return prevNode.getElementType() == expectedType;
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes) {
    return isPrecededBy(node, expectedTypes, IElementType.EMPTY_ARRAY);
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes, TokenSet skipTypes) {
    return isPrecededBy(node, expectedTypes, skipTypes.getTypes());
  }

  public static boolean isPrecededBy(@Nullable ASTNode node, TokenSet expectedTypes, IElementType... skipTypes) {
    ASTNode prevNode = node == null ? null : node.getTreePrev();
    while (prevNode != null && (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes))) {
      prevNode = prevNode.getTreePrev();
    }
    if (prevNode == null) return false;
    return expectedTypes.contains(prevNode.getElementType());
  }

  public static boolean hasPrecedingSiblingOfType(@Nullable ASTNode node, IElementType expectedSiblingType, IElementType... skipTypes) {
    for (ASTNode prevNode = node == null ? null : node.getTreePrev(); prevNode != null; prevNode = prevNode.getTreePrev()) {
      if (isWhitespaceOrEmpty(prevNode) || isOneOf(prevNode, skipTypes)) continue;
      if (prevNode.getElementType() == expectedSiblingType) return true;
    }
    return false;
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType) {
    return isFollowedBy(node, expectedType, IElementType.EMPTY_ARRAY);
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType, TokenSet skipTypes) {
    return isFollowedBy(node, expectedType, skipTypes.getTypes());
  }

  public static boolean isFollowedBy(@Nullable ASTNode node, IElementType expectedType, IElementType... skipTypes) {
    ASTNode nextNode = node == null ? null : node.getTreeNext();
    while (nextNode != null && (isWhitespaceOrEmpty(nextNode) || isOneOf(nextNode, skipTypes))) {
      nextNode = nextNode.getTreeNext();
    }
    if (nextNode == null) return false;
    return nextNode.getElementType() == expectedType;
  }

  public static boolean isIncomplete(@Nullable ASTNode node) {
    ASTNode lastChild = node == null ? null : node.getLastChildNode();
    while (lastChild != null && lastChild.getElementType() == TokenType.WHITE_SPACE) {
      lastChild = lastChild.getTreePrev();
    }
    if (lastChild == null) return false;
    if (lastChild.getElementType() == TokenType.ERROR_ELEMENT) return true;
    return isIncomplete(lastChild);
  }

  public static boolean containsWhiteSpacesOnly(@Nullable ASTNode node) {
    return CodeStyleInternalHelper.getInstance().containsWhiteSpacesOnly(node);
  }

  /**
   * There is a possible case that we want to adjust white space which is not represented at the AST/PSI tree, e.g.
   * we might have a multiline comment which uses tabs for inner lines indents and want to replace them by spaces.
   * There is no white space element then, the only leaf is the comment itself.
   * <p/>
   * This method allows such 'inner element modifications', i.e. it receives information on what new text should be used
   * at the target inner element range and performs corresponding replacement by generating new leaf with adjusted text
   * and replacing the old one by it.
   *
   * @param newWhiteSpaceText new text to use at the target inner element range
   * @param holder            target range holder
   * @param whiteSpaceRange   target range which text should be replaced by the given one
   */
  public static void replaceInnerWhiteSpace(@Nonnull final String newWhiteSpaceText, @Nonnull final ASTNode holder, @Nonnull final TextRange whiteSpaceRange) {
    CodeStyleInternalHelper.getInstance().replaceInnerWhiteSpace(newWhiteSpaceText, holder, whiteSpaceRange);
  }

  public static void replaceWhiteSpace(final String whiteSpace, final ASTNode leafElement, final IElementType whiteSpaceToken, @Nullable final TextRange textRange) {
    CodeStyleInternalHelper.getInstance().replaceWhiteSpace(whiteSpace, leafElement, whiteSpaceToken, textRange);
  }

  public static void replaceLastWhiteSpace(final ASTNode astNode, final String whiteSpace, final TextRange textRange) {
    CodeStyleInternalHelper.getInstance().replaceLastWhiteSpace(astNode, whiteSpace, textRange);
  }

  /**
   * @return <code>true</code> explicitly called 'reformat' is in  progress at the moment; <code>false</code> otherwise
   */
  public static boolean isFormatterCalledExplicitly() {
    String commandName = CommandProcessor.getInstance().getCurrentCommandName();
    return commandName != null && FORMATTER_ACTION_NAMES.contains(commandName);
  }
}
