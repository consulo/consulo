package consulo.execution.configuration.fragment;

/**
 * An on/off option: shown as a chip while on, removing the chip switches it off.
 */
public interface TagRunConfigurationFragment<T> extends RunConfigurationFragment<T> {
    boolean isSet(T target);

    void set(T target, boolean value);

    @Override
    default RunConfigurationFragmentType getType() {
        return RunConfigurationFragmentType.TAG;
    }

    @Override
    default boolean isVisibleByDefault(T target) {
        return isSet(target);
    }
}
