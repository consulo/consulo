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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.GotoClassOrTypeContributor;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.NavigationItem;
import consulo.platform.Platform;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;

public class GotoSymbolModel2 extends FilteringGotoByModel<Language> {
  private String[] mySeparators;

  public GotoSymbolModel2(@Nonnull Project project) {
    super(project, project.getApplication().getExtensionPoint(GotoSymbolContributor.class).getExtensionList());
  }

  @Override
  protected Language filterValueFor(NavigationItem item) {
    return item instanceof PsiElement ? ((PsiElement) item).getLanguage() : null;
  }

  @Nullable
  @Override
  protected synchronized Collection<Language> getFilterItems() {
    Collection<Language> result = super.getFilterItems();
    if (result == null) {
      return result;
    }
    Collection<Language> items = new HashSet<>(result);
    items.add(Language.ANY);
    return items;
  }

  @Override
  public String getPromptText() {
    return IdeLocalize.promptGotosymbolEnterSymbolName().get();
  }

  @Override
  public LocalizeValue getCheckBoxName() {
    return IdeLocalize.checkboxIncludeNonProjectSymbols();
  }

  @Override
  public String getNotInMessage() {
    return IdeLocalize.labelNoMatchesFoundInProject().get();
  }

  @Override
  public String getNotFoundMessage() {
    return IdeLocalize.labelNoMatchesFound().get();
  }

  @Override
  public boolean loadInitialCheckBoxState() {
    return false;
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
  }

  @Override
  public String getFullName(Object element) {
    for(ChooseByNameContributor c: getContributorList()) {
      if (c instanceof GotoClassOrTypeContributor) {
        String result = ((GotoClassOrTypeContributor) c).getQualifiedName((NavigationItem) element);
        if (result != null) return result;
      }
    }

    if (element instanceof PsiElement) {
      PsiElement psiElement = (PsiElement)element;

      String containerText = SymbolPresentationUtil.getSymbolContainerText(psiElement);
      return containerText + "." + getElementName(element);
    }

    return getElementName(element);
  }

  @Override
  @Nonnull
  public String[] getSeparators() {
    if (mySeparators == null) {
      mySeparators = GotoClassModel2.getSeparatorsFromContributors(getContributorList());
    }
    return mySeparators;
  }

  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.class";
  }

  @Override
  public boolean willOpenEditor() {
    return true;
  }
}
