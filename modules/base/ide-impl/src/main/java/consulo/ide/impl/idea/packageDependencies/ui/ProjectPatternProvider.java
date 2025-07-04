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
import consulo.application.AllIcons;
import consulo.application.ReadAction;
import consulo.content.scope.PackageSet;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.packageDependencies.DependencyUISettings;
import consulo.ide.impl.psi.search.scope.packageSet.FilePatternPackageSet;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPackageSupportProviders;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Set;

/**
 * @author anna
 * @since 2008-01-16
 */
@ExtensionImpl(id = ProjectPatternProvider.FILE, order = "last")
public class ProjectPatternProvider extends PatternDialectProvider {

  public static final String FILE = "file";

  private static final Logger LOG = Logger.getInstance(ProjectPatternProvider.class);


  @Override
  public TreeModel createTreeModel(final Project project, final Marker marker) {
    return FileTreeModelBuilder.createTreeModel(project, false, marker);
  }

  @Override
  public TreeModel createTreeModel(
    final Project project,
    final Set<PsiFile> deps,
    final Marker marker,
    final DependenciesPanel.DependencyPanelSettings settings
  ) {
    return FileTreeModelBuilder.createTreeModel(project, false, deps, marker, settings);
  }

  @Override
  public String getDisplayName() {
    return IdeLocalize.titleProject().get();
  }

  @Override
  @Nonnull
  public String getId() {
    return FILE;
  }

  @Override
  public AnAction[] createActions(Project project, final Runnable update) {
    return new AnAction[]{new CompactEmptyMiddlePackagesAction(update)};
  }

  @Override
  @Nullable
  public PackageSet createPackageSet(final PackageDependenciesNode node, final boolean recursively) {
    if (node instanceof ModuleGroupNode moduleGroupNode) {
      if (!recursively) {
        return null;
      }
      @NonNls final String modulePattern = "group:" + moduleGroupNode.getModuleGroup().toString();
      return new FilePatternPackageSet(modulePattern, "*//*");
    }
    else if (node instanceof ModuleNode moduleNode) {
      if (!recursively) {
        return null;
      }
      final String modulePattern = moduleNode.getModuleName();
      return new FilePatternPackageSet(modulePattern, "*/");
    }

    else if (node instanceof DirectoryNode directoryNode) {
      String pattern = directoryNode.getFQName();
      if (pattern != null) {
        if (pattern.length() > 0) {
          pattern += recursively ? "//*" : "/*";
        }
        else {
          pattern += recursively ? "*/" : "*";
        }
      }
      return new FilePatternPackageSet(getModulePattern(node), pattern);
    }
    else if (node instanceof FileNode fNode) {
      if (recursively) return null;
      final PsiFile file = (PsiFile)fNode.getPsiElement();
      if (file == null) return null;
      final VirtualFile virtualFile = file.getVirtualFile();
      LOG.assertTrue(virtualFile != null);
      final VirtualFile contentRoot = ProjectRootManager.getInstance(file.getProject()).getFileIndex().getContentRootForFile(virtualFile);
      if (contentRoot == null) return null;
      final String fqName = VfsUtilCore.getRelativePath(virtualFile, contentRoot, '/');
      if (fqName != null) return new FilePatternPackageSet(getModulePattern(node), fqName);
    }
    return null;
  }

  @Override
  public Image getIcon() {
    return AllIcons.General.ProjectTab;
  }

  private static final class CompactEmptyMiddlePackagesAction extends ToggleAction {
    private final Runnable myUpdate;

    CompactEmptyMiddlePackagesAction(Runnable update) {
      super(
        IdeLocalize.actionCompactEmptyMiddlePackages(),
        IdeLocalize.actionCompactEmptyMiddlePackages(),
        PlatformIconGroup.objectbrowserCompactemptypackages()
      );
      myUpdate = update;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent event) {
      return DependencyUISettings.getInstance().UI_COMPACT_EMPTY_MIDDLE_PACKAGES;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent event, boolean flag) {
      DependencyUISettings.getInstance().UI_COMPACT_EMPTY_MIDDLE_PACKAGES = flag;
      myUpdate.run();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      Project eventProject = e.getData(Project.KEY);
      if (eventProject == null) {
        return;
      }
      e.getPresentation().setVisible(
          FILE.equals(DependencyUISettings.getInstance().getScopeType())
              && ReadAction.compute(() -> PsiPackageSupportProviders.isPackageSupported(eventProject))
      );
    }
  }
}
