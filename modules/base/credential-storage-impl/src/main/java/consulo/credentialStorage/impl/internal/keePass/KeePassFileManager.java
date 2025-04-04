package consulo.credentialStorage.impl.internal.keePass;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.credentialStorage.impl.internal.CredentialStoreUiService;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionSpec;
import consulo.credentialStorage.impl.internal.encrypt.EncryptionUtil;
import consulo.credentialStorage.impl.internal.kdbx.IncorrectMainPasswordException;
import consulo.credentialStorage.impl.internal.kdbx.Kdbx;
import consulo.credentialStorage.impl.internal.kdbx.KdbxPassword;
import consulo.credentialStorage.impl.internal.kdbx.KeePassDatabase;
import consulo.credentialStorage.localize.CredentialStorageLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class KeePassFileManager {

    private static final Logger LOG = Logger.getInstance(KeePassFileManager.class);
    private static final String MAIN_KEY_FILE_NAME = "keepass.key"; // Adjust if needed

    private final Path file;
    private final EncryptionSpec mainKeyEncryptionSpec;
    private final Supplier<SecureRandom> secureRandomSupplier;
    private final MainKeyFileStorage mainKeyFileStorage;

    public KeePassFileManager(Path file, Path mainKeyFile, EncryptionSpec mainKeyEncryptionSpec, Supplier<SecureRandom> secureRandomSupplier) {
        this.file = file;
        this.mainKeyEncryptionSpec = mainKeyEncryptionSpec;
        this.secureRandomSupplier = secureRandomSupplier;
        this.mainKeyFileStorage = new MainKeyFileStorage(mainKeyFile);
    }

    public void clear() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            byte[] mainPassword = mainKeyFileStorage.load();
            if (mainPassword != null) {
                KeePassDatabase db = Kdbx.loadKdbx(file, KdbxPassword.createAndClear(mainPassword));
                KeePassCredentialStore store = new KeePassCredentialStore(file, mainKeyFileStorage, db);
                store.clear();
                store.save(mainKeyEncryptionSpec);
                return;
            }
        }
        catch (Exception e) {
            if (!(e instanceof IncorrectMainPasswordException)
                && (ApplicationManager.getApplication() == null || !ApplicationManager.getApplication().isUnitTestMode())) {
                LOG.error(e);
            }
        }
        try {
            Files.deleteIfExists(file);
        }
        catch (Exception ex) {
            LOG.warn("Failed to delete file: " + file, ex);
        }
    }

    public void importDatabase(Path fromFile, AnActionEvent event, UIAccess uiAccess) {
        if (file.equals(fromFile)) {
            return;
        }
        try {
            doImportOrUseExisting(fromFile, event);
        }
        catch (IncorrectMainPasswordException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.warn(e);
            Component contextComponent = event != null ? event.getData(UIExAWTDataKey.CONTEXT_COMPONENT) : null;

            uiAccess.give(() -> {
                if (contextComponent != null) {
                    Messages.showErrorDialog(
                        contextComponent,
                        CredentialStorageLocalize.keePassDialogTitleCannotImport().get(),
                        CredentialStorageLocalize.keePassDialogMessage().get()
                    );
                }
                else {
                    Messages.showErrorDialog(
                        CredentialStorageLocalize.keePassDialogTitleCannotImport().get(),
                        CredentialStorageLocalize.keePassDialogMessage().get()
                    );
                }
            });
        }
    }

    /**
     * Throws IncorrectMainPasswordException if the user cancels the main password dialog.
     */
    public void useExisting() throws IncorrectMainPasswordException {
        try {
            if (Files.exists(file)) {
                if (!doImportOrUseExisting(file, null)) {
                    throw new IncorrectMainPasswordException();
                }
            }
            else {
                var mainKey = KeePassCredentialStore.generateRandomMainKey(mainKeyEncryptionSpec, secureRandomSupplier.get());
                KeePassCredentialStore.saveDatabase(file, new KeePassDatabase(), mainKey, mainKeyFileStorage, secureRandomSupplier.get());
            }
        }
        catch (IncorrectMainPasswordException e) {
            throw e;
        }
        catch (Exception e) {
            // Wrap or rethrow as needed
            throw new RuntimeException(e);
        }
    }

    private boolean doImportOrUseExisting(Path sourceFile, AnActionEvent event) throws Exception {
        Component contextComponent = event != null ? event.getData(UIExAWTDataKey.CONTEXT_COMPONENT) : null;
        Path possibleMainKeyFile = sourceFile.getParent().resolve(MAIN_KEY_FILE_NAME);
        MainKeyFileStorage possibleStorage = new MainKeyFileStorage(possibleMainKeyFile);
        byte[] loadedMainPassword = possibleStorage.load();
        final AtomicReference<byte[]> mainPasswordRef = new AtomicReference<>(loadedMainPassword);

        if (loadedMainPassword != null) {
            try {
                Kdbx.loadKdbx(sourceFile, new KdbxPassword(loadedMainPassword));
            }
            catch (IncorrectMainPasswordException e) {
                LOG.warn("On import \"" + sourceFile + "\" found existing main key file \"" + possibleMainKeyFile + "\" but key is not correct");
                mainPasswordRef.set(null);
            }
        }

        // TODO !
//        if (mainPasswordRef.get() == null && !requestMainPassword(
//            CredentialStorageLocalize.keePassDialogRequestMainTitle(),
//            null,
//            contextComponent,
//            value -> {
//                try {
//                    try {
//                        Kdbx.loadKdbx(sourceFile, new KdbxPassword(value));
//                    }
//                    catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                    mainPasswordRef.set(value);
//                    return null;
//                }
//                catch (IncorrectMainPasswordException ex) {
//                    return CredentialStorageLocalize.dialogMessageMainPasswordNotCorrect();
//                }
//            }
//        )) {
//            return false;
//        }

        if (!sourceFile.equals(this.file)) {
            Files.copy(sourceFile, this.file, StandardCopyOption.REPLACE_EXISTING);
        }
        mainKeyFileStorage.save(createMainKey(mainPasswordRef.get(), false));
        return true;
    }

    public void askAndSetMainKey(AnActionEvent event, @Nonnull LocalizeValue topNote, Runnable onPasswordSet) {
        Component contextComponent = event != null ? event.getData(UIExAWTDataKey.CONTEXT_COMPONENT) : null;
        KeePassDatabase db;
        try {
            if (Files.exists(file)) {
                byte[] loaded = mainKeyFileStorage.load();
                if (loaded == null) {
                    throw new IncorrectMainPasswordException(true);
                }
                try {
                    db = Kdbx.loadKdbx(file, new KdbxPassword(loaded));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                db = new KeePassDatabase();
            }
        }
        catch (IncorrectMainPasswordException e) {
            return;
            //TODO return requestCurrentAndNewKeys(contextComponent);
        }

        requestMainPassword(
            CredentialStorageLocalize.keePassDialogTitleSetMainPassword(),
            topNote,
            contextComponent,
            value -> {
                KeePassCredentialStore.saveDatabase(file, db, createMainKey(value, false), mainKeyFileStorage, secureRandomSupplier.get());
                return LocalizeValue.of();
            },
            onPasswordSet
        );
    }

    protected boolean requestCurrentAndNewKeys(Component contextComponent) {
        return CredentialStoreUiService.getInstance().showChangeMainPasswordDialog(contextComponent, this::doSetNewMainPassword);
    }

    protected boolean doSetNewMainPassword(char[] current, char[] newPassword) {
        byte[] currentBytes = EncryptionUtil.toByteArrayAndClear(current);
        KeePassDatabase db;
        try {
            db = Kdbx.loadKdbx(file, KdbxPassword.createAndClear(currentBytes));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] newBytes = EncryptionUtil.toByteArrayAndClear(newPassword);
        KeePassCredentialStore.saveDatabase(file, db, createMainKey(newBytes, false), mainKeyFileStorage, secureRandomSupplier.get());
        return false;
    }

    private MainKey createMainKey(byte[] value, boolean isAutoGenerated) {
        return new MainKey(value, isAutoGenerated, mainKeyEncryptionSpec);
    }

    protected void requestMainPassword(LocalizeValue title,
                                       LocalizeValue topNote,
                                       Component contextComponent,
                                       Function<byte[], LocalizeValue> okHandler,
                                       Runnable onChanged) {
        DialogService dialogService = Application.get().getInstance(DialogService.class);

        Dialog dialog;
        if (contextComponent == null) {
            dialog = dialogService.build(new SetMainPasswordDialogDescriptor(title, okHandler, topNote));
        }
        else {
            dialog = dialogService.build(TargetAWT.wrap(contextComponent), new SetMainPasswordDialogDescriptor(title, okHandler, topNote));
        }

        CompletableFuture<DialogValue> future = dialog.showAsync();
        future.whenComplete((dialogValue, throwable) -> {
            if (dialogValue instanceof SetMainPasswordDialogDescriptor.NewMasterPasswordResult) {
                onChanged.run();
            }
        });
    }

    public void saveMainKeyToApplyNewEncryptionSpec() {
        byte[] existing = mainKeyFileStorage.load();
        if (existing == null) {
            return;
        }
        mainKeyFileStorage.save(createMainKey(existing, mainKeyFileStorage.isAutoGenerated()));
    }

    public void setCustomMainPasswordIfNeeded(Path defaultDbFile) {
        if (file.equals(defaultDbFile)) {
            return;
        }
        if (!mainKeyFileStorage.isAutoGenerated()) {
            return;
        }
        askAndSetMainKey(null, CredentialStorageLocalize.keePassTopNote(), () -> {
        });
    }
}
