/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.psi.impl.source.resolve.reference;

import consulo.ide.impl.idea.openapi.paths.GlobalPathReferenceProvider;
import consulo.language.psi.path.PathReferenceManager;
import consulo.application.util.UserDataCache;
import consulo.ide.impl.idea.openapi.vcs.IssueNavigationConfiguration;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.SmartList;
import consulo.application.util.BombedStringUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ArbitraryPlaceUrlReferenceProvider extends PsiReferenceProvider {
  private static final UserDataCache<CachedValue<PsiReference[]>, PsiElement, Object> ourRefsCache = new UserDataCache<CachedValue<PsiReference[]>, PsiElement, Object>("psielement.url.refs") {
    private final AtomicReference<GlobalPathReferenceProvider> myReferenceProvider = new AtomicReference<>();

    @Override
    protected CachedValue<PsiReference[]> compute(final PsiElement element, Object p) {
      return CachedValuesManager.getManager(element.getProject()).createCachedValue(() -> {
        IssueNavigationConfiguration navigationConfiguration = IssueNavigationConfiguration.getInstance(element.getProject());
        if (navigationConfiguration == null) {
          return CachedValueProvider.Result.create(PsiReference.EMPTY_ARRAY, element);
        }

        List<PsiReference> refs = null;
        GlobalPathReferenceProvider provider = myReferenceProvider.get();
        CharSequence commentText = BombedStringUtil.newBombedCharSequence(element.getText(), 500);
        for (IssueNavigationConfiguration.LinkMatch link : navigationConfiguration.findIssueLinks(commentText)) {
          if (refs == null) refs = new SmartList<>();
          if (provider == null) {
            provider = (GlobalPathReferenceProvider)PathReferenceManager.getInstance().getGlobalWebPathReferenceProvider();
            myReferenceProvider.lazySet(provider);
          }
          provider.createUrlReference(element, link.getTargetUrl(), link.getRange(), refs);
        }
        PsiReference[] references = refs != null ? refs.toArray(new PsiReference[refs.size()]) : PsiReference.EMPTY_ARRAY;
        return new CachedValueProvider.Result<>(references, element, navigationConfiguration);
      }, false);
    }
  };

  @Nonnull
  @Override
  public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull ProcessingContext context) {
    return ourRefsCache.get(element, null).getValue();
  }
}
