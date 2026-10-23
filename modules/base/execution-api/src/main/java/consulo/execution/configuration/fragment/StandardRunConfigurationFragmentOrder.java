package consulo.execution.configuration.fragment;

/**
 * Positions of the platform fragments; plugins place theirs relative to these (e.g. {@code ENVIRONMENT + 10}).
 */
public final class StandardRunConfigurationFragmentOrder {
    /**
     * Negative orders place a shared fragment before the configuration type's own fragments (e.g. "Run on").
     */
    public static final int BEFORE_TYPE = -100;
    public static final int PLATFORM_TARGET = BEFORE_TYPE;
    public static final int FIRST = 0;
    public static final int PROGRAM_PARAMETERS = 100;
    public static final int WORKING_DIRECTORY = 200;
    public static final int ENVIRONMENT = 300;
    public static final int MODULE = 350;
    public static final int REDIRECT_INPUT = 400;
    public static final int COVERAGE = 800;
    public static final int LOGS = 900;
    public static final int LAST = 1000;

    private StandardRunConfigurationFragmentOrder() {
    }
}
