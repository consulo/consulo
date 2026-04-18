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

package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiNamedElement;
import consulo.language.util.IncorrectOperationException;
import consulo.navigation.ItemPresentation;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class FakePsiElement extends PsiElementBase implements PsiNamedElement, ItemPresentation {
    @Override
    public ItemPresentation getPresentation() {
        return this;
    }

    @RequiredReadAction
    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    @RequiredReadAction
    @Override
    public PsiElement[] getChildren() {
        return PsiElement.EMPTY_ARRAY;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getFirstChild() {
        return null;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getLastChild() {
        return null;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNextSibling() {
        return null;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getPrevSibling() {
        return null;
    }

    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return TextRange.EMPTY_RANGE;
    }

    @RequiredReadAction
    @Override
    public int getStartOffsetInParent() {
        return 0;
    }

    @RequiredReadAction
    @Override
    public int getTextLength() {
        return 0;
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement findElementAt(int offset) {
        return null;
    }

    @Override
    public int getTextOffset() {
        return 0;
    }

    @RequiredReadAction
    @Override
    public @Nullable String getText() {
        return null;
    }

    @RequiredReadAction
    @Override
    public char[] textToCharArray() {
        return new char[0];
    }

    @RequiredReadAction
    @Override
    public boolean textContains(char c) {
        return false;
    }

    @Override
    public @Nullable ASTNode getNode() {
        return null;
    }

    @Override
    public String getPresentableText() {
        return getName();
    }

    @Override
    public @Nullable String getLocationString() {
        return null;
    }

    @Override
    public @Nullable Image getIcon() {
        return null;
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(String name) throws IncorrectOperationException {
        return null;
    }

    @Override
    public @Nullable PsiManager getManager() {
        PsiElement parent = getParent();
        if (parent != null) {
            return parent.getManager();
        }
        throw new IllegalArgumentException("Parent must be not null for return PsiManager, or override this method. Class: " + getClass());
    }
}
