// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.component.util.PluginExceptionUtil;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.NavBarItemProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Delegates old implementations
 *
 * @see NavBarModelExtension
 */
@ExtensionImpl(order = "last")
public class DefaultNavBarItemProvider implements NavBarItemProvider {

    @Override
    @RequiredReadAction
    public @Nullable NavBarItem findParent(NavBarItem item) {
        if (!(item instanceof PsiNavBarItem psiItem)) {
            return null;
        }

        try {
            PsiUtilCore.ensureValid(psiItem.getData());
        }
        catch (Throwable t) {
            return null;
        }

        // TODO: cache all roots? (like passing through NavBarModelBuilder.traverseToRoot)
        // TODO: hash all roots? (Set instead of Sequence)

        Project project;
        try {
            project = psiItem.getData().getProject();
        }
        catch (Throwable t) {
            project = null;
        }

        if (project != null) {
            ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            List<VirtualFile> allRoots = new ArrayList<>();
            VirtualFile baseDir = project.getBaseDir();
            if (baseDir != null) {
                allRoots.add(baseDir);
            }
            allRoots.addAll(additionalRoots(project));
            allRoots.removeIf(root -> root.getParent() != null && projectFileIndex.isInContent(root.getParent()));

            if (allRoots.contains(PsiUtilCore.getVirtualFile(psiItem.getData()))) {
                return null;
            }
        }

        PsiElement parent = parentFromOldExtensions(psiItem);
        if (parent == null) {
            return null;
        }
        PsiFile containingFile = parent.getContainingFile();
        if (containingFile != null && containingFile.getVirtualFile() == null) {
            return null;
        }

        PsiElement adjustedParent = adjustedParent(parent, psiItem.getOwnerExtension());
        if (adjustedParent != null) {
            return new PsiNavBarItem(adjustedParent, psiItem.getOwnerExtension());
        }
        else {
            return null;
        }
    }

    private static PsiElement originalParent(PsiElement parent) {
        PsiElement originalElement = parent.getOriginalElement();
        if (!(originalElement instanceof PsiCompiledElement) || parent instanceof PsiCompiledElement) {
            ensurePsiFromExtensionIsValid(originalElement, "Original parent is invalid", parent.getClass());
            return originalElement;
        }
        else {
            return parent;
        }
    }

    private static @Nullable PsiElement adjustedParent(PsiElement parent, @Nullable NavBarModelExtension ownerExtension) {
        PsiElement originalParent = originalParent(parent);
        PsiElement adjustedByOwner = ownerExtension != null ? ownerExtension.adjustElement(originalParent) : null;
        if (adjustedByOwner != null) {
            ensurePsiFromExtensionIsValid(adjustedByOwner, "Owner extension returned invalid psi after adjustment", ownerExtension.getClass());
            return adjustedByOwner;
        }
        else {
            return adjustWithAllExtensions(originalParent);
        }
    }

    @Override
    @RequiredReadAction
    public Iterable<NavBarItem> iterateChildren(NavBarItem item) {
        if (!(item instanceof DefaultNavBarItem<?> defaultItem)) {
            return List.of();
        }
        List<NavBarItem> result = new ArrayList<>();
        for (NavBarModelExtension ext : Application.get().getExtensionList(NavBarModelExtension.class)) {
            List<Object> children = new ArrayList<>();
            ext.processChildren(defaultItem.getData(), null /*TODO: think about passing root here*/, child -> {
                children.add(child);
                return true;
            });
            for (Object child : children) {
                NavBarItem childItem = compatibilityNavBarItem(child, ext);
                if (childItem != null) {
                    result.add(childItem);
                }
            }
        }
        return result;
    }

    private static @Nullable PsiElement parentFromOldExtensions(PsiNavBarItem item) {
        for (NavBarModelExtension ext : Application.get().getExtensionList(NavBarModelExtension.class)) {
            try {
                PsiElement parent = ext.getParent(item.getData());
                if (parent == null || parent == item.getData()) {
                    continue;
                }
                ensurePsiFromExtensionIsValid(parent, "Extension returned invalid parent", ext.getClass());
                return parent;
            }
            catch (ProcessCanceledException pce) {
                // implementations may throw PCE manually, try to replace it with expected exception
                ProgressManager.checkCanceled();
            }
        }
        return null;
    }

    public static <T> @Nullable T fromOldExtensions(Function<? super NavBarModelExtension, ? extends @Nullable T> selector) {
        for (NavBarModelExtension ext : Application.get().getExtensionList(NavBarModelExtension.class)) {
            T selected = selector.apply(ext);
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    public static @Nullable PsiElement adjustWithAllExtensions(PsiElement element) {
        PsiElement result = element;

        List<NavBarModelExtension> extensions = Application.get().getExtensionList(NavBarModelExtension.class);
        for (int i = extensions.size() - 1; i >= 0; i--) {
            NavBarModelExtension ext = extensions.get(i);
            result = ext.adjustElement(result);
            if (result == null) {
                return null;
            }
            ensurePsiFromExtensionIsValid(result, "Invalid psi returned from " + ext.getClass() + " while adjusting", ext.getClass());
        }
        return result;
    }

    private static List<VirtualFile> additionalRoots(Project project) {
        List<VirtualFile> resultRoots = new ArrayList<>();
        for (NavBarModelExtension ext : Application.get().getExtensionList(NavBarModelExtension.class)) {
            resultRoots.addAll(ext.additionalRoots(project));
        }
        return resultRoots;
    }

    static void ensurePsiFromExtensionIsValid(PsiElement psi, String message, @Nullable Class<?> clazz) {
        try {
            PsiUtilCore.ensureValid(psi);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            if (clazz != null) {
                throw PluginExceptionUtil.createByClass(message + ", psi class: " + psi.getClass().getCanonicalName(), t, clazz);
            }
            else {
                throw new IllegalStateException(message + ", psi class: " + psi.getClass().getCanonicalName(), t);
            }
        }
    }

    public static @Nullable NavBarItem compatibilityNavBarItem(Object o, @Nullable NavBarModelExtension ext) {
        if (o instanceof Project project) {
            return new ProjectNavBarItem(project);
        }
        if (o instanceof Module module) {
            return new ModuleNavBarItem(module);
        }
        if (o instanceof PsiElement psiElement) {
            if (ext != null && ext.normalizeChildren()) {
                PsiElement adjusted = adjustWithAllExtensions(psiElement);
                return adjusted != null ? new PsiNavBarItem(adjusted, null) : null;
            }
            else {
                return new PsiNavBarItem(psiElement, null);
            }
        }
        if (o instanceof OrderEntry orderEntry) {
            return new OrderEntryNavBarItem(orderEntry);
        }
        return new DefaultNavBarItem<>(o);
    }
}
