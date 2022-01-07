/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.roots.ContentFolderTypeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public class DirectoryInfoImpl extends DirectoryInfo {
  protected final VirtualFile myRoot;//original project root for which this information is calculated
  private final Module module; // module to which content it belongs or null
  private final VirtualFile libraryClassRoot; // class root in library
  private final VirtualFile contentRoot;
  private final VirtualFile sourceRoot;
  private final ContentFolder myContentFolder;
  protected final boolean myInModuleSource;
  protected final boolean myInLibrarySource;
  protected final boolean myExcluded;
  private final ContentFolderTypeProvider mySourceRootTypeId;
  private final String myUnloadedModuleName;

  DirectoryInfoImpl(@Nonnull VirtualFile root,
                    Module module,
                    VirtualFile contentRoot,
                    VirtualFile sourceRoot,
                    ContentFolder contentFolder,
                    VirtualFile libraryClassRoot,
                    boolean inModuleSource,
                    boolean inLibrarySource,
                    boolean isExcluded, ContentFolderTypeProvider sourceRootTypeId,
                    @Nullable String unloadedModuleName) {
    myRoot = root;
    this.module = module;
    this.libraryClassRoot = libraryClassRoot;
    myContentFolder = contentFolder;
    this.contentRoot = contentRoot;
    this.sourceRoot = sourceRoot;
    myInModuleSource = inModuleSource;
    myInLibrarySource = inLibrarySource;
    myExcluded = isExcluded;
    myUnloadedModuleName = unloadedModuleName;
    mySourceRootTypeId = sourceRootTypeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return myRoot.equals(((DirectoryInfoImpl)o).myRoot);
  }

  @Override
  public int hashCode() {
    return myRoot.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("DirectoryInfoImpl{");
    sb.append("myRoot=").append(myRoot);
    sb.append(", module=").append(module);
    sb.append(", libraryClassRoot=").append(libraryClassRoot);
    sb.append(", contentRoot=").append(contentRoot);
    sb.append(", sourceRoot=").append(sourceRoot);
    sb.append(", myContentFolder=").append(myContentFolder);
    sb.append(", myInModuleSource=").append(myInModuleSource);
    sb.append(", myInLibrarySource=").append(myInLibrarySource);
    sb.append(", myExcluded=").append(myExcluded);
    sb.append(", mySourceRootTypeId=").append(mySourceRootTypeId);
    sb.append(", myUnloadedModuleName='").append(myUnloadedModuleName).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean isInProject() {
    return !myExcluded;
  }

  @Override
  public boolean isInProject(@Nonnull VirtualFile file) {
    return !isExcluded(file);
  }

  @Override
  public boolean isIgnored() {
    return false;
  }

  @Override
  @Nullable
  public VirtualFile getSourceRoot() {
    return sourceRoot;
  }

  @Nullable
  @Override
  public ContentFolder getContentFolder() {
    return myContentFolder;
  }

  @Override
  public VirtualFile getLibraryClassRoot() {
    return libraryClassRoot;
  }

  @Override
  @Nullable
  public VirtualFile getContentRoot() {
    return contentRoot;
  }

  @Override
  public boolean isInModuleSource() {
    return myInModuleSource;
  }

  @Override
  public boolean isInLibrarySource() {
    return myInLibrarySource;
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile file) {
    return myInLibrarySource;
  }

  @Override
  public boolean isExcluded() {
    return myExcluded;
  }

  @Override
  public boolean isExcluded(@Nonnull VirtualFile file) {
    return myExcluded;
  }

  @Override
  public boolean isInModuleSource(@Nonnull VirtualFile file) {
    return myInModuleSource;
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public ContentFolderTypeProvider getSourceRootTypeId() {
    return mySourceRootTypeId;
  }

  @Override
  public String getUnloadedModuleName() {
    return myUnloadedModuleName;
  }

  @Nonnull
  public VirtualFile getRoot() {
    return myRoot;
  }
}
