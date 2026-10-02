// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiTarget;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

class DelegatePsiTargetSimpleNamePomTarget extends DelegateSimpleNamePomTarget implements PsiTarget {
    private final PsiTarget myPsiTarget;

    DelegatePsiTargetSimpleNamePomTarget(PsiTarget delegate, String name, @Nullable Image icon, LocalizeValue typeName) {
        super(delegate, name, icon, typeName);
        myPsiTarget = delegate;
    }

    @Override
    public PsiElement getNavigationElement() {
        return myPsiTarget.getNavigationElement();
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return myPsiTarget.isValid();
    }
}
