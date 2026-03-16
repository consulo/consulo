/*
 * Copyright 2013-2025 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.DeleteProvider;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.Objects;

/**
 * A {@link UiDataRule} that derives PSI, Module, and VirtualFile data
 * from the navbar selection lazily under read access.
 * <p>
 * This separates EDT-safe data capture (done by {@link NavBarPanel#uiDataSnapshot})
 * from PSI-requiring data resolution (done here under {@code tryRunReadAction}).
 *
 * @author VISTALL
 * @since 2025-03-02
 */
@ExtensionImpl
public class NavBarBgtDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        Project project = snapshot.get(Project.KEY);
        if (project == null) {
            return;
        }

        List<?> items = snapshot.get(NavBarPanel.NAV_BAR_ITEMS);
        if (items == null || items.isEmpty()) {
            return;
        }

        sink.lazy(Module.KEY, () -> {
            // First check for Module directly in selection
            for (Object item : items) {
                if (item instanceof Module module && !module.isDisposed()) {
                    return module;
                }
            }
            // Then check for PsiElement and get its module
            for (Object item : items) {
                if (item instanceof PsiElement element) {
                    return element.getModule();
                }
            }
            return null;
        });

        sink.lazy(LangDataKeys.MODULE_CONTEXT, () -> {
            for (Object item : items) {
                if (item instanceof PsiDirectory directory) {
                    VirtualFile dir = directory.getVirtualFile();
                    if (ProjectRootsUtil.isModuleContentRoot(dir, project)) {
                        return directory.getModule();
                    }
                }
            }
            return null;
        });

        sink.lazy(PsiElement.KEY, () -> {
            for (Object item : items) {
                if (item instanceof PsiElement element && element.isValid()) {
                    return element;
                }
            }
            return null;
        });

        sink.lazy(PsiElement.KEY_OF_ARRAY, () -> {
            PsiElement[] elements = items.stream()
                .filter(PsiElement.class::isInstance)
                .map(PsiElement.class::cast)
                .filter(PsiElement::isValid)
                .toArray(PsiElement[]::new);
            return elements.length > 0 ? elements : null;
        });

        sink.lazy(VirtualFile.KEY_OF_ARRAY, () -> {
            VirtualFile[] files = items.stream()
                .filter(PsiElement.class::isInstance)
                .map(PsiElement.class::cast)
                .filter(PsiElement::isValid)
                .map(PsiUtilCore::getVirtualFile)
                .filter(Objects::nonNull)
                .distinct()
                .toArray(VirtualFile[]::new);
            return files.length > 0 ? files : null;
        });

        sink.lazy(Navigatable.KEY_OF_ARRAY, () -> {
            Navigatable[] navigatables = items.stream()
                .filter(Navigatable.class::isInstance)
                .map(Navigatable.class::cast)
                .toArray(Navigatable[]::new);
            return navigatables.length > 0 ? navigatables : null;
        });

        sink.lazy(DeleteProvider.KEY, () -> {
            boolean hasModule = items.stream().anyMatch(Module.class::isInstance);
            return hasModule ? new ModuleDeleteProvider() : new DeleteHandler.DefaultDeleteProvider();
        });
    }
}
