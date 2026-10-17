// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.impl.internal.statusBar;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.language.Language;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.codeStyle.ui.internal.CodeStyleSettingsNavigator;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.localize.UILocalize;

@ExtensionImpl(id = "codeStyleWidget", order = "after selectionModeWidget")
public class CodeStyleStatusBarWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    
    @Override
    public StatusBarWidget createWidget(Project project) {
        return new CodeStyleStatusBarWidget(project, this);
    }

    
    @Override
    public String getDisplayName() {
        return UILocalize.statusBarCodeStyleWidgetName().get();
    }

    
    @RequiredReadAction
    public static DumbAwareAction createDefaultIndentConfigureAction(PsiFile psiFile) {
        LocalizeValue langName = getLangName(psiFile);
        Project project = psiFile.getProject();
        return DumbAwareAction.create(
            ApplicationLocalize.codeStyleWidgetConfigureIndents(langName),
            event -> project.getApplication().getInstance(CodeStyleSettingsNavigator.class).showIndentSettings(project, langName)
        );
    }

    @RequiredReadAction
    private static LocalizeValue getLangName(PsiFile psiFile) {
        Language language = psiFile.getLanguage();
        LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.findUsingBaseLanguage(language);
        if (provider != null && provider.getIndentOptionsEditor() != null) {
            LocalizeValue name = provider.getConfigurableDisplayName();
            if (name.isNotEmpty()) {
                return name;
            }
        }
        return language.getDisplayName();
    }
}
