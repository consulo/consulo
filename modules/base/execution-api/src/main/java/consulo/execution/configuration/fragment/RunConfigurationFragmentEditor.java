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
}
