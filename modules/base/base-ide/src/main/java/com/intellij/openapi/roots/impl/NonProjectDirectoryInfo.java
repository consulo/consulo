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
class NonProjectDirectoryInfo extends DirectoryInfo {
  public static final NonProjectDirectoryInfo IGNORED = new NonProjectDirectoryInfo("ignored") {
    @Override
    public boolean isIgnored() {
      return true;
    }
  };
  public static final NonProjectDirectoryInfo EXCLUDED = new NonProjectDirectoryInfo("excluded from project") {
    @Override
    public boolean isExcluded() {
      return true;
    }
  };
  public static final NonProjectDirectoryInfo NOT_UNDER_PROJECT_ROOTS = new NonProjectDirectoryInfo("not under project roots");
  public static final NonProjectDirectoryInfo INVALID = new NonProjectDirectoryInfo("invalid");
  public static final NonProjectDirectoryInfo NOT_SUPPORTED_VIRTUAL_FILE_IMPLEMENTATION = new NonProjectDirectoryInfo("not supported VirtualFile implementation");
  private final String myDebugName;

  private NonProjectDirectoryInfo(String debugName) {
    myDebugName = debugName;
  }

  @Override
  public boolean isInProject() {
    return false;
  }

  @Override
  public boolean isInProject(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public String toString() {
    return "DirectoryInfo: " + myDebugName;
  }

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public boolean isIgnored() {
    return false;
  }

  @Override
  @Nullable
  public VirtualFile getSourceRoot() {
    return null;
  }

  @Nullable
  @Override
  public ContentFolder getContentFolder() {
    return null;
  }

  @Override
  public VirtualFile getLibraryClassRoot() {
    return null;
  }

  @Override
  @Nullable
  public VirtualFile getContentRoot() {
    return null;
  }

  @Override
  public boolean isInModuleSource() {
    return false;
  }

  @Override
  public boolean isInLibrarySource() {
    return false;
  }

  @Override
  public boolean isInLibrarySource(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public boolean isExcluded() {
    return false;
  }

  @Override
  public boolean isExcluded(@Nonnull VirtualFile file) {
    return isExcluded();
  }

  @Override
  public boolean isInModuleSource(@Nonnull VirtualFile file) {
    return false;
  }

  @Override
  public Module getModule() {
    return null;
  }

  @Override
  public String getUnloadedModuleName() {
    return null;
  }

  @Override
  public ContentFolderTypeProvider getSourceRootTypeId() {
    return null;
  }
}
