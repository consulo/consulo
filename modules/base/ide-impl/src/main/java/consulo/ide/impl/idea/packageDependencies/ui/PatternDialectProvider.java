/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.extension.ExtensionPointName;
import consulo.content.scope.PackageSet;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * @author anna
 * @since 2008-01-16
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PatternDialectProvider {
    private static final ExtensionPointCacheKey<PatternDialectProvider, Map<String, PatternDialectProvider>> CACHE_KEY =
        ExtensionPointCacheKey.groupBy("PatternDialectProvider", PatternDialectProvider::getId);
    public static final ExtensionPointName<PatternDialectProvider> EP_NAME = ExtensionPointName.create(PatternDialectProvider.class);

    @Nullable
    public static PatternDialectProvider findById(String id) {
        Map<String, PatternDialectProvider> map =
            Application.get().getExtensionPoint(PatternDialectProvider.class).getOrBuildCache(CACHE_KEY);
        return map.get(id);
    }

    public abstract TreeModel createTreeModel(Project project, Marker marker);

    public abstract TreeModel createTreeModel(
        Project project,
        Set<PsiFile> deps,
        Marker marker,
        DependenciesPanel.DependencyPanelSettings settings
    );

    public abstract String getDisplayName();

    @Nonnull
    public abstract String getId();

    public abstract AnAction[] createActions(Project project, Runnable update);

    @Nullable
    public abstract PackageSet createPackageSet(PackageDependenciesNode node, boolean recursively);

    @Nullable
    protected static String getModulePattern(PackageDependenciesNode node) {
        ModuleNode moduleParent = getModuleParent(node);
        return moduleParent != null ? moduleParent.getModuleName() : null;
    }

    @Nullable
    protected static ModuleNode getModuleParent(PackageDependenciesNode node) {
        if (node instanceof ModuleNode moduleNode) {
            return moduleNode;
        }
        if (node == null || node instanceof RootNode) {
            return null;
        }
        return getModuleParent((PackageDependenciesNode)node.getParent());
    }

    public abstract Image getIcon();
}
