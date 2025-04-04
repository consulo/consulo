module consulo.container.api {
    requires consulo.util.nodep;

    // add temp dependency
    requires transitive java.scripting;
    // required by gson
    requires transitive java.sql;
    // this dependency fix batik runtime
    requires transitive jdk.xml.dom;
    // required consulo-util-lang
    requires transitive jdk.unsupported;

    requires transitive jdk.net;

    exports consulo.container;
    exports consulo.container.boot;
    exports consulo.container.classloader;
    exports consulo.container.plugin;
    exports consulo.container.plugin.util;
    exports consulo.container.util;

    exports consulo.container.internal to
        consulo.application.api,
        consulo.application.impl,
        consulo.project.impl,
        consulo.desktop.awt.bootstrap,
        consulo.desktop.swt.bootstrap,
        consulo.desktop.swt.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.external.service.impl,
        consulo.test.impl,
        consulo.logging.logback.impl,
        consulo.ide.impl;

    exports consulo.container.internal.plugin to
        consulo.application.api,
        consulo.ide.impl,
        consulo.proxy,
        consulo.external.service.impl;

    exports consulo.container.internal.plugin.classloader to
        consulo.application.api,
        consulo.desktop.awt.bootstrap,
        consulo.desktop.swt.bootstrap,
        consulo.proxy;

    uses consulo.container.boot.ContainerStartup;

    uses consulo.container.internal.PluginManagerInternal;

    provides consulo.container.internal.PluginManagerInternal with consulo.container.internal.plugin.PluginManagerInternalImpl;
}