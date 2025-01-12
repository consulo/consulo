/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.inject.advanced.impl.internal.inject;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.inject.advanced.InjectedLanguage;
import consulo.language.inject.advanced.InjectorUtils;
import consulo.language.inject.advanced.TemporaryPlacesRegistry;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
@ExtensionImpl
public class TemporaryPlacesInjector implements MultiHostInjector {
  private static final Logger LOG = Logger.getInstance(TemporaryPlacesInjector.class);

  private final TemporaryPlacesRegistry myRegistry;

  @Inject
  public TemporaryPlacesInjector(TemporaryPlacesRegistry registry) {
    myRegistry = registry;
  }

  @Nonnull
  @Override
  public Class<? extends PsiElement> getElementClass() {
    return PsiLanguageInjectionHost.class;
  }

  @Override
  public void injectLanguages(@Nonnull final MultiHostRegistrar registrar, @Nonnull final PsiElement context) {
    if (!(context instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost)context).isValidHost()) {
      return;
    }
    PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;

    PsiFile containingFile = context.getContainingFile();
    InjectedLanguage injectedLanguage = myRegistry.getLanguageFor(host, containingFile);
    Language language = injectedLanguage != null ? injectedLanguage.getLanguage() : null;
    if (language == null) {
      return;
    }

    final ElementManipulator<PsiLanguageInjectionHost> manipulator = ElementManipulators.getManipulator(host);
    if (manipulator == null) {
      LOG.error("Registered inject language for host: " +  host.getClass().getSimpleName() + ", but ElementManipulator not registered");
      return;
    }
    List<Trinity<PsiLanguageInjectionHost, InjectedLanguage, TextRange>> trinities = Collections.singletonList(Trinity.create(host, injectedLanguage, manipulator.getRangeInElement(host)));
    InjectorUtils.registerInjection(language, trinities, containingFile, registrar);
    InjectorUtils.registerSupport(myRegistry.getLanguageInjectionSupport(), false, context, language);
  }
}
