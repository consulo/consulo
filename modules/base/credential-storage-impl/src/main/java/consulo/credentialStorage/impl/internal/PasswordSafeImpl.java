package consulo.credentialStorage.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.persist.SettingsSavingComponent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ServiceImpl
@Singleton
public class PasswordSafeImpl extends BasePasswordSafe implements SettingsSavingComponent {

    @Inject
    public PasswordSafeImpl(ApplicationConcurrency applicationConcurrency) {
        super(applicationConcurrency);
    }

    @Override
    protected PasswordSafeSettings getSettings() {
        return ApplicationManager.getApplication().getInstance(PasswordSafeSettings.class);
    }
}
