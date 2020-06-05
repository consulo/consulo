/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.mock;

import consulo.disposer.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
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

  @Nonnull
  @Override
  public GlobalSearchScope getModuleRuntimeScope(final boolean includeTests) {
    return new MockGlobalSearchScope();
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleScope() {
    return new MockGlobalSearchScope();
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleScope(boolean includeTests) {
    return new MockGlobalSearchScope();
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleTestsWithDependentsScope() {
    return new MockGlobalSearchScope();
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(final boolean includeTests) {
    return new MockGlobalSearchScope();

    //throw new UnsupportedOperationException( "Method getModuleWithDependenciesAndLibrariesScope is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleWithDependenciesScope() {
    return new MockGlobalSearchScope();
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleContentWithDependenciesScope() {
    throw new UnsupportedOperationException("Method getModuleContentWithDependenciesScope is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleContentScope() {
    throw new UnsupportedOperationException("Method getModuleContentScope is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleWithDependentsScope() {
    throw new UnsupportedOperationException("Method getModuleWithDependentsScope is not yet implemented in " + getClass().getName());
  }

  @Nonnull
  @Override
  public GlobalSearchScope getModuleWithLibrariesScope() {
    throw new UnsupportedOperationException("Method getModuleWithLibrariesScope is not yet implemented in " + getClass().getName());
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
