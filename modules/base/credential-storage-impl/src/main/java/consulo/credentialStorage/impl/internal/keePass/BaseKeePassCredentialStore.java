package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialAttributesUtil;
import consulo.credentialStorage.CredentialStore;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.impl.internal.kdbx.KdbxEntry;
import consulo.credentialStorage.impl.internal.kdbx.KeePassDatabase;
import consulo.util.lang.StringUtil;

/**
 * BaseKeePassCredentialStore provides common logic for KeePass-based credential stores.
 */
public abstract class BaseKeePassCredentialStore implements CredentialStore {
    public static final String ROOT_GROUP_NAME = CredentialAttributesUtil.SERVICE_NAME_PREFIX;

    /**
     * Subclasses must provide the underlying KeePassDatabase.
     *
     * @return the KeePassDatabase instance
     */
    protected abstract KeePassDatabase getDb();

    @Override
    public Credentials get(CredentialAttributes attributes) {
        // Get the root group by name.
        var group = getDb().getRootGroup().getGroup(ROOT_GROUP_NAME);
        if (group == null) {
            return null;
        }
        // Obtain a possibly nullified username.
        String userName = StringUtil.nullize(attributes.getUserName());
        // Look up the entry by service name and user name.
        var entry = group.getEntry(attributes.getServiceName(), StringUtil.nullize(userName));
        if (entry == null) {
            return null;
        }
        // Return new Credentials using the specified username (or the one from entry if absent)
        // and the password (retrieved via get() on the stored password).
        return new Credentials(userName != null ? userName : entry.getUserName(),
            entry.getPassword() != null ? entry.getPassword().get() : null);
    }

    @Override
    public void set(CredentialAttributes attributes, Credentials credentials) {
        if (credentials == null) {
            // Remove the entry if credentials are null.
            var group = getDb().getRootGroup().getGroup(ROOT_GROUP_NAME);
            if (group != null) {
                group.removeEntry(attributes.getServiceName(), StringUtil.nullize(attributes.getUserName()));
            }
        }
        else {
            // Get or create the root group.
            var group = getDb().getRootGroup().getOrCreateGroup(ROOT_GROUP_NAME);
            String userName = StringUtil.nullize(attributes.getUserName());
            if (userName == null) {
                userName = credentials.getUserName();
            }
            // If the service name equals SERVICE_NAME_PREFIX, use the provided userName; otherwise search without a username.
            KdbxEntry entry;
            if (attributes.getServiceName().equals(CredentialAttributesUtil.SERVICE_NAME_PREFIX)) {
                entry = group.getEntry(attributes.getServiceName(), userName);
            }
            else {
                entry = group.getEntry(attributes.getServiceName(), null);
            }
            if (entry == null) {
                entry = group.createEntry(attributes.getServiceName(), userName);
            }
            else {
                entry.setUserName(userName);
            }
            // If password is to be kept only in memory or credentials have no password, clear the stored password.
            if (attributes.isPasswordMemoryOnly() || credentials.getPassword() == null) {
                entry.setPassword(null);
            }
            else {
                entry.setPassword(getDb().protectValue(credentials.getPassword()));
            }
        }
        // If the database has unsaved changes, mark it dirty.
        if (getDb().isDirty()) {
            markDirty();
        }
    }

    /**
     * Subclasses should implement this method to mark the underlying database as dirty.
     */
    protected abstract void markDirty();
}
