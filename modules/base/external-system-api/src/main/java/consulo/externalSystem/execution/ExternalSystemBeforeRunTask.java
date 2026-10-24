/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.externalSystem.execution;

import consulo.execution.BeforeRunTask;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * Before-run task of an external system. Each system subclasses it, so the owning
 * {@link ExternalSystemBeforeRunTaskProvider} is found by the task's class.
 *
 * @author Vladislav.Soroka
 * @since 2014-05-30
 */
public abstract class ExternalSystemBeforeRunTask extends BeforeRunTask {
    private @Nullable String mySystemId;
    private @Nullable String myTasks;
    private @Nullable String myExternalProjectPath;
    private @Nullable String myVmOptions;
    private @Nullable String myScriptParameters;

    @Attribute("systemId")
    public @Nullable String getSystemId() {
        return mySystemId;
    }

    public void setSystemId(@Nullable String systemId) {
        mySystemId = systemId;
    }

    @Attribute("tasks")
    public @Nullable String getTasks() {
        return myTasks;
    }

    public void setTasks(@Nullable String tasks) {
        myTasks = StringUtil.nullize(tasks, true);
    }

    @Attribute("externalProjectPath")
    public @Nullable String getExternalProjectPath() {
        return myExternalProjectPath;
    }

    public void setExternalProjectPath(@Nullable String externalProjectPath) {
        myExternalProjectPath = externalProjectPath;
    }

    @Attribute("vmOptions")
    public @Nullable String getVmOptions() {
        return myVmOptions;
    }

    public void setVmOptions(@Nullable String vmOptions) {
        myVmOptions = vmOptions;
    }

    @Attribute("scriptParameters")
    public @Nullable String getScriptParameters() {
        return myScriptParameters;
    }

    public void setScriptParameters(@Nullable String scriptParameters) {
        myScriptParameters = scriptParameters;
    }

    /**
     * A new settings object with this task's values.
     */
    public ExternalSystemTaskExecutionSettings createTaskExecutionSettings() {
        ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
        settings.setExternalSystemIdString(StringUtil.notNullize(mySystemId));
        settings.setTaskNames(StringUtil.split(StringUtil.notNullize(myTasks), " "));
        settings.setExternalProjectPath(myExternalProjectPath);
        settings.setVmOptions(myVmOptions);
        settings.setScriptParameters(myScriptParameters);
        return settings;
    }

    public void applyTaskExecutionSettings(ExternalSystemTaskExecutionSettings settings) {
        setTasks(StringUtil.join(settings.getTaskNames(), " "));
        setExternalProjectPath(settings.getExternalProjectPath());
        setVmOptions(settings.getVmOptions());
        setScriptParameters(settings.getScriptParameters());
    }
}
