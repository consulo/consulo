// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.tree.project;

import consulo.component.ComponentManager;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import static consulo.application.util.registry.Registry.is;

public interface ProjectFileNode {
  /**
   * Returns one of the following identifiers for the node:
   * <dl>
   * <dt>Module</dt><dd>a module to which this file belongs;</dd>
   * <dt>Project</dt><dd>a project indicates that a file does not belong to any module, but is located under the project directory;</dd>
   * <dt>VirtualFile</dt><dd>a topmost directory that contains this file (specifies a tree view without modules).</dd>
   * </dl>
   */
  
  Object getRootID();

  
  VirtualFile getVirtualFile();

  default boolean contains(VirtualFile file, ComponentManager area, boolean strict) {
    Object id = getRootID();
    if (id instanceof ComponentManager && !id.equals(area)) return false;
    return VirtualFileUtil.isAncestor(getVirtualFile(), file, strict);
  }

  /**
   * Returns a {@link Module} to which the specified {@code file} belongs;
   * or a {@link Project} if the specified {@code file} does not belong to any module, but is located under the base project directory;
   * or {@code null} if the specified {@code file} does not correspond to the given {@code project}
   */
  @Nullable
  static ComponentManager findArea(VirtualFile file, @Nullable Project project) {
    if (project == null || project.isDisposed() || !file.isValid()) return null;
    Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file, false);
    if (module != null) return module.isDisposed() ? null : module;
    if (!is("projectView.show.base.dir")) return null;
    VirtualFile ancestor = findBaseDir(project);
    // file does not belong to any content root, but it is located under the project directory and not ignored
    return ancestor == null || FileTypeRegistry.getInstance().isFileIgnored(file) || !VirtualFileUtil.isAncestor(ancestor, file, false) ? null : project;
  }

  /**
   * Returns a base directory for the specified {@code project}, or {@code null} if it does not exist.
   */
  @Nullable
  static VirtualFile findBaseDir(@Nullable Project project) {
    if (project == null || project.isDisposed()) return null;
    String path = project.getBasePath();
    return path == null ? null : LocalFileSystem.getInstance().findFileByPath(path);
  }
}
