package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.impl.internal.kdbx.KeePassDatabase;

public class InMemoryCredentialStore extends BaseKeePassCredentialStore {
    private final KeePassDatabase myDb;

    public InMemoryCredentialStore() {
        myDb = new KeePassDatabase();
    }

    @Override
    public KeePassDatabase getDb() {
        return myDb;
    }

    @Override
    public void markDirty() {
    }
}
