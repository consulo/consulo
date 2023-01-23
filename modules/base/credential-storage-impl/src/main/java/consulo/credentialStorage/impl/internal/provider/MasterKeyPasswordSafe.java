/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.credentialStorage.impl.internal.provider;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.SystemInfo;
import consulo.credentialStorage.PasswordSafeException;
import consulo.credentialStorage.impl.internal.ByteArrayWrapper;
import consulo.credentialStorage.impl.internal.provider.masterKey.EncryptionUtil;
import consulo.credentialStorage.impl.internal.provider.masterKey.MasterPasswordUnavailableException;
import consulo.credentialStorage.impl.internal.provider.masterKey.PasswordDatabase;
import consulo.credentialStorage.impl.internal.provider.masterKey.WindowsCryptUtils;
import consulo.credentialStorage.impl.internal.ui.MasterPasswordDialog;
import consulo.credentialStorage.impl.internal.ui.ResetPasswordDialog;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The password safe that stores information in configuration file encrypted by master password
 */
public class MasterKeyPasswordSafe extends BasePasswordSafeProvider {
  /**
   * The test password key
   */
  private static final String TEST_PASSWORD_KEY = "TEST_PASSWORD:";
  /**
   * The test password value
   */
  private static final String TEST_PASSWORD_VALUE = "test password";
  /**
   * The password database instance
   */
  final PasswordDatabase database;
  /**
   * The key to use to encrypt data
   */
  private transient final AtomicReference<byte[]> key = new AtomicReference<byte[]>();

  /**
   * The constructor
   *
   * @param database the password database
   */
  public MasterKeyPasswordSafe(PasswordDatabase database) {
    this.database = database;
  }

  /**
   * @return true if the component is running in the test mode
   */
  protected boolean isTestMode() {
    return false;
  }

  /**
   * Reset password for the password safe (clears password database). The method is used from plugin's UI.
   *
   * @param password the password to set
   * @param encrypt  if the password should be encrypted an stored is master database
   */
  public void resetMasterPassword(String password, boolean encrypt) {
    this.key.set(EncryptionUtil.genPasswordKey(password));
    database.clear();
    try {
      storePassword(null, MasterKeyPasswordSafe.class, testKey(password), TEST_PASSWORD_VALUE);
      if (encrypt) {
        database.setPasswordInfo(encryptPassword(password));
      }
      else {
        database.setPasswordInfo(new byte[0]);
      }
    }
    catch (PasswordSafeException e) {
      throw new IllegalStateException("There should be no problem with password at this point", e);
    }
  }

  /**
   * Set password to use (used from plugin's UI)
   *
   * @param password the password
   * @return true, if password is a correct one
   */
  public boolean setMasterPassword(String password) {
    byte[] savedKey = this.key.get();
    this.key.set(EncryptionUtil.genPasswordKey(password));
    String rc;
    try {
      rc = getPassword(null, MasterKeyPasswordSafe.class, testKey(password));
    }
    catch (PasswordSafeException e) {
      throw new IllegalStateException("There should be no problem with password at this point", e);
    }
    if (!TEST_PASSWORD_VALUE.equals(rc)) {
      this.key.set(savedKey);
      return false;
    }
    else {
      return true;
    }

  }

  /**
   * Encrypt database with new password
   *
   * @param oldPassword the old password
   * @param newPassword the new password
   * @param encrypt
   * @return re-encrypted database
   */
  public boolean changeMasterPassword(String oldPassword, String newPassword, boolean encrypt) {
    if (!setMasterPassword(oldPassword)) {
      return false;
    }
    byte[] oldKey = key.get();
    byte[] newKey = EncryptionUtil.genPasswordKey(newPassword);
    ByteArrayWrapper testKey = new ByteArrayWrapper(EncryptionUtil.dbKey(oldKey, MasterKeyPasswordSafe.class, testKey(oldPassword)));
    HashMap<ByteArrayWrapper, byte[]> oldDb = new HashMap<ByteArrayWrapper, byte[]>();
    database.copyTo(oldDb);
    HashMap<ByteArrayWrapper, byte[]> newDb = new HashMap<ByteArrayWrapper, byte[]>();
    for (Map.Entry<ByteArrayWrapper, byte[]> e : oldDb.entrySet()) {
      if (testKey.equals(e.getKey())) {
        continue;
      }
      byte[] decryptedKey = EncryptionUtil.decryptKey(oldKey, e.getKey().unwrap());
      String decryptedText = EncryptionUtil.decryptText(oldKey, e.getValue());
      newDb.put(new ByteArrayWrapper(EncryptionUtil.encryptKey(newKey, decryptedKey)), EncryptionUtil.encryptText(newKey, decryptedText));
    }
    synchronized (database.getDbLock()) {
      resetMasterPassword(newPassword, encrypt);
      database.putAll(newDb);
    }
    return true;
  }


