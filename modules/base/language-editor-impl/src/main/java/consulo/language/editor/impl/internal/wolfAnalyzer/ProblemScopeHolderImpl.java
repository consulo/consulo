/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.impl.internal.wolfAnalyzer;

import consulo.annotation.component.ServiceImpl;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.wolfAnalyzer.ProblemScopeHolder;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-04-12
 */
@Singleton
@ServiceImpl
public class ProblemScopeHolderImpl implements ProblemScopeHolder {
    private final NamedScope myProblemsScope;

    @Inject
    public ProblemScopeHolderImpl(@Nonnull Project thisProject, @Nonnull Provider<WolfTheProblemSolver> wolfTheProblemSolverProvider) {
        final String text = "file:*//*";

        myProblemsScope = new NamedScope(AnalysisScopeLocalize.predefinedScopeProblemsName().get(), new AbstractPackageSet(text) {
            @Override
            public boolean contains(VirtualFile file, Project project, NamedScopesHolder holder) {
                return project == thisProject && wolfTheProblemSolverProvider.get().isProblemFile(file);
            }
        });
    }

    @Nonnull
    @Override
    public NamedScope getProblemsScope() {
        return myProblemsScope;
    }
}
