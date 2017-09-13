/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.servlet;

import com.google.common.base.Strings;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.TransactionGuardImpl;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DeferredIcon;
import com.intellij.util.ui.UIUtil;
import consulo.ui.*;
import consulo.ui.image.Image;
import consulo.ui.internal.WGwtTreeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.function.Function;

public class AppUIBuilder {
  public static final ViewSettings ourViewSettings = new ViewSettings() {
    @Override
    public boolean isShowMembers() {
      return false;
    }

    @Override
    public boolean isAbbreviatePackageNames() {
      return false;
    }

    @Override
    public boolean isFlattenPackages() {
      return false;
    }

    @Override
    public boolean isHideEmptyMiddlePackages() {
      return false;
    }

    @Override
    public boolean isShowLibraryContents() {
      return true;
    }

    @NotNull
    @Override
    public <T> T getViewOption(@NotNull KeyWithDefaultValue<T> option) {
      return option.getDefaultValue();
    }

    @Override
    public boolean isStructureView() {
      return false;
    }

    @Override
    public boolean isShowModules() {
      return true;
    }
  };


  @RequiredUIAccess
  public static void build(@NotNull final Window window) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    if (application == null || !application.isLoaded()) {
      window.setContent(Components.label("Not loaded"));
      return;
    }

    final Project project = UIUtil.invokeAndWaitIfNeeded(new Computable<Project>() {
      @Override
      public Project compute() {
        return getOrLoadProject("R:/_github.com/consulo/mssdw");
      }
    });
    if (project == null) {
      return;
    }

    buildContent(window, project);
  }

  @RequiredUIAccess
  private static void buildContent(@NotNull Window window, @NotNull Project project) {
    final Menu file = MenuItems.menu("File");
    file.add(MenuItems.menu("New").add(MenuItems.item("Class")));
    file.separate();
    file.add(MenuItems.item("Exit"));

    window.setMenuBar(MenuItems.menuBar().add(file).add(MenuItems.item("Help")));

    final TabbedLayout tabbed = Layouts.tabbed();

    final VerticalLayout vertical = Layouts.vertical();

    ValueGroup<Boolean> group = ValueGroups.boolGroup();

    final RadioButton component = Components.radioButton("Test 1", true);
    vertical.add(component);
    final RadioButton component1 = Components.radioButton("Test 2");
    vertical.add(component1);

    group.add(component).add(component1);

    tabbed.addTab("Hello", vertical);

    final LabeledLayout labeled = Layouts.labeled("Some Panel Label");
    tabbed.addTab("Hello2", labeled.set(Components.label("test 1")));


    TreeModel<AbstractTreeNode> model = new TreeModel<AbstractTreeNode>() {

      @Override
      public void fetchChildren(@NotNull Function<AbstractTreeNode, TreeNode<AbstractTreeNode>> nodeFactory, @Nullable AbstractTreeNode parentNode) {
        AbstractTreeNode it;
        if (parentNode == null) {
          it = new ProjectViewProjectNode(project, ourViewSettings);

        }
        else {
          it = parentNode;
        }

        Collection<AbstractTreeNode> children = ReadAction.compute((ThrowableComputable<Collection, RuntimeException>)it::getChildren);

        for (AbstractTreeNode child : children) {
          TreeNode<AbstractTreeNode> node = nodeFactory.apply(child);
          node.setLeaf(ReadAction.compute((ThrowableComputable<Collection, RuntimeException>)child::getChildren).isEmpty());
          node.setRender((t, itemPresentation) -> {
            UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
              try {
                t.update();
              }
              catch (Exception e) {
                e.printStackTrace();
              }
            });
            PresentationData presentation = child.getPresentation();
            Icon icon = presentation.getIcon(true);
            if (icon instanceof DeferredIcon) {
              final Icon finalIcon = icon;
              icon = ReadAction.compute(((DeferredIcon)finalIcon)::evaluate);
            }
            itemPresentation.setIcon((Image)icon);
            itemPresentation.append(Strings.nullToEmpty(presentation.getPresentableText()));
          });
        }
      }
    };

    final SplitLayout splitLayout = Layouts.horizontalSplit();

    WGwtTreeImpl<AbstractTreeNode> tree = new WGwtTreeImpl<>(model);

    splitLayout.setFirstComponent(tree);
    splitLayout.setSecondComponent(tabbed);
    splitLayout.setProportion(20);

    window.setContent(splitLayout);
  }

  @Nullable
  public static Project getOrLoadProject(String path) {
    final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(path);
    if (fileByPath == null) {
      return null;
    }
    ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
    Project[] openProjects = projectManager.getOpenProjects();
    for (Project temp : openProjects) {
      if (fileByPath.equals(temp.getBaseDir())) {
        return temp;
      }
    }

    TransactionGuardImpl.getInstance().submitTransactionAndWait(() -> {
      try {
        projectManager.loadAndOpenProject(path);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });
    return null;
  }
}
