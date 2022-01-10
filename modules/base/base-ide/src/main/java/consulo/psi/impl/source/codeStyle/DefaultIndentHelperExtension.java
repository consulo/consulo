/*
 * Copyright 2013-2018 consulo.io
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
package consulo.psi.impl.source.codeStyle;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.IndentHelper;
import com.intellij.psi.impl.source.codeStyle.IndentHelperImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-09-26
 */
public class DefaultIndentHelperExtension implements IndentHelperExtension {
  @Override
  public boolean isAvaliable(@Nonnull PsiFile file) {
    return true;
  }

  @RequiredReadAction
  @Override
  public int getIndentInner(@Nonnull IndentHelper indentHelper, @Nonnull PsiFile file, @Nonnull ASTNode element, boolean includeNonSpace, int recursionLevel) {
    if (recursionLevel > TOO_BIG_WALK_THRESHOLD) return 0;

    if (element.getTreePrev() != null) {
      ASTNode prev = element.getTreePrev();
      ASTNode lastCompositePrev;
      while (prev instanceof CompositeElement && !TreeUtil.isStrongWhitespaceHolder(prev.getElementType())) {
        lastCompositePrev = prev;
        prev = prev.getLastChildNode();
        if (prev == null) { // element.prev is "empty composite"
          return getIndentInner(indentHelper, file, lastCompositePrev, includeNonSpace, recursionLevel + 1);
        }
      }

      String text = prev.getText();
      int index = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));

      if (index >= 0) {
        return IndentHelperImpl.getIndent(file, text.substring(index + 1), includeNonSpace);
      }

      if (includeNonSpace) {
        return getIndentInner(indentHelper, file, prev, includeNonSpace, recursionLevel + 1) + IndentHelperImpl.getIndent(file, text, includeNonSpace);
      }


      ASTNode parent = prev.getTreeParent();
      ASTNode child = prev;
      while (parent != null) {
        if (child.getTreePrev() != null) break;
        child = parent;
        parent = parent.getTreeParent();
      }

      if (parent == null) {
        return IndentHelperImpl.getIndent(file, text, includeNonSpace);
      }
      else {
        return getIndentInner(indentHelper, file, prev, includeNonSpace, recursionLevel + 1);
      }
    }
    else {
      if (element.getTreeParent() == null) {
        return 0;
      }
      return getIndentInner(indentHelper, file, element.getTreeParent(), includeNonSpace, recursionLevel + 1);
    }
  }
}
