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
package com.intellij.psi.impl;

import com.intellij.ide.TypePresentationService;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.pom.PomNamedTarget;
import com.intellij.pom.PomRenameableTarget;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiTarget;
import com.intellij.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ide.IconDescriptorUpdaters;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

  @RequiredReadAction
  @Override
  public String getName() {
    if (myTarget instanceof PomNamedTarget) {
      return ((PomNamedTarget)myTarget).getName();
    }
    return null;
  }

  @Override
  public boolean isWritable() {
    if (myTarget instanceof PomRenameableTarget) {
      return ((PomRenameableTarget)myTarget).isWritable();
    }
    return false;
  }

  @Override
  public String getTypeName() {
    throw new UnsupportedOperationException("Method getTypeName is not yet implemented for " + myTarget.getClass().getName() + "; see PomDescriptionProvider");
  }

  @Nonnull
  @Override
  public PsiElement getNavigationElement() {
    if (myTarget instanceof PsiTarget) {
      return ((PsiTarget)myTarget).getNavigationElement();
    }
    return super.getNavigationElement();
  }

  @Override
  public Image getIcon() {
    Image icon = TypePresentationService.getInstance().getIcon(myTarget);
    if (icon != null) return icon;

    if (myTarget instanceof PsiTarget) {
      return IconDescriptorUpdaters.getIcon(((PsiTarget)myTarget).getNavigationElement(), 0);
    }
    return null;
  }

  @Override
  public boolean isValid() {
    if (myTarget instanceof PsiTarget) {
      return ((PsiTarget)myTarget).getNavigationElement().isValid();
    }

    return myTarget.isValid();
  }

  @RequiredWriteAction
  @Override
  public PsiElement setName(@NonNls @Nonnull String name) throws IncorrectOperationException {
    if (myTarget instanceof PomRenameableTarget) {
      ((PomRenameableTarget)myTarget).setName(name);
      return this;
    } 
    throw new UnsupportedOperationException("Cannot rename " + myTarget);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PomTargetPsiElementImpl that = (PomTargetPsiElementImpl)o;

    if (!myTarget.equals(that.myTarget)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myTarget.hashCode();
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return equals(another) ||
           (another != null && myTarget instanceof PsiTarget && another.isEquivalentTo(((PsiTarget)myTarget).getNavigationElement()));
  }

  @Override
  public PsiElement getContext() {
    if (myTarget instanceof PsiTarget) {
      return ((PsiTarget)myTarget).getNavigationElement();
    }
    return null;
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
  public boolean canNavigate() {
    return myTarget.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myTarget.canNavigateToSource();
  }

  @Override
  @Nullable
  public PsiFile getContainingFile() {
    if (myTarget instanceof PsiTarget) {
      return ((PsiTarget)myTarget).getNavigationElement().getContainingFile();
    }
    return null;
  }

  @Override
  @Nonnull
  public Language getLanguage() {
    if (myTarget instanceof PsiTarget) {
      return ((PsiTarget)myTarget).getNavigationElement().getLanguage();
    }
    return Language.ANY;
  }

  @Nonnull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public String getLocationString() {
    if (myTarget instanceof PsiTarget) {
      PsiFile file = ((PsiTarget)myTarget).getNavigationElement().getContainingFile();
      if (file != null) {
        return "(" + file.getName() + ")";
      }
    }
    return super.getLocationString();
  }
}