  /**
   * The test key
   *
   * @param password the password for the test key
   * @return the test key
   */
  private static String testKey(String password) {
    return TEST_PASSWORD_KEY + password;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected byte[] key(@Nullable final Project project) throws PasswordSafeException {
    Application application = Application.get();
    if (!isTestMode() && application.isHeadlessEnvironment()) {
      throw new MasterPasswordUnavailableException("The provider is not available in headless environment");
    }
    if (key.get() == null) {
      if (isPasswordEncrypted()) {
        try {
          String s = decryptPassword(database.getPasswordInfo());
          setMasterPassword(s);
        }
        catch (PasswordSafeException e) {
          // ignore exception and ask password
        }
      }
      if (key.get() == null) {
        final Ref<PasswordSafeException> ex = new Ref<PasswordSafeException>();
        application.invokeAndWait(new Runnable() {
          public void run() {
            if (key.get() == null) {
              try {
                if (isTestMode()) {
                  throw new MasterPasswordUnavailableException("Master password must be specified in test mode.");
                }
                if (database.isEmpty()) {
                  if (!ResetPasswordDialog.newPassword(project, MasterKeyPasswordSafe.this)) {
                    throw new MasterPasswordUnavailableException("Master password is required to store passwords in the database.");
                  }
                }
                else {
                  MasterPasswordDialog.askPassword(project, MasterKeyPasswordSafe.this);
                }
              }
              catch (PasswordSafeException e) {
                ex.set(e);
              }
              catch (Exception e) {
                //noinspection ThrowableInstanceNeverThrown
                ex.set(new MasterPasswordUnavailableException("The problem with retrieving the password", e));
              }
            }
          }
        }, application.getDefaultModalityState());
        //noinspection ThrowableResultOfMethodCallIgnored
        if (ex.get() != null) {
          throw ex.get();
        }
      }
    }
    return this.key.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    if (database.isEmpty()) {
      return null;
    }
    return super.getPassword(project, requester, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePassword(@Nullable Project project, Class requester, String key) throws PasswordSafeException {
    if (database.isEmpty()) {
      return;
    }
    super.removePassword(project, requester, key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected byte[] getEncryptedPassword(byte[] key) {
    return database.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void removeEncryptedPassword(byte[] key) {
    database.remove(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void storeEncryptedPassword(byte[] key, byte[] encryptedPassword) {
    database.put(key, encryptedPassword);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSupported() {
    return !ApplicationManager.getApplication().isHeadlessEnvironment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return "This provider stores passwords in IDEA config and uses master password to encrypt other passwords. " + "The passwords for the same resources are shared between different projects.";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "Master Key PasswordSafe";
  }

  /**
   * @return true, if OS protected passwords are supported for the current platform
   */
  @SuppressWarnings({"MethodMayBeStatic"})
  public boolean isOsProtectedPasswordSupported() {
    // TODO extension point needed?
    return SystemInfo.isWindows;
  }


  /**
   * Encrypt master password
   *
   * @param pw the password to encrypt
   * @return the encrypted password
   * @throws MasterPasswordUnavailableException if encryption fails
   */
  private static byte[] encryptPassword(String pw) throws MasterPasswordUnavailableException {
    assert SystemInfo.isWindows;
    return WindowsCryptUtils.protect(EncryptionUtil.getUTF8Bytes(pw));
  }

  /**
   * Decrypt master password
   *
   * @param pw the password to decrypt
   * @return the decrypted password
   * @throws MasterPasswordUnavailableException if decryption fails
   */
  private static String decryptPassword(byte[] pw) throws MasterPasswordUnavailableException {
    assert SystemInfo.isWindows;
    try {
      return new String(WindowsCryptUtils.unprotect(pw), "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("UTF-8 not available", e);
    }
  }

  /**
   * @return true, if the password is currently encrypted in the database
   */
  public boolean isPasswordEncrypted() {
    byte[] i = database.getPasswordInfo();
    return i != null && i.length > 0;
  }

  /**
   * @return check if provider database is empty
   */
  public boolean isEmpty() {
    return database.isEmpty();
  }
}
