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
package consulo.compiler.impl.internal.artifact;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompileContext;
import consulo.compiler.CompilerManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.generic.CompileItem;
import consulo.compiler.generic.GenericCompiler;
import consulo.compiler.generic.GenericCompilerInstance;
import consulo.compiler.generic.VirtualFilePersistentState;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
@ExtensionImpl(id = "artifactCompiler", order = "last")
public class ArtifactsCompiler extends GenericCompiler<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState> {
    private static final Key<Set<String>> WRITTEN_PATHS_KEY = Key.create("artifacts_written_paths");
    private static final Key<Set<Artifact>> CHANGED_ARTIFACTS = Key.create("affected_artifacts");

    public ArtifactsCompiler() {
        super("artifacts_compiler", 0, GenericCompiler.CompileOrderPlace.PACKAGING);
    }

    @Nullable
    public static ArtifactsCompiler getInstance(@Nonnull Project project) {
        ArtifactsCompiler[] compilers = CompilerManager.getInstance(project).getCompilers(ArtifactsCompiler.class);
        return compilers.length == 1 ? compilers[0] : null;
    }

    public static void addChangedArtifact(CompileContext context, Artifact artifact) {
        Set<Artifact> artifacts = context.getUserData(CHANGED_ARTIFACTS);
        if (artifacts == null) {
            artifacts = new HashSet<>();
            context.putUserData(CHANGED_ARTIFACTS, artifacts);
        }
        artifacts.add(artifact);
    }

    public static void addWrittenPaths(CompileContext context, Set<String> writtenPaths) {
        Set<String> paths = context.getUserData(WRITTEN_PATHS_KEY);
        if (paths == null) {
            paths = new HashSet<>();
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
    public GenericCompilerInstance<
        ArtifactBuildTarget,
        ? extends CompileItem<String, VirtualFilePersistentState, ArtifactPackagingItemOutputState>,
        String,
        VirtualFilePersistentState,
        ArtifactPackagingItemOutputState
    >
    createInstance(@Nonnull CompileContext context) {
        return new ArtifactsCompilerInstance(context);
    }

    @Override
    @Nonnull
    public String getDescription() {
        return "Artifacts Packaging Compiler";
    }

    @Nullable
    public static Set<Artifact> getChangedArtifacts(CompileContext compileContext) {
        return compileContext.getUserData(CHANGED_ARTIFACTS);
    }

    @Nullable
    public static Set<String> getWrittenPaths(@Nonnull CompileContext context) {
        return context.getUserData(WRITTEN_PATHS_KEY);
    }
}
