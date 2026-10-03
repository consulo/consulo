// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url.parameter;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.find.FindUsagesOptions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.usage.UsageInfo;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ExtensionImpl
public final class RenameableSemElementFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(PsiElement element) {
        return SemElementRenamePsiElementProcessorUtil.supportedElement(element);
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        return new FindUsagesHandler(element) {
            @Override
            public boolean supportConsuloUI() {
                return true;
            }

            @Override
            public boolean processElementUsages(PsiElement element, Predicate<UsageInfo> processor, FindUsagesOptions options) {
                return Application.get().runReadAction((Supplier<Boolean>) () -> {
                    for (PsiElement companion : SemElementRenamePsiElementProcessorUtil.getCompanions(element)) {
                        if (!super.processElementUsages(companion, processor, options)) {
                            return false;
                        }
                        if (companion.getContainingFile() != null && !processor.test(new UsageInfo(companion))) {
                            return false;
                        }
                    }
                    return true;
                }) && super.processElementUsages(element, processor, options);
            }

            @Override
            public Collection<PsiReference> findReferencesToHighlight(PsiElement target, SearchScope searchScope) {
                List<PsiReference> result = new SmartList<>();
                for (PsiElement companion : SemElementRenamePsiElementProcessorUtil.getCompanions(target, false)) {
                    result.addAll(super.findReferencesToHighlight(companion, searchScope));
                    PsiReference fakeReference =
                        SemElementRenamePsiElementProcessorUtil.createFakeReferenceForHighlighting(companion, target);
                    if (fakeReference != null) {
                        result.add(fakeReference);
                    }
                }
                result.addAll(super.findReferencesToHighlight(target, searchScope));

                Set<Pair<PsiElement, TextRange>> seen = new HashSet<>();
                List<PsiReference> distinct = new ArrayList<>();
                for (PsiReference ref : result) {
                    if (seen.add(Pair.create(ref.getElement(), ref.getRangeInElement()))) {
                        distinct.add(ref);
                    }
                }
                return distinct;
            }
        };
    }
}
