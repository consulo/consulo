/**
 * @author VISTALL
 * @since 2022-03-23
 */
module consulo.language.editor.impl {
    requires consulo.mcp.server.api;
    // TODO remove this dependency in future
    requires java.desktop;
    requires forms.rt;

    requires transitive consulo.language.editor.api;
    requires transitive consulo.file.template.api;

    requires consulo.external.service.api;
    requires consulo.undo.redo.api;
    requires consulo.application.impl;
    requires consulo.project.impl;
    requires consulo.language.editor.ui.api;
    requires consulo.code.editor.impl;

    requires consulo.language.impl;
    requires consulo.navigation.api;

    exports consulo.language.editor.impl.action;
    exports consulo.language.editor.impl.codeVision;
    exports consulo.language.editor.impl.intention;
    exports consulo.language.editor.impl.highlight;
    exports consulo.language.editor.impl.inspection;
    exports consulo.language.editor.impl.inspection.reference;

    exports consulo.language.editor.impl.internal.action to consulo.ide.impl;
    exports consulo.language.editor.impl.internal to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.daemon to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.completion to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;
    exports consulo.language.editor.impl.internal.completion.lookup to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;
    exports consulo.language.editor.impl.internal.intention to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.ide.impl,
        consulo.ide.impl,
        consulo.web.editor.impl;
    exports consulo.language.editor.impl.internal.parser to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.inspection.scheme to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.inspection to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.psi.path to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.template to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.highlight to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.rawHighlight to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.markup to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.desktop.qt.editor.impl,
        consulo.desktop.qt.ide.impl,
        consulo.desktop.qt.ui.impl,
        consulo.file.editor.impl,
        consulo.ide.impl,
        consulo.version.control.system.impl,
        consulo.web.editor.impl,
        consulo.web.ide,
        consulo.web.ui.impl;
    exports consulo.language.editor.impl.internal.hint to
        consulo.desktop.awt.editor.impl,
        consulo.desktop.awt.ide.impl,
        consulo.desktop.awt.ui.impl,
        consulo.ide.impl;

    exports consulo.language.editor.impl.internal.inlay to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.inlay.param to consulo.ide.impl;
    exports consulo.language.editor.impl.internal.inlay.setting to consulo.ide.impl;

    opens consulo.language.editor.impl.internal.inlay.setting to consulo.util.xml.serializer;
    opens consulo.language.editor.impl.internal.readerMode to consulo.util.xml.serializer;
    opens consulo.language.editor.impl.internal.template to
        consulo.application.impl,
        consulo.util.xml.serializer;
    opens consulo.language.editor.impl.internal.inspection.scheme to
        consulo.component.impl,
        consulo.util.xml.serializer;
    // listener calls via messagebus
    opens consulo.language.editor.impl.internal.rawHighlight to consulo.compiler.impl;
}