/**
 * @author VISTALL
 * @since 21-Jul-22
 */
module consulo.external.service.impl {
    requires consulo.application.api;
    requires consulo.external.service.api;
    requires consulo.http.api;
    requires consulo.builtin.web.server.api;
    requires consulo.configuration.editor.api;
    requires consulo.http.adapter.httpclient4;
    requires consulo.virtual.file.status.api;
    requires consulo.component.store.api;

    requires java.prefs;

    requires consulo.ui.ex.api;

    requires org.apache.commons.codec;
    requires org.apache.commons.lang3;
    requires org.apache.commons.compress;
    requires org.lz4.java;

    requires com.google.gson;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    exports consulo.externalService.impl.internal.statistic to consulo.ide.impl;

    exports consulo.externalService.impl.internal to consulo.desktop.awt.ide.impl;

    exports consulo.externalService.impl.internal.update to consulo.desktop.awt.ide.impl;

    exports consulo.externalService.impl.internal.repository to consulo.desktop.awt.ide.impl;

    exports consulo.externalService.impl.internal.errorReport to consulo.desktop.awt.ide.impl;

    opens consulo.externalService.impl.internal.statistic to
        consulo.component.impl,
        consulo.util.xml.serializer;

    opens consulo.externalService.impl.internal.update to consulo.util.xml.serializer;

    opens consulo.externalService.impl.internal.repository.api.pluginHistory to
        consulo.util.xml.serializer,
        com.google.gson;

    opens consulo.externalService.impl.internal.plugin to
        com.google.gson,
        consulo.util.xml.serializer;

    opens consulo.externalService.impl.internal to
        consulo.util.xml.serializer;

    opens consulo.externalService.impl.internal.pluginHistory to
        consulo.util.xml.serializer;

    opens consulo.externalService.impl.internal.repository.api to
        com.google.gson;
}