// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.navigationBar.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.util.pointer.Pointer;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.navigationBar.NavBarItem;
import consulo.navigationBar.internal.NavBarInternal;
import consulo.navigationBar.model.NavBarVmItem;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class NavBarBgtDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        Project project = snapshot.get(Project.KEY);
        if (project == null) {
            return;
        }
        List<NavBarVmItem> selection = snapshot.get(NavBarVmItem.SELECTED_ITEMS);
        if (selection == null) {
            return;
        }
        List<Pointer<? extends NavBarItem>> pointers = new ArrayList<>();
        for (NavBarVmItem item : selection) {
            if (item instanceof IdeNavBarVmItem ideItem) {
                pointers.add(ideItem.getPointer());
            }
        }
        if (pointers.isEmpty()) {
            return;
        }

        sink.set(PlatformDataKeys.SELECTED_ITEM, selection.isEmpty() ? null : selection.get(0));
        sink.set(PlatformDataKeys.SELECTED_ITEMS, selection.toArray());

        sink.lazy(IdeView.KEY, () -> new NavBarIdeView(pointers));
        defaultSnapshot(project, sink, pointers);
        Application.get().getExtensionPoint(NavBarModelExtension.class).forEachExtensionSafe(extension -> {
            extension.uiDataSnapshot(sink, snapshot);
        });
    }

    private static void defaultSnapshot(Project project, DataSink sink, List<Pointer<? extends NavBarItem>> pointers) {
        sink.lazy(Module.KEY, () -> {
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof ModuleNavBarItem moduleItem) {
                    return moduleItem.getData();
                }
            }
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof PsiNavBarItem psiItem) {
                    Module module = ModuleUtilCore.findModuleForPsiElement(psiItem.getData());
                    if (module != null) {
                        return module;
                    }
                }
            }
            return null;
        });
        sink.lazy(LangDataKeys.MODULE_CONTEXT, () -> {
            PsiDirectory dir = null;
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof PsiNavBarItem psiItem && psiItem.getData() instanceof PsiDirectory directory) {
                    dir = directory;
                    break;
                }
            }
            if (dir != null && ProjectRootsUtil.isModuleContentRoot(dir.getVirtualFile(), project)) {
                return ModuleUtilCore.findModuleForPsiElement(dir);
            }
            else {
                return null;
            }
        });
        sink.lazy(PsiElement.KEY, () -> {
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof PsiNavBarItem psiItem) {
                    return psiItem.getData();
                }
            }
            return null;
        });
        sink.lazy(PsiElement.KEY_OF_ARRAY, () -> {
            List<PsiElement> result = new ArrayList<>();
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof PsiNavBarItem psiItem) {
                    result.add(psiItem.getData());
                }
            }
            return result.isEmpty() ? null : result.toArray(PsiElement.EMPTY_ARRAY);
        });
        sink.lazy(VirtualFile.KEY_OF_ARRAY, () -> {
            Set<VirtualFile> result = new LinkedHashSet<>();
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof PsiNavBarItem psiItem) {
                    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiItem.getData());
                    if (virtualFile != null) {
                        result.add(virtualFile);
                    }
                }
            }
            return result.isEmpty() ? null : result.toArray(VirtualFile.EMPTY_ARRAY);
        });
        sink.lazy(Navigatable.KEY_OF_ARRAY, () -> {
            List<Navigatable> result = new ArrayList<>();
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof DefaultNavBarItem<?> defaultItem && defaultItem.getData() instanceof Navigatable navigatable) {
                    result.add(navigatable);
                }
            }
            return result.isEmpty() ? null : result.toArray(Navigatable.EMPTY_ARRAY);
        });
        sink.lazy(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, () -> {
            boolean hasModule = false;
            for (NavBarItem item : dereference(pointers)) {
                if (item instanceof ModuleNavBarItem) {
                    hasModule = true;
                    break;
                }
            }

            NavBarInternal navBarInternal = Application.get().getInstance(NavBarInternal.class);
            if (hasModule) {
                return navBarInternal.getModuleDeleteProvider();
            }
            else {
                return navBarInternal.getDefaultDeleteProvider();
            }
        });
    }

    private static List<NavBarItem> dereference(List<Pointer<? extends NavBarItem>> pointers) {
        List<NavBarItem> result = new ArrayList<>(pointers.size());
        for (Pointer<? extends NavBarItem> pointer : pointers) {
            NavBarItem item = pointer.dereference();
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }
}
