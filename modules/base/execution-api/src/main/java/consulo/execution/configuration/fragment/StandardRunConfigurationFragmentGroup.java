package consulo.execution.configuration.fragment;

/**
 * Ids of the platform {@link RunConfigurationFragmentType#GROUP} fragments, for {@link RunConfigurationFragment#getParentId()}.
 */
public final class StandardRunConfigurationFragmentGroup {
    public static final String LOGS = "log";
    public static final String COVERAGE = "coverage";

    private StandardRunConfigurationFragmentGroup() {
    }
}
