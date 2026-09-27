/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it;

import consulo.application.WriteAction;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.util.ModuleRootModificationUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * Laying out the modules of a project under test: creating them, giving them content, source and test roots, and
 * wiring them to each other. Each call takes its own write action, and nested use inside an outer one is fine.
 *
 * @author VISTALL
 */
public final class HeadlessModules {
    private HeadlessModules() {
    }

    public static Module createModule(Project project, String name, Path directory) {
        return WriteAction.compute(() -> {
            ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
            Module module = moduleModel.newModule(name, directory.toString());
            moduleModel.commit();
            return module;
        });
    }

    /**
     * A module named {@code name} whose single content root is {@code contentRoot}.
     */
    public static Module createModule(Project project, String name, VirtualFile contentRoot) {
        return WriteAction.compute(() -> {
            Module module = createModule(project, name, Path.of(contentRoot.getPath()));
            addContentRoot(module, contentRoot);
            return module;
        });
    }

    public static void addContentRoot(Module module, VirtualFile root) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            rootModel.addContentEntry(root);
            rootModel.commit();
        });
    }

    /**
     * Marks {@code root} as production sources, inside whichever content root already holds it, or as a content
     * root of its own when none does.
     */
    public static void addSourceRoot(Module module, VirtualFile root) {
        addFolder(module, root, ProductionContentFolderTypeProvider.getInstance());
    }

    public static void addTestSourceRoot(Module module, VirtualFile root) {
        addFolder(module, root, TestContentFolderTypeProvider.getInstance());
    }

    public static void addDependency(Module from, Module to) {
        ModuleRootModificationUtil.addDependency(from, to);
    }

    public static void removeModule(Project project, Module module) {
        WriteAction.run(() -> {
            ModifiableModuleModel moduleModel = ModuleManager.getInstance(project).getModifiableModel();
            moduleModel.disposeModule(module);
            moduleModel.commit();
        });
    }

    private static void addFolder(Module module, VirtualFile root, ContentFolderTypeProvider type) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            ContentEntry entry = findContentEntry(rootModel, root);
            if (entry == null) {
                entry = rootModel.addContentEntry(root);
            }
            entry.addFolder(root, type);
            rootModel.commit();
        });
    }

    private static @Nullable ContentEntry findContentEntry(ModifiableRootModel rootModel, VirtualFile file) {
        for (ContentEntry entry : rootModel.getContentEntries()) {
            VirtualFile entryFile = entry.getFile();
            if (entryFile != null && VirtualFileUtil.isAncestor(entryFile, file, false)) {
                return entry;
            }
        }
        return null;
    }
}
