// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.impl.ProjectViewSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.*;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationUtil;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class PsiNavigationSupportImpl implements PsiNavigationSupport {
    @RequiredReadAction
    @Nullable
    @Override
    public Navigatable getDescriptor(@Nonnull PsiElement element) {
        if (!canNavigate(element)) {
            return null;
        }

        if (element instanceof PomTargetPsiElement) {
            return ((PomTargetPsiElement) element).getTarget();
        }

        PsiElement navigationElement = element.getNavigationElement();
        if (navigationElement instanceof PomTargetPsiElement) {
            return ((PomTargetPsiElement) navigationElement).getTarget();
        }

        int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(navigationElement);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }

        OpenFileDescriptorFactory.Builder builder = OpenFileDescriptorFactory.getInstance(navigationElement.getProject()).newBuilder(virtualFile);
        builder.offset(offset);
        builder.useCurrentWindow(NavigationUtil.USE_CURRENT_WINDOW.isIn(navigationElement));
        return builder.build();
    }

    @RequiredReadAction
    @Override
    public boolean canNavigate(@Nullable PsiElement element) {
        if (element == null || !element.isValid()) {
            return false;
        }

        VirtualFile file = PsiUtilCore.getVirtualFile(element.getNavigationElement());
        return file != null && file.isValid() && !file.is(VFileProperty.SPECIAL) && !VirtualFileUtil.isBrokenLink(file);
    }

    @Override
    public void navigateToDirectory(@Nonnull PsiDirectory psiDirectory, boolean requestFocus) {
        ProjectViewSelectInTarget.select(psiDirectory.getProject(), this, ProjectViewPaneImpl.ID, null, psiDirectory.getVirtualFile(), requestFocus);
    }
}
