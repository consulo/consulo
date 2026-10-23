package consulo.execution.configuration;

import consulo.execution.configuration.fragment.FragmentStateHolder;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import org.jspecify.annotations.Nullable;

public interface WorkingDirectoryAware extends FragmentStateHolder {
    default @Nullable String getWorkingDirectory() {
        return getFragmentState(WorkingDirectoryFragmentState.class).getPath();
    }

    default void setWorkingDirectory(@Nullable String value) {
        getFragmentState(WorkingDirectoryFragmentState.class).setPath(value);
    }
}
