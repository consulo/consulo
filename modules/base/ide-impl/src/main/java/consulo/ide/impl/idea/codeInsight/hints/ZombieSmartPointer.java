// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.document.util.Segment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public final class ZombieSmartPointer implements SmartPsiElementPointer<PsiElement> {
    private Supplier<Project> projectSupp;
    private Supplier<VirtualFile> fileSupp;

    public void setProjectSupp(Supplier<Project> projectSupp) {
        this.projectSupp = projectSupp;
    }

    public void setFileSupp(Supplier<VirtualFile> fileSupp) {
        this.fileSupp = fileSupp;
    }

    @Override
    public PsiElement getElement() {
        return null;
    }

    @Override
    public PsiFile getContainingFile() {
        return null;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return projectSupp.get();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return fileSupp.get();
    }

    @Override
    public Segment getRange() {
        return null;
    }

    @Override
    public Segment getPsiRange() {
        return null;
    }
}
