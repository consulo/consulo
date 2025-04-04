package consulo.credentialStorage.impl.internal.ui;

import consulo.application.dumb.DumbAware;
import consulo.application.util.SystemInfo;
import consulo.configurable.ConfigurationException;
import consulo.configurable.IdeaConfigurableUi;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.impl.internal.*;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSupportFactory;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionType;
import consulo.credentialStorage.impl.internal.gpg.Pgp;
import consulo.credentialStorage.impl.internal.gpg.PgpKey;
import consulo.credentialStorage.impl.internal.kdbx.IncorrectMainPasswordException;
import consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore;
import consulo.credentialStorage.impl.internal.keePass.KeePassFileManager;
import consulo.credentialStorage.impl.internal.keePass.MainKeyFileStorage;
import consulo.credentialStorage.internal.CredentialStoreManager;
import consulo.credentialStorage.internal.ProviderType;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.FileChooserFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.FileChooserTextBoxBuilder;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBRadioButton;
import consulo.ui.ex.awt.MessageDialogBuilder;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.MutableListModel;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static consulo.credentialStorage.impl.internal.keePass.KeePassCredentialStore.getDefaultDbFile;

public class PasswordSafeConfigurableUi implements IdeaConfigurableUi<PasswordSafeSettings> {
    private static class GearActionGroup extends DefaultActionGroup implements DumbAware {
        public GearActionGroup() {
        }

        @Nullable
        @Override
        protected Image getTemplateIcon() {
            return PlatformIconGroup.generalGearplain();
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }

        @Override
        public boolean isPopup() {
            return true;
        }
    }

    private CheckBox usePgpKey;
    private ComboBox<PgpKey> pgpKeyCombo;
    private JRadioButton keepassRadioButton;
    private FileChooserTextBoxBuilder.Controller keePassDbFile;

    private final MutableListModel<PgpKey> pgpListModel = MutableListModel.of(List.of());
    private final Supplier<Pgp> pgp = LazyValue.notNull(Pgp::new);
    // https://youtrack.jetbrains.com/issue/IDEA-200188
    // reuse to avoid delays - on Linux SecureRandom is quite slow
    private final Supplier<SecureRandom> secureRandom = LazyValue.notNull(CredentialStoreUtil::createSecureRandom);

    private final PasswordSafeSettings settings;

    private static final String DB_FILE_NAME = "keepass.kdbx";

    private Map<ProviderType, RadioButton> myProviderButtons = new HashMap<>();

    public PasswordSafeConfigurableUi(PasswordSafeSettings settings) {
        this.settings = settings;
    }

    @Override
    @RequiredUIAccess
    public void reset(@Nonnull PasswordSafeSettings settings) {
        List<PgpKey> secretKeys;
        try {
            secretKeys = pgp.get().listKeys();
        }
        catch (Exception e) {
            SharedLogger.LOG.warn(e);
            secretKeys = List.of();
        }
        
        pgpListModel.replaceAll(secretKeys);

        usePgpKey.setLabelText(usePgpKeyText());

        RadioButton button = myProviderButtons.get(settings.getProviderType());
        if (button != null) {
            button.setValue(true);
        }

        if (keePassDbFile != null) {
            String keepassDb = settings.getKeepassDb();
            keePassDbFile.setValue((keepassDb == null || keepassDb.isEmpty()) ? getDefaultDbFile().toString() : keepassDb);
        }
    }

    @Override
    public boolean isModified(@Nonnull PasswordSafeSettings settings) {
        if (getActiveProviderType() != settings.getProviderType()) {
            return true;
        }

        if (keePassDbFile == null) {
            return false;
        }

        return isKeepassFileLocationChanged(settings);
    }

