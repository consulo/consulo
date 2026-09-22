/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.model.execution;

import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Keeps external system task execution parameters. Basically, this is a model class which holds data represented when
 * a user opens run configuration editor for corresponding external system.
 *
 * @author Denis Zhdanov
 * @since 2013-05-24
 */
@Tag("ExternalSystemSettings")
public class ExternalSystemTaskExecutionSettings implements Cloneable {
    public static final String TAG_NAME = "ExternalSystemSettings";

    private List<String> myTaskNames = new ArrayList<>();
    private List<String> myTaskDescriptions = new ArrayList<>();

    private @Nullable String myExecutionName;
    private String myExternalSystemIdString;
    private String myExternalProjectPath;
    private String myVmOptions;
    private String myScriptParameters;

    public @Nullable String getExecutionName() {
        return myExecutionName;
    }

    public void setExecutionName(@Nullable String executionName) {
        myExecutionName = executionName;
    }

    public String getExternalSystemIdString() {
        return myExternalSystemIdString;
    }

    public void setExternalSystemIdString(String externalSystemIdString) {
        myExternalSystemIdString = externalSystemIdString;
    }

    public String getExternalProjectPath() {
        return myExternalProjectPath;
    }

    public void setExternalProjectPath(String externalProjectPath) {
        myExternalProjectPath = externalProjectPath;
    }

    public String getVmOptions() {
        return myVmOptions;
    }

    public void setVmOptions(String vmOptions) {
        myVmOptions = vmOptions;
    }

    public String getScriptParameters() {
        return myScriptParameters;
    }

    public void setScriptParameters(String scriptParameters) {
        myScriptParameters = scriptParameters;
    }

    public List<String> getTaskNames() {
        return myTaskNames;
    }

    public void setTaskNames(List<String> taskNames) {
        myTaskNames = taskNames;
    }

    public List<String> getTaskDescriptions() {
        return myTaskDescriptions;
    }

    public void setTaskDescriptions(List<String> taskDescriptions) {
        myTaskDescriptions = taskDescriptions;
    }

    @Override
    public ExternalSystemTaskExecutionSettings clone() {
        ExternalSystemTaskExecutionSettings result = new ExternalSystemTaskExecutionSettings();
        result.setExecutionName(getExecutionName());
        result.setExternalSystemIdString(getExternalSystemIdString());
        result.setExternalProjectPath(getExternalProjectPath());
        result.setVmOptions(getVmOptions());
        result.setScriptParameters(getScriptParameters());
        result.setTaskNames(new ArrayList<>(getTaskNames()));
        result.setTaskDescriptions(new ArrayList<>(getTaskDescriptions()));
        return result;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(myTaskNames);
        result = 31 * result + Objects.hashCode(myExecutionName);
        result = 31 * result + Objects.hashCode(myExternalSystemIdString);
        result = 31 * result + Objects.hashCode(myExternalProjectPath);
        result = 31 * result + Boolean.hashCode(StringUtil.isEmpty(myVmOptions));
        result = 31 * result + Boolean.hashCode(StringUtil.isEmpty(myScriptParameters));
        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExternalSystemTaskExecutionSettings that = (ExternalSystemTaskExecutionSettings) o;

        return Objects.equals(myExecutionName, that.myExecutionName)
            && Objects.equals(myExternalProjectPath, that.myExternalProjectPath)
            && Objects.equals(myExternalSystemIdString, that.myExternalSystemIdString)
            && Objects.equals(myTaskNames, that.myTaskNames)
            && StringUtil.isEmpty(myVmOptions) == StringUtil.isEmpty(that.myVmOptions)
            && StringUtil.isEmpty(myScriptParameters) == StringUtil.isEmpty(that.myScriptParameters);
    }
}
