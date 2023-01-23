package consulo.credentialStorage.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SimpleConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.impl.internal.PasswordSafeImpl;
import consulo.credentialStorage.impl.internal.PasswordSafeSettings;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A configurable for password safe
 */
@ExtensionImpl
public class PasswordSafeConfigurable extends SimpleConfigurable<PasswordSafeConfigurable.Root> implements ApplicationConfigurable {
  public static class Root implements Supplier<Component> {
    private RadioButton myDoNotRememberPasswordsRadioButton;
    private RadioButton myRememberPasswordsUntilClosingRadioButton;
    private RadioButton myRememberOnDiskProtectedRadioButton;

    private Button myResetMasterPasswordButton;
    private Button myChangeMasterPasswordButton;

    private final PasswordSafeImpl myPasswordSafe;

    public Root(PasswordSafe passwordSafe) {
      myPasswordSafe = (PasswordSafeImpl)passwordSafe;
    }

    @Override
    @RequiredUIAccess
    public Component get() {
      VerticalLayout mainLayout = VerticalLayout.create();

      myDoNotRememberPasswordsRadioButton = RadioButton.create(LocalizeValue.localizeTODO("Do &not remember passwords"));
      myRememberPasswordsUntilClosingRadioButton = RadioButton.create(LocalizeValue.localizeTODO("Remember passwords &until the application is closed"));
      myRememberOnDiskProtectedRadioButton = RadioButton.create(LocalizeValue.localizeTODO("Remember on &disk (protected with master password)"));

      mainLayout.add(myDoNotRememberPasswordsRadioButton);
      mainLayout.add(myRememberPasswordsUntilClosingRadioButton);
      mainLayout.add(myRememberOnDiskProtectedRadioButton);

      ValueGroups.boolGroup().add(myDoNotRememberPasswordsRadioButton).add(myRememberPasswordsUntilClosingRadioButton).add(myRememberOnDiskProtectedRadioButton);

      myResetMasterPasswordButton = Button.create(LocalizeValue.localizeTODO("&Reset Master Password"));
      myChangeMasterPasswordButton = Button.create(LocalizeValue.localizeTODO("&Change Master Password"));

      mainLayout.add(HorizontalLayout.create().add(myResetMasterPasswordButton).add(myChangeMasterPasswordButton));

      @RequiredUIAccess Runnable updater = () -> {
        boolean isDisk = myRememberOnDiskProtectedRadioButton.getValue();
        myChangeMasterPasswordButton.setEnabled(isDisk && !isMasterKeyEmpty(myPasswordSafe));
        myResetMasterPasswordButton.setEnabled(isDisk);
      };

      myDoNotRememberPasswordsRadioButton.addValueListener(e -> updater.run());
      myRememberPasswordsUntilClosingRadioButton.addValueListener(e -> updater.run());
      myRememberOnDiskProtectedRadioButton.addValueListener(e -> updater.run());

      myChangeMasterPasswordButton.addClickListener(e -> {
        if (!isMasterKeyEmpty(myPasswordSafe)) {
          ChangeMasterKeyDialog.changePassword(null, myPasswordSafe.getMasterKeyProvider());
          updater.run();
        }
      });
      myResetMasterPasswordButton.addClickListener(e -> {
        if (isMasterKeyEmpty(myPasswordSafe)) {
          ResetPasswordDialog.newPassword(null, myPasswordSafe.getMasterKeyProvider());
        }
        else {
          ResetPasswordDialog.resetPassword(null, myPasswordSafe.getMasterKeyProvider());
        }
        updater.run();
      });

      return LabeledLayout.create(LocalizeValue.localizeTODO("Remembering passwords"), mainLayout);
    }

    /**
     * Check if master key provider is empty
     *
     * @param ps the password safe component
     * @return true if the provider is empty
     */
    private static boolean isMasterKeyEmpty(PasswordSafeImpl ps) {
      return ps.getMasterKeyProvider().isEmpty();
    }

    /**
     * Load component state from settings
     *
     * @param settings the settings to use
     */
    @RequiredUIAccess
    public void reset(PasswordSafeSettings settings) {
      PasswordSafeSettings.ProviderType t = settings.getProviderType();
      switch (t) {
        case DO_NOT_STORE:
          myDoNotRememberPasswordsRadioButton.setValue(true);
          break;
        case MEMORY_ONLY:
          myRememberPasswordsUntilClosingRadioButton.setValue(true);
          break;
        case MASTER_PASSWORD:
          myRememberOnDiskProtectedRadioButton.setValue(true);
          break;
        default:
          throw new IllegalStateException("Unknown provider type: " + t);
      }
    }

    /**
     * @return the provider type
     */
    private PasswordSafeSettings.ProviderType getProviderType() {
      if (myDoNotRememberPasswordsRadioButton.getValue()) {
        return PasswordSafeSettings.ProviderType.DO_NOT_STORE;
      }
      if (myRememberPasswordsUntilClosingRadioButton.getValue()) {
        return PasswordSafeSettings.ProviderType.MEMORY_ONLY;
      }
      return PasswordSafeSettings.ProviderType.MASTER_PASSWORD;
    }

    /**
     * Check if the option panel modified the settings
     *
     * @param settings the settings to compare with
     * @return true, if values were modified
     */
    public boolean isModified(PasswordSafeSettings settings) {
      return getProviderType() != settings.getProviderType();
    }

    /**
     * Save UI state to the settings
     *
     * @param settings the settings to use
     */
    public void apply(PasswordSafeSettings settings) {
      settings.setProviderType(getProviderType());
    }
  }

  private final Provider<PasswordSafeSettings> mySettings;
  private final Provider<PasswordSafe> myPasswordSafe;

  @Inject
  public PasswordSafeConfigurable(@Nonnull Provider<PasswordSafeSettings> settings, @Nonnull Provider<PasswordSafe> passwordSafe) {
    mySettings = settings;
    myPasswordSafe = passwordSafe;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Root createPanel(@Nonnull Disposable uiDisposable) {
    return new Root(myPasswordSafe.get());
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Root component) {
    return component.isModified(mySettings.get());
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull Root component) throws ConfigurationException {
    component.apply(mySettings.get());
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull Root component) {
    component.reset(mySettings.get());
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return "Passwords";
  }

  @Override
  @Nonnull
  public String getId() {
    return "application.passwordSafe";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }
}
