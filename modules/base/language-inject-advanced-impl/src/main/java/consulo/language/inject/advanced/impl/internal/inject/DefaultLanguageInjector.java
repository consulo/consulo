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
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.InjectorUtils;
import consulo.language.inject.advanced.LanguageInjectionSupport;
import consulo.language.inject.advanced.internal.ProjectInjectionConfiguration;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ExtensionImpl(order = "last")
public final class DefaultLanguageInjector implements MultiHostInjector {

  private final ProjectInjectionConfiguration myInjectionConfiguration;
  private final LanguageInjectionSupport[] mySupports;

  @Inject
  public DefaultLanguageInjector(ProjectInjectionConfiguration configuration) {
    myInjectionConfiguration = configuration;
    mySupports = ArrayUtil.toObjectArray(InjectorUtils.getActiveInjectionSupports(), LanguageInjectionSupport.class);
  }

  @Nonnull
  @Override
  public Class<? extends PsiElement> getElementClass() {
    return PsiLanguageInjectionHost.class;
  }

  @Override
  public void injectLanguages(@Nonnull final MultiHostRegistrar registrar, @Nonnull final PsiElement context) {
    if (!(context instanceof PsiLanguageInjectionHost) || !((PsiLanguageInjectionHost)context).isValidHost()) return;
    PsiLanguageInjectionHost host = (PsiLanguageInjectionHost)context;

    for (LanguageInjectionSupport support : mySupports) {
      if (!support.isApplicableTo(host)) continue;
      if (!support.useDefaultInjector(host)) continue;

      for (BaseInjection injection : myInjectionConfiguration.getInjections(support.getId())) {
        if (!injection.acceptsPsiElement(host)) continue;
        if (!InjectorUtils.registerInjectionSimple(host, injection, support, registrar)) continue;
        return;
      }
    }
  }
}