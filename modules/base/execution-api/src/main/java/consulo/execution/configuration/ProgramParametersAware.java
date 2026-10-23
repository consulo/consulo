package consulo.execution.configuration;

import consulo.execution.configuration.fragment.FragmentStateHolder;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import org.jspecify.annotations.Nullable;

public interface ProgramParametersAware extends FragmentStateHolder {
    default @Nullable String getProgramParameters() {
        return getFragmentState(ProgramParametersFragmentState.class).getValue();
    }

    default void setProgramParameters(@Nullable String value) {
        getFragmentState(ProgramParametersFragmentState.class).setValue(value);
    }
}
