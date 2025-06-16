/**
 * @author VISTALL
 * @since 2025-06-15
 */
module consulo.language.code.style.impl {
    requires consulo.language.code.style.api;
    requires consulo.language.code.style.ui.api;

    requires consulo.language.editor.api;

    requires consulo.language.inject.impl;
    requires consulo.language.impl;

    requires consulo.code.editor.api;

    requires consulo.ui.ex.api;

    // TODO we need remove this
    requires java.desktop;

    // TODO remove this after all code migrated
    exports consulo.language.codeStyle.impl.internal to consulo.ide.impl;
    exports consulo.language.codeStyle.impl.internal.formatting to consulo.ide.impl;
    exports consulo.language.codeStyle.impl.internal.arrangement to consulo.ide.impl;

    opens consulo.language.codeStyle.impl.internal to consulo.util.xml.serializer;
}