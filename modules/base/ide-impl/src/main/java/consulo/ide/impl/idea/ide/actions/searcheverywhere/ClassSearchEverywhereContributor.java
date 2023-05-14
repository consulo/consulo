// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.actions.GotoClassAction;
import consulo.ide.impl.idea.ide.actions.GotoClassPresentationUpdater;
import consulo.ide.impl.idea.ide.util.gotoByName.FilteringGotoByModel;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoClassModel2;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoClassSymbolConfiguration;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.ui.IdeUICustomization;
import consulo.language.DependentLanguage;
import consulo.language.Language;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.LanguageUtil;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Konstantin Bulenkov
 */
public class ClassSearchEverywhereContributor extends AbstractGotoSEContributor {

  private static final Pattern ourPatternToDetectAnonymousClasses = Pattern.compile("([.\\w]+)((\\$[\\d]+)*(\\$)?)");
  private static final Pattern ourPatternToDetectMembers = Pattern.compile("(.+)(#)(.*)");

  private final PersistentSearchEverywhereContributorFilter<Language> myFilter;

  public ClassSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
    super(project, context);
    myFilter = project == null ? null : createLanguageFilter(project);
  }

  @Nonnull
  @Override
  public String getGroupName() {
    return GotoClassPresentationUpdater.getTabTitle(true);
  }

  @Nonnull
  @Override
  public String getFullGroupName() {
    String[] split = GotoClassPresentationUpdater.getActionTitle().split("/");
    return Arrays.stream(split).map(StringUtil::pluralize).collect(Collectors.joining("/"));
  }

  @Nonnull
  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.include.non.project.classes", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @Override
  public int getSortWeight() {
    return 100;
  }

  @Nonnull
  @Override
  protected FilteringGotoByModel<Language> createModel(@Nonnull Project project) {
    GotoClassModel2 model = new GotoClassModel2(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @Nonnull
  @Override
  public List<AnAction> getActions(@Nonnull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  @Nonnull
  @Override
  public String filterControlSymbols(@Nonnull String pattern) {
    if (pattern.indexOf('#') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectMembers);
    }

    if (pattern.indexOf('$') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectAnonymousClasses);
    }

    return super.filterControlSymbols(pattern);
  }

  @Override
  public int getElementPriority(@Nonnull Object element, @Nonnull String searchPattern) {
    return super.getElementPriority(element, searchPattern) + 5;
  }

  @Override
  protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
    String path = pathToAnonymousClass(searchText);
    if (path != null) {
      psiElement = GotoClassAction.getElement(psiElement, path);
    }
    return super.preparePsi(psiElement, modifiers, searchText);
  }

  @Nullable
  @Override
  protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
    Navigatable res = super.createExtendedNavigatable(psi, searchText, modifiers);
    if (res != null) {
      return res;
    }

    VirtualFile file = PsiUtilCore.getVirtualFile(psi);
    String memberName = getMemberName(searchText);
    if (file != null && memberName != null) {
      Navigatable delegate = GotoClassAction.findMember(memberName, searchText, psi, file);
      if (delegate != null) {
        return new Navigatable() {
          @Override
          public void navigate(boolean requestFocus) {
            PopupNavigationUtil.activateFileWithPsiElement(psi, openInCurrentWindow(modifiers));
            delegate.navigate(true);

          }

          @Override
          public boolean canNavigate() {
            return delegate.canNavigate();
          }

          @Override
          public boolean canNavigateToSource() {
            return delegate.canNavigateToSource();
          }
        };
      }
    }

    return null;
  }

  private static String pathToAnonymousClass(String searchedText) {
    return pathToAnonymousClass(ourPatternToDetectAnonymousClasses.matcher(searchedText));
  }

  @Nullable
  public static String pathToAnonymousClass(Matcher matcher) {
    if (matcher.matches()) {
      String path = matcher.group(2);
      if (path != null) {
        path = path.trim();
        if (path.endsWith("$") && path.length() >= 2) {
          path = path.substring(0, path.length() - 2);
        }
        if (!path.isEmpty()) return path;
      }
    }

    return null;
  }

  private static String getMemberName(String searchedText) {
    final int index = searchedText.lastIndexOf('#');
    if (index == -1) {
      return null;
    }

    String name = searchedText.substring(index + 1).trim();
    return StringUtil.isEmpty(name) ? null : name;
  }

  @Nonnull
  static PersistentSearchEverywhereContributorFilter<Language> createLanguageFilter(@Nonnull Project project) {
    List<Language> items = Language.getRegisteredLanguages().stream().filter(lang -> lang != Language.ANY && !(lang instanceof DependentLanguage)).sorted(LanguageUtil.LANGUAGE_COMPARATOR)
            .collect(Collectors.toList());
    GotoClassSymbolConfiguration persistentConfig = GotoClassSymbolConfiguration.getInstance(project);
    return new PersistentSearchEverywhereContributorFilter<>(items, persistentConfig, Language::getDisplayName, language -> {
      final LanguageFileType fileType = language.getAssociatedFileType();
      return fileType != null ? fileType.getIcon() : null;
    });
  }
}
