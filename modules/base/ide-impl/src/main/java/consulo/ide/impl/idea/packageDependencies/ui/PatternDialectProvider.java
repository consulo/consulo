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

/*
 * User: anna
 * Date: 16-Jan-2008
 */
package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class PatternDialectProvider {
  public static final ExtensionPointName<PatternDialectProvider> EP_NAME = ExtensionPointName.create(PatternDialectProvider.class);

  public static PatternDialectProvider getInstance(String shortName) {
    for (PatternDialectProvider provider : EP_NAME.getExtensionList()) {
      if (Comparing.strEqual(provider.getShortName(), shortName)) return provider;
    }
    return null; //todo replace with File
  }

  public abstract TreeModel createTreeModel(Project project, Marker marker);

  public abstract TreeModel createTreeModel(Project project, Set<PsiFile> deps, Marker marker, final DependenciesPanel.DependencyPanelSettings settings);

  public abstract String getDisplayName();

  @NonNls
  @Nonnull
  public abstract String getShortName();

  public abstract AnAction[] createActions(Project project, final Runnable update);

  @Nullable
  public abstract PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively);

  @Nullable
  protected static String getModulePattern(final PackageDependenciesNode node) {
    final ModuleNode moduleParent = getModuleParent(node);
    return moduleParent != null ? moduleParent.getModuleName() : null;
  }

  @Nullable
  protected static ModuleNode getModuleParent(PackageDependenciesNode node) {
    if (node instanceof ModuleNode) return (ModuleNode)node;
    if (node == null || node instanceof RootNode) return null;
    return getModuleParent((PackageDependenciesNode)node.getParent());
  }

  public abstract Image getIcon();
}
