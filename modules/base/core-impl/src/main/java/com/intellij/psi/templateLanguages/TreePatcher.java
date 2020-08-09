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
package com.intellij.psi.templateLanguages;

import com.intellij.lang.ASTFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.CharTable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TreePatcher {

  /**
   * Inserts toInsert into tree
   *
   * @apiNote Inserting must not change the position (offset) of the new node in the tree (otherwise we will receive broken tree)
   */
  void insert(@Nonnull CompositeElement parent, @Nullable TreeElement anchorBefore, @Nonnull OuterLanguageElement toInsert);

  /**
   * Splits the leaf into two leaves with the same type as the original leaf
   *
   * @return first part of the split
   */
  @Nonnull
  default LeafElement split(@Nonnull LeafElement leaf, int offset, @Nonnull CharTable table) {
    CharSequence chars = leaf.getChars();
    LeafElement leftPart = ASTFactory.leaf(leaf.getElementType(), table.intern(chars, 0, offset));
    LeafElement rightPart = ASTFactory.leaf(leaf.getElementType(), table.intern(chars, offset, chars.length()));
    leaf.rawInsertAfterMe(leftPart);
    leftPart.rawInsertAfterMe(rightPart);
    leaf.rawRemove();
    return leftPart;
  }

  /**
   * Removes "middle" part of the leaf and returns the new leaf with content of the right and left parts
   * e.g. if we process whitespace leaf " \n " and range "1, 2" the result will be new leaf with content "  "
   */
  @Nonnull
  default LeafElement removeRange(@Nonnull LeafElement leaf, @Nonnull TextRange rangeToRemove, @Nonnull CharTable table) {
    CharSequence chars = leaf.getChars();
    String res = rangeToRemove.replace(chars.toString(), "");
    LeafElement newLeaf = ASTFactory.leaf(leaf.getElementType(), table.intern(res));
    leaf.rawInsertBeforeMe(newLeaf);
    leaf.rawRemove();
    return newLeaf;
  }
}
