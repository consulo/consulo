package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.ide.impl.idea.codeInsight.hints.ParameterHintsPassFactory;
import consulo.language.Language;
import consulo.language.editor.impl.internal.inlay.setting.ImmediateConfigurable;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.inlay.Option;
import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.localize.LocalizeValue;

import java.util.List;
import java.util.stream.Collectors;

public class ParameterInlayProviderSettingsModel extends InlayProviderSettingsModel {
    public static final String ID = "parameter.hints.old";

    private final InlayParameterHintsProvider provider;
    private final ParameterHintsSettingsPanel panel;
    private final List<ImmediateConfigurable.Case> cases;
    private final List<OptionState> optionStates;

    public ParameterInlayProviderSettingsModel(InlayParameterHintsProvider provider,
                                               Language language) {
        super(ParameterNameHintsSettings.getInstance().isEnabledForLanguage(language), ID, language);
        this.provider = provider;
        this.panel = new ParameterHintsSettingsPanel(language, provider.isBlackListSupported());
        // Initialize option states
        this.optionStates = provider.getSupportedOptions().stream()
            .map(option -> new OptionState(option, option.get()))
            .collect(Collectors.toList());
        // Create cases
        this.cases = provider.getSupportedOptions().stream().map(option -> {
            OptionState state = optionStates.stream()
                .filter(s -> s.option.equals(option))
                .findFirst().orElseThrow();
            return new ImmediateConfigurable.Case(
                option.getName(),
                option.getId(),
                () -> state.state,
                newValue -> state.state = newValue,
                option.getExtendedDescription() != LocalizeValue.of() ? option.getExtendedDescription().get() : null
            );
        }).collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return LanguageEditorLocalize.settingsInlayParameterHintsPanelName().get();
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.PARAMETERS_GROUP;
    }

    @Override
    public String getPreviewText() {
        return null;
    }

    @Override
    public String getCasePreview(ImmediateConfigurable.Case caseInfo) {
        return provider.getPreviewFileText().get();
    }

    @Override
    public Language getCasePreviewLanguage(ImmediateConfigurable.Case caseInfo) {
        return getLanguage();
    }

    @Override
    public String getCaseDescription(ImmediateConfigurable.Case caseInfo) {
        return provider.getProperty("inlay.parameters." + caseInfo.getId());
    }

    @Override
    public ParameterHintsSettingsPanel getComponent() {
        return panel;
    }

    @Override
    public String getDescription() {
        return provider.getDescription().get();
    }

    @Override
    public void apply() {
        ParameterNameHintsSettings.getInstance().setEnabledForLanguage(isEnabled(), getLanguage());
        for (OptionState state : optionStates) {
            state.apply();
        }
        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
    }

    @Override
    public boolean isModified() {
        if (isEnabled() != ParameterNameHintsSettings.getInstance().isEnabledForLanguage(getLanguage())) {
            return true;
        }
        return optionStates.stream().anyMatch(OptionState::isModified);
    }

    @Override
    public void reset() {
        setEnabled(ParameterNameHintsSettings.getInstance().isEnabledForLanguage(getLanguage()));
        optionStates.forEach(OptionState::reset);
    }

    @Override
    public List<ImmediateConfigurable.Case> getCases() {
        return cases;
    }

    private static class OptionState {
        final Option option;
        boolean state;

        OptionState(Option option, boolean initial) {
            this.option = option;
            this.state = initial;
        }

        boolean isModified() {
            return state != option.get();
        }

        void reset() {
            state = option.get();
        }

        void apply() {
            option.set(state);
        }
    }
}
