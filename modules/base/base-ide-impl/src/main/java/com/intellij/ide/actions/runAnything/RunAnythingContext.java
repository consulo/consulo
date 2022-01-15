/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.ide.actions.runAnything;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * from kotlin
 */
public class RunAnythingContext {
  public static final class ProjectContext extends RunAnythingContext {
    private Project myProject;

    public ProjectContext(Project project) {
      super(IdeBundle.message("run.anything.context.project"), StringUtil.notNullize(project.getBasePath()));
      myProject = project;
    }

    @Nonnull
    public Project getProject() {
      return myProject;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ProjectContext that = (ProjectContext)o;
      return Objects.equals(myProject, that.myProject);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myProject);
    }
  }

  public static final class BrowseRecentDirectoryContext extends RunAnythingContext {
    public static final BrowseRecentDirectoryContext INSTANCE = new BrowseRecentDirectoryContext();

    public BrowseRecentDirectoryContext() {
      super(IdeBundle.message("run.anything.context.browse.directory"), "", AllIcons.Nodes.Folder);
    }
  }

  public static final class RecentDirectoryContext extends RunAnythingContext {
    @Nonnull
    private final String myPath;

    public RecentDirectoryContext(@Nonnull String path) {
      super(FileUtil.getLocationRelativeToUserHome(path), "", AllIcons.Nodes.Folder);
      myPath = path;
    }

    @Nonnull
    public String getPath() {
      return myPath;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RecentDirectoryContext that = (RecentDirectoryContext)o;
      return Objects.equals(myPath, that.myPath);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myPath);
    }
  }

  public static final class ModuleContext extends RunAnythingContext {
    private final Module myModule;

    public ModuleContext(@Nonnull Module module) {
      super(module.getName(), calcDescription(module), AllIcons.Nodes.Module);
      myModule = module;
    }

    @Nonnull
    private static String calcDescription(Module module) {
      String basePath = module.getProject().getBasePath();
      if (basePath != null) {
        String modulePath = module.getModuleDirPath();
        if (modulePath == null) {
          VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
          if (contentRoots.length == 1) {
            modulePath = contentRoots[0].getPath();
          }
        }

        if (modulePath != null) {
          String relativePath = FileUtil.getRelativePath(basePath, modulePath, '/');
          if (relativePath != null) {
            return relativePath;
          }
        }
      }
      return "undefined";
    }

    @Nonnull
    public Module getModule() {
      return myModule;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ModuleContext that = (ModuleContext)o;
      return Objects.equals(myModule, that.myModule);
    }

    @Override
    public int hashCode() {
      return Objects.hash(myModule);
    }
  }

  protected final String label;
  protected final String description;
  protected final Image icon;

  private RunAnythingContext(String label) {
    this(label, "", null);
  }

  private RunAnythingContext(String label, String description) {
    this(label, description, null);
  }

  private RunAnythingContext(String label, String description, Image icon) {
    this.label = label;
    this.description = description;
    this.icon = icon;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

  public Image getIcon() {
    return icon;
  }
}
