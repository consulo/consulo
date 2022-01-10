// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.statusbar;

import com.intellij.application.options.CodeStyleSchemesConfigurable;
import com.intellij.application.options.codeStyle.OtherFileTypesCodeStyleConfigurable;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.ui.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CodeStyleStatusBarWidgetFactory extends StatusBarEditorBasedWidgetFactory {
  @Override
  public
  @Nonnull
  String getId() {
    return CodeStyleStatusBarWidget.WIDGET_ID;
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return new CodeStyleStatusBarWidget(project);
  }

  @Override
  public
  @Nls
  @Nonnull
  String getDisplayName() {
    return UIBundle.message("status.bar.code.style.widget.name");
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    Disposer.dispose(widget);
  }

  @Nonnull
  public static DumbAwareAction createDefaultIndentConfigureAction(@Nonnull PsiFile psiFile) {
    String langName = getLangName(psiFile);
    return DumbAwareAction.create(ApplicationBundle.message("code.style.widget.configure.indents", langName), event -> {
      Configurable configurable = findCodeStyleConfigurableId(psiFile.getProject(), langName);
      if (configurable instanceof CodeStyleSchemesConfigurable.CodeStyleConfigurableWrapper) {
        ShowSettingsUtil.getInstance()
                .editConfigurable(event.getProject(), configurable, () -> ((CodeStyleSchemesConfigurable.CodeStyleConfigurableWrapper)configurable).selectTab(ApplicationBundle.message("title.tabs.and.indents")));
      }
    });
  }

  @Nonnull
  private static String getLangName(@Nonnull PsiFile psiFile) {
    final Language language = psiFile.getLanguage();
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.findUsingBaseLanguage(language);
    if (provider != null && provider.getIndentOptionsEditor() != null) {
      String name = provider.getConfigurableDisplayName();
      if (name != null) {
        return name;
      }
    }
    return language.getDisplayName();
  }

  @Nullable
  private static Configurable findCodeStyleConfigurableId(@Nonnull Project project, @Nonnull String langName) {
    CodeStyleSchemesConfigurable topConfigurable = new CodeStyleSchemesConfigurable(project);
    SearchableConfigurable found = topConfigurable.findSubConfigurable(langName);
    return found != null ? found : topConfigurable.findSubConfigurable(OtherFileTypesCodeStyleConfigurable.getDisplayNameText());
  }
}
