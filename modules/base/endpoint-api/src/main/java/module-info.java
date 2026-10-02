import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-10-02
 */
@NullMarked
module consulo.endpoint.api {
    requires transitive consulo.language.api;
    requires transitive consulo.language.impl;
    requires transitive consulo.language.editor.api;
    requires transitive consulo.language.editor.ui.api;
    requires transitive consulo.ui.ex.api;
    requires transitive consulo.util.concurrent.coroutine;
    requires transitive com.google.gson;

    requires consulo.project.ui.api;

    exports consulo.endpoint;
    exports consulo.endpoint.client.generator;
    exports consulo.endpoint.http;
    exports consulo.endpoint.http.request;
    exports consulo.endpoint.icon;
    exports consulo.endpoint.inspection;
    exports consulo.endpoint.intention;
    exports consulo.endpoint.localize;
    exports consulo.endpoint.mime;
    exports consulo.endpoint.oas;
    exports consulo.endpoint.presentation;
    exports consulo.endpoint.url;
    exports consulo.endpoint.url.inlay;
    exports consulo.endpoint.url.parameter;
    exports consulo.endpoint.url.reference;
    exports consulo.endpoint.util;

    exports consulo.endpoint.internal to consulo.endpoint.impl;
}
