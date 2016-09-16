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

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.ui.UIUtil;
import consulo.ui.*;
import consulo.ui.internal.WGwtTreeImpl;
import consulo.web.AppInit;
import consulo.web.servlet.ui.UIBuilder;
import consulo.web.servlet.ui.UIServlet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.annotation.WebServlet;

public class AppUIBuilder extends UIBuilder {
  private static final ViewSettings ourViewSettings = new ViewSettings() {
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

  @WebServlet("/app")
  public static class Servlet extends UIServlet {
    public Servlet() {
      super(AppUIBuilder.class);
    }
  }

  @Override
  protected void build(@NotNull final Window window) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    if (application == null || !application.isLoaded()) {
      AppInit.initApplication();

      while (true) {
        application = ApplicationManagerEx.getApplicationEx();
        if (application != null && application.isLoaded()) {
          break;
        }

        TimeoutUtil.sleep(500L);
      }
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

  private void buildContent(@NotNull Window window, @NotNull Project project) {
    final Menu file = MenuItems.menu("File");
    file.add(MenuItems.menu("New").add(MenuItems.item("Class")));
    file.separate();
    file.add(MenuItems.item("Exit"));

    window.setMenuBar(MenuItems.menuBar().add(file).add(MenuItems.item("Help")));

    final SplitLayout splitLayout = Layouts.horizontalSplit();
    final TabbedLayout tabbed = Layouts.tabbed();

    final VerticalLayout vertical = Layouts.vertical();

    BooleanValueGroup group = new BooleanValueGroup();

    final RadioButton component = Components.radioButton("Test 1", true);
    vertical.add(component);
    final RadioButton component1 = Components.radioButton("Test 2");
    vertical.add(component1);

    group.add(component).add(component1);

    tabbed.addTab("Hello", vertical);

    final LabeledLayout labeled = Layouts.labeled("Some Panel Label");
    tabbed.addTab("Hello2", labeled.set(Components.label("test 1")));

    ProjectViewProjectNode rootNode = new ProjectViewProjectNode(project, ourViewSettings);

    WGwtTreeImpl<AbstractTreeNode<?>> tree = new WGwtTreeImpl<AbstractTreeNode<?>>(rootNode);

    splitLayout.setFirstComponent(tree);
    splitLayout.setSecondComponent(tabbed);
    splitLayout.setProportion(20);

    window.setContent(splitLayout);
  }

  @Nullable
  private Project getOrLoadProject(String path) {
    final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(path);
    if (fileByPath == null) {
      return null;
    }
    try {
      final Project project;
      ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
      Project[] openProjects = projectManager.getOpenProjects();
      for (Project temp : openProjects) {
        if (fileByPath.equals(temp.getBaseDir())) {
          return temp;
        }
      }

      project = projectManager.loadProject(path);
      if (project == null) {
        return null;
      }
      projectManager.openTestProject(project);
      final StartupManagerImpl startupManager = (StartupManagerImpl)StartupManager.getInstance(project);
      startupManager.runStartupActivities();
      startupManager.startCacheUpdate();
      return project;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
