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
package consulo.language.editor.ui.internal;

import consulo.application.Application;
import consulo.language.editor.ui.PsiElementListNavigator;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.popup.JBPopup;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 29-Apr-22
 */
public interface LanguageEditorPopupFactory {
  static LanguageEditorPopupFactory getInstance() {
    return Application.get().getInstance(LanguageEditorPopupFactory.class);
  }

  @Nonnull
  PsiElementListNavigator.NavigateOrPopupBuilder builder(@Nonnull NavigatablePsiElement[] targets, String title);

  @Nonnull
  JBPopup getPsiElementPopup(final Object[] elements, final Map<PsiElement, GotoRelatedItem> itemsMap, final String title, final boolean showContainingModules, final Predicate<Object> processor);
}
