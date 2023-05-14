/*
 * Copyright 2013-2022 consulo.io
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

import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08/12/2022
 */
public abstract class BaseConcatenation2InjectorAdapter implements MultiHostInjector {
  private final Project myProject;

  public BaseConcatenation2InjectorAdapter(Project project) {
    myProject = project;
  }

  @Override
  public final void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement context) {
    InjectedLanguageManager.getInstance(myProject).injectLanguagesFromConcatenationAdapter(registrar, context, this::computeAnchorAndOperands);
  }

  @Nonnull
  protected abstract Pair<PsiElement, PsiElement[]> computeAnchorAndOperands(@Nonnull PsiElement context);
}
