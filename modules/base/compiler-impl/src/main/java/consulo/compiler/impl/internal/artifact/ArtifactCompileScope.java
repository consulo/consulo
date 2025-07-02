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
package consulo.compiler.impl.internal.artifact;

import consulo.annotation.access.RequiredReadAction;
import consulo.compiler.CompilerManager;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.ArtifactElementType;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.compiler.artifact.element.ProductionModuleOutputElementType;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.scope.ModuleCompileScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author nik
 */
public class ArtifactCompileScope {
    private static final Key<Boolean> FORCE_ARTIFACT_BUILD = Key.create("force_artifact_build");
    private static final Key<Artifact[]> ARTIFACTS_KEY = Key.create("artifacts");
    private static final Key<Set<Artifact>> CACHED_ARTIFACTS_KEY = Key.create("cached_artifacts");
    private static final Key<Key<?>> ARTIFACTS_CONTENT_ID_KEY = Key.create("build_artifacts_task");

    private ArtifactCompileScope() {
    }

    @RequiredReadAction
    public static ModuleCompileScope createScopeForModulesInArtifacts(
        @Nonnull Project project,
        @Nonnull Collection<? extends Artifact> artifacts
    ) {
        boolean[] includeTestScope = new boolean[1];
        final Set<Module> modules = ArtifactUtil.getModulesIncludedInArtifacts(artifacts, project, includeTestScope);
        return new ModuleCompileScope(project, modules.toArray(new Module[modules.size()]), true, includeTestScope[0]);
    }

    public static CompileScope createArtifactsScope(
        @Nonnull Project project,
        @Nonnull Collection<Artifact> artifacts
    ) {
        return createArtifactsScope(project, artifacts, false);
    }

    public static CompileScope createArtifactsScope(
        @Nonnull Project project,
        @Nonnull Collection<Artifact> artifacts,
        final boolean forceArtifactBuild
    ) {
        return createScopeWithArtifacts(createScopeForModulesInArtifacts(project, artifacts), artifacts, true, forceArtifactBuild);
    }

    public static CompileScope createScopeWithArtifacts(
        final CompileScope baseScope,
        @Nonnull Collection<Artifact> artifacts,
        boolean useCustomContentId
    ) {
        return createScopeWithArtifacts(baseScope, artifacts, useCustomContentId, false);
    }

    public static CompileScope createScopeWithArtifacts(
        final CompileScope baseScope,
        @Nonnull Collection<Artifact> artifacts,
        boolean useCustomContentId,
        final boolean forceArtifactBuild
    ) {
        baseScope.putUserData(ARTIFACTS_KEY, artifacts.toArray(new Artifact[artifacts.size()]));
        if (useCustomContentId) {
            baseScope.putUserData(CompilerManager.CONTENT_ID_KEY, ARTIFACTS_CONTENT_ID_KEY);
        }
        if (forceArtifactBuild) {
            baseScope.putUserData(FORCE_ARTIFACT_BUILD, Boolean.TRUE);
        }
        return baseScope;
    }

    public static Set<Artifact> getArtifactsToBuild(
        final Project project,
        final CompileScope compileScope,
        final boolean addIncludedArtifactsWithOutputPathsOnly
    ) {
        final Artifact[] artifactsFromScope = getArtifacts(compileScope);
        final ArtifactManager artifactManager = ArtifactManager.getInstance(project);
        PackagingElementResolvingContext context = artifactManager.getResolvingContext();
        if (artifactsFromScope != null) {
            return addIncludedArtifacts(Arrays.asList(artifactsFromScope), context, addIncludedArtifactsWithOutputPathsOnly);
        }

        final Set<Artifact> cached = compileScope.getUserData(CACHED_ARTIFACTS_KEY);
        if (cached != null) {
            return cached;
        }

        Set<Artifact> artifacts = new HashSet<>();
        final Set<Module> modules = new HashSet<>(Arrays.asList(compileScope.getAffectedModules()));
        final List<Module> allModules = Arrays.asList(ModuleManager.getInstance(project).getModules());
        for (Artifact artifact : artifactManager.getArtifacts()) {
            if (artifact.isBuildOnMake()) {
                if (modules.containsAll(allModules)
                    || containsModuleOutput(artifact, modules, context)) {
                    artifacts.add(artifact);
                }
            }
        }
        Set<Artifact> result = addIncludedArtifacts(artifacts, context, addIncludedArtifactsWithOutputPathsOnly);
        compileScope.putUserData(CACHED_ARTIFACTS_KEY, result);
        return result;
    }

    @Nullable
    public static Artifact[] getArtifacts(CompileScope compileScope) {
        return compileScope.getUserData(ARTIFACTS_KEY);
    }

    public static boolean isArtifactRebuildForced(@Nonnull CompileScope scope) {
        return Boolean.TRUE.equals(scope.getUserData(FORCE_ARTIFACT_BUILD));
    }

    private static boolean containsModuleOutput(
        Artifact artifact,
        final Set<Module> modules,
        final PackagingElementResolvingContext context
    ) {
        return !ArtifactUtil.processPackagingElements(artifact,
            ProductionModuleOutputElementType.getInstance(), it -> {
                final Module module = it.findModule(context);
                return module == null || !modules.contains(module);
            }, context, true
        );
    }

    @Nonnull
    private static Set<Artifact> addIncludedArtifacts(
        @Nonnull Collection<Artifact> artifacts,
        @Nonnull PackagingElementResolvingContext context,
        final boolean withOutputPathOnly
    ) {
        Set<Artifact> result = new HashSet<>();
        for (Artifact artifact : artifacts) {
            collectIncludedArtifacts(artifact, context, new HashSet<>(), result, withOutputPathOnly);
        }
        return result;
    }

    private static void collectIncludedArtifacts(
        Artifact artifact,
        final PackagingElementResolvingContext context,
        final Set<Artifact> processed,
        final Set<Artifact> result,
        final boolean withOutputPathOnly
    ) {
        if (!processed.add(artifact)) {
            return;
        }
        if (!withOutputPathOnly || !StringUtil.isEmpty(artifact.getOutputPath())) {
            result.add(artifact);
        }

        ArtifactUtil.processPackagingElements(artifact, ArtifactElementType.getInstance(), element -> {
            Artifact included = element.findArtifact(context);
            if (included != null) {
                collectIncludedArtifacts(included, context, processed, result, withOutputPathOnly);
            }
            return true;
        }, context, false);
    }
}
