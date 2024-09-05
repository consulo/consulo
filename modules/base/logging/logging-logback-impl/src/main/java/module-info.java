/**
 * @author VISTALL
 * @since 2024-09-05
 */
module consulo.logging.logback.impl {
    requires consulo.ide.impl;
    requires consulo.logging.api;
    requires consulo.container.impl;

    requires ch.qos.logback.core;

    opens ch.qos.logback.classic.encoder to ch.qos.logback.core;
    opens ch.qos.logback.classic.pattern to ch.qos.logback.core;
    opens ch.qos.logback.classic.filter to ch.qos.logback.core;
    opens ch.qos.logback.classic to ch.qos.logback.core;
    opens consulo.logger.internal.impl.logback.appender to ch.qos.logback.core;

    provides consulo.logging.attachment.AttachmentFactory with consulo.logger.internal.impl.logback.attachment.AttachmentFactoryImpl;
    provides consulo.logging.internal.LoggerFactoryProvider with consulo.logger.internal.impl.logback.LogbackLoggerFactoryProvider;
}