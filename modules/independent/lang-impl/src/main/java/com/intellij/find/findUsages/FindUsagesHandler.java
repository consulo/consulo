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
package com.intellij.find.findUsages;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author peter
 * @see FindUsagesHandlerFactory
 */
public abstract class FindUsagesHandler {
  // return this handler if you want to cancel the search
  @Nonnull
  public static final FindUsagesHandler NULL_HANDLER = new NullFindUsagesHandler();

  @Nonnull
  private final PsiElement myPsiElement;

  protected FindUsagesHandler(@Nonnull PsiElement psiElement) {
    myPsiElement = psiElement;
  }

  @Nonnull
  public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
    @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
    return new CommonFindUsagesDialog(myPsiElement, getProject(), getFindUsagesOptions(ctx), toShowInNewTab, mustOpenInNewTab, isSingleFile, this);
  }

  @Nonnull
  public final PsiElement getPsiElement() {
    return myPsiElement;
  }

  @Nonnull
  public final Project getProject() {
    return myPsiElement.getProject();
  }

  @Nonnull
  public PsiElement[] getPrimaryElements() {
    return new PsiElement[]{myPsiElement};
  }

  @Nonnull
  public PsiElement[] getSecondaryElements() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Nullable
  protected String getHelpId() {
    return FindUsagesManager.getHelpID(myPsiElement);
  }

  @Nonnull
  public static FindUsagesOptions createFindUsagesOptions(@Nonnull Project project, @Nullable final DataContext dataContext) {
    FindUsagesOptions findUsagesOptions = new FindUsagesOptions(project, dataContext);
    findUsagesOptions.isUsages = true;
    findUsagesOptions.isSearchForTextOccurrences = true;
    return findUsagesOptions;
  }

  @Nonnull
  public FindUsagesOptions getFindUsagesOptions() {
    return getFindUsagesOptions(null);
  }

  @Nonnull
  public FindUsagesOptions getFindUsagesOptions(@Nullable final DataContext dataContext) {
    FindUsagesOptions options = createFindUsagesOptions(getProject(), dataContext);
    options.isSearchForTextOccurrences &= isSearchForTextOccurrencesAvailable(getPsiElement(), false);
    return options;
  }

  public boolean processElementUsages(@Nonnull final PsiElement element,
                                      @Nonnull final Processor<UsageInfo> processor,
                                      @Nonnull final FindUsagesOptions options) {
    final ReadActionProcessor<PsiReference> refProcessor = new ReadActionProcessor<PsiReference>() {
      @Override
      public boolean processInReadAction(final PsiReference ref) {
        TextRange rangeInElement = ref.getRangeInElement();
        return processor.process(new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
      }
    };

    final SearchScope scope = options.searchScope;

    final boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

    if (options.isUsages) {
      boolean success =
              ReferencesSearch.search(new ReferencesSearch.SearchParameters(element, scope, false, options.fastTrack)).forEach(refProcessor);
      if (!success) return false;
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

  public boolean processUsagesInText(@Nonnull final PsiElement element,
                                     @Nonnull Processor<UsageInfo> processor,
                                     @Nonnull GlobalSearchScope searchScope) {
    Collection<String> stringToSearch = ApplicationManager.getApplication().runReadAction(new NullableComputable<Collection<String>>() {
      @Override
      public Collection<String> compute() {
        return getStringsToSearch(element);
      }
    });
    if (stringToSearch == null) return true;
    return FindUsagesHelper.processUsagesInText(element, stringToSearch, searchScope, processor);
  }

  @Nullable
  protected Collection<String> getStringsToSearch(@Nonnull final PsiElement element) {
    if (element instanceof PsiNamedElement) {
      return ContainerUtil.createMaybeSingletonList(((PsiNamedElement)element).getName());
    }

    return Collections.singleton(element.getText());
  }

  @SuppressWarnings("deprecation")
  protected boolean isSearchForTextOccurrencesAvailable(@Nonnull PsiElement psiElement, boolean isSingleFile) {
    return isSearchForTextOccurencesAvailable(psiElement, isSingleFile);
  }

  /** @deprecated use/override {@link #isSearchForTextOccurrencesAvailable(PsiElement, boolean)} instead (to be removed in IDEA 18) */
  @SuppressWarnings({"SpellCheckingInspection", "UnusedParameters"})
  protected boolean isSearchForTextOccurencesAvailable(@Nonnull PsiElement psiElement, boolean isSingleFile) {
    return false;
  }

  @Nonnull
  public Collection<PsiReference> findReferencesToHighlight(@Nonnull PsiElement target, @Nonnull SearchScope searchScope) {
    return ReferencesSearch.search(target, searchScope, false).findAll();
  }

  private static class NullFindUsagesHandler extends FindUsagesHandler {
    private NullFindUsagesHandler() {
      super(PsiUtilCore.NULL_PSI_ELEMENT);
    }

    @Nonnull
    @Override
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public PsiElement[] getPrimaryElements() {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public PsiElement[] getSecondaryElements() {
      throw new IncorrectOperationException();
    }

    @Nullable
    @Override
    protected String getHelpId() {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public FindUsagesOptions getFindUsagesOptions() {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean processElementUsages(@Nonnull PsiElement element,
                                        @Nonnull Processor<UsageInfo> processor,
                                        @Nonnull FindUsagesOptions options) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean processUsagesInText(@Nonnull PsiElement element,
                                       @Nonnull Processor<UsageInfo> processor,
                                       @Nonnull GlobalSearchScope searchScope) {
      throw new IncorrectOperationException();
    }

    @Nullable
    @Override
    protected Collection<String> getStringsToSearch(@Nonnull PsiElement element) {
      throw new IncorrectOperationException();
    }

    @Override
    protected boolean isSearchForTextOccurrencesAvailable(@Nonnull PsiElement psiElement, boolean isSingleFile) {
      throw new IncorrectOperationException();
    }

    @Nonnull
    @Override
    public Collection<PsiReference> findReferencesToHighlight(@Nonnull PsiElement target, @Nonnull SearchScope searchScope) {
      throw new IncorrectOperationException();
    }
  }
}
