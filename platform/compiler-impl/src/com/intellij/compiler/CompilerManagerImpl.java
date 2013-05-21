/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * @author VISTALL
 * @since 10:44/21.05.13
 */
public class CompilerManagerImpl extends CompilerManager {
  @Override
  public boolean isCompilationActive() {
    return false;
  }

  @Override
  public void addCompiler(@NotNull com.intellij.openapi.compiler.Compiler compiler) {

  }

  @Override
  public void addTranslatingCompiler(@NotNull TranslatingCompiler compiler, Set<FileType> inputTypes, Set<FileType> outputTypes) {

  }

  @NotNull
  @Override
  public Set<FileType> getRegisteredInputTypes(@NotNull TranslatingCompiler compiler) {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public Set<FileType> getRegisteredOutputTypes(@NotNull TranslatingCompiler compiler) {
    return Collections.emptySet();
  }

  @Override
  public void removeCompiler(@NotNull Compiler compiler) {

  }

  @NotNull
  @Override
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass) {
    return (T[])Array.newInstance(compilerClass, 0);
  }

  @NotNull
  @Override
  public <T extends Compiler> T[] getCompilers(@NotNull Class<T> compilerClass, CompilerFilter filter) {
    return (T[])Array.newInstance(compilerClass, 0);
  }

  @Override
  public void addCompilableFileType(@NotNull FileType type) {

  }

  @Override
  public void removeCompilableFileType(@NotNull FileType type) {

  }

  @Override
  public boolean isCompilableFileType(@NotNull FileType type) {
    return false;
  }

  @Override
  public void addBeforeTask(@NotNull CompileTask task) {

  }

  @Override
  public void addAfterTask(@NotNull CompileTask task) {

  }

  @NotNull
  @Override
  public CompileTask[] getBeforeTasks() {
    return new CompileTask[0];
  }

  @NotNull
  @Override
  public CompileTask[] getAfterTasks() {
    return new CompileTask[0];
  }

  @Override
  public void compile(@NotNull VirtualFile[] files, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void compile(@NotNull Module module, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void compile(@NotNull CompileScope scope, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void make(@Nullable CompileStatusNotification callback) {

  }

  @Override
  public void make(@NotNull Module module, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void make(@NotNull Project project, @NotNull Module[] modules, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void make(@NotNull CompileScope scope, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public void make(@NotNull CompileScope scope, CompilerFilter filter, @Nullable CompileStatusNotification callback) {

  }

  @Override
  public boolean isUpToDate(@NotNull CompileScope scope) {
    return false;
  }

  @Override
  public void rebuild(@Nullable CompileStatusNotification callback) {

  }

  @Override
  public void executeTask(@NotNull CompileTask task, @NotNull CompileScope scope, String contentName, @Nullable Runnable onTaskFinished) {

  }

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener) {

  }

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener, @NotNull Disposable parentDisposable) {

  }

  @Override
  public void removeCompilationStatusListener(@NotNull CompilationStatusListener listener) {

  }

  @Override
  public boolean isExcludedFromCompilation(@NotNull VirtualFile file) {
    return false;
  }

  @NotNull
  @Override
  public OutputToSourceMapping getJavaCompilerOutputMapping() {
    return null;
  }

  @NotNull
  @Override
  public CompileScope createFilesCompileScope(@NotNull VirtualFile[] files) {
    return null;
  }

  @NotNull
  @Override
  public CompileScope createModuleCompileScope(@NotNull Module module, boolean includeDependentModules) {
    return null;
  }

  @NotNull
  @Override
  public CompileScope createModulesCompileScope(@NotNull Module[] modules, boolean includeDependentModules) {
    return null;
  }

  @NotNull
  @Override
  public CompileScope createModuleGroupCompileScope(@NotNull Project project, @NotNull Module[] modules, boolean includeDependentModules) {
    return null;
  }

  @NotNull
  @Override
  public CompileScope createProjectCompileScope(@NotNull Project project) {
    return null;
  }

  @Override
  public boolean isValidationEnabled(Module moduleType) {
    return false;
  }
}
