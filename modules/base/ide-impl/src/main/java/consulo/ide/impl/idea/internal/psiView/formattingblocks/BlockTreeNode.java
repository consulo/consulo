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
package consulo.ide.impl.idea.internal.psiView.formattingblocks;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.template.DataLanguageBlockWrapper;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.tree.PresentationData;

import jakarta.annotation.Nonnull;
import java.awt.*;
public class BlockTreeNode extends SimpleNode {
  private final Block myBlock;

  public BlockTreeNode(Block block, BlockTreeNode parent) {
    super(parent);
    myBlock = block;
  }

  public Block getBlock() {
    return myBlock;
  }

  @Override
  public BlockTreeNode[] getChildren() {
    return ContainerUtil.map2Array(myBlock.getSubBlocks(), BlockTreeNode.class, block -> new BlockTreeNode(block, BlockTreeNode.this));
  }

  @Override
  protected void update(PresentationData presentation) {
    String name = myBlock.getClass().getSimpleName();
    if (myBlock instanceof DataLanguageBlockWrapper) {
      name += " (" + ((DataLanguageBlockWrapper)myBlock).getOriginal().getClass().getSimpleName() + ")";
    }
    presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

    if (myBlock.getIndent() != null) {
      presentation.addText(" " + String.valueOf(myBlock.getIndent()).replaceAll("[<>]", " "), SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    else {
      presentation.addText(" Indent: null", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
    if (myBlock.getAlignment() != null) {
      presentation
        .addText(" " + String.valueOf(myBlock.getAlignment()), new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color.darkGray));
    }
    if (myBlock.getWrap() != null) {
      presentation
        .addText(" " + String.valueOf(myBlock.getWrap()), new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.BLUE));
    }
  }


  @Nonnull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{myBlock};
  }

  @Override
  public boolean isAlwaysLeaf() {
    return myBlock.isLeaf();
  }
}
