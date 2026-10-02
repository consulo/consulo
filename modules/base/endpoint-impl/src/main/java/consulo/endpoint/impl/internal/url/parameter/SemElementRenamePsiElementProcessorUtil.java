// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.endpoint.impl.internal.url.reference.QueryParameterSemElementSupport;
import consulo.endpoint.url.parameter.RenameableSemElement;
import consulo.endpoint.url.parameter.RenameableSemElementSupport;
import consulo.endpoint.url.parameter.RenameableSemElementUtil;
import consulo.language.editor.highlight.usage.HighlightUsagesUtil;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

final class SemElementRenamePsiElementProcessorUtil {
    static final List<RenameableSemElementSupport<?>> RENAMEABLE_SEM_ELEMENT_SUPPORT =
        List.of(QueryParameterSemElementSupport.INSTANCE, PathVariableSemElementSupport.INSTANCE);

    private SemElementRenamePsiElementProcessorUtil() {
    }

    @RequiredReadAction
    static @Nullable PsiReference createFakeReferenceForHighlighting(PsiElement psiElement, PsiElement target) {
        PsiElement navigationElement = psiElement.getNavigationElement();
        if (navigationElement == null) {
            return null;
        }
        PsiFile containingFile = navigationElement.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        TextRange identifierRange = HighlightUsagesUtil.getNameIdentifierRange(containingFile, navigationElement);
        if (identifierRange == null) {
            return null;
        }
        return new PsiReferenceBase.Immediate<>(
            navigationElement,
            identifierRange.shiftLeft(navigationElement.getTextRange().getStartOffset()),
            target
        );
    }

    static List<PsiElement> getCompanions(PsiElement element) {
        return getCompanions(element, true);
    }

    static List<PsiElement> getCompanions(PsiElement element, boolean addPomTargetForSem) {
        List<PsiElement> companions = new SmartList<>();
        if (element instanceof PomTargetPsiElement pomTargetPsiElement) {
            for (RenameableSemElementSupport<?> support : RENAMEABLE_SEM_ELEMENT_SUPPORT) {
                Iterable<PsiElement> collection = support.findReferencingPsiElements(pomTargetPsiElement.getTarget());
                boolean touched = false;
                for (PsiElement psiElement : collection) {
                    touched = true;
                    companions.add(psiElement);
                }
                if (touched) {
                    break;
                }
            }
        }
        if (addPomTargetForSem) {
            PomTargetPsiElement pomTargetPsiElement = provide(support -> createPomTargetFromSemElement(support, element));
            if (pomTargetPsiElement != null) {
                companions.add(pomTargetPsiElement);
                for (PsiElement companion : getCompanions(pomTargetPsiElement, false)) {
                    if (!companion.equals(element)) {
                        companions.add(companion);
                    }
                }
            }
        }
        return companions;
    }

    static boolean supportedElement(PsiElement element) {
        if (element instanceof PomTargetPsiElement pomTargetPsiElement) {
            for (RenameableSemElementSupport<?> support : RENAMEABLE_SEM_ELEMENT_SUPPORT) {
                if (support.supportsTarget(pomTargetPsiElement.getTarget())) {
                    return true;
                }
            }
        }
        if (provide(support -> createPomTargetFromSemElement(support, element)) != null) {
            return true;
        }
        return false;
    }

    static <T> @Nullable T provide(Function<RenameableSemElementSupport<?>, @Nullable T> call) {
        for (RenameableSemElementSupport<?> support : RENAMEABLE_SEM_ELEMENT_SUPPORT) {
            T result = call.apply(support);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    static <T extends RenameableSemElement> @Nullable PomTargetPsiElement createPomTargetFromSemElement(
        RenameableSemElementSupport<T> support,
        PsiElement semHolder
    ) {
        T queryParameterSem = RenameableSemElementUtil.getSemElement(support, semHolder);
        if (queryParameterSem == null) {
            return null;
        }
        return support.createPomTargetPsi(semHolder.getProject(), queryParameterSem);
    }
}
