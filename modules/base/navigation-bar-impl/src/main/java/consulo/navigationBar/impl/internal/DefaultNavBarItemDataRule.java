// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiUtilCore;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.module.Module;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.NavBarItemProvider;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(order = "last")
public class DefaultNavBarItemDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        sink.lazyValue(NavBarItem.NAVBAR_ITEM_KEY, dataProvider -> {
            NavBarItem item = getNavBarItem(dataProvider);
            return item == null ? null : item.createPointer();
        });
    }

    @RequiredReadAction
    public static @Nullable NavBarItem getNavBarItem(DataSnapshot dataProvider) {
        // leaf element -- either from old EP impls or default one
        // owner -- EP extension provided the leaf (if any)
        PsiElement leaf = null;
        NavBarModelExtension owner = null;
        DataContext dataContext = new DataContext() {
            @Override
            public <T> @Nullable T getData(Key<T> key) {
                return dataProvider.get(key);
            }
        };
        for (NavBarModelExtension ext : Application.get().getExtensionList(NavBarModelExtension.class)) {
            PsiElement leafElement = ext.getLeafElement(dataContext);
            if (leafElement != null) {
                leaf = leafElement;
                owner = ext;
                break;
            }
        }
        if (leaf == null) {
            leaf = fromDataContext(dataProvider);
            owner = null;
        }
        if (leaf == null) {
            return null;
        }

        if (leaf.isValid()) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(leaf);
            if (virtualFile != null && Boolean.TRUE.equals(virtualFile.getUserData(NavBarModelExtension.IGNORE_IN_NAVBAR))) {
                return null;
            }
            return new PsiNavBarItem(leaf, owner);
        }
        else {
            // Narrow down the root element to the first interesting one
            Module module = dataProvider.get(Module.KEY);
            if (module != null) {
                return new ModuleNavBarItem(module);
            }

            Project project = dataProvider.get(Project.KEY);
            if (project == null) {
                return null;
            }
            ProjectNavBarItem projectItem = new ProjectNavBarItem(project);

            NavBarItem childItem = firstChild(projectItem);
            if (childItem == null) {
                return projectItem;
            }

            NavBarItem grandChildItem = firstChild(childItem);
            if (grandChildItem == null) {
                return childItem;
            }

            return grandChildItem;
        }
    }

    @RequiredReadAction
    private static @Nullable NavBarItem firstChild(NavBarItem item) {
        for (NavBarItemProvider ext : Application.get().getExtensionList(NavBarItemProvider.class)) {
            for (NavBarItem child : ext.iterateChildren(item)) {
                return child;
            }
        }
        return null;
    }

    @RequiredReadAction
    private static @Nullable PsiElement fromDataContext(DataSnapshot dataProvider) {
        PsiFile psiFile = dataProvider.get(PsiFile.KEY);
        if (psiFile != null) {
            DefaultNavBarItemProvider.ensurePsiFromExtensionIsValid(psiFile, "Context PSI_FILE is invalid", psiFile.getClass());
            return DefaultNavBarItemProvider.adjustWithAllExtensions(psiFile);
        }
        PsiFileSystemItem fileSystemItem = PsiUtilCore.findFileSystemItem(dataProvider.get(Project.KEY), dataProvider.get(VirtualFile.KEY));
        if (fileSystemItem != null) {
            DefaultNavBarItemProvider.ensurePsiFromExtensionIsValid(fileSystemItem, "Context fileSystemItem is invalid", fileSystemItem.getClass());
            return DefaultNavBarItemProvider.adjustWithAllExtensions(fileSystemItem);
        }
        return null;
    }
}
