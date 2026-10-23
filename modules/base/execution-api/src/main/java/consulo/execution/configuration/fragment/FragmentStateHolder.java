package consulo.execution.configuration.fragment;

public interface FragmentStateHolder {
    default <S> S getFragmentState(Class<S> stateClass) {
        throw new UnsupportedOperationException(getClass().getName() + " does not hold fragment states");
    }
}
