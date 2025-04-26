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
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.IndentHelperExtension;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeUtil;
import consulo.language.impl.psi.IndentHelper;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-09-26
 */
@ExtensionImpl(id = "default", order = "last")
public class DefaultIndentHelperExtension implements IndentHelperExtension {
    private final IndentHelper myIndentHelper;

    @Inject
    public DefaultIndentHelperExtension(IndentHelper indentHelper) {
        myIndentHelper = indentHelper;
    }

    @Override
    public boolean isAvailable(@Nonnull PsiFile file) {
        return true;
    }

    @RequiredReadAction
    @Override
    public int getIndentInner(@Nonnull PsiFile file, @Nonnull ASTNode element, boolean includeNonSpace, int recursionLevel) {
        if (recursionLevel > TOO_BIG_WALK_THRESHOLD) {
            return 0;
        }

        if (element.getTreePrev() != null) {
            ASTNode prev = element.getTreePrev();
            ASTNode lastCompositePrev;
            while (prev instanceof CompositeElement && !TreeUtil.isStrongWhitespaceHolder(prev.getElementType())) {
                lastCompositePrev = prev;
                prev = prev.getLastChildNode();
                if (prev == null) { // element.prev is "empty composite"
                    return getIndentInner(file, lastCompositePrev, includeNonSpace, recursionLevel + 1);
                }
            }

            String text = prev.getText();
            int index = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));

            if (index >= 0) {
                return myIndentHelper.getIndent(file, text.substring(index + 1), includeNonSpace);
            }

            if (includeNonSpace) {
                return getIndentInner(file, prev, includeNonSpace, recursionLevel + 1) + myIndentHelper.getIndent(file, text, includeNonSpace);
            }

            ASTNode parent = prev.getTreeParent();
            ASTNode child = prev;
            while (parent != null) {
                if (child.getTreePrev() != null) {
                    break;
                }
                child = parent;
                parent = parent.getTreeParent();
            }

            if (parent == null) {
                return myIndentHelper.getIndent(file, text, includeNonSpace);
            }
            else {
                return getIndentInner(file, prev, includeNonSpace, recursionLevel + 1);
            }
        }
        else {
            if (element.getTreeParent() == null) {
                return 0;
            }
            return getIndentInner(file, element.getTreeParent(), includeNonSpace, recursionLevel + 1);
        }
    }
}
