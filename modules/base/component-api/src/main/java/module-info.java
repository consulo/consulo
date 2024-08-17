/**
 * @author VISTALL
 * @since 16/01/2022
 */
module consulo.component.api {
    requires transitive consulo.disposer.api;
    requires transitive consulo.logging.api;
    requires transitive consulo.util.dataholder;
    requires transitive consulo.util.lang;
    requires transitive consulo.util.collection.primitive;
    requires transitive consulo.util.io;
    requires transitive consulo.container.api;
    requires transitive consulo.annotation;
    requires transitive consulo.ui.api;
    requires transitive consulo.util.xml.serializer;
    requires transitive consulo.util.concurrent;
    requires transitive consulo.util.collection;
    requires transitive consulo.platform.api;
    requires transitive consulo.util.jdom;
    requires transitive org.jdom;
    requires transitive jakarta.inject;

    requires static consulo.hacking.java.base;
    requires static com.ibm.icu;

    exports consulo.component;
    exports consulo.component.bind;
    exports consulo.component.extension;
    exports consulo.component.extension.preview;
    exports consulo.component.persist;
    exports consulo.component.persist.scheme;
    exports consulo.component.macro;
    exports consulo.component.messagebus;
    exports consulo.component.util;
    exports consulo.component.util.localize;
    exports consulo.component.util.pointer;
    exports consulo.component.util.text;
    exports consulo.component.util.graph;
    exports consulo.component.util.config;

    exports consulo.component.internal to
        consulo.component.impl,
        consulo.ide.impl,
        consulo.application.api,
        consulo.datacontext.api,
        consulo.virtual.file.system.api;

    exports consulo.component.internal.inject to consulo.component.impl,
        consulo.application.impl,
        consulo.project.impl,
        consulo.module.impl,
        consulo.ide.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.swt.ide.impl,
        consulo.test.impl,
        consulo.language.editor.api;

    uses consulo.component.bind.InjectingBinding;
    uses consulo.component.bind.TopicBinding;
    uses consulo.component.internal.inject.RootInjectingContainerFactory;
}