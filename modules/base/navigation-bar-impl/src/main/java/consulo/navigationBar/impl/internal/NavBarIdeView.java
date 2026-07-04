// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.component.util.pointer.Pointer;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.internal.NavBarInternal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class NavBarIdeView implements IdeView {
    private final List<Pointer<? extends NavBarItem>> mySelection;

    NavBarIdeView(List<Pointer<? extends NavBarItem>> selection) {
        mySelection = selection;
    }

    @Override
    @RequiredReadAction
    public PsiDirectory[] getDirectories() {
        List<PsiDirectory> result = new ArrayList<>();
        for (Pointer<? extends NavBarItem> pointer : mySelection) {
            NavBarItem item = pointer.dereference();
            if (item != null) {
                result.addAll(NavBarItemUtil.psiDirectories(item));
            }
        }
        return result.toArray(PsiDirectory.EMPTY_ARRAY);
    }

    @Override
    public @Nullable PsiDirectory getOrChooseDirectory() {
        NavBarInternal navBarInternal = Application.get().getInstance(NavBarInternal.class);
        return (@Nullable PsiDirectory) navBarInternal.getOrChooseDirectory(this);
    }

    @Override
    public void selectElement(PsiElement element) {
        if (element instanceof Navigatable navigatable) {
            if (navigatable.canNavigate()) {
                navigatable.navigate(true);
            }
        }
    }
}
