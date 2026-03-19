// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.codeStyle.statusbar;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.ide.impl.idea.application.options.CodeStyleSchemesConfigurable;
import consulo.ide.impl.idea.application.options.codeStyle.OtherFileTypesCodeStyleConfigurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.localize.UILocalize;
import org.jspecify.annotations.Nullable;

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
        return DumbAwareAction.create(
            ApplicationLocalize.codeStyleWidgetConfigureIndents(langName),
            event -> {
                Configurable configurable = findCodeStyleConfigurableId(psiFile.getProject(), langName);
                if (configurable instanceof CodeStyleSchemesConfigurable.CodeStyleConfigurableWrapper configurableWrapper) {
                    ShowSettingsUtil.getInstance().editConfigurable(
                        event.getData(Project.KEY),
                        configurable,
                        () -> configurableWrapper.selectTab(ApplicationLocalize.titleTabsAndIndents())
                    );
                }
            }
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

    private static @Nullable Configurable findCodeStyleConfigurableId(Project project, LocalizeValue langName) {
        CodeStyleSchemesConfigurable topConfigurable = new CodeStyleSchemesConfigurable(project);
        SearchableConfigurable found = topConfigurable.findSubConfigurable(langName);
        return found != null ? found : topConfigurable.findSubConfigurable(OtherFileTypesCodeStyleConfigurable.getDisplayNameText());
    }
}
