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
package consulo.ide.impl.idea.ide.scopeView.nodes;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.Iconable;
import consulo.ide.impl.idea.packageDependencies.ui.PackageDependenciesNode;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author anna
 * @since 2006-01-30
 */
public class BasePsiNode<T extends PsiElement> extends PackageDependenciesNode {
    private final @Nullable SmartPsiElementPointer<T> myPsiElementPointer;
    private Image myIcon;

    @RequiredReadAction
    public BasePsiNode(T element) {
        super(element.getProject());
        if (element.isValid()) {
            myPsiElementPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
        }
        else {
            myPsiElementPointer = null;
        }
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiElement getPsiElement() {
        if (myPsiElementPointer == null) {
            return null;
        }
        PsiElement element = myPsiElementPointer.getElement();
        return element != null && element.isValid() ? element : null;
    }

    @Override
    @RequiredReadAction
    public Image getIcon() {
        PsiElement element = getPsiElement();
        if (myIcon == null) {
            myIcon = element != null && element.isValid()
                ? IconDescriptorUpdaters.getIcon(element, Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS)
                : null;
        }
        return myIcon;
    }

    @Override
    @RequiredReadAction
    public @Nullable ColorValue getColor() {
        if (myColor == null && getContainingFile() != null) {
            myColor = FileStatusManager.getInstance(myProject).getStatus(myPsiElementPointer.getVirtualFile()).getColor();
            if (myColor == null) {
                myColor = NOT_CHANGED;
            }
        }
        return myColor == NOT_CHANGED ? null : myColor;
    }

    @Override
    public int getWeight() {
        return 4;
    }

    @Override
    public int getContainingFiles() {
        return 0;
    }

    @Override
    @RequiredReadAction
    public boolean equals(@Nullable Object o) {
        if (isEquals()) {
            return super.equals(o);
        }
        return this == o
            || o instanceof BasePsiNode<?> that && Objects.equals(getPsiElement(), that.getPsiElement());
    }

    @Override
    @RequiredReadAction
    public int hashCode() {
        return Objects.hashCode(getPsiElement());
    }

    @RequiredReadAction
    public @Nullable PsiFile getContainingFile() {
        return myPsiElementPointer == null ? null : myPsiElementPointer.getContainingFile();
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        PsiElement element = getPsiElement();
        return element != null && element.isValid();
    }

    public boolean isDeprecated() {
        return false;
    }
}
