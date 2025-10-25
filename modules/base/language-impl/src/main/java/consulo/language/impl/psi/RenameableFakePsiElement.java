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
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public abstract class RenameableFakePsiElement extends FakePsiElement implements PsiMetaOwner, PsiPresentableMetaData {
    private final PsiElement myParent;

    protected RenameableFakePsiElement(PsiElement parent) {
        myParent = parent;
    }

    @Nullable
    @Override
    public Image getIcon() {
        return null;
    }

    @Override
    public PsiElement getParent() {
        return myParent;
    }

    @Override
    public PsiFile getContainingFile() {
        return myParent.getContainingFile();
    }

    @Nullable
    @Override
    @RequiredReadAction
    public abstract String getName();

    @Override
    @Nonnull
    @RequiredReadAction
    public Language getLanguage() {
        return getContainingFile().getLanguage();
    }

    @Override
    @Nonnull
    public Project getProject() {
        return myParent.getProject();
    }

    @Nonnull
    @Override
    public PsiManager getManager() {
        return PsiManager.getInstance(getProject());
    }

    @Override
    @Nullable
    public PsiMetaData getMetaData() {
        return this;
    }

    @Override
    public PsiElement getDeclaration() {
        return this;
    }

    @Override
    @RequiredReadAction
    public String getName(PsiElement context) {
        return getName();
    }

    @Override
    public void init(PsiElement element) {
    }

    @Override
    public Object[] getDependences() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return TextRange.EMPTY_RANGE;
    }
}
