/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.content.bundle.SdkUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.util.ModuleContentUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarPresentation {
    private static final SimpleTextAttributes WOLFED = new SimpleTextAttributes(null, null, JBColor.red, SimpleTextAttributes.STYLE_WAVED);

    private final Project myProject;

    public NavBarPresentation(Project project) {
        myProject = project;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Nullable
    public Image getIcon(Object object) {
        if (!NavBarModel.isValid(object)) {
            return null;
        }
        if (object instanceof Project) {
            return PlatformIconGroup.nodesProject();
        }
        if (object instanceof Module) {
            return PlatformIconGroup.nodesModule();
        }
        try {
            if (object instanceof PsiElement element) {
                Image icon =
                    AccessRule.read(() -> element.isValid() ? IconDescriptorUpdaters.getIcon(element, 0) : null);

                if (icon != null && (icon.getHeight() > JBUI.scale(16) || icon.getWidth() > JBUI.scale(16))) {
                    icon = ImageEffects.resize(icon, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
                }
                return icon;
            }
        }
        catch (IndexNotReadyException e) {
            return null;
        }
        if (object instanceof ModuleExtensionWithSdkOrderEntry moduleExtensionWithSdkOrderEntry) {
            return SdkUtil.getIcon(moduleExtensionWithSdkOrderEntry.getSdk());
        }
        if (object instanceof LibraryOrderEntry) {
            return PlatformIconGroup.nodesPplibfolder();
        }
        if (object instanceof ModuleOrderEntry) {
            return PlatformIconGroup.nodesModule();
        }
        return null;
    }

    @Nonnull
    protected String getPresentableText(Object object) {
        return getPresentableText(object, false);
    }

    @Nonnull
    protected String getPresentableText(Object object, boolean forPopup) {
        String text = calcPresentableText(object, forPopup);
        return text.length() > 50 ? text.substring(0, 47) + "..." : text;
    }

    @Nonnull
    public static String calcPresentableText(Object object, boolean forPopup) {
        if (!NavBarModel.isValid(object)) {
            return IdeLocalize.nodeStructureviewInvalid().get();
        }
        for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
            String text = modelExtension.getPresentableText(object, forPopup);
            if (text != null) {
                return text;
            }
        }
        return object.toString();
    }

    protected SimpleTextAttributes getTextAttributes(Object object, boolean selected) {
        if (!NavBarModel.isValid(object)) {
            return SimpleTextAttributes.REGULAR_ATTRIBUTES;
        }
        if (object instanceof PsiElement element) {
            if (!Application.get().runReadAction((Supplier<Boolean>)element::isValid)) {
                return SimpleTextAttributes.GRAYED_ATTRIBUTES;
            }
            PsiFile psiFile = element.getContainingFile();
            if (psiFile != null) {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                return new SimpleTextAttributes(
                    null,
                    selected ? null : TargetAWT.to(FileStatusManager.getInstance(myProject).getStatus(virtualFile).getColor()),
                    JBColor.red,
                    WolfTheProblemSolver.getInstance(myProject).isProblemFile(virtualFile)
                        ? SimpleTextAttributes.STYLE_WAVED
                        : SimpleTextAttributes.STYLE_PLAIN
                );
            }
            else {
                if (object instanceof PsiDirectory directory) {
                    VirtualFile vDir = directory.getVirtualFile();
                    if (vDir.getParent() == null || ProjectRootsUtil.isModuleContentRoot(vDir, myProject)) {
                        return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
                    }
                }

                if (wolfHasProblemFilesBeneath((PsiElement)object)) {
                    return WOLFED;
                }
            }
        }
        else if (object instanceof Module module) {
            if (WolfTheProblemSolver.getInstance(myProject).hasProblemFilesBeneath(module)) {
                return WOLFED;
            }

        }
        else if (object instanceof Project project) {
            Module[] modules = Application.get().runReadAction((Supplier<Module[]>)() -> ModuleManager.getInstance(project).getModules());
            for (Module module : modules) {
                if (WolfTheProblemSolver.getInstance(project).hasProblemFilesBeneath(module)) {
                    return WOLFED;
                }
            }
        }
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    public static boolean wolfHasProblemFilesBeneath(PsiElement scope) {
        return WolfTheProblemSolver.getInstance(scope.getProject()).hasProblemFilesBeneath(virtualFile -> {
            if (scope instanceof PsiDirectory directory) {
                if (!VfsUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) {
                    return false;
                }
                return ModuleContentUtil.findModuleForFile(virtualFile, scope.getProject()) == scope.getModule();
            }
            else if (scope instanceof PsiDirectoryContainer directoryContainer) {
                // TODO: remove. It doesn't look like we'll have packages in navbar ever again
                for (PsiDirectory directory : directoryContainer.getDirectories()) {
                    if (VfsUtil.isAncestor(directory.getVirtualFile(), virtualFile, false)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }
}
