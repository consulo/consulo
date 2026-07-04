// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.NavBarItemProvider;
import consulo.navigationBar.model.NavBarItemPresentationData;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class NavBarItemUtil {
    private static final Comparator<NavBarItem> WEIGHT_COMPARATOR = Comparator.comparingInt(item -> -item.weight());
    private static final Comparator<NavBarItem> NAME_COMPARATOR = Comparator.comparing(
        item -> ((NavBarItemPresentationData) item.presentation()).text(),
        StringUtil::naturalCompare
    );
    private static final Comparator<NavBarItem> SIBLINGS_COMPARATOR = WEIGHT_COMPARATOR.thenComparing(NAME_COMPARATOR);

    private NavBarItemUtil() {
    }

    @RequiredReadAction
    public static List<NavBarItem> pathToItem(NavBarItem item) {
        Application.get().assertReadAccessAllowed();
        List<NavBarItem> result = new ArrayList<>();
        NavBarItem current = item;
        while (current != null) {
            result.add(current);
            ProgressManager.checkCanceled();
            current = findParent(current);
        }
        Collections.reverse(result);
        return result;
    }

    @RequiredReadAction
    private static @Nullable NavBarItem findParent(NavBarItem item) {
        for (NavBarItemProvider ext : Application.get().getExtensionList(NavBarItemProvider.class)) {
            NavBarItem parent = ext.findParent(item);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    @RequiredReadAction
    public static List<NavBarItem> children(NavBarItem item) {
        Application.get().assertReadAccessAllowed();
        List<NavBarItem> children = iterateAllChildren(item);
        children.sort(SIBLINGS_COMPARATOR);
        return children;
    }

    @RequiredReadAction
    private static List<NavBarItem> iterateAllChildren(NavBarItem item) {
        List<NavBarItem> result = new ArrayList<>();
        for (NavBarItemProvider ext : Application.get().getExtensionList(NavBarItemProvider.class)) {
            for (NavBarItem child : ext.iterateChildren(item)) {
                result.add(child);
            }
        }
        return result;
    }

    @RequiredReadAction
    static boolean isModuleContentRoot(PsiDirectory directory) {
        VirtualFile dir = directory.getVirtualFile();
        return dir.getParent() == null || ProjectRootsUtil.isModuleContentRoot(dir, directory.getProject());
    }

    @RequiredReadAction
    public static List<PsiDirectory> psiDirectories(NavBarItem item) {
        Application.get().assertReadAccessAllowed();
        if (item instanceof PsiNavBarItem psiItem) {
            if (psiItem.getData() instanceof PsiDirectory directory) {
                return List.of(directory);
            }
            if (psiItem.getData() instanceof PsiDirectoryContainer container) {
                return List.of(container.getDirectories());
            }
            PsiFile containingFile = psiItem.getData().getContainingFile();
            PsiDirectory dir = containingFile != null ? containingFile.getContainingDirectory() : null;
            if (dir != null) {
                return List.of(dir);
            }
            return List.of();
        }
        else if (item instanceof ModuleNavBarItem moduleItem) {
            Module data = moduleItem.getData();
            PsiManager psiManager = PsiManager.getInstance(data.getProject());
            List<PsiDirectory> result = new ArrayList<>();
            for (VirtualFile sourceRoot : ModuleRootManager.getInstance(data)
                .getContentFolderFiles(LanguageContentFolderScopes.productionAndTest())) {
                PsiDirectory directory = psiManager.findDirectory(sourceRoot);
                if (directory != null) {
                    result.add(directory);
                }
            }
            return result;
        }
        else {
            // TODO obtain directories from other NavBarItem implementations
            return List.of();
        }
    }
}
