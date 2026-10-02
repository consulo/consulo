// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.url.parameter;

import consulo.language.pom.PomTarget;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.sem.SemKey;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Adds the "usage" semantics to the {@link #getSemKey()}, meaning that all {@link PsiElement}s for which {@link #getSemKey()} is defined
 * will be considered as "usage" of the {@link PomTargetPsiElement} returned from {@link #createPomTargetPsi} ( or any
 * {@link PomTargetPsiElement} which is equal)
 */
public interface RenameableSemElementSupport<T extends RenameableSemElement> {
    /**
     * A {@link SemKey} for which "usage" semantic is added
     */
    SemKey<T> getSemKey();

    /**
     * @return all {@link #getSemKey()}-"references" for the {@code pomTarget}
     * All returned {@link PsiElement}s should have the {@link #getSemKey()} defined for them via {@link consulo.language.sem.SemService}.
     * For instance, for the PathVariable PomTarget it will return PsiParameters that uses it
     *
     * @param pomTarget implementation-specific POM target.
     * Ok, different PomTargets will be passed there, but the implementation should cast and process its own
     */
    Iterable<PsiElement> findReferencingPsiElements(PomTarget pomTarget);

    /**
     * @return {@code true} if the given {@code pomTarget} could be handled by current implementation
     */
    boolean supportsTarget(PomTarget pomTarget);

    /**
     * @return {@link PomTargetPsiElement} that will be used for reference search.
     * @param sem data, that should be used to create {@link PomTargetPsiElement}.
     * It implies that {@code T} should contain enough data to create a distinguishable {@link PomTargetPsiElement},
     * though it doesn't mean that {@code sem} is the only way to create such {@link PomTargetPsiElement}s
     */
    @Nullable PomTargetPsiElement createPomTargetPsi(Project project, T sem);
}
