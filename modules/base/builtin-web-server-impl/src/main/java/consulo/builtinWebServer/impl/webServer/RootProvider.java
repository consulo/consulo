// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.impl.webServer;

import consulo.application.ReadAction;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum RootProvider {
    SOURCE {
        @Override
        VirtualFile[] getRoots(ModuleRootManager rootManager) {
            return rootManager.getContentFolderFiles(LanguageContentFolderScopes.all(false));
        }
    },
    CONTENT {
        @Override
        VirtualFile[] getRoots(ModuleRootManager rootManager) {
            return rootManager.getContentRoots();
        }
    },
    EXCLUDED {
        @Override
        VirtualFile[] getRoots(ModuleRootManager rootManager) {
            return rootManager.getContentFolderFiles(LanguageContentFolderScopes.excluded());
        }
    };

    abstract VirtualFile[] getRoots(ModuleRootManager rootManager);

    static List<VirtualFile> getAllRoots(Project project) {
        return ReadAction.compute(() -> {
            Module[] modules = ModuleManager.getInstance(project).getModules();
            List<VirtualFile> roots = new ArrayList<>();
            for (RootProvider rootProvider : values()) {
                for (Module module : modules) {
                    if (module.isDisposed()) {
                        continue;
                    }
                    Collections.addAll(roots, rootProvider.getRoots(ModuleRootManager.getInstance(module)));
                }
            }
            return roots;
        });
    }
}
