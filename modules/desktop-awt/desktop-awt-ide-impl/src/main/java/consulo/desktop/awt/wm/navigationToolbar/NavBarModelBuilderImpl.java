// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.component.extension.ExtensionPoint;
import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.ide.navigationToolbar.NavBarModelExtensions;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.util.lang.ref.SimpleReference;
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
        ExtensionPoint<NavBarModelExtension> extensionPoint = psiElement.getApplication().getExtensionPoint(NavBarModelExtension.class);
        SimpleReference<PsiElement> elem = SimpleReference.create(normalize(psiElement, ownerExtension));
        for (PsiElement next = null; !elem.isNull(); elem.set(normalize(next, ownerExtension)), next = null) {
            // check if we're running circles due to getParent()->normalize/adjust()
            if (model.contains(elem.get())) {
                break;
            }
            model.add(elem.get());

            // check if a root is reached
            VirtualFile vFile = PsiUtilCore.getVirtualFile(elem.get());
            if (roots.contains(vFile)) {
                break;
            }

            PsiElement nextCandidate = extensionPoint.computeSafeIfAny(ext -> {
                PsiElement parent = ext.getParent(elem.get());
                return parent != elem.get() ? parent : null;
            });
            if (nextCandidate != null) {
                next = nextCandidate;
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
