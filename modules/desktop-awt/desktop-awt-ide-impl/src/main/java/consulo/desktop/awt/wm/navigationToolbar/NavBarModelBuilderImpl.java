// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.ide.navigationToolbar.NavBarModelExtensions;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

public class NavBarModelBuilderImpl extends NavBarModelBuilder {
    @Override
    public void traverseToRoot(
        @Nonnull PsiElement psiElement,
        @Nonnull Set<VirtualFile> roots,
        @Nonnull List<Object> model,
        @Nullable NavBarModelExtension ownerExtension
    ) {
        List<NavBarModelExtension> extensions = NavBarModelExtension.EP_NAME.getExtensionList();

        for (PsiElement e = normalize(psiElement, ownerExtension), next = null; e != null;
             e = normalize(next, ownerExtension), next = null) {
            // check if we're running circles due to getParent()->normalize/adjust()
            if (model.contains(e)) {
                break;
            }
            model.add(e);

            // check if a root is reached
            VirtualFile vFile = PsiUtilCore.getVirtualFile(e);
            if (roots.contains(vFile)) {
                break;
            }

            for (NavBarModelExtension ext : extensions) {
                PsiElement parent = ext.getParent(e);
                if (parent != null && parent != e) {
                    //noinspection AssignmentToForLoopParameter
                    next = parent;
                    break;
                }
            }
        }
    }

    protected static PsiElement normalize(@Nullable PsiElement e) {
        return NavBarModelExtensions.normalize(getOriginalElement(e));
    }

    @Nullable
    protected static PsiElement normalize(@Nullable PsiElement e, NavBarModelExtension ownerExtension) {
        PsiElement originalElement = getOriginalElement(e);
        if (ownerExtension != null) {
            return originalElement != null ? ownerExtension.adjustElement(originalElement) : null;
        }
        return NavBarModelExtensions.normalize(originalElement);
    }

    @Nullable
    private static PsiElement getOriginalElement(@Nullable PsiElement e) {
        if (e == null || !e.isValid()) {
            return null;
        }

        PsiFile containingFile = e.getContainingFile();
        if (containingFile != null && containingFile.getVirtualFile() == null) {
            return null;
        }

        PsiElement orig = e.getOriginalElement();
        return !(e instanceof PsiCompiledElement) && orig instanceof PsiCompiledElement ? e : orig;
    }
}
