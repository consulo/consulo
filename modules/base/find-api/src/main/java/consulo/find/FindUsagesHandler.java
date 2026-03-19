/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.find;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.util.ReadActionProcessor;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.util.TextRange;
import consulo.find.ui.AbstractFindUsagesDialog;
import consulo.find.ui.AbstractFindUsagesDialogDescriptor;
import consulo.find.ui.CommonFindUsagesDialog;
import consulo.find.ui.CommonFindUsagesDialogDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.UsageInfo;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author peter
 * @see FindUsagesHandlerFactory
 */
public abstract class FindUsagesHandler {
    // return this handler if you want to cancel the search
    
    public static final FindUsagesHandler NULL_HANDLER = new NullFindUsagesHandler();

    
    private final PsiElement myPsiElement;

    protected FindUsagesHandler(PsiElement psiElement) {
        myPsiElement = psiElement;
    }

    public boolean supportConsuloUI() {
        return false;
    }

    
    @Deprecated
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
        @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
        return new CommonFindUsagesDialog(
            myPsiElement,
            getProject(),
            getFindUsagesOptions(ctx),
            toShowInNewTab,
            mustOpenInNewTab,
            isSingleFile,
            this
        );
    }

    
    @RequiredUIAccess
    public AbstractFindUsagesDialogDescriptor createFindUsagesDialogDescriptor(
        DataContext ctx,
        boolean isSingleFile,
        boolean toShowInNewTab,
        boolean mustOpenInNewTab
    ) {
        return new CommonFindUsagesDialogDescriptor(
            myPsiElement,
            getProject(),
            getFindUsagesOptions(ctx),
            toShowInNewTab,
            mustOpenInNewTab,
            isSingleFile,
            this
        );
    }

    
    public final PsiElement getPsiElement() {
        return myPsiElement;
    }

    
    public final Project getProject() {
        return myPsiElement.getProject();
    }

    
    public PsiElement[] getPrimaryElements() {
        return new PsiElement[]{myPsiElement};
    }

    
    public PsiElement[] getSecondaryElements() {
        return PsiElement.EMPTY_ARRAY;
    }

    public @Nullable String getHelpId() {
        return AccessRule.read(() -> FindUsagesHelper.getHelpID(myPsiElement));
    }

    
    @RequiredReadAction
    public static FindUsagesOptions createFindUsagesOptions(Project project, @Nullable DataContext dataContext) {
        FindUsagesOptions findUsagesOptions = new FindUsagesOptions(project, dataContext);
        findUsagesOptions.isUsages = true;
        findUsagesOptions.isSearchForTextOccurrences = true;
        return findUsagesOptions;
    }

    
    public FindUsagesOptions getFindUsagesOptions() {
        return getFindUsagesOptions(null);
    }

    
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
        FindUsagesOptions options = createFindUsagesOptions(getProject(), dataContext);
        options.isSearchForTextOccurrences &= isSearchForTextOccurrencesAvailable(getPsiElement(), false);
        return options;
    }

    public boolean processElementUsages(
        PsiElement element,
        final Predicate<UsageInfo> processor,
        FindUsagesOptions options
    ) {
        ReadActionProcessor<PsiReference> refProcessor = new ReadActionProcessor<>() {
            @Override
            public boolean processInReadAction(PsiReference ref) {
                TextRange rangeInElement = ref.getRangeInElement();
                return processor.test(new UsageInfo(
                    ref.getElement(),
                    rangeInElement.getStartOffset(),
                    rangeInElement.getEndOffset(),
                    false
                ));
            }
        };

        SearchScope scope = options.searchScope;

        boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

        if (options.isUsages) {
            boolean success =
                ReferencesSearch.search(new ReferencesSearch.SearchParameters(element, scope, false, options.fastTrack))
                    .forEach(refProcessor);
            if (!success) {
                return false;
            }
        }

        if (searchText) {
            if (options.fastTrack != null) {
                options.fastTrack.searchCustom(consumer -> processUsagesInText(element, processor, (GlobalSearchScope)scope));
            }
            else {
                return processUsagesInText(element, processor, (GlobalSearchScope)scope);
            }
        }
        return true;
    }

    public boolean processUsagesInText(
        PsiElement element,
        Predicate<UsageInfo> processor,
        GlobalSearchScope searchScope
    ) {
        Collection<String> stringToSearch =
            Application.get().runReadAction((Supplier<Collection<String>>)() -> getStringsToSearch(element));
        return stringToSearch == null || FindUsagesHelper.processUsagesInText(element, stringToSearch, searchScope, processor);
        }

    protected @Nullable Collection<String> getStringsToSearch(PsiElement element) {
        if (element instanceof PsiNamedElement) {
            return ContainerUtil.createMaybeSingletonList(((PsiNamedElement)element).getName());
        }

        return Collections.singleton(element.getText());
    }

    @SuppressWarnings("deprecation")
    protected boolean isSearchForTextOccurrencesAvailable(PsiElement psiElement, boolean isSingleFile) {
        return isSearchForTextOccurencesAvailable(psiElement, isSingleFile);
    }

    /**
     * @deprecated use/override {@link #isSearchForTextOccurrencesAvailable(PsiElement, boolean)} instead (to be removed in IDEA 18)
     */
    @SuppressWarnings({"SpellCheckingInspection", "UnusedParameters"})
    protected boolean isSearchForTextOccurencesAvailable(PsiElement psiElement, boolean isSingleFile) {
        return false;
    }

    
    public Collection<PsiReference> findReferencesToHighlight(PsiElement target, SearchScope searchScope) {
        return ReferencesSearch.search(target, searchScope, false).findAll();
    }

    private static class NullFindUsagesHandler extends FindUsagesHandler {
        private NullFindUsagesHandler() {
            super(PsiUtilCore.NULL_PSI_ELEMENT);
        }

        
        @Override
        public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
            throw new IncorrectOperationException();
        }

        @RequiredUIAccess
        
        @Override
        public AbstractFindUsagesDialogDescriptor createFindUsagesDialogDescriptor(
            DataContext ctx,
            boolean isSingleFile,
            boolean toShowInNewTab,
            boolean mustOpenInNewTab
        ) {
            throw new IncorrectOperationException();
        }

        
        @Override
        public PsiElement[] getPrimaryElements() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public PsiElement[] getSecondaryElements() {
            throw new IncorrectOperationException();
        }

        @Nullable
        @Override
        public String getHelpId() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public FindUsagesOptions getFindUsagesOptions() {
            throw new IncorrectOperationException();
        }

        
        @Override
        public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean processElementUsages(
            PsiElement element,
            Predicate<UsageInfo> processor,
            FindUsagesOptions options
        ) {
            throw new IncorrectOperationException();
        }

        @Override
        public boolean processUsagesInText(
            PsiElement element,
            Predicate<UsageInfo> processor,
            GlobalSearchScope searchScope
        ) {
            throw new IncorrectOperationException();
        }

        @Nullable
        @Override
        protected Collection<String> getStringsToSearch(PsiElement element) {
            throw new IncorrectOperationException();
        }

        @Override
        protected boolean isSearchForTextOccurrencesAvailable(PsiElement psiElement, boolean isSingleFile) {
            throw new IncorrectOperationException();
        }

        
        @Override
        public Collection<PsiReference> findReferencesToHighlight(PsiElement target, SearchScope searchScope) {
            throw new IncorrectOperationException();
        }
    }
}
