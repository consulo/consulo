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
    requires consulo.local.history.api;

    requires gnu.trove;

    opens consulo.language.index.impl.internal.stub to consulo.util.xml.serializer;
}