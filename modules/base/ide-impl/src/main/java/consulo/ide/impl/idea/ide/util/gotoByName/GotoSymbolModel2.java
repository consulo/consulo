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

import consulo.ide.IdeBundle;
import consulo.ide.navigation.GotoSymbolContributor;
import consulo.language.Language;
import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.GotoClassOrTypeContributor;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.application.util.SystemInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.SymbolPresentationUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    final Collection<Language> result = super.getFilterItems();
    if (result == null) {
      return result;
    }
    final Collection<Language> items = new HashSet<Language>(result);
    items.add(Language.ANY);
    return items;
  }

  @Override
  public String getPromptText() {
    return IdeBundle.message("prompt.gotosymbol.enter.symbol.name");
  }

  @Override
  public String getCheckBoxName() {
    return IdeBundle.message("checkbox.include.non.project.symbols");
  }

  @Override
  public String getNotInMessage() {
    return IdeBundle.message("label.no.matches.found.in.project");
  }

  @Override
  public String getNotFoundMessage() {
    return IdeBundle.message("label.no.matches.found");
  }

  @Override
  public char getCheckBoxMnemonic() {
    // Some combination like Alt+N, Ant+O, etc are a dead sysmbols, therefore
    // we have to change mnemonics for Mac users.
    return SystemInfo.isMac?'P':'n';
  }

  @Override
  public boolean loadInitialCheckBoxState() {
    return false;
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
  }

  @Override
  public String getFullName(final Object element) {
    for(ChooseByNameContributor c: getContributorList()) {
      if (c instanceof GotoClassOrTypeContributor) {
        String result = ((GotoClassOrTypeContributor) c).getQualifiedName((NavigationItem) element);
        if (result != null) return result;
      }
    }

    if (element instanceof PsiElement) {
      final PsiElement psiElement = (PsiElement)element;

      final String containerText = SymbolPresentationUtil.getSymbolContainerText(psiElement);
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
