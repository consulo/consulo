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
package com.intellij.compiler.impl;

import consulo.compiler.make.impl.CompositeDependencyCache;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.roots.ContentFolderTypeProvider;

import java.util.Collection;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 4, 2007
 */
public class CompileContextExProxy implements CompileContextEx {
  private final CompileContextEx myDelegate;

  public CompileContextExProxy(CompileContextEx delegate) {
    myDelegate = delegate;
  }

  @Override
  public Project getProject() {
    return myDelegate.getProject();
  }

  @Override
  public CompositeDependencyCache getDependencyCache() {
    return myDelegate.getDependencyCache();
  }

  @Override
  public VirtualFile getSourceFileByOutputFile(final VirtualFile outputFile) {
    return myDelegate.getSourceFileByOutputFile(outputFile);
  }

  @Override
  public void addMessage(final CompilerMessage message) {
    myDelegate.addMessage(message);
  }

  @Override
  @Nonnull
  public Set<VirtualFile> getTestOutputDirectories() {
    return myDelegate.getTestOutputDirectories();
  }

  @Override
  public boolean isInTestSourceContent(@Nonnull final VirtualFile fileOrDir) {
    return myDelegate.isInTestSourceContent(fileOrDir);
  }

  @Override
  public boolean isInSourceContent(@Nonnull final VirtualFile fileOrDir) {
    return myDelegate.isInSourceContent(fileOrDir);
  }

  @Override
  public void addScope(final CompileScope additionalScope) {
    myDelegate.addScope(additionalScope);
  }

  @Override
  public void addMessage(final CompilerMessageCategory category,
                         final String message, @Nullable final String url, final int lineNum, final int columnNum) {
    myDelegate.addMessage(category, message, url, lineNum, columnNum);
  }

  @Override
  public void addMessage(final CompilerMessageCategory category, final String message, @javax.annotation.Nullable final String url,
                         final int lineNum,
                         final int columnNum,
                         final Navigatable navigatable) {
    myDelegate.addMessage(category, message, url, lineNum, columnNum, navigatable);
  }

  @Override
  public CompilerMessage[] getMessages(final CompilerMessageCategory category) {
    return myDelegate.getMessages(category);
  }

  @Override
  public int getMessageCount(final CompilerMessageCategory category) {
    return myDelegate.getMessageCount(category);
  }

  @Override
  public ProgressIndicator getProgressIndicator() {
    return myDelegate.getProgressIndicator();
  }

  @Override
  public CompileScope getCompileScope() {
    return myDelegate.getCompileScope();
  }

  @Override
  public void requestRebuildNextTime(final String message) {
    myDelegate.requestRebuildNextTime(message);
  }

  @Override
  public Module getModuleByFile(final VirtualFile file) {
    return myDelegate.getModuleByFile(file);
  }

  @Override
  public VirtualFile[] getSourceRoots(final Module module) {
    return myDelegate.getSourceRoots(module);
  }

  @Override
  public VirtualFile[] getAllOutputDirectories() {
    return myDelegate.getAllOutputDirectories();
  }

  @Override
  public VirtualFile getModuleOutputDirectory(final Module module) {
    return myDelegate.getModuleOutputDirectory(module);
  }

  @Override
  public VirtualFile getModuleOutputDirectoryForTests(final Module module) {
    return myDelegate.getModuleOutputDirectoryForTests(module);
  }

  @Override
  public VirtualFile getOutputForFile(Module module, VirtualFile virtualFile) {
    return myDelegate.getOutputForFile(module, virtualFile);
  }

  @Nullable
  @Override
  public VirtualFile getOutputForFile(Module module, ContentFolderTypeProvider contentFolderType) {
    return myDelegate.getOutputForFile(module, contentFolderType);
  }

  @Override
  public boolean isMake() {
    return myDelegate.isMake();
  }

  @Override
  public boolean isRebuild() {
    return myDelegate.isRebuild();
  }

  @Override
  public <T> T getUserData(@Nonnull final Key<T> key) {
    return myDelegate.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull final Key<T> key, final T value) {
    myDelegate.putUserData(key, value);
  }

  @Override
  public void recalculateOutputDirs() {
    myDelegate.recalculateOutputDirs();
  }

  @Override
  public void markGenerated(Collection<VirtualFile> files) {
    myDelegate.markGenerated(files);
  }

  @Override
  public boolean isGenerated(VirtualFile file) {
    return myDelegate.isGenerated(file);
  }

  @Override
  public long getStartCompilationStamp() {
    return myDelegate.getStartCompilationStamp();
  }

  @Override
  public void assignModule(@Nonnull VirtualFile root, @Nonnull Module module, boolean isTestSource, com.intellij.openapi.compiler.Compiler compiler) {
    myDelegate.assignModule(root, module, isTestSource, compiler);
  }
}
