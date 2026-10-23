package consulo.sandboxPlugin.ide.run;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.xml.serializer.annotation.Attribute;

@CompiledJson
public class SandExecutableConfigurationOptions extends RunConfigurationOptions {
    static final String DEFAULT_EXECUTABLE = "/bin/echo";

    private String myExecutable = DEFAULT_EXECUTABLE;

    @Attribute("executable")
    public String getExecutable() {
        return myExecutable;
    }

    public void setExecutable(String executable) {
        myExecutable = executable.trim();
    }
}
