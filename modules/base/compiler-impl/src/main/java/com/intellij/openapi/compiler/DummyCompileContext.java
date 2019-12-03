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
package com.intellij.openapi.compiler;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import consulo.compiler.ModuleCompilerPathsManager;
import javax.annotation.Nonnull;

import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;

public class DummyCompileContext implements CompileContext {
  protected DummyCompileContext() {
  }

  private static final DummyCompileContext OUR_INSTANCE = new DummyCompileContext();

  public static DummyCompileContext getInstance() {
    return OUR_INSTANCE;
  }

  @Override
  public Project getProject() {
    return null;
  }

  @Override
  public void addMessage(CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
  }


  @Override
  public void addMessage(CompilerMessageCategory category,
                         String message,
                         @javax.annotation.Nullable String url,
                         int lineNum,
                         int columnNum,
                         Navigatable navigatable) {
  }

  @Override
  public CompilerMessage[] getMessages(CompilerMessageCategory category) {
    return CompilerMessage.EMPTY_ARRAY;
  }

  @Override
  public int getMessageCount(CompilerMessageCategory category) {
    return 0;
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return null;
  }

  @Override
  public CompileScope getCompileScope() {
    return null;
  }

  @Override
  public void requestRebuildNextTime(String message) {
  }

  @Override
  public Module getModuleByFile(VirtualFile file) {
    return null;
  }

  @Override
  public VirtualFile[] getSourceRoots(Module module) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Override
  public VirtualFile[] getAllOutputDirectories() {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Override
  public VirtualFile getModuleOutputDirectory(final Module module) {
    return ApplicationManager.getApplication().runReadAction(new Computable<VirtualFile>() {
      @Override
      public VirtualFile compute() {
        return ModuleCompilerPathsManager.getInstance(module)
          .getCompilerOutput(ProductionContentFolderTypeProvider.getInstance());
      }
    });
  }

  @Override
  public VirtualFile getModuleOutputDirectoryForTests(Module module) {
    return null;
  }

  @Override
  public VirtualFile getOutputForFile(Module module, VirtualFile virtualFile) {
    return null;
  }

  @javax.annotation.Nullable
  @Override
  public VirtualFile getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
    return null;
  }

  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, T value) {
  }

  @Override
  public boolean isMake() {
    return false; // stub implementation
  }

  @Override
  public boolean isRebuild() {
    return false;
  }
}
