package consulo.credentialStorage.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.IdeaConfigurableBase;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.credentialStorage.impl.internal.PasswordSafeSettings;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@ExtensionImpl
public class PasswordSafeConfigurable extends IdeaConfigurableBase<PasswordSafeConfigurableUi, PasswordSafeSettings> implements ProjectConfigurable {

    private final PasswordSafeSettings settings;

    @Inject
    public PasswordSafeConfigurable(PasswordSafeSettings settings) {
        super("application.passwordSafe",
            CredentialStorageLocalize.passwordSafeConfigurable().get(),
            "reference.ide.settings.password.safe"
        );
        this.settings = settings;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @Nonnull
    @Override
    public PasswordSafeSettings getSettings() {
        return settings;
    }

    @Override
    public PasswordSafeConfigurableUi createUi() {
        return new PasswordSafeConfigurableUi(settings);
    }
}
