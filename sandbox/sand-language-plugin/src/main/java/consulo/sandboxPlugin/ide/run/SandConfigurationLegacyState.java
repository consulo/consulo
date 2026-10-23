package consulo.sandboxPlugin.ide.run;

import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

public class SandConfigurationLegacyState {
    @Attribute("programParameters")
    public @Nullable String programParameters;

    @Attribute("workingDirectory")
    public @Nullable String workingDirectory;

    @Attribute("entryPoint")
    public @Nullable String entryPoint;

    @Attribute("expression")
    public @Nullable String expression;

    @Attribute("profile")
    public @Nullable String profile;

    @Attribute("tags")
    public @Nullable String tags;

    @Attribute("passParentEnvs")
    public @Nullable String passParentEnvs;
}
