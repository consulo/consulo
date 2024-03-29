// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.language.Language;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class BreadcrumbsUtilEx {
  @Nullable
  static FileViewProvider findViewProvider(final VirtualFile file, final Project project) {
    if (file == null || file.isDirectory()) return null;
    return PsiManager.getInstance(project).findViewProvider(file);
  }

  @Nullable
  static BreadcrumbsProvider findProvider(VirtualFile file, @Nullable Project project, @Nullable Boolean forcedShown) {
    return project == null ? null : findProvider(findViewProvider(file, project), forcedShown);
  }

  @Nullable
  public static BreadcrumbsProvider findProvider(@Nullable FileViewProvider viewProvider, @Nullable Boolean forceShown) {
    if (viewProvider == null) return null;

    if (forceShown == null) {
      return findProvider(true, viewProvider);
    }
    return forceShown ? findProvider(false, viewProvider) : null;
  }

  @Nullable
  public static BreadcrumbsProvider findProvider(boolean checkSettings, @Nonnull FileViewProvider viewProvider) {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    if (checkSettings && !settings.isBreadcrumbsShown()) return null;

    Language baseLang = viewProvider.getBaseLanguage();
    if (checkSettings && !isBreadcrumbsShownFor(baseLang)) return null;

    BreadcrumbsProvider provider = BreadcrumbsUtil.getInfoProvider(baseLang);
    if (provider == null) {
      for (Language language : viewProvider.getLanguages()) {
        if (!checkSettings || isBreadcrumbsShownFor(language)) {
          provider = BreadcrumbsUtil.getInfoProvider(language);
          if (provider != null) break;
        }
      }
    }
    return provider;
  }

  public static boolean isBreadcrumbsShownFor(Language language) {
    String id = findLanguageWithBreadcrumbSettings(language);
    return EditorSettingsExternalizable.getInstance().isBreadcrumbsShownFor(id);
  }

  public static String findLanguageWithBreadcrumbSettings(Language language) {
    EditorSettingsExternalizable settings = EditorSettingsExternalizable.getInstance();
    Language base = language;
    while (base != null) {
      if (settings.hasBreadcrumbSettings(base.getID())) {
        return base.getID();
      }
      base = base.getBaseLanguage();
    }
    return language.getID();
  }
}
