module consulo.external.process.auth.helper.rt {
    requires java.net.http;

    exports externalApp;
    exports externalApp.nativessh;
    exports externalApp.sudoAskPass;
}
