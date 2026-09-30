/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.util.ModificationTracker;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DiagramProvider<T> {
    private static final DiagramColorManager DEFAULT_COLOR_MANAGER = new DiagramColorManagerBase();

    public abstract String getID();

    public abstract DiagramVisibilityManager getVisibilityManager();

    public abstract DiagramNodeContentManager getNodeContentManager();

    public abstract DiagramElementManager<T> getElementManager();

    public abstract DiagramVfsResolver<T> getVfsResolver();

    public abstract DiagramRelationshipManager<T> getRelationshipManager();

    public abstract DiagramDataModel<T> createDataModel(Project project, @Nullable T element, @Nullable VirtualFile file);

    public abstract ModificationTracker getModificationTracker(Project project);

    public DiagramColorManager getColorManager() {
        return DEFAULT_COLOR_MANAGER;
    }

    public @Nullable DiagramExtras<T> getExtras() {
        return null;
    }

    public static @Nullable DiagramProvider<?> findProvider(DataContext context) {
        return Application.get().getExtensionPoint(DiagramProvider.class).findFirstSafe(provider -> isApplicable(provider, context));
    }

    public static List<DiagramProvider<?>> findProviders(DataContext context) {
        List<DiagramProvider<?>> providers = new ArrayList<>();
        Application.get().getExtensionPoint(DiagramProvider.class).forEach(provider -> {
            if (isApplicable(provider, context)) {
                providers.add(provider);
            }
        });
        return providers;
    }

    public static @Nullable DiagramProvider<?> findByID(String id) {
        return Application.get().getExtensionPoint(DiagramProvider.class).findFirstSafe(provider -> provider.getID().equals(id));
    }

    private static boolean isApplicable(DiagramProvider<?> provider, DataContext context) {
        DiagramElementManager<?> manager = provider.getElementManager();
        Object element = manager.findInDataContext(context);
        return element != null && manager.isAcceptableAsNode(element);
    }
}
