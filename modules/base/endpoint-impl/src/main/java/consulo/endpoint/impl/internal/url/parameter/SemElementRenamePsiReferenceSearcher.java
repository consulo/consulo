// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.scope.SearchScope;
import consulo.endpoint.url.reference.UrlPathContextUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceService;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.RequestResultProcessor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.project.util.query.QueryExecutorBase;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@ExtensionImpl
public final class SemElementRenamePsiReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
    implements ReferencesSearchQueryExecutor {

    public SemElementRenamePsiReferenceSearcher() {
        super(true);
    }

    @Override
    public void processQuery(ReferencesSearch.SearchParameters queryParameters, Predicate<? super PsiReference> consumer) {
        UrlPathContextUtil.forbidExpensiveUrlContext(() -> {
            searchWord(queryParameters);
            return null;
        });
    }

    @RequiredReadAction
    private static void searchWord(ReferencesSearch.SearchParameters queryParameters) {
        PsiElement target = queryParameters.getElementToSearch();
        if (!(SemElementRenamePsiElementProcessorUtil.provide(
            support -> SemElementRenamePsiElementProcessorUtil.createPomTargetFromSemElement(support, target)
        ) instanceof PsiNamedElement pathVariableDefinition)) {
            return;
        }
        String name = pathVariableDefinition.getName();
        if (name == null || name.isEmpty()) {
            return;
        }
        PsiFile containingFile = target.getContainingFile();
        SearchScope searchScope = containingFile != null ? new LocalSearchScope(containingFile) : pathVariableDefinition.getUseScope();

        Set<PsiElement> knowTargets = ConcurrentHashMap.newKeySet();
        knowTargets.add(pathVariableDefinition);
        knowTargets.add(target);

        queryParameters.getOptimizer().searchWord(
            name,
            searchScope,
            UsageSearchContext.IN_CODE,
            true,
            pathVariableDefinition,
            new RequestResultProcessor(pathVariableDefinition, target) {
                @Override
                @RequiredReadAction
                public boolean processTextOccurrence(PsiElement psiElement, int offset, Predicate<? super PsiReference> consumer) {
                    PsiReferenceService.Hints hints = new PsiReferenceService.Hints(null, offset);
                    for (PsiReference reference : PsiReferenceService.getService().getReferences(psiElement, hints)) {
                        PsiElement resolve = reference.resolve();
                        if (resolve == null) {
                            continue;
                        }
                        if (knowTargets.contains(resolve)) {
                            if (!consumer.test(reference)) {
                                return false;
                            }
                            continue;
                        }
                        if (Objects.equals(
                            SemElementRenamePsiElementProcessorUtil.provide(
                                support -> SemElementRenamePsiElementProcessorUtil.createPomTargetFromSemElement(support, resolve)
                            ),
                            pathVariableDefinition
                        )) {
                            if (!consumer.test(reference)) {
                                return false;
                            }
                            if (knowTargets.add(resolve)) {
                                PsiReference fakeReference =
                                    SemElementRenamePsiElementProcessorUtil.createFakeReferenceForHighlighting(resolve, target);
                                if (fakeReference != null && !consumer.test(fakeReference)) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        );
    }
}
