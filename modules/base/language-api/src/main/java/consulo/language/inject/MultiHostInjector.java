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
package consulo.language.inject;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;

import jakarta.annotation.Nonnull;

/**
 * @see PsiLanguageInjectionHost
 *
 * @author cdr
 * @since 2007-09-10
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface MultiHostInjector {
  @Nonnull
  Class<? extends PsiElement> getElementClass();

  void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement context);
}