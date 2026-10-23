package consulo.sandboxPlugin.ide.run;

import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

public class SandExecutableConfigurationLegacyState {
    @Attribute("executable")
    public @Nullable String executable;

    @Attribute("programParameters")
    public @Nullable String programParameters;

    @Attribute("workingDirectory")
    public @Nullable String workingDirectory;

    @Attribute("passParentEnvs")
    public @Nullable String passParentEnvs;

    @Attribute("platformTarget")
    public @Nullable String platformTarget;
}
