// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPass;
import consulo.ide.impl.idea.codeInsight.hints.InlayProviderPassInfo;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.DeclarativeInlayHintsSettings;
import consulo.language.editor.impl.internal.inlay.setting.ImmediateConfigurable;
import consulo.language.editor.impl.internal.inlay.setting.InlayDumpUtil;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.editor.inlay.DeclarativeInlayHintsCustomSettingsProvider;
import consulo.language.editor.inlay.DeclarativeInlayOptionInfo;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeclarativeHintsProviderSettingsModel extends InlayProviderSettingsModel {
    private static final Key<PreviewEntries> PREVIEW_ENTRIES = Key.create("declarative.inlays.preview.entries");

    private final DeclarativeInlayHintsProvider providerDescription;
    private final Project project;
    private final DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
    @SuppressWarnings("unchecked")
    private DeclarativeInlayHintsCustomSettingsProvider<Object> customSettingsProvider;
    private Object savedSettings;
    private final List<MutableOption> options;
    private final List<ImmediateConfigurable.Case> cases;

    @SuppressWarnings("unchecked")
    public DeclarativeHintsProviderSettingsModel(DeclarativeInlayHintsProvider providerDescription,
                                                 boolean isEnabled,
                                                 Language language,
                                                 Project project) {
        super(isEnabled, providerDescription.getId(), language);
        this.providerDescription = providerDescription;
        this.project = project;
        this.customSettingsProvider = (DeclarativeInlayHintsCustomSettingsProvider<Object>) DeclarativeInlayHintsCustomSettingsProvider.getCustomSettingsProvider(getId(), language);
        if (customSettingsProvider == null) {
            customSettingsProvider = new DefaultSettingsProvider();
        }

        this.savedSettings = customSettingsProvider.getSettingsCopy();
        this.options = loadOptionsFromSettings();
        this.cases = options.stream()
            .map(option -> new ImmediateConfigurable.Case(
                option.description.name(),
                option.description.id(),
                () -> option.isEnabled,
                newValue -> option.isEnabled = newValue,
                option.description.description()))
            .collect(Collectors.toList());
    }

    private List<MutableOption> loadOptionsFromSettings() {
        return providerDescription.getOptions().stream()
            .map(opt -> {
                boolean byDefault = opt.isEnabledByDefault();
                Boolean saved = settings.isOptionEnabled(opt.id(), providerDescription.getId());
                boolean enabled = saved != null ? saved : byDefault;
                return new MutableOption(opt, enabled);
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<ImmediateConfigurable.Case> getCases() {
        return cases;
    }

    @Override
    public InlayGroup getGroup() {
        return providerDescription.getGroup();
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return providerDescription.getName();
    }

    @Override
    public JComponent getComponent() {
        return customSettingsProvider.createComponent(project, getLanguage());
    }

    @Override
    public String getDescription() {
        return providerDescription.getDescription().get();
    }

    @Override
    public String getPreviewText() {
        String preview = providerDescription.getPreviewFileText().get();
        return InlayDumpUtil.removeInlays(preview);
    }

    @Override
    public PsiFile createFile(Project project, FileType fileType, Document document) {
        PsiFile file = super.createFile(project, fileType, document);
        String preview = providerDescription.getPreviewFileText().get();
        PreviewEntries entries = new PreviewEntries(null,
            DeclarativeHintsDumpUtil.extractHints(preview));
        file.putUserData(PREVIEW_ENTRIES, entries);
        return file;
    }

    @Override
    public Runnable collectData(final Editor editor, final PsiFile file) {
        String providerId = providerDescription.getId();
        Map<String, Boolean> enabledOptions = options.stream()
            .collect(Collectors.toMap(opt -> opt.description.id(), opt -> true));
        PreviewEntries previewEntries = file.getUserData(PREVIEW_ENTRIES);
        String caseId = previewEntries != null ? previewEntries.caseId : null;
        boolean enabled = caseId != null ?
            options.stream().anyMatch(opt -> opt.description.id().equals(caseId) && opt.isEnabled)
            : isEnabled();
        DeclarativeInlayHintsPass pass = new DeclarativeInlayHintsPass(file, editor,
            List.of(new InlayProviderPassInfo(providerDescription, providerId, enabledOptions)), false, !enabled);
        pass.doCollectInformation(new EmptyProgressIndicator());
        return pass::doApplyInformationToEditor;
    }

    @Override
    public String getCasePreview(ImmediateConfigurable.Case optionCase) {
        if (optionCase == null) return getPreviewText();
        String preview = providerDescription.getPreviewFileText().get();
        return InlayDumpUtil.removeInlays(preview);
    }

    @Override
    public Language getCasePreviewLanguage(ImmediateConfigurable.Case optionCase) {
        return getLanguage();
    }

    @Override
    public String getCaseDescription(ImmediateConfigurable.Case optionCase) {
        return providerDescription.getOptions().stream()
            .filter(o -> o.id().equals(optionCase.getId()))
            .findFirst()
            .map(o -> o.description().get())
            .orElse(null);
    }

    @Override
    public void apply() {
        for (MutableOption option : options) {
            settings.setOptionEnabled(option.description.id(), getId(), option.isEnabled);
        }
        settings.setProviderEnabled(getId(), isEnabled());
        Object newSettings = customSettingsProvider.getSettingsCopy();
        customSettingsProvider.persistSettings(project, newSettings, getLanguage());
        savedSettings = newSettings;
    }

    private boolean isProviderEnabledInSettings() {
        Boolean enabled = settings.isProviderEnabled(providerDescription.getId());
        return enabled != null ? enabled : providerDescription.isEnabledByDefault();
    }

    @Override
    public boolean isModified() {
        if (isEnabled() != isProviderEnabledInSettings()) return true;
        if (customSettingsProvider.isDifferentFrom(project, savedSettings)) return true;
        for (MutableOption option : options) {
            Boolean saved = settings.isOptionEnabled(option.description.id(), getId());
            boolean inSettings = saved != null ? saved : option.description.isEnabledByDefault();
            if (option.isEnabled != inSettings) return true;
        }
        return false;
    }

    @Override
    public void reset() {
        for (MutableOption option : options) {
            Boolean saved = settings.isOptionEnabled(option.description.id(), getId());
            option.isEnabled = saved != null ? saved : option.description.isEnabledByDefault();
        }
        settings.setProviderEnabled(providerDescription.getId(), isProviderEnabledInSettings());
        customSettingsProvider.persistSettings(project, savedSettings, getLanguage());
    }

    private static class MutableOption {
        final DeclarativeInlayOptionInfo description;
        boolean isEnabled;

        MutableOption(DeclarativeInlayOptionInfo description, boolean isEnabled) {
            this.description = description;
            this.isEnabled = isEnabled;
        }
    }

    private static class PreviewEntries {
        final String caseId;
        final List<DeclarativeHintsDumpUtil.ExtractedHintInfo> hintInfos;

        PreviewEntries(String caseId, List<DeclarativeHintsDumpUtil.ExtractedHintInfo> hintInfos) {
            this.caseId = caseId;
            this.hintInfos = hintInfos;
        }
    }

    private static class DefaultSettingsProvider implements DeclarativeInlayHintsCustomSettingsProvider<Object> {
        private final JPanel component = new JPanel();

        @Override
        public String getProviderId() {
            return null;
        }

        @Override
        public JComponent createComponent(Project project, Language language) {
            return component;
        }

        @Override
        public Object getSettingsCopy() {
            return null;
        }

        @Override
        public void persistSettings(Project project, Object settings, Language language) {
        }

        @Override
        public void putSettings(Project project, Object settings, Language language) {
        }

        @Override
        public boolean isDifferentFrom(Project project, Object settings) {
            return false;
        }

        @Nonnull
        @Override
        public Language getLanguage() {
            return null;
        }
    }
}
