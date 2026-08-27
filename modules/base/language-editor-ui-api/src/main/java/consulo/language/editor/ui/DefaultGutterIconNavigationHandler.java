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
package consulo.language.editor.ui;

import consulo.annotation.UsedInPlugin;
import consulo.application.Application;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.ui.navigation.PsiTargetNavigationService;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author yole
 */
@UsedInPlugin
public class DefaultGutterIconNavigationHandler<T extends PsiElement> implements GutterIconNavigationHandler<T> {
  private final Collection<? extends NavigatablePsiElement> myReferences;
  private final String myTitle;

  public DefaultGutterIconNavigationHandler(Collection<? extends NavigatablePsiElement> references, String title) {
    myReferences = references;
    myTitle = title;
  }

  public Collection<? extends NavigatablePsiElement> getReferences() {
    return myReferences;
  }

  @RequiredUIAccess
  @Override
  public void navigate(ComponentEvent<?> e, T elt) {
    Application.get().getInstance(PsiTargetNavigationService.class)
      .newNavigator(() -> new ArrayList<NavigatablePsiElement>(myReferences))
      .title(LocalizeValue.of(myTitle))
      .navigate(e, elt.getProject());
  }
}
