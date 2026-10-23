package consulo.execution.configuration.fragment;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Collects the components of a {@link RunConfigurationFragmentEditor}; the host decides where they are placed.
 */
public interface RunConfigurationFragmentLayout {
    /**
     * A row with a label, aligned with the labels of every other fragment.
     */
    @RequiredUIAccess
    void addLabeled(LocalizeValue label, Component component);

    /**
     * A row without a label, aligned with the inputs.
     */
    @RequiredUIAccess
    void addUnlabeled(Component component);

    /**
     * A full width component placed after the rows.
     */
    @RequiredUIAccess
    void addBottom(Component component);
}
