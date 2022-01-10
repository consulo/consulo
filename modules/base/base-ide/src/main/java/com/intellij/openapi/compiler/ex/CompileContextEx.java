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
package com.intellij.openapi.compiler.ex;

import consulo.compiler.make.impl.CompositeDependencyCache;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Set;

public interface CompileContextEx extends CompileContext {
  CompositeDependencyCache getDependencyCache();

  @Nullable
  VirtualFile getSourceFileByOutputFile(VirtualFile outputFile);

  void addMessage(CompilerMessage message);

  @Nonnull
  Set<VirtualFile> getTestOutputDirectories();

  /**
   * the same as FileIndex.isInTestSourceContent(), but takes into account generated output dirs
   */
  boolean isInTestSourceContent(@Nonnull VirtualFile fileOrDir);

  boolean isInSourceContent(@Nonnull VirtualFile fileOrDir);

  void addScope(CompileScope additionalScope);

  long getStartCompilationStamp();

  void recalculateOutputDirs();

  void markGenerated(Collection<VirtualFile> files);

  boolean isGenerated(VirtualFile file);

  void assignModule(@Nonnull VirtualFile root, @Nonnull Module module, boolean isTestSource, @javax.annotation.Nullable com.intellij.openapi.compiler.Compiler compiler);
}
