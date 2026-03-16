// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.introduce;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

public class PsiIntroduceTarget<T extends PsiElement> implements IntroduceTarget {
    protected final SmartPsiElementPointer<T> myPointer;

    public PsiIntroduceTarget(T psi) {
        myPointer = SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
    }

    @RequiredReadAction
    @Override
    public TextRange getTextRange() {
        return getPlace().getTextRange();
    }

    @RequiredReadAction
    @Override
    public @Nullable T getPlace() {
        return myPointer.getElement();
    }

    @RequiredReadAction
    @Override
    public String render() {
        return getPlace().getText();
    }

    @Override
    public boolean isValid() {
        return myPointer.getElement() != null;
    }
}
