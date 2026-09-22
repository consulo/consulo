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
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 * @since 2014-05-30
 */
public class ExternalSystemBeforeRunTask extends BeforeRunTask<ExternalSystemBeforeRunTask> {
    private final ExternalSystemTaskExecutionSettings myTaskExecutionSettings;

    public ExternalSystemBeforeRunTask(Key<ExternalSystemBeforeRunTask> providerId, ProjectSystemId systemId) {
        super(providerId);
        myTaskExecutionSettings = new ExternalSystemTaskExecutionSettings();
        myTaskExecutionSettings.setExternalSystemIdString(systemId.getId());
    }

    public ExternalSystemTaskExecutionSettings getTaskExecutionSettings() {
        return myTaskExecutionSettings;
    }

    @Override
    public void writeExternal(Element element) {
        super.writeExternal(element);

        element.setAttribute("tasks", StringUtil.join(myTaskExecutionSettings.getTaskNames(), " "));
        if (myTaskExecutionSettings.getExternalProjectPath() != null) {
            element.setAttribute("externalProjectPath", myTaskExecutionSettings.getExternalProjectPath());
        }
        if (myTaskExecutionSettings.getVmOptions() != null) {
            element.setAttribute("vmOptions", myTaskExecutionSettings.getVmOptions());
        }
        if (myTaskExecutionSettings.getScriptParameters() != null) {
            element.setAttribute("scriptParameters", myTaskExecutionSettings.getScriptParameters());
        }
    }

    @Override
    public void readExternal(Element element) {
        super.readExternal(element);
        myTaskExecutionSettings.setTaskNames(StringUtil.split(StringUtil.notNullize(element.getAttributeValue("tasks")), " "));
        myTaskExecutionSettings.setExternalProjectPath(element.getAttributeValue("externalProjectPath"));
        myTaskExecutionSettings.setVmOptions(element.getAttributeValue("vmOptions"));
        myTaskExecutionSettings.setScriptParameters(element.getAttributeValue("scriptParameters"));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ExternalSystemBeforeRunTask that
            && super.equals(o)
            && myTaskExecutionSettings.equals(that.myTaskExecutionSettings);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + myTaskExecutionSettings.hashCode();
    }
}
