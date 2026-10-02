// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.endpoint.url.parameter.PathVariablePomTarget;
import consulo.endpoint.url.parameter.PathVariableSem;
import consulo.endpoint.url.parameter.RenameableSemElementSupport;
import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.sem.SemKey;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PathVariableSemElementSupport implements RenameableSemElementSupport<PathVariableSem> {
    public static final PathVariableSemElementSupport INSTANCE = new PathVariableSemElementSupport();

    private PathVariableSemElementSupport() {
    }

    @Override
    public SemKey<PathVariableSem> getSemKey() {
        return PathVariableSem.PATH_VARIABLE_SEM_KEY;
    }

    @Override
    public Iterable<PsiElement> findReferencingPsiElements(PomTarget pomTarget) {
        if (!(pomTarget instanceof PathVariablePomTarget pathVariablePomTarget)) {
            return List.of();
        }
        return pathVariablePomTarget.findSemDefinitionPsiElement();
    }

    @Override
    public boolean supportsTarget(PomTarget pomTarget) {
        return pomTarget instanceof PathVariablePomTarget;
    }

    @Override
    public @Nullable PomTargetPsiElement createPomTargetPsi(Project project, PathVariableSem sem) {
        return sem.getPathVariablePsiElement();
    }
}
