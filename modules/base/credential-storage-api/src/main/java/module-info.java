/**
 * @author VISTALL
 * @since 2023-01-23
 */
@SuppressWarnings("module")
module consulo.credential.storage.api {
    requires transitive consulo.application.api;
    requires transitive consulo.project.api;

    exports consulo.credentialStorage;
    exports consulo.credentialStorage.ui;
    exports consulo.credentialStorage.localize;

    exports consulo.credentialStorage.internal to consulo.credential.storage.impl;
}