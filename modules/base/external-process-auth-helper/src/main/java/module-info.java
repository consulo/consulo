import org.jspecify.annotations.NullMarked;

@NullMarked
module consulo.external.process.auth.helper {
    requires transitive consulo.external.process.auth.helper.rt;
    requires transitive consulo.project.api;
    requires consulo.builtin.web.server.api;
    requires consulo.credential.storage.api;

    exports consulo.externalProcessAuthHelper;
    exports consulo.externalProcessAuthHelper.localize;
}
