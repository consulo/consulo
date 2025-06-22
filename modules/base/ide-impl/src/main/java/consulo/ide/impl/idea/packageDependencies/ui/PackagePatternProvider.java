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

import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.psi.search.scope.packageSet.PatternPackageSet;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.packageDependency.PackageElementHelper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Set;

/**
 * @author anna
 * @since 2008-01-16
 */
@ExtensionImpl(id = "package", order = "before file")
public class PackagePatternProvider extends PatternDialectProvider {
    @NonNls
    public static final String PACKAGES = "package";
    private static final Logger LOG = Logger.getInstance(PackagePatternProvider.class);

    @Nullable
    private static GeneralGroupNode getGroupParent(PackageDependenciesNode node) {
        if (node instanceof GeneralGroupNode generalGroupNode) {
            return generalGroupNode;
        }
        if (node == null || node instanceof RootNode) {
            return null;
        }
        return getGroupParent((PackageDependenciesNode) node.getParent());
    }

    @Override
    public PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively) {
        GeneralGroupNode groupParent = getGroupParent(node);
        String scope1 = PatternPackageSet.SCOPE_ANY;
        if (groupParent != null) {
            String name = groupParent.toString();
            if (TreeModelBuilder.PRODUCTION_NAME.equals(name)) {
                scope1 = PatternPackageSet.SCOPE_SOURCE;
            }
            else if (TreeModelBuilder.TEST_NAME.equals(name)) {
                scope1 = PatternPackageSet.SCOPE_TEST;
            }
            else if (TreeModelBuilder.LIBRARY_NAME.equals(name)) {
                scope1 = PatternPackageSet.SCOPE_LIBRARY;
            }
        }
        final String scope = scope1;
        if (node instanceof ModuleGroupNode groupNode) {
            if (!recursively) return null;
            @NonNls final String modulePattern = "group:" + groupNode.getModuleGroup().toString();
            return new PatternPackageSet("*..*", scope, modulePattern);
        }
        else if (node instanceof ModuleNode moduleNode) {
            if (!recursively) return null;
            final String modulePattern = moduleNode.getModuleName();
            return new PatternPackageSet("*..*", scope, modulePattern);
        }
        else if (node instanceof PackageNode packageNode) {
            String pattern = packageNode.getPackageQName();
            if (pattern != null) {
                pattern += recursively ? "..*" : ".*";
            }
            else {
                pattern = recursively ? "*..*" : "*";
            }

            return new PatternPackageSet(pattern, scope, getModulePattern(node));
        }
        else if (node instanceof FileNode fNode) {
            if (recursively) return null;
            final PsiElement element = fNode.getPsiElement();
            String qName = null;

            if (element != null) {
                qName =  element.getProject()
                    .getExtensionPoint(PackageElementHelper.class)
                    .computeSafeIfAny(h -> h.getQualifiedName(element));
            }

            if (qName != null) {
                return new PatternPackageSet(qName, scope, getModulePattern(node));
            }
        }
        else if (node instanceof GeneralGroupNode) {
            return new PatternPackageSet("*..*", scope, null);
        }

        return null;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesCopyoffolder();
    }

    @Override
    public TreeModel createTreeModel(final Project project, final Marker marker) {
        return TreeModelBuilder.createTreeModel(project, false, marker);
    }

    @Override
    public TreeModel createTreeModel(
        final Project project,
        final Set<PsiFile> deps,
        final Marker marker,
        final DependenciesPanel.DependencyPanelSettings settings
    ) {
        return TreeModelBuilder.createTreeModel(project, false, deps, marker, settings);
    }

    @Override
    public String getDisplayName() {
        return IdeLocalize.titlePackages().get();
    }

    @Override
    @Nonnull
    public String getId() {
        return PACKAGES;
    }

    @Override
    public AnAction[] createActions(Project project, final Runnable update) {
        return new AnAction[]{new GroupByScopeTypeAction(update)};
    }
}
