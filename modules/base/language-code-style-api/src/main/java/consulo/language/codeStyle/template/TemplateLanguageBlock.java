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

import consulo.language.ast.ASTNode;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.AbstractBlock;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Chmutov
 * @since 2009-06-26
 */
public abstract class TemplateLanguageBlock extends AbstractBlock implements BlockWithParent {
  private final TemplateLanguageBlockFactory myBlockFactory;
  private final CodeStyleSettings mySettings;
  private List<DataLanguageBlockWrapper> myForeignChildren;
  private boolean myChildrenBuilt = false;
  private BlockWithParent myParent;

  protected TemplateLanguageBlock(@Nonnull TemplateLanguageBlockFactory blockFactory, @Nonnull CodeStyleSettings settings,
                                  @Nonnull ASTNode node, @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    this(node, null, null, blockFactory, settings, foreignChildren);
  }

  protected TemplateLanguageBlock(@Nonnull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment,
                                  @Nonnull TemplateLanguageBlockFactory blockFactory,
                                  @Nonnull CodeStyleSettings settings,
                                  @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    super(node, wrap, alignment);
    myBlockFactory = blockFactory;
    myForeignChildren = foreignChildren;
    mySettings = settings;
  }

  @Override
  protected List<Block> buildChildren() {
    myChildrenBuilt = true;
    if (isLeaf()) {
      return EMPTY;
    }
    final ArrayList<TemplateLanguageBlock> tlChildren = new ArrayList<>(5);
    for (ASTNode childNode = getNode().getFirstChildNode(); childNode != null; childNode = childNode.getTreeNext()) {
      if (FormatterUtil.containsWhiteSpacesOnly(childNode)) continue;
      if (shouldBuildBlockFor(childNode)) {
        final TemplateLanguageBlock childBlock = myBlockFactory
                .createTemplateLanguageBlock(childNode, createChildWrap(childNode), createChildAlignment(childNode), null, mySettings);
        childBlock.setParent(this);
        tlChildren.add(childBlock);
      }
    }
    final List<Block> children = (List<Block>)(myForeignChildren == null ? tlChildren : BlockUtil.mergeBlocks(tlChildren, myForeignChildren));
    //BlockUtil.printBlocks(getTextRange(), children);
    return BlockUtil.setParent(children, this);
  }

  protected boolean shouldBuildBlockFor(ASTNode childNode) {
    return childNode.getElementType() != getTemplateTextElementType() || noForeignChildren();
  }

  private boolean noForeignChildren() {
    return (myForeignChildren == null || myForeignChildren.isEmpty());
  }

  void addForeignChild(@Nonnull DataLanguageBlockWrapper foreignChild) {
    initForeignChildren();
    myForeignChildren.add(foreignChild);
  }

  void addForeignChildren(List<DataLanguageBlockWrapper> foreignChildren) {
    initForeignChildren();
    myForeignChildren.addAll(foreignChildren);
  }

  private void initForeignChildren() {
    assert !myChildrenBuilt;
    if (myForeignChildren == null) {
      myForeignChildren = new ArrayList<>(5);
    }
  }

  @Override
  @Nullable
  public Spacing getSpacing(@Nullable Block child1, @Nonnull Block child2) {
    if (child1 instanceof DataLanguageBlockWrapper && child2 instanceof DataLanguageBlockWrapper) {
      return ((DataLanguageBlockWrapper)child1).getRightHandSpacing((DataLanguageBlockWrapper)child2);
    }
    return null;
  }

  /**
   * Invoked when the current base language block is located inside a template data language block to determine the spacing after the current block.
   * @param rightNeighbor the block to the right of the current one
   * @param parent the parent block
   * @param thisBlockIndex the index of the current block in the parent block subblocks
   * @return the spacing between the current block and its right neighbor
   */
  @Nullable
  public Spacing getRightNeighborSpacing(@Nonnull Block rightNeighbor, @Nonnull DataLanguageBlockWrapper parent, int thisBlockIndex) {
    return null;
  }

  @Override
  public boolean isLeaf() {
    return noForeignChildren() && getNode().getFirstChildNode() == null;
  }

  protected abstract IElementType getTemplateTextElementType();

  @Override
  public BlockWithParent getParent() {
    return myParent;
  }

  @Override
  public void setParent(BlockWithParent newParent) {
    myParent = newParent;
  }

  /**
   * Checks if DataLanguageBlockFragmentWrapper must be created for the given text range.
   * @param range The range to check.
   * @return True by default.
   */
  public boolean isRequiredRange(TextRange range) {
    return true;
  }

  protected Wrap createChildWrap(ASTNode child) {
    return Wrap.createWrap(WrapType.NONE, false);
  }

  protected Alignment createChildAlignment(ASTNode child) {
    return null;
  }

  public CodeStyleSettings getSettings() {
    return mySettings;
  }

  public List<DataLanguageBlockWrapper> getForeignChildren() {
    return myForeignChildren;
  }

  @Nullable
  public Wrap substituteTemplateChildWrap(@Nonnull DataLanguageBlockWrapper child, @Nullable Wrap childWrap) {
    return childWrap;
  }
}

