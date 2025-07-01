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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.compiler.Compiler;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.internal.AdditionalCompileScopeProvider;
import consulo.compiler.scope.CompileScope;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.Set;
import java.util.function.Predicate;

/**
 * @author nik
 */
@ExtensionImpl
public class ArtifactAdditionalCompileScopeProvider extends AdditionalCompileScopeProvider {
    @Override
    public CompileScope getAdditionalScope(
        @Nonnull CompileScope baseScope,
        @Nonnull Predicate<Compiler> filter,
        @Nonnull Project project
    ) {
        if (ArtifactCompileScope.getArtifacts(baseScope) != null) {
            return null;
        }
        ArtifactsCompiler compiler = ArtifactsCompiler.getInstance(project);
        if (compiler == null || !filter.test(compiler)) {
            return null;
        }
        return ReadAction.compute(() -> {
            Set<Artifact> artifacts = ArtifactCompileScope.getArtifactsToBuild(project, baseScope, false);
            return ArtifactCompileScope.createScopeForModulesInArtifacts(project, artifacts);
        });
    }
}