    private ProviderType getActiveProviderType() {
        for (Map.Entry<ProviderType, RadioButton> entry : myProviderButtons.entrySet()) {
            if (entry.getValue().getValueOrError()) {
                return entry.getKey();
            }
        }
        return CredentialStoreManager.getInstance().defaultProvider();
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public JComponent getComponent(Disposable disposable) {

        keepassRadioButton = new JBRadioButton();

        CredentialStoreManager credentialStoreManager = CredentialStoreManager.getInstance();

        VerticalLayout buttonsLayout = VerticalLayout.create();

        ValueGroup<Boolean> group = ValueGroups.boolGroup();

        RadioButton nativeKeychainButton = RadioButton.create(CredentialStorageLocalize.passwordsafeconfigurableInNativeKeychain());
        nativeKeychainButton.setVisible(credentialStoreManager.isSupported(ProviderType.KEYCHAIN));

        myProviderButtons.put(ProviderType.KEYCHAIN, nativeKeychainButton);
        group.add(nativeKeychainButton);
        buttonsLayout.add(nativeKeychainButton);

        RadioButton keePassButton = RadioButton.create(CredentialStorageLocalize.passwordsafeconfigurableInKeepass());
        keePassButton.setVisible(CredentialStoreManager.getInstance().isSupported(ProviderType.KEEPASS));

        VerticalLayout keePassPanel = VerticalLayout.create();
        keePassPanel.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, 24);
        keePassButton.addValueListener(event -> keePassPanel.setEnabledRecursive(event.getValue()));

        GearActionGroup gearActionGroup = new GearActionGroup();
        gearActionGroup.addAll(new ClearKeePassDatabaseAction(),
            new ImportKeePassDatabaseAction(),
            new ChangeKeePassDatabaseMasterPasswordAction()
        );

        FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
        builder.uiDisposable(disposable);
        builder.dialogTitle(CredentialStorageLocalize.passwordsafeconfigurableKeepassDatabaseFile());
        builder.lastActions(gearActionGroup);
        
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor();
        descriptor.withFileFilter(file -> "kdbx".equals(file.getExtension()));
        builder.fileChooserDescriptor(descriptor);
        keePassDbFile = builder.build();

        TextBox keePassPathField = keePassDbFile.getComponent();
        Label keePassPathLabel = Label.create(CredentialStorageLocalize.settingsPasswordDatabase());
        keePassPathLabel.setTarget(keePassPathField);

        keePassPanel.add(DockLayout.create().left(keePassPathLabel).center(keePassPathField));

        usePgpKey = CheckBox.create(usePgpKeyText());

        pgpKeyCombo = ComboBox.create(pgpListModel);
        pgpKeyCombo.setTextRender(pgpKey -> {
            if (pgpKey == null) {
                return LocalizeValue.of();
            }
            return LocalizeValue.localizeTODO(pgpKey.getUserId() + "(" + pgpKey.getKeyId() + ")");
        });
        keePassPanel.add(DockLayout.create().left(usePgpKey).center(pgpKeyCombo));

        myProviderButtons.put(ProviderType.KEEPASS, keePassButton);
        group.add(keePassButton);
        buttonsLayout.add(keePassButton);
        buttonsLayout.add(keePassPanel);

        keePassPanel.setEnabledRecursive(false);

        RadioButton doNotSaveButton = RadioButton.create(CredentialStorageLocalize.passwordsafeconfigurableDoNotSave());
        doNotSaveButton.setVisible(CredentialStoreManager.getInstance().isSupported(ProviderType.MEMORY_ONLY));

        myProviderButtons.put(ProviderType.MEMORY_ONLY, doNotSaveButton);
        group.add(doNotSaveButton);
        buttonsLayout.add(doNotSaveButton);

        LabeledLayout labeledLayout = LabeledLayout.create(CredentialStorageLocalize.passwordsafeconfigurableSavePassword(), buttonsLayout);

        return (JComponent) TargetAWT.to(labeledLayout);
    }

    private boolean isKeepassFileLocationChanged(PasswordSafeSettings settings) {
        String newDb = getNewDbFileAsString();
        String currentDb = settings.getKeepassDb();
        return keepassRadioButton.isSelected() && !StringUtil.equals(newDb, currentDb);
    }

