// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.function.Predicate;

@ExtensionImpl
public class InlaySettingsConfigurable implements Configurable, SearchableConfigurable, Configurable.NoScroll, ProjectConfigurable {
    public static final String INLAY_ID = "inlay.hints";
    private final Project project;
    private InlaySettingsPanel panel;

    @Inject
    public InlaySettingsConfigurable(Project project) {
        this.project = project;
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(@Nonnull Disposable disposable) {
        if (panel == null) {
            panel = new InlaySettingsPanel(project);
        }
        return panel;
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return panel.getTree();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return panel.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() {
        panel.apply();
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        panel.reset();
    }

    @Override
    public Runnable enableSearch(String option) {
        return panel.enableSearch(option);
    }

    public void selectModel(Language language, Predicate<InlayProviderSettingsModel> selector) {
        panel.selectModel(language, selector);
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return LanguageEditorLocalize.settingsInlayHintsPanelName().get();
    }

    @Nonnull
    @Override
    public String getId() {
        return INLAY_ID;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Override
    public String getHelpTopic() {
        return "settings.inlays";
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        panel = null;
    }

    //    public static boolean showInlaySettings(Project project, Language language, Predicate<InlayProviderSettingsModel> selector) {
//        ShowSettingsUtil.getInstance().showSettingsDialog(project, InlaySettingsConfigurable.class, configurable -> {
//            if (selector != null) {
//                ((InlaySettingsConfigurable) configurable).selectModel(language, selector);
//            }
//        });
//        return true;
//    }
}
