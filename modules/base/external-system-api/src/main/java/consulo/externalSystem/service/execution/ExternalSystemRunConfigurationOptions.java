package consulo.externalSystem.service.execution;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * State of an {@link ExternalSystemRunConfiguration}; the external system itself is given by the configuration type.
 */
@CompiledJson
public class ExternalSystemRunConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myExternalProjectPath;
    private List<String> myTaskNames = new ArrayList<>();
    private List<String> myTaskDescriptions = new ArrayList<>();
    private @Nullable String myExecutionName;
    private @Nullable String myVmOptions;
    private @Nullable String myScriptParameters;

    @Attribute("externalProjectPath")
    public @Nullable String getExternalProjectPath() {
        return myExternalProjectPath;
    }

    public void setExternalProjectPath(@Nullable String externalProjectPath) {
        myExternalProjectPath = StringUtil.nullize(externalProjectPath);
    }

    @Tag("tasks")
    @AbstractCollection(surroundWithTag = false, elementTag = "task", elementValueAttribute = "name")
    public List<String> getTaskNames() {
        return myTaskNames;
    }

    public void setTaskNames(List<String> taskNames) {
        myTaskNames = new ArrayList<>(taskNames);
    }

    @Tag("taskDescriptions")
    @AbstractCollection(surroundWithTag = false, elementTag = "description", elementValueAttribute = "value")
    public List<String> getTaskDescriptions() {
        return myTaskDescriptions;
    }

    public void setTaskDescriptions(List<String> taskDescriptions) {
        myTaskDescriptions = new ArrayList<>(taskDescriptions);
    }

    @Attribute("executionName")
    public @Nullable String getExecutionName() {
        return myExecutionName;
    }

    public void setExecutionName(@Nullable String executionName) {
        myExecutionName = StringUtil.nullize(executionName);
    }

    @Attribute("vmOptions")
    public @Nullable String getVmOptions() {
        return myVmOptions;
    }

    public void setVmOptions(@Nullable String vmOptions) {
        myVmOptions = StringUtil.nullize(vmOptions);
    }

    @Attribute("scriptParameters")
    public @Nullable String getScriptParameters() {
        return myScriptParameters;
    }

    public void setScriptParameters(@Nullable String scriptParameters) {
        myScriptParameters = StringUtil.nullize(scriptParameters);
    }

    public void copyFrom(ExternalSystemTaskExecutionSettings settings) {
        setExternalProjectPath(settings.getExternalProjectPath());
        setTaskNames(settings.getTaskNames());
        setTaskDescriptions(settings.getTaskDescriptions());
        setExecutionName(settings.getExecutionName());
        setVmOptions(settings.getVmOptions());
        setScriptParameters(settings.getScriptParameters());
    }

    public void copyTo(ExternalSystemTaskExecutionSettings settings) {
        settings.setExternalProjectPath(getExternalProjectPath());
        settings.setTaskNames(new ArrayList<>(getTaskNames()));
        settings.setTaskDescriptions(new ArrayList<>(getTaskDescriptions()));
        settings.setExecutionName(getExecutionName());
        settings.setVmOptions(getVmOptions());
        settings.setScriptParameters(getScriptParameters());
    }
}
