/**
 * @author VISTALL
 * @since 2025-04-12
 */
module consulo.language.copyright.impl {
    requires consulo.language.copyright.api;

    requires consulo.language.editor.ui.api;
    requires consulo.language.editor.impl;

    requires consulo.version.control.system.api;

    requires consulo.ui.ex.api;

    requires velocity.engine.core;

    // TODO remove in future!
    requires java.desktop;
    requires consulo.ui.ex.awt.api;
    requires forms.rt;

    opens consulo.language.copyright.impl.internal.pattern to velocity.engine.core;

    opens consulo.language.copyright.impl.internal.action to consulo.util.xml.serializer;
}