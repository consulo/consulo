module consulo.logging.api {
    requires transitive consulo.annotation;
    requires consulo.util.nodep;
    requires consulo.util.lang;
    requires consulo.container.api;

    exports consulo.logging;
    exports consulo.logging.attachment;
    exports consulo.logging.util;

    exports consulo.logging.internal to
        consulo.logging.logback.impl,
        consulo.ide.impl,
        consulo.application.impl,
        consulo.external.service.impl,
        consulo.application.api,
        consulo.desktop.awt.ide.impl;

    uses consulo.logging.internal.LoggerFactoryProvider;
    uses consulo.logging.attachment.AttachmentFactory;
}