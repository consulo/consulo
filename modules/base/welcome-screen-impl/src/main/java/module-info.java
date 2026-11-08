import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2025-09-17
 */
@NullMarked
module consulo.welcome.screen.impl {
    requires consulo.project.api;
    requires consulo.configuration.editor.api;
    requires consulo.file.editor.api;
    requires consulo.ide.impl;
    requires consulo.welcome.screen.api;
    requires consulo.module.creation.impl;
    requires consulo.ui.impl;
    requires consulo.ui.ex.impl;
    requires consulo.base.localize.library;
}
