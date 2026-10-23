package consulo.execution.configuration;

import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.FragmentStateHolder;

import java.util.Map;

public interface EnvironmentAware extends FragmentStateHolder {
    default Map<String, String> getEnvs() {
        return getFragmentState(EnvironmentFragmentState.class).getEnvs();
    }

    default void setEnvs(Map<String, String> envs) {
        getFragmentState(EnvironmentFragmentState.class).setEnvs(envs);
    }

    default boolean isPassParentEnvs() {
        return getFragmentState(EnvironmentFragmentState.class).isPassParentEnvs();
    }

    default void setPassParentEnvs(boolean passParentEnvs) {
        getFragmentState(EnvironmentFragmentState.class).setPassParentEnvs(passParentEnvs);
    }
}
