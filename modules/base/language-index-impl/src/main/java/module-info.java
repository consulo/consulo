/**
 * @author VISTALL
 * @since 2025-08-20
 */
module consulo.language.index.impl {
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.impl;
    requires consulo.application.impl;
    requires consulo.project.ui.api;
    requires consulo.project.impl;
    requires consulo.local.history.api;
    requires consulo.virtual.file.system.impl;

    requires gnu.trove;
    requires it.unimi.dsi.fastutil;
    requires com.dynatrace.hash4j;

    exports consulo.language.index.impl.internal.hints;

    opens consulo.language.index.impl.internal.stub to consulo.util.xml.serializer;
    opens consulo.language.index.impl.internal.gist to consulo.application.impl;
}