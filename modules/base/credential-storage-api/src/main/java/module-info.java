/**
 * @author VISTALL
 * @since 23/01/2023
 */
module consulo.credential.storage.api {
    requires transitive consulo.application.api;
    requires transitive consulo.project.api;

    exports consulo.credentialStorage;
    exports consulo.credentialStorage.ui;
    exports consulo.credentialStorage.localize;

    exports consulo.credentialStorage.internal to consulo.credential.storage.impl;
}