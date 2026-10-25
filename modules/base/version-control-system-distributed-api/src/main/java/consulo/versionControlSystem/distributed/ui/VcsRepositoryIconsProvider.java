// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.distributed.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.log.ui.VcsLogColorManagerFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.TreeSet;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsRepositoryIconsProvider {
    private static final int ICON_SIZE = 12;

    public static VcsRepositoryIconsProvider getInstance(Project project) {
        return project.getInstance(VcsRepositoryIconsProvider.class);
    }

    private final Project myProject;
    private final VcsLogColorManagerFactory myColorManagerFactory;

    private @Nullable Set<VirtualFile> myRoots;
    private @Nullable VcsLogColorManager myColorManager;

    @Inject
    public VcsRepositoryIconsProvider(Project project, VcsLogColorManagerFactory colorManagerFactory) {
        myProject = project;
        myColorManagerFactory = colorManagerFactory;
    }

    public Image getIcon(Repository repository) {
        ColorValue color = getColor(repository);
        return color != null ? ImageEffects.colorFilled(ICON_SIZE, ICON_SIZE, color) : PlatformIconGroup.nodesFolder();
    }

    public @Nullable ColorValue getColor(Repository repository) {
        VcsLogColorManager colorManager = getColorManager();
        VirtualFile root = repository.getRoot();
        Set<VirtualFile> roots = myRoots;
        if (roots == null || !roots.contains(root)) {
            return null;
        }
        return colorManager.getRootColor(root);
    }

    private synchronized VcsLogColorManager getColorManager() {
        Set<VirtualFile> roots = new TreeSet<>((a, b) -> a.getPath().compareTo(b.getPath()));
        for (Repository repository : VcsRepositoryManager.getInstance(myProject).getRepositories()) {
            roots.add(repository.getRoot());
        }
        VcsLogColorManager colorManager = myColorManager;
        if (colorManager == null || !roots.equals(myRoots)) {
            colorManager = myColorManagerFactory.create(roots);
            myColorManager = colorManager;
            myRoots = roots;
        }
        return colorManager;
    }
}
