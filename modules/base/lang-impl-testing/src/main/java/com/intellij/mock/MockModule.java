/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.mock;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
@Deprecated
public class MockModule extends MockComponentManager implements Module {
  private final Project myProject;

  public MockModule(@Nonnull Disposable parentDisposable) {
    this(null, parentDisposable);
  }

  public MockModule(@Nullable final Project project, @Nonnull Disposable parentDisposable) {
    super((MockComponentManager)project, parentDisposable);
    myProject = project;
  }

  @Nullable
  @Override
  public VirtualFile getModuleDir() {
    throw new UnsupportedOperationException("Method getModuleDir is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  @Override
  public String getModuleDirPath() {
    return null;
  }

  @Nonnull
  @Override
  public String getModuleDirUrl() {
    return "";
  }

  @Override
  @Nonnull
  public String getName() {
    return "MockModule";
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }
}
