package consulo.credentialStorage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Please see <a href="https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html">Storing Sensitive Data</a>.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PasswordSafe extends CredentialStore {

    /**
     * Returns the PasswordSafe service instance.
     */
    static PasswordSafe getInstance() {
        return ApplicationManager.getApplication().getInstance(PasswordSafe.class);
    }

    /**
     * Returns the state of the "Remember" check box. The state is global.
     */
    boolean isRememberPasswordByDefault();

    /**
     * Sets the state of the "Remember" check box.
     */
    void setRememberPasswordByDefault(boolean rememberPasswordByDefault);

    /**
     * Returns true if the password is stored only in memory.
     */
    boolean isMemoryOnly();

    /**
     * Stores the credentials for the given attributes.
     *
     * @param attributes  the credential attributes
     * @param credentials the credentials (may be null)
     * @param memoryOnly  if true, store credentials only in memory
     */
    void set(CredentialAttributes attributes, Credentials credentials, boolean memoryOnly);

    /**
     * Asynchronously retrieves the credentials for the given attributes.
     *
     * @param attributes the credential attributes
     * @return a Promise that resolves to the credentials (or null if none)
     */
    CompletableFuture<Credentials> getAsync(CredentialAttributes attributes);

    /**
     * Determines whether the password for the given attributes is stored only in memory.
     *
     * @param attributes  the credential attributes
     * @param credentials the credentials
     * @return true if the password is stored only in memory, false otherwise
     */
    boolean isPasswordStoredOnlyInMemory(CredentialAttributes attributes, Credentials credentials);

    /**
     * @deprecated Please use {@link #set(CredentialAttributes, Credentials)}
     */
    @Deprecated(forRemoval = true)
    default void storePassword(@SuppressWarnings("unused") @Nullable Project project, @Nonnull Class<?> requestor, @Nonnull String key, @Nullable String value) {
        set(new CredentialAttributes(requestor.getName(), key, requestor), value == null ? null : new Credentials(key, value));
    }

    /**
     * @deprecated use {@link #get(CredentialAttributes)} + {@link Credentials#getPasswordAsString()}
     */
    @Deprecated(forRemoval = true)
    default @Nullable String getPassword(@SuppressWarnings("unused") @Nullable Project project, @Nonnull Class<?> requestor, @Nonnull String key) {
        var credentials = get(new CredentialAttributes(requestor.getName(), key, requestor));
        return credentials == null ? null : credentials.getPasswordAsString();
    }

    @Nullable
    Credentials get(@Nonnull CredentialAttributes attributes);

    void set(@Nonnull CredentialAttributes attributes, @Nullable Credentials credentials);
}
