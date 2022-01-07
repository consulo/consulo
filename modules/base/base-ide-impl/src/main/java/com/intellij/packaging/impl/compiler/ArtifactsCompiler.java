/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.packaging.impl.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.generic.CompileItem;
import com.intellij.openapi.compiler.generic.GenericCompiler;
import com.intellij.openapi.compiler.generic.GenericCompilerInstance;
import com.intellij.openapi.compiler.generic.VirtualFilePersistentState;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
public class ArtifactsCompiler extends GenericCompiler<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> {
  private static final Key<Set<String>> WRITTEN_PATHS_KEY = Key.create("artifacts_written_paths");
  private static final Key<Set<Artifact>> CHANGED_ARTIFACTS = Key.create("affected_artifacts");

  public ArtifactsCompiler() {
    super("artifacts_compiler", 0, GenericCompiler.CompileOrderPlace.PACKAGING);
  }

  @javax.annotation.Nullable
  public static ArtifactsCompiler getInstance(@Nonnull Project project) {
    final ArtifactsCompiler[] compilers = CompilerManager.getInstance(project).getCompilers(ArtifactsCompiler.class);
    return compilers.length == 1 ? compilers[0] : null;
  }

  public static void addChangedArtifact(final CompileContext context, Artifact artifact) {
    Set<Artifact> artifacts = context.getUserData(CHANGED_ARTIFACTS);
    if (artifacts == null) {
      artifacts = new HashSet<Artifact>();
      context.putUserData(CHANGED_ARTIFACTS, artifacts);
    }
    artifacts.add(artifact);
  }

  public static void addWrittenPaths(final CompileContext context, Set<String> writtenPaths) {
    Set<String> paths = context.getUserData(WRITTEN_PATHS_KEY);
    if (paths == null) {
      paths = new HashSet<String>();
      context.putUserData(WRITTEN_PATHS_KEY, paths);
    }
    paths.addAll(writtenPaths);
  }

  @Nonnull
  @Override
  public KeyDescriptor<String> getItemKeyDescriptor() {
    return STRING_KEY_DESCRIPTOR;
  }

  @Nonnull
  @Override
  public DataExternalizer<VirtualFilePersistentState> getSourceStateExternalizer() {
    return VirtualFilePersistentState.EXTERNALIZER;
  }

  @Nonnull
  @Override
  public DataExternalizer<ArtifactPackagingItemOutputState> getOutputStateExternalizer() {
    return new ArtifactPackagingItemExternalizer();
  }

  @Nonnull
  @Override
  public GenericCompilerInstance<ArtifactBuildTarget, ? extends CompileItem<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState>, String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> createInstance(
    @Nonnull CompileContext context) {
    return new ArtifactsCompilerInstance(context);
  }

  @Nonnull
  public String getDescription() {
    return "Artifacts Packaging Compiler";
  }

  @Nullable
  public static Set<Artifact> getChangedArtifacts(final CompileContext compileContext) {
    return compileContext.getUserData(CHANGED_ARTIFACTS);
  }

  @Nullable
  public static Set<String> getWrittenPaths(@Nonnull CompileContext context) {
    return context.getUserData(WRITTEN_PATHS_KEY);
  }
}
