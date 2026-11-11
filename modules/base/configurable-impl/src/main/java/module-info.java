import org.jspecify.annotations.NullMarked;

/**
 * The settings search - the index of the settings pages and the options switched from Find Action.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@NullMarked
module consulo.configurable.impl {
    requires consulo.configurable.api;
    requires consulo.ui.ex.api;
    requires consulo.util.interner;
    requires com.ibm.icu;
}
