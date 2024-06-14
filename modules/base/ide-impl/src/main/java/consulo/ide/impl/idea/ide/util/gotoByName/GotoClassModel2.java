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

import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.GotoClassOrTypeContributor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.navigation.NavigationItem;
import consulo.platform.Platform;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GotoClassModel2 extends FilteringGotoByModel<Language> {
  private String[] mySeparators;

  public GotoClassModel2(@Nonnull Project project) {
    super(project, project.getApplication().getExtensionList(GotoClassOrTypeContributor.class));
  }

  @Override
  protected Language filterValueFor(NavigationItem item) {
    return item instanceof PsiElement ? ((PsiElement) item).getLanguage() : null;
  }

  @Override
  protected synchronized Collection<Language> getFilterItems() {
    final Collection<Language> result = super.getFilterItems();
    if (result == null) {
      return null;
    }
    final Collection<Language> items = new HashSet<>(result);
    items.add(Language.ANY);
    return items;
  }

  @Override
  @Nullable
  public String getPromptText() {
    return IdeLocalize.promptGotoclassEnterClassName().get();
  }

  @Override
  public String getCheckBoxName() {
    return IdeLocalize.checkboxIncludeNonProjectClasses().get();
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
  public char getCheckBoxMnemonic() {
    // Some combination like Alt+N, Ant+O, etc are a dead symbols, therefore
    // we have to change mnemonics for Mac users.
    return Platform.current().os().isMac() ? 'P' : 'n';
  }

  @Override
  public boolean loadInitialCheckBoxState() {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    return Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries")) &&
           Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.includeLibraries"));
  }

  @Override
  public void saveInitialCheckBoxState(boolean state) {
    PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    if (Boolean.TRUE.toString().equals(propertiesComponent.getValue("GoToClass.toSaveIncludeLibraries"))){
      propertiesComponent.setValue("GoToClass.includeLibraries", Boolean.toString(state));
    }
  }

  @Override
  public String getFullName(final Object element) {
    if (element instanceof PsiElement && !((PsiElement)element).isValid()) {
      return null;
    }

    for (ChooseByNameContributor c : getContributorList()) {
      if (c instanceof GotoClassOrTypeContributor) {
        String result = ((GotoClassOrTypeContributor)c).getQualifiedName((NavigationItem)element);
        if (result != null) return result;
      }
    }

    return getElementName(element);
  }

  @Override
  @Nonnull
  public String[] getSeparators() {
    if (mySeparators == null) {
      mySeparators = getSeparatorsFromContributors(getContributorList());
    }
    return mySeparators;
  }

  public static String[] getSeparatorsFromContributors(List<? extends ChooseByNameContributor> contributors) {
    final Set<String> separators = new HashSet<>();
    separators.add(".");
    for(ChooseByNameContributor c: contributors) {
      if (c instanceof GotoClassOrTypeContributor) {
        ContainerUtil.addIfNotNull(separators, ((GotoClassOrTypeContributor)c).getQualifiedNameSeparator());
      }
    }
    return separators.toArray(new String[separators.size()]);
  }

  @Override
  public String getHelpId() {
    return "procedures.navigating.goto.class";
  }

  @Nonnull
  @Override
  public String removeModelSpecificMarkup(@Nonnull String pattern) {
    if (pattern.startsWith("@")) return pattern.substring(1);
    return pattern;
  }

  @Override
  public boolean willOpenEditor() {
    return true;
  }

  @Override
  public boolean sameNamesForProjectAndLibraries() {
    return !FileBasedIndex.ourEnableTracingOfKeyHashToVirtualFileMapping;
  }
}
