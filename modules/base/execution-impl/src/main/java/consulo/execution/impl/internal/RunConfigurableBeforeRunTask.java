package consulo.execution.impl.internal;

import com.dslplatform.json.CompiledJson;
import consulo.execution.BeforeRunTask;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

@CompiledJson
public class RunConfigurableBeforeRunTask extends BeforeRunTask {
    private @Nullable String myConfigurationName;
    private @Nullable String myConfigurationType;

    @Attribute("configurationName")
    public @Nullable String getConfigurationName() {
        return myConfigurationName;
    }

    public void setConfigurationName(@Nullable String configurationName) {
        myConfigurationName = StringUtil.nullize(configurationName);
    }

    @Attribute("configurationType")
    public @Nullable String getConfigurationType() {
        return myConfigurationType;
    }

    public void setConfigurationType(@Nullable String configurationType) {
        myConfigurationType = StringUtil.nullize(configurationType);
    }

    public void setSettings(@Nullable RunnerAndConfigurationSettings settings) {
        ConfigurationType type = settings == null ? null : settings.getType();
        setConfigurationName(settings == null ? null : settings.getName());
        setConfigurationType(type == null ? null : type.getId());
    }

    public boolean isFor(RunnerAndConfigurationSettings settings) {
        ConfigurationType type = settings.getType();
        return type != null && settings.getName().equals(myConfigurationName) && type.getId().equals(myConfigurationType);
    }
}
