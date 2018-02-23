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
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class DefaultFileIndexFacade extends FileIndexFacade {
  private final Project myProject;
  private final VirtualFile myBaseDir;

  public DefaultFileIndexFacade(@Nonnull final Project project) {
    super(project);
    myProject = project;
    myBaseDir = project.getBaseDir();
  }

  @Override
  public boolean isInContent(@Nonnull final VirtualFile file) {
    return VfsUtil.isAncestor(getBaseDir(), file, false);
  }

  @Override
  public boolean isInSource(@Nonnull VirtualFile file) {
    return isInContent(file);
  }

  @Override
  public boolean isInSourceContent(@Nonnull VirtualFile file) {
    return isInContent(file);
  }

  @Override
  public boolean isInLibraryClasses(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isExcludedFile(@Nonnull final VirtualFile file) {
    return false;
  }

  @Override
  public boolean isUnderIgnored(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public Module getModuleForFile(@Nonnull VirtualFile file) {
    return null;
  }

  @Nonnull
  @Override
  public ModificationTracker getRootModificationTracker() {
    return ModificationTracker.NEVER_CHANGED;
  }

  @Override
  public boolean isValidAncestor(@Nonnull final VirtualFile baseDir, @Nonnull final VirtualFile childDir) {
    return VfsUtil.isAncestor(baseDir, childDir, false);
  }

  private VirtualFile getBaseDir() {
    return myBaseDir;
  }
}
