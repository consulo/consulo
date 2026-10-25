// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight;

import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This implementation collects specific direct children of top level elements and specific direct children of specific composite elements.
 */
public abstract class AbstractCodeBlockSupportHandler implements CodeBlockSupportHandler {
  /**
   * @return All element types which don't have a parent, e.g. {@code IF_STATEMENT}.
   * {@link #getDirectChildrenElementTypes} should never return these element types
   */
  protected abstract TokenSet getTopLevelElementTypes();

  /**
   * @return Elements which should be highlighted. These elements don't have children.
   * {@link #getDirectChildrenElementTypes} should return an empty TokenSet for an element type if it is a keyword.
   */
  protected abstract TokenSet getKeywordElementTypes();

  /**
   * @return Block element types which should be used for navigation.
   */
  protected abstract TokenSet getBlockElementTypes();

  /**
   * The method defines a highlighting tree.
   * Highlighting tree nodes are PSI elements.
   * A highlighting tree has an edge from A to B if and only if
   * <ul>
   * <li>A is a parent for B in the PSI tree</li>
   * <li>{@code getDirectChildrenElementTypes(getElementType(A)).contains(getElementType(B))}</li>
   * </ul>
   * If two keyword elements have a common ancestor in a highlighting tree they will be highlighted together.
   * The method should be consistent with {@link #getTopLevelElementTypes()} and {@link #getKeywordElementTypes()}.
   * <p/>
   * For example, for the given if statement:
   * <br>
   * <pre>
   * if expr
   * elsif expr
   * else
   * end
   * </pre>
   * the children should be defined as follows:
   * <pre>
   * IF_STATEMENT => {kIF, ELSIF_BLOCK, ELSE_BLOCK, kEND}
   * ELSIF_BLOCK => {kELSEIF}
   * ELSE_BLOCK => {kELSE}
   * </pre>
   */
  protected abstract TokenSet getDirectChildrenElementTypes(@Nullable IElementType parentElementType);

  @Override
  public List<TextRange> getCodeBlockMarkerRanges(PsiElement elementAtCursor) {
    TokenSet keywordElementTypes = getKeywordElementTypes();
    if (!keywordElementTypes.contains(PsiUtilCore.getElementType(elementAtCursor))) {
      return Collections.emptyList();
    }
    final PsiElement rootElement = getParentByTokenSet(elementAtCursor, getTopLevelElementTypes());
    if (rootElement == null) {
      return Collections.emptyList();
    }
    return computeMarkersRanges(rootElement, keywordElementTypes);
  }

  @Override
  public TextRange getCodeBlockRange(PsiElement elementAtCursor) {
    PsiElement obj = getParentByTokenSet(elementAtCursor, getBlockElementTypes());
    TextRange value = obj == null ? null : obj.getTextRange();
    return value == null ? TextRange.EMPTY_RANGE : value;
  }

  private List<TextRange> computeMarkersRanges(PsiElement rootElement, TokenSet keywordsElementTypes) {
    final IElementType type = PsiUtilCore.getElementType(rootElement);
    if (keywordsElementTypes.contains(type)) {
      return Collections.singletonList(rootElement.getTextRange());
    }
    TokenSet directChildrenTypes = getDirectChildrenElementTypes(type);
    List<TextRange> result = new ArrayList<>();
    PsiElement currentElement = rootElement.getFirstChild();
    while (currentElement != null) {
      IElementType currentElementType = PsiUtilCore.getElementType(currentElement);
      if (directChildrenTypes.contains(currentElementType)) {
        result.addAll(computeMarkersRanges(currentElement, keywordsElementTypes));
      }
      currentElement = currentElement.getNextSibling();
    }
    return result;
  }

  private static @Nullable PsiElement getParentByTokenSet(PsiElement element, TokenSet tokenSet) {
    PsiElement run = element;
    while (run != null && !tokenSet.contains(PsiUtilCore.getElementType(run))) {
      run = run.getParent();
    }
    return run;
  }
}
