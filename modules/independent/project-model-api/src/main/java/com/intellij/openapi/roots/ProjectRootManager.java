/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * Allows to query and modify the list of root directories belonging to a project.
 */
public abstract class ProjectRootManager extends SimpleModificationTracker {
  /**
   * Returns the project root manager instance for the specified project.
   *
   * @param project the project for which the instance is requested.
   * @return the instance.
   */
  public static ProjectRootManager getInstance(Project project) {
    return project.getComponent(ProjectRootManager.class);
  }

  /**
   * Returns the file index for the project.
   *
   * @return the file index instance.
   */
  @Nonnull
  public abstract ProjectFileIndex getFileIndex();

  /**
   * Creates new enumerator instance to process dependencies of all modules in the project. Only first level dependencies of
   * modules are processed so {@link OrderEnumerator#recursively()} option is ignored and {@link OrderEnumerator#withoutDepModules()} option is forced
   * @return new enumerator instance
   */
  @Nonnull
  public abstract OrderEnumerator orderEntries();

  /**
   * Creates new enumerator instance to process dependencies of several modules in the project. Caching is not supported for this enumerator
   * @param modules modules to process
   * @return new enumerator instance
   */
  @Nonnull
  public abstract OrderEnumerator orderEntries(@Nonnull Collection<? extends Module> modules);

  /**
   * Unlike getContentRoots(), this includes the project base dir. Is this really necessary?
   * TODO: remove this method?
   * @return
   */
  public abstract VirtualFile[] getContentRootsFromAllModules();

  /**
   * Returns the list of content root URLs for all modules in the project.
   *
   * @return the list of content root URLs.
   */
  @Nonnull
  public abstract List<String> getContentRootUrls();

    /**
    * Returns the list of content roots for all modules in the project.
    *
    * @return the list of content roots.
    */
  @Nonnull
  public abstract VirtualFile[] getContentRoots();

  /**
   * Returns the list of source roots under the content roots for all modules in the project.
   *
   * @return the list of content source roots.
   */
  public abstract VirtualFile[] getContentSourceRoots();
}
