// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.util.pointer.Pointer;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.model.NavBarItemPresentation;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

public class DefaultNavBarItem<T> implements NavBarItem {
    private final T myData;

    public DefaultNavBarItem(T data) {
        myData = data;
    }

    public T getData() {
        return myData;
    }

    @Override
    public Pointer<? extends NavBarItem> createPointer() {
        return Pointer.hardPointer(this);
    }

    @Override
    @RequiredReadAction
    public NavBarItemPresentation presentation() {
        Image icon = DefaultNavBarItemProvider.fromOldExtensions(ext -> ext.getIcon(myData));
        if (icon == null) {
            icon = getIcon();
        }

        String text = DefaultNavBarItemProvider.fromOldExtensions(ext -> ext.getPresentableText(myData, false));
        if (text == null) {
            text = getText(false);
        }
        String popupText = DefaultNavBarItemProvider.fromOldExtensions(ext -> ext.getPresentableText(myData, true));
        if (popupText == null) {
            popupText = getText(true);
        }

        return new NavBarItemPresentationData(
            icon,
            text,
            popupText,
            getTextAttributes(),
            myData instanceof PsiElement element && element.getContainingFile() != null,
            myData instanceof PsiDirectory directory && NavBarItemUtil.isModuleContentRoot(directory)
        );
    }

    public @Nullable Image getIcon() {
        return null;
    }

    public String getText(boolean forPopup) {
        return String.valueOf(myData);
    }

    public SimpleTextAttributes getTextAttributes() {
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    public static SimpleTextAttributes navBarErrorAttributes() {
        SimpleTextAttributes schemeAttributes = TextAttributesUtil.fromTextAttributes(
            EditorColorsManager.getInstance()
                .getSchemeForCurrentUITheme()
                .getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)
        );
        return SimpleTextAttributes.merge(
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_WAVED, schemeAttributes.getFgColor()),
            schemeAttributes
        );
    }

    @RequiredReadAction
    public static @Nullable NavBarItem createDefaultNavBarItem(Project project, VirtualFile virtualFile) {
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiElement psiElement = virtualFile.isDirectory()
            ? psiManager.findDirectory(virtualFile)
            : psiManager.findFile(virtualFile);
        if (psiElement == null) {
            return null;
        }
        return new PsiNavBarItem(psiElement, null);
    }

    @RequiredReadAction
    static boolean wolfHasProblemFilesBeneath(PsiElement scope) {
        return WolfTheProblemSolver.getInstance(scope.getProject()).hasProblemFilesBeneath(virtualFile -> {
            if (scope instanceof PsiDirectory directory) {
                if (!VirtualFileUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) {
                    return false;
                }
                return ModuleUtilCore.findModuleForFile(virtualFile, directory.getProject())
                    == ModuleUtilCore.findModuleForPsiElement(scope);
            }
            else if (scope instanceof PsiDirectoryContainer container) {
                // TODO: remove. It doesn't look like we'll have packages in navbar ever again
                for (PsiDirectory directory : container.getDirectories()) {
                    if (VirtualFileUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        });
    }
}
