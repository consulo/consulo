// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.application.dumb.IndexNotReadyException;
import consulo.component.util.pointer.Pointer;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.navigation.Navigatable;
import consulo.navigationBar.NavBarItem;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

public final class PsiNavBarItem extends DefaultNavBarItem<PsiElement> {
    private final @Nullable NavBarModelExtension myOwnerExtension;

    public PsiNavBarItem(PsiElement data, @Nullable NavBarModelExtension ownerExtension) {
        super(data);
        myOwnerExtension = ownerExtension;
    }

    public @Nullable NavBarModelExtension getOwnerExtension() {
        return myOwnerExtension;
    }

    @Override
    public Pointer<? extends NavBarItem> createPointer() {
        NavBarModelExtension ownerExtension = myOwnerExtension;
        SmartPsiElementPointer<PsiElement> smartPointer = SmartPointerManager.createPointer(getData());
        return Pointer.delegatingPointer(smartPointer::getElement, psi -> new PsiNavBarItem(psi, ownerExtension));
    }

    @Override
    public @Nullable Navigatable navigationRequest() {
        return getData() instanceof Navigatable navigatable ? navigatable : null;
    }

    @Override
    public @Nullable Image getIcon() {
        try {
            Image icon = IconDescriptorUpdaters.getIcon(getData(), 0);
            int maxDimension = Image.DEFAULT_ICON_SIZE * 2;
            if (icon.getHeight() > maxDimension || icon.getWidth() > maxDimension) {
                icon = ImageEffects.resize(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
            }
            return icon;
        }
        catch (IndexNotReadyException e) {
            return null;
        }
    }

    @Override
    public SimpleTextAttributes getTextAttributes() {
        PsiFile psiFile = getData().getContainingFile();

        if (psiFile != null) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                return new SimpleTextAttributes(null, null, DefaultNavBarItem.navBarErrorAttributes().getWaveColor(), SimpleTextAttributes.STYLE_PLAIN);
            }
            WolfTheProblemSolver problemSolver = WolfTheProblemSolver.getInstance(getData().getProject());
            int style = problemSolver.isProblemFile(virtualFile)
                ? DefaultNavBarItem.navBarErrorAttributes().getStyle()
                : SimpleTextAttributes.STYLE_PLAIN;
            FileStatusManager fileStatusManager = FileStatusManager.getInstance(getData().getProject());
            return new SimpleTextAttributes(
                null,
                TargetAWT.to(fileStatusManager.getStatus(virtualFile).getColor()),
                DefaultNavBarItem.navBarErrorAttributes().getWaveColor(),
                style
            );
        }
        else {
            if (getData() instanceof PsiDirectory directory) {
                VirtualFile vDir = directory.getVirtualFile();
                if (vDir.getParent() == null || ProjectRootsUtil.isModuleContentRoot(vDir, directory.getProject())) {
                    return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
                }
            }

            if (DefaultNavBarItem.wolfHasProblemFilesBeneath(getData())) {
                return DefaultNavBarItem.navBarErrorAttributes();
            }
        }
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    @Override
    public boolean navigateOnClick() {
        // TODO remove once DB plugin is rewritten
        Boolean shouldExpandOnClick = DefaultNavBarItemProvider.fromOldExtensions(ext -> ext.shouldExpandOnClick(getData()));
        if (shouldExpandOnClick != null) {
            return !shouldExpandOnClick;
        }
        // end of todo

        return !(getData() instanceof PsiDirectory) && !(getData() instanceof PsiDirectoryContainer);
    }

    @Override
    public int weight() {
        if (getData() instanceof PsiDirectoryContainer) {
            return 4;
        }
        if (getData() instanceof PsiDirectory) {
            return 4;
        }
        if (getData() instanceof PsiFile) {
            return 2;
        }
        if (getData() instanceof PsiNamedElement) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }
}
