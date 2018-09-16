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
package com.intellij.ui;

import javax.annotation.Nullable;

import javax.swing.tree.TreeNode;

public class CheckboxTreeBase extends CheckboxTreeNoPolicy {
  public static class CheckPolicy {
    final boolean checkChildrenWithCheckedParent;
    final boolean uncheckChildrenWithUncheckedParent;
    final boolean checkParentWithCheckedChild;
    final boolean uncheckParentWithUncheckedChild;

    public CheckPolicy(final boolean checkChildrenWithCheckedParent,
                       final boolean uncheckChildrenWithUncheckedParent,
                       final boolean checkParentWithCheckedChild,
                       final boolean uncheckParentWithUncheckedChild) {
      this.checkChildrenWithCheckedParent = checkChildrenWithCheckedParent;
      this.uncheckChildrenWithUncheckedParent = uncheckChildrenWithUncheckedParent;
      this.checkParentWithCheckedChild = checkParentWithCheckedChild;
      this.uncheckParentWithUncheckedChild = uncheckParentWithUncheckedChild;
    }
  }

  private final CheckPolicy myCheckPolicy;
  private static final CheckPolicy DEFAULT_POLICY = new CheckPolicy(true, true, false, true);

  public CheckboxTreeBase() {
    this(new CheckboxTreeCellRendererBase(), null);
  }

  public CheckboxTreeBase(final CheckboxTreeCellRendererBase cellRenderer, CheckedTreeNode root) {
    this(cellRenderer, root, DEFAULT_POLICY);
  }

  public CheckboxTreeBase(CheckboxTreeCellRendererBase cellRenderer, @Nullable CheckedTreeNode root, CheckPolicy checkPolicy) {
    super(cellRenderer, root);
    myCheckPolicy = checkPolicy;
  }

  @Override
  protected void adjustParentsAndChildren(final CheckedTreeNode node, final boolean checked) {
    changeNodeState(node, checked);
    if (!checked) {
      if (myCheckPolicy.uncheckParentWithUncheckedChild) {
        TreeNode parent = node.getParent();
        while (parent != null) {
          if (parent instanceof CheckedTreeNode) {
            changeNodeState((CheckedTreeNode)parent, false);
          }
          parent = parent.getParent();
        }
      }
      if (myCheckPolicy.uncheckChildrenWithUncheckedParent) {
        checkOrUncheckChildren(node, false);
      }
    }
    else {
      if (myCheckPolicy.checkChildrenWithCheckedParent) {
        checkOrUncheckChildren(node, true);
      }

      if (myCheckPolicy.checkParentWithCheckedChild) {
        TreeNode parent = node.getParent();
        while (parent != null) {
          if (parent instanceof CheckedTreeNode) {
            changeNodeState((CheckedTreeNode)parent, true);
          }
          parent = parent.getParent();
        }
      }

    }
    repaint();
  }
}
