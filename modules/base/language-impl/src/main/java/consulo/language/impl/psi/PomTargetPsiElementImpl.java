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
import consulo.application.presentation.TypePresentationService;
import consulo.language.Language;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.pom.PomNamedTarget;
import consulo.language.pom.PomRenameableTarget;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiTarget;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class PomTargetPsiElementImpl extends RenameableFakePsiElement implements PomTargetPsiElement {
    private final PomTarget myTarget;
    private final Project myProject;

    public PomTargetPsiElementImpl(@Nonnull PsiTarget target) {
        this(target.getNavigationElement().getProject(), target);
    }

    public PomTargetPsiElementImpl(@Nonnull Project project, @Nonnull PomTarget target) {
        super(null);
        myProject = project;
        myTarget = target;
    }

    @Override
    @Nonnull
    public PomTarget getTarget() {
        return myTarget;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getName() {
        if (myTarget instanceof PomNamedTarget namedTarget) {
            return namedTarget.getName();
        }
        return null;
    }

    @Override
    public boolean isWritable() {
        if (myTarget instanceof PomRenameableTarget renameableTarget) {
            return renameableTarget.isWritable();
        }
        return false;
    }

    @Override
    public String getTypeName() {
        throw new UnsupportedOperationException("Method getTypeName is not yet implemented for " + myTarget.getClass()
            .getName() + "; see PomDescriptionProvider");
    }

    @Nonnull
    @Override
    public PsiElement getNavigationElement() {
        if (myTarget instanceof PsiTarget target) {
            return target.getNavigationElement();
        }
        return super.getNavigationElement();
    }

    @Override
    @RequiredReadAction
    public Image getIcon() {
        Image icon = TypePresentationService.getInstance().getIcon(myTarget);
        if (icon != null) {
            return icon;
        }

        if (myTarget instanceof PsiTarget target) {
            return IconDescriptorUpdaters.getIcon(target.getNavigationElement(), 0);
        }
        return null;
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        if (myTarget instanceof PsiTarget target) {
            return target.getNavigationElement().isValid();
        }

        return myTarget.isValid();
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
        if (myTarget instanceof PomRenameableTarget renameableTarget) {
            renameableTarget.setName(name);
            return this;
        }
        throw new UnsupportedOperationException("Cannot rename " + myTarget);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PomTargetPsiElementImpl that = (PomTargetPsiElementImpl) o;

        return myTarget.equals(that.myTarget);
    }

    @Override
    public int hashCode() {
        return myTarget.hashCode();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return equals(another)
            || another != null && myTarget instanceof PsiTarget target && another.isEquivalentTo(target.getNavigationElement());
    }

    @Override
    public PsiElement getContext() {
        return myTarget instanceof PsiTarget target ? target.getNavigationElement() : null;
    }

    @Override
    @Nullable
    public PsiElement getParent() {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        myTarget.navigate(requestFocus);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return myTarget.canNavigate();
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return myTarget.canNavigateToSource();
    }

    @Override
    @Nullable
    public PsiFile getContainingFile() {
        if (myTarget instanceof PsiTarget target) {
            return target.getNavigationElement().getContainingFile();
        }
        return null;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Language getLanguage() {
        if (myTarget instanceof PsiTarget target) {
            return target.getNavigationElement().getLanguage();
        }
        return Language.ANY;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    @RequiredReadAction
    public String getLocationString() {
        if (myTarget instanceof PsiTarget target) {
            PsiFile file = target.getNavigationElement().getContainingFile();
            if (file != null) {
                return "(" + file.getName() + ")";
            }
        }
        return super.getLocationString();
    }
}
