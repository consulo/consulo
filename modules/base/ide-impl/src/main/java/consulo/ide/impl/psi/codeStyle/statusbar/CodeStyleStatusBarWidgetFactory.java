// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.codeStyle.statusbar;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.impl.idea.application.options.CodeStyleSchemesConfigurable;
import consulo.ide.impl.idea.application.options.codeStyle.OtherFileTypesCodeStyleConfigurable;
import consulo.language.editor.CommonDataKeys;
import consulo.language.Language;
import consulo.application.ApplicationBundle;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.ui.ex.UIBundle;
import consulo.disposer.Disposer;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionImpl(id = "codeStyleWidget", order = "after selectionModeWidget")
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
                .editConfigurable(event.getData(CommonDataKeys.PROJECT), configurable, () -> ((CodeStyleSchemesConfigurable.CodeStyleConfigurableWrapper)configurable).selectTab(ApplicationBundle.message("title.tabs.and.indents")));
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
