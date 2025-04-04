package consulo.credentialStorage.internal;

public enum ProviderType {
    MEMORY_ONLY,
    KEYCHAIN,
    KEEPASS,

    /**
     * Unused, but can't be removed because it might be stored in the config and we must correctly deserialize it.
     *
     * @deprecated Use MEMORY_ONLY instead.
     */
    @Deprecated
    DO_NOT_STORE
}
