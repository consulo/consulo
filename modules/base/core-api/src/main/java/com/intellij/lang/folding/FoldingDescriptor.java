// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.lang.folding;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.BitUtil;
import com.intellij.util.ObjectUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Defines a single folding region in the code.
 *
 * <p><a name="Dependencies"><b>Dependencies</b></a></p>
 * Dependencies are objects (in particular, instances of {@link com.intellij.openapi.util.ModificationTracker},
 * more info - {@link com.intellij.psi.util.CachedValueProvider.Result#getDependencyItems here}),
 * which can be tracked for changes, that should trigger folding regions recalculation for an editor (initiating code folding pass).
 *
 * @author max
 * @see FoldingBuilder
 */
public class FoldingDescriptor {
  public static final FoldingDescriptor[] EMPTY = new FoldingDescriptor[0];

  private static final byte FLAG_NEVER_EXPANDS = 1;
  private static final byte FLAG_COLLAPSED_BY_DEFAULT_DEFINED = 1 << 1;
  private static final byte FLAG_COLLAPSED_BY_DEFAULT = 1 << 2;
  private static final byte FLAG_CAN_BE_REMOVED_WHEN_COLLAPSED = 1 << 3;
  private static final byte FLAG_GUTTER_MARK_ENABLED_FOR_SINGLE_LINE = 1 << 4;

  private final ASTNode myElement;
  private final TextRange myRange;
  @Nullable
  private final FoldingGroup myGroup;
  private final Set<Object> myDependencies;
  private String myPlaceholderText;
  private byte myFlags;

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node  The node to which the folding region is related. The node is then passed to
   *              {@link FoldingBuilder#getPlaceholderText(ASTNode)} and
   *              {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range The folded text range in file
   */
  public FoldingDescriptor(@Nonnull ASTNode node, @Nonnull TextRange range) {
    this(node, range, null);
  }

  public FoldingDescriptor(@Nonnull PsiElement element, @Nonnull TextRange range) {
    this(ObjectUtils.assertNotNull(element.getNode()), range, null);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node  The node to which the folding region is related. The node is then passed to
   *              {@link FoldingBuilder#getPlaceholderText(ASTNode)} and
   *              {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range The folded text range in file
   * @param group Regions with the same group instance expand and collapse together.
   */
  public FoldingDescriptor(@Nonnull ASTNode node, @Nonnull TextRange range, @Nullable FoldingGroup group) {
    this(node, range, group, Collections.emptySet());
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node         The node to which the folding region is related. The node is then passed to
   *                     {@link FoldingBuilder#getPlaceholderText(ASTNode)} and
   *                     {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range        The folded text range in file
   * @param group        Regions with the same group instance expand and collapse together.
   * @param dependencies folding dependencies: other files or elements that could change
   *                     folding description, see <a href="#Dependencies">Dependencies</a>
   */
  public FoldingDescriptor(@Nonnull ASTNode node, @Nonnull TextRange range, @Nullable FoldingGroup group, Set<Object> dependencies) {
    this(node, range, group, dependencies, false);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node         The node to which the folding region is related. The node is then passed to
   *                     {@link FoldingBuilder#getPlaceholderText(ASTNode)} and
   *                     {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range        The folded text range in file
   * @param group        Regions with the same group instance expand and collapse together.
   * @param dependencies folding dependencies: other files or elements that could change, see <a href="#Dependencies">Dependencies</a>
   * @param neverExpands shall be true for fold regions that must not be ever expanded.
   */
  public FoldingDescriptor(@Nonnull ASTNode node, @Nonnull TextRange range, @Nullable FoldingGroup group, Set<Object> dependencies, boolean neverExpands) {
    this(node, range, group, dependencies, neverExpands, null, null);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param e               PSI element to which the folding region is related.
   * @param start           Folded text range's start offset in file
   * @param end             Folded text range's end offset in file
   * @param group           Regions with the same group instance expand and collapse together.
   * @param placeholderText Text displayed instead of folded text, when the region is collapsed
   */
  public FoldingDescriptor(@Nonnull PsiElement e, int start, int end, @Nullable FoldingGroup group, @Nonnull String placeholderText) {
    this(e.getNode(), new TextRange(start, end), group, placeholderText);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node            The node to which the folding region is related. The node is then passed to
   *                        {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range           The folded text range in file
   * @param group           Regions with the same group instance expand and collapse together.
   * @param placeholderText Text displayed instead of folded text, when the region is collapsed
   */
  public FoldingDescriptor(@Nonnull ASTNode node, @Nonnull TextRange range, @Nullable FoldingGroup group, @Nonnull String placeholderText) {
    this(node, range, group, Collections.emptySet(), false, placeholderText, null);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node               The node to which the folding region is related. The node is then passed to
   *                           {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range              The folded text range in file
   * @param group              Regions with the same group instance expand and collapse together.
   * @param placeholderText    Text displayed instead of folded text, when the region is collapsed
   * @param collapsedByDefault Whether the region should be collapsed for newly opened files
   * @param dependencies       folding dependencies: other files or elements that could change, see <a href="#Dependencies">Dependencies</a>
   */
  public FoldingDescriptor(@Nonnull ASTNode node,
                           @Nonnull TextRange range,
                           @Nullable FoldingGroup group,
                           @Nonnull String placeholderText,
                           @Nullable Boolean collapsedByDefault,
                           @Nonnull Set<Object> dependencies) {
    this(node, range, group, dependencies, false, placeholderText, collapsedByDefault);
  }

  /**
   * Creates a folding region related to the specified AST node and covering the specified
   * text range.
   *
   * @param node               The node to which the folding region is related. The node is then passed to
   *                           {@link FoldingBuilder#getPlaceholderText(ASTNode)} and
   *                           {@link FoldingBuilder#isCollapsedByDefault(ASTNode)}.
   * @param range              The folded text range in file
   * @param group              Regions with the same group instance expand and collapse together.
   * @param dependencies       folding dependencies: other files or elements that could change, see <a href="#Dependencies">Dependencies</a>
   * @param neverExpands       shall be true for fold regions that must not be ever expanded.
   * @param placeholderText    Text displayed instead of folded text, when the region is collapsed
   * @param collapsedByDefault Whether the region should be collapsed for newly opened files
   */
  public FoldingDescriptor(@Nonnull ASTNode node,
                           @Nonnull TextRange range,
                           @Nullable FoldingGroup group,
                           @Nonnull Set<Object> dependencies,
                           boolean neverExpands,
                           @Nullable String placeholderText,
                           @Nullable Boolean collapsedByDefault) {
    assert range.getLength() > 0 : range + ", text: " + node.getText() + ", language = " + node.getPsi().getLanguage();
    myElement = node;
    myRange = range;
    myGroup = group;
    myDependencies = dependencies;
    assert !myDependencies.contains(null);
    myPlaceholderText = placeholderText;
    setFlag(FLAG_NEVER_EXPANDS, neverExpands);
    if (collapsedByDefault != null) {
      setFlag(FLAG_COLLAPSED_BY_DEFAULT_DEFINED, true);
      setFlag(FLAG_COLLAPSED_BY_DEFAULT, collapsedByDefault);
    }
  }

  /**
   * @return the node to which the folding region is related.
   */
  @Nonnull
  public ASTNode getElement() {
    return myElement;
  }

  /**
   * Returns the folded text range.
   *
   * @return the folded text range.
   */
  @Nonnull
  public TextRange getRange() {
    return myRange;
  }

  @Nullable
  public FoldingGroup getGroup() {
    return myGroup;
  }

  @Nullable
  public String getPlaceholderText() {
    return myPlaceholderText == null ? calcPlaceholderText() : myPlaceholderText;
  }

  //@ApiStatus.Internal
  public final String getCachedPlaceholderText() {
    return myPlaceholderText;
  }

  /**
   * @param placeholderText null means FoldingBuilder.getPlaceholderText will be used
   */
  public void setPlaceholderText(@Nullable String placeholderText) {
    myPlaceholderText = placeholderText;
  }

  protected String calcPlaceholderText() {
    PsiElement psiElement = myElement.getPsi();
    if (psiElement == null) return null;
    FoldingBuilder foldingBuilder = LanguageFolding.INSTANCE.forLanguage(psiElement.getLanguage());
    if (foldingBuilder == null) return null;
    return foldingBuilder instanceof FoldingBuilderEx ? ((FoldingBuilderEx)foldingBuilder).getPlaceholderText(myElement, myRange) : foldingBuilder.getPlaceholderText(myElement);
  }

  @Nonnull
  public Set<Object> getDependencies() {
    return myDependencies;
  }

  public boolean isNonExpandable() {
    return getFlag(FLAG_NEVER_EXPANDS);
  }

  public boolean canBeRemovedWhenCollapsed() {
    return getFlag(FLAG_CAN_BE_REMOVED_WHEN_COLLAPSED);
  }

  @Nullable
  public Boolean isCollapsedByDefault() {
    return getFlag(FLAG_COLLAPSED_BY_DEFAULT_DEFINED) ? getFlag(FLAG_COLLAPSED_BY_DEFAULT) : null;
  }

  /**
   * By default, collapsed regions are not removed automatically, even if related PSI elements become invalid.
   * This method allows to override default behaviour for specific regions.
   */
  public void setCanBeRemovedWhenCollapsed(boolean canBeRemovedWhenCollapsed) {
    setFlag(FLAG_CAN_BE_REMOVED_WHEN_COLLAPSED, canBeRemovedWhenCollapsed);
  }

  /**
   * @see #setGutterMarkEnabledForSingleLine(boolean)
   */
  public boolean isGutterMarkEnabledForSingleLine() {
    return getFlag(FLAG_GUTTER_MARK_ENABLED_FOR_SINGLE_LINE);
  }

  /**
   * See javadoc for {@link com.intellij.openapi.editor.FoldRegion#setGutterMarkEnabledForSingleLine(boolean)}.
   *
   * @see #isGutterMarkEnabledForSingleLine()
   */
  public void setGutterMarkEnabledForSingleLine(boolean value) {
    setFlag(FLAG_GUTTER_MARK_ENABLED_FOR_SINGLE_LINE, value);
  }

  private boolean getFlag(byte mask) {
    return BitUtil.isSet(myFlags, mask);
  }

  private void setFlag(byte mask, boolean value) {
    myFlags = BitUtil.set(myFlags, mask, value);
  }

  @SuppressWarnings("HardCodedStringLiteral")
  @Override
  public String toString() {
    return myRange + " for AST: " + myElement;
  }
}
