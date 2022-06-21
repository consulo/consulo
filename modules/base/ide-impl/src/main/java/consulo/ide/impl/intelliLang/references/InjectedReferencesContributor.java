/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.intelliLang.references;

import consulo.document.util.TextRange;
import consulo.ide.impl.intelliLang.Configuration;
import consulo.ide.impl.intelliLang.inject.InjectedLanguage;
import consulo.ide.impl.intelliLang.inject.InjectorUtils;
import consulo.ide.impl.intelliLang.inject.TemporaryPlacesRegistry;
import consulo.ide.impl.intelliLang.inject.config.BaseInjection;
import consulo.ide.impl.psi.injection.LanguageInjectionSupport;
import consulo.language.inject.ReferenceInjector;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.inject.impl.internal.InjectedReferenceVisitor;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.SmartList;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.Ref;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Dmitry Avdeev
 *         Date: 01.08.13
 */
public class InjectedReferencesContributor extends PsiReferenceContributor {

  private static final Key<PsiReference[]> INJECTED_REFERENCES = Key.create("injected references");

  public static boolean isInjected(@Nullable PsiReference reference) {
    return reference != null && reference.getElement().getUserData(INJECTED_REFERENCES) != null;
  }

  @Nullable
  public static PsiReference[] getInjectedReferences(PsiElement element) {
    element.getReferences();
    return element.getUserData(INJECTED_REFERENCES);
  }

  @Override
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(PlatformPatterns.psiElement(), new PsiReferenceProvider() {
      @Nonnull
      @Override
      public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull final ProcessingContext context) {
        List<ReferenceInjector> extensions = ReferenceInjector.EXTENSION_POINT_NAME.getExtensionList();
        final List<PsiReference> references = new SmartList<PsiReference>();
        Configuration configuration = Configuration.getProjectInstance(element.getProject());
        final Ref<Boolean> injected = new Ref<Boolean>(Boolean.FALSE);
        for (ReferenceInjector injector : extensions) {
          Collection<BaseInjection> injections = configuration.getInjectionsByLanguageId(injector.getId());
          for (BaseInjection injection : injections) {
            if (injection.acceptForReference(element)) {
              injected.set(Boolean.TRUE);
              LanguageInjectionSupport support = InjectorUtils.findInjectionSupport(injection.getSupportId());
              element.putUserData(LanguageInjectionSupport.INJECTOR_SUPPORT, support);
              List<TextRange> area = injection.getInjectedArea(element);
              for (TextRange range : area) {
                references.addAll(Arrays.asList(injector.getReferences(element, context, range)));
              }
            }
          }
        }
        if (element instanceof PsiLanguageInjectionHost) {
          final TemporaryPlacesRegistry registry = TemporaryPlacesRegistry.getInstance(element.getProject());
          InjectedLanguage language = registry.getLanguageFor((PsiLanguageInjectionHost)element, element.getContainingFile());
          if (language != null) {
            ReferenceInjector injector = ReferenceInjector.findById(language.getID());
            if (injector != null) {
              injected.set(Boolean.TRUE);
              element.putUserData(LanguageInjectionSupport.INJECTOR_SUPPORT, registry.getLanguageInjectionSupport());
              TextRange range = ElementManipulators.getValueTextRange(element);
              references.addAll(Arrays.asList(injector.getReferences(element, context, range)));
            }
          }
          else {
            InjectedLanguageUtil.enumerate(element, element.getContainingFile(), false, new InjectedReferenceVisitor() {
              @Override
              public void visitInjectedReference(@Nonnull ReferenceInjector injector, @Nonnull List<? extends PsiLanguageInjectionHost.Shred> places) {
                injected.set(Boolean.TRUE);
                element.putUserData(LanguageInjectionSupport.INJECTOR_SUPPORT, registry.getLanguageInjectionSupport());
                for (PsiLanguageInjectionHost.Shred place : places) {
                  references.addAll(Arrays.asList(injector.getReferences(element, context, place.getRangeInsideHost())));
                }
              }
            });
          }
        }
        PsiReference[] array = references.toArray(new PsiReference[references.size()]);
        element.putUserData(INJECTED_REFERENCES, injected.get() ? array : null);
        return array;
      }
    });
  }
}
