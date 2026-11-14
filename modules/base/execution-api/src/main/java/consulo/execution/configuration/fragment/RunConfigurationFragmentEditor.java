package consulo.execution.configuration.fragment;

import consulo.configurable.ConfigurationException;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * The editor of a visible {@link RunConfigurationFragment}. Created after its components exist, so it needs no nullable fields;
 * disposed through the parent passed to {@link RunConfigurationFragment#createEditor}.
 */
public interface RunConfigurationFragmentEditor<T> {
    @RequiredUIAccess
    void addRows(RunConfigurationFragmentLayout layout);

    @RequiredUIAccess
    void reset(T target);

    @RequiredUIAccess
    void apply(T target) throws ConfigurationException;

    /**
     * Called once after the editor is created, before its first {@link #reset}: the page with the other fragments, for an editor
     * which reacts to or drives them.
     */
    @RequiredUIAccess
    default void attach(RunConfigurationFragmentHost host) {
    }

    /**
     * Enables or disables the components of the editor, see {@link RunConfigurationFragmentHost#setFragmentEnabled}. The values of
     * a disabled editor are still applied.
     */
    @RequiredUIAccess
    default void setEnabled(boolean enabled) {
    }
}
