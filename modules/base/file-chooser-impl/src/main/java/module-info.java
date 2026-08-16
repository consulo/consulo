import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-08-11
 */
@NullMarked
module consulo.file.chooser.impl {
    requires java.desktop;

    requires consulo.file.chooser.api;
    requires consulo.project.api;
    requires consulo.util.concurrent.coroutine;
    requires consulo.logging.api;
    requires consulo.ui.ex.api;
    requires consulo.base.icon.library;
}
