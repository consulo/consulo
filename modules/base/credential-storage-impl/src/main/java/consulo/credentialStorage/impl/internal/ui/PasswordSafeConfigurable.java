package consulo.credentialStorage.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.configurable.StandardConfigurableIds;
import consulo.credentialStorage.impl.internal.PasswordSafeSettings;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

@ExtensionImpl
public class PasswordSafeConfigurable implements SearchableConfigurable, ProjectConfigurable {

    private final PasswordSafeSettings settings;

    private @Nullable PasswordSafeConfigurableUi ui;

    @Inject
    public PasswordSafeConfigurable(PasswordSafeSettings settings) {
        this.settings = settings;
    }

    @Override
    public String getId() {
        return "application.passwordSafe";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return CredentialStorageLocalize.passwordSafeConfigurable();
    }

    @Override
    public @Nullable String getHelpTopic() {
        return "reference.ide.settings.password.safe";
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        if (ui == null) {
            ui = createUi();
        }
        return ui.createUIComponent(parentDisposable);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return ui != null && ui.isModified(settings);
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (ui != null) {
            ui.apply(settings);
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        if (ui != null) {
            ui.reset(settings);
        }
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        ui = null;
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    private PasswordSafeConfigurableUi createUi() {
        return new PasswordSafeConfigurableUi(settings);
    }
}
