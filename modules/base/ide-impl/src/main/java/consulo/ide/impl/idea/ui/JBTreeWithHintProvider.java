/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.dataContext.DataManager;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

/**
 * @author Konstantin Bulenkov
 * @deprecated
 * @see HintUpdateSupply
 */
public class JBTreeWithHintProvider extends DnDAwareTree {
  {
    new HintUpdateSupply(this) {
      @Override
      protected PsiElement getPsiElementForHint(Object selectedValue) {
        return JBTreeWithHintProvider.this.getPsiElementForHint(selectedValue);
      }
    };
  }

  public JBTreeWithHintProvider() {
  }

  public JBTreeWithHintProvider(TreeModel treemodel) {
    super(treemodel);
  }

  public JBTreeWithHintProvider(TreeNode root) {
    super(root);
  }

  @Nullable
  protected PsiElement getPsiElementForHint(Object selectedValue) {
    return DataManager.getInstance().getDataContext(this).getData(PsiElement.KEY);
  }
}