    @Override
    public void apply(@Nonnull PasswordSafeSettings settings) throws ConfigurationException {
        String newPgpKeyId = (getNewPgpKey() != null) ? getNewPgpKey().getKeyId() : null;
        boolean pgpKeyChanged = !StringUtil.equals(newPgpKeyId, this.settings.getState().getPgpKeyId());
        ProviderType oldProviderType = this.settings.getProviderType();

        settings.setProviderType(getActiveProviderType());

        ProviderType providerType = this.settings.getProviderType();

        // Close current store (do not save, and close even if not memory-only)
        ((PasswordSafeImpl) PasswordSafe.getInstance()).closeCurrentStore(false, providerType != ProviderType.MEMORY_ONLY);

        PasswordSafeImpl passwordSafe = (PasswordSafeImpl) PasswordSafe.getInstance();
        if (oldProviderType != providerType) {
            if (providerType == ProviderType.MEMORY_ONLY) {
                // Nothing else required.
            }
            else if (providerType == ProviderType.KEYCHAIN) {
                try {
                    CredentialStore store = BasePasswordSafe.createPersistentCredentialStore();
                    if (store == null) {
                        throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordInternalErrorNoAvailableCredentialStoreImplementation());
                    }
                    passwordSafe.setCurrentProvider(store);
                }
                catch (UnsatisfiedLinkError e) {
                    SharedLogger.LOG.warn(e);

                    if (SystemInfo.isLinux) {
                        throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordPackageLibsecret10IsNotInstalled());
                    }
                    else {
                        throw new ConfigurationException(LocalizeValue.ofNullable(e.getMessage()));
                    }
                }
            }
            else if (providerType == ProviderType.KEEPASS) {
                createAndSaveKeePassDatabaseWithNewOptions(settings);
            }
            else {
                throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordUnknownProviderType(providerType));
            }
        }
        else if (isKeepassFileLocationChanged(settings)) {
            createAndSaveKeePassDatabaseWithNewOptions(settings);
        }
        else if (providerType == ProviderType.KEEPASS && pgpKeyChanged) {
            try {
                KeePassFileManager mgr = createKeePassFileManager();
                if (mgr != null) {
                    mgr.saveMainKeyToApplyNewEncryptionSpec();
                }
            }
            catch (Exception e) {
                SharedLogger.LOG.error(e);
                throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordInternalError(e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }

        if (providerType == ProviderType.KEEPASS) {
            KeePassFileManager mgr = createKeePassFileManager();
            if (mgr != null) {
                mgr.setCustomMainPasswordIfNeeded(getDefaultDbFile());
            }
        }

        settings.setProviderType(providerType);
    }

    private void createAndSaveKeePassDatabaseWithNewOptions(PasswordSafeSettings settings) throws ConfigurationException {
        Path newDbFile = getNewDbFile();
        if (newDbFile == null) {
            throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordKeepassDatabasePathIsEmpty());
        }

        if (Files.isDirectory(newDbFile)) {
            throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordKeepassDatabaseFileIsDirectory());
        }

        if (!newDbFile.getFileName().toString().endsWith(".kdbx")) {
            throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordKeepassDatabaseFileShouldEndsWithKdbx());
        }

        settings.setKeepassDb(newDbFile.toString());

        try {
            KeePassFileManager manager = new KeePassFileManager(newDbFile, KeePassCredentialStore.getDefaultMainPasswordFile(), getEncryptionSpec(), secureRandom);
            manager.useExisting();
        }
        catch (IncorrectMainPasswordException e) {
            throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordMasterPasswordForKeepassDatabaseIsNotCorrect());
        }
        catch (Exception e) {
            SharedLogger.LOG.warn(e);

            throw new ConfigurationException(CredentialStorageLocalize.settingsPasswordInternalError(e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }

    private Path getNewDbFile() {
        String pathStr = getNewDbFileAsString();
        return (pathStr != null) ? Paths.get(pathStr) : null;
    }

    private String getNewDbFileAsString() {
        if (keePassDbFile == null) {
            return null;
        }
        String text = keePassDbFile.getValue().trim();
        return text.isEmpty() ? null : text;
    }

    private LocalizeValue usePgpKeyText() {
        return (pgpListModel.getSize() == 0)
            ? CredentialStorageLocalize.passwordsafeconfigurableProtectMasterPasswordUsingPgpKeyNoKeys()
            : CredentialStorageLocalize.passwordsafeconfigurableProtectMasterPasswordUsingPgpKey();
    }

    private PgpKey getSelectedPgpKey() {
        String currentKeyId = settings.getState().getPgpKeyId();
        if (currentKeyId == null) {
            return null;
        }
        for (int i = 0; i < pgpListModel.getSize(); i++) {
            PgpKey key = pgpListModel.get(i);
            if (key.getKeyId().equals(currentKeyId)) {
                return key;
            }
        }
        return (pgpListModel.getSize() > 0) ? pgpListModel.get(0) : null;
    }

    private KeePassFileManager createKeePassFileManager() {
        Path newDb = getNewDbFile();
        if (newDb == null) {
            return null;
        }
        return new KeePassFileManager(newDb, KeePassCredentialStore.getDefaultMainPasswordFile(), getEncryptionSpec(), secureRandom);
    }

    private EncryptionSpec getEncryptionSpec() {
        PgpKey pgpKey = getNewPgpKey();
        if (pgpKey == null) {
            return new EncryptionSpec(EncryptionSupportFactory.getDefaultEncryptionType(), null);
        }
        else {
            return new EncryptionSpec(EncryptionType.PGP_KEY, pgpKey.getKeyId());
        }
    }

    @Nullable
    private PgpKey getNewPgpKey() {
        return pgpKeyCombo.getValue();
    }

    // --- Inner action classes ---

    private class ClearKeePassDatabaseAction extends DumbAwareAction {
        public ClearKeePassDatabaseAction() {
            super(CredentialStorageLocalize.actionTextPasswordSafeClear());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent event) {
            boolean confirmed = MessageDialogBuilder.yesNo(
                    CredentialStorageLocalize.passwordsafeconfigurableClearPasswords().get(),
                    CredentialStorageLocalize.passwordsafeconfigurableAreYouSure().get())
                .yesText(CredentialStorageLocalize.passwordsafeconfigurableRemovePasswords().get())
                .project(event.getData(Project.KEY))
                .isYes();
            if (!confirmed) {
                return;
            }

            closeCurrentStore();

            SharedLogger.LOG.info("Passwords cleared", new Error());
            KeePassFileManager mgr = createKeePassFileManager();
            if (mgr != null) {
                mgr.clear();
            }
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            Path dbFile = getNewDbFile();
            e.getPresentation().setEnabled(dbFile != null && Files.exists(dbFile));
        }

        @Nonnull
        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    private class ImportKeePassDatabaseAction extends DumbAwareAction {
        public ImportKeePassDatabaseAction() {
            super(CredentialStorageLocalize.actionTextPasswordSafeImport());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            closeCurrentStore();

            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
            descriptor.withFileFilter(file -> "kdbx".equals(file.getExtension()));


            Project project = e.getData(Project.KEY);

            FileChooserDialog dialog =
                FileChooserFactory.getInstance().createFileChooser(descriptor, project, e.getData(UIExAWTDataKey.CONTEXT_COMPONENT));

            UIAccess uiAccess = UIAccess.current();

            dialog.chooseAsync(project, VirtualFile.EMPTY_ARRAY).doWhenDone((v) -> {
                if (v == null || v.length == 0) {
                    return;
                }

                VirtualFile selectedFile = v[0];

                KeePassFileManager keePassFileManager = createKeePassFileManager();
                if (keePassFileManager != null) {
                    keePassFileManager.importDatabase(selectedFile.toNioPath(), e, uiAccess);
                }
            });
        }
    }

    private class ChangeKeePassDatabaseMasterPasswordAction extends DumbAwareAction {
        public ChangeKeePassDatabaseMasterPasswordAction() {
            super(new MainKeyFileStorage(KeePassCredentialStore.getDefaultMainPasswordFile()).isAutoGenerated()
                ? CredentialStorageLocalize.actionSetPasswordText()
                : CredentialStorageLocalize.actionChangePasswordText());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent event) {
            closeCurrentStore();
            KeePassFileManager mgr = createKeePassFileManager();
            if (mgr == null) {
                return;
            }
            
            mgr.askAndSetMainKey(event, LocalizeValue.of(), () -> {
                getTemplatePresentation().setTextValue(CredentialStorageLocalize.settingsPasswordChangeMasterPassword());
            });
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getNewDbFileAsString() != null);
        }

        @Nonnull
        @Override
        public ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    // --- Static utility method to close the current store ---
    private static void closeCurrentStore() {
        ((PasswordSafeImpl) PasswordSafe.getInstance()).closeCurrentStore(true, false);
    }
}
