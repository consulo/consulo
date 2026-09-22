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
package consulo.externalSystem.impl.internal.util;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Unique key which encapsulates information about target ide and external projects.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2013-04-10
 */
public class IntegrationKey {
    private final String myIdeProjectName;

    private final String myIdeProjectLocationHash;

    private final ProjectSystemId myExternalSystemId;

    private final String myExternalProjectConfigPath;

    public IntegrationKey(Project ideProject, ProjectSystemId externalSystemId, String externalProjectConfigPath) {
        this(ideProject.getName(), ideProject.getLocationHash(), externalSystemId, externalProjectConfigPath);
    }

    public IntegrationKey(
        String ideProjectName,
        String ideProjectLocationHash,
        ProjectSystemId externalSystemId,
        String externalProjectConfigPath
    ) {
        myIdeProjectName = ideProjectName;
        myIdeProjectLocationHash = ideProjectLocationHash;
        myExternalSystemId = externalSystemId;
        myExternalProjectConfigPath = externalProjectConfigPath;
    }

    public String getIdeProjectName() {
        return myIdeProjectName;
    }

    public String getIdeProjectLocationHash() {
        return myIdeProjectLocationHash;
    }

    public ProjectSystemId getExternalSystemId() {
        return myExternalSystemId;
    }

    public String getExternalProjectConfigPath() {
        return myExternalProjectConfigPath;
    }

    @Override
    public int hashCode() {
        int result = myIdeProjectName.hashCode();
        result = 31 * result + myIdeProjectLocationHash.hashCode();
        result = 31 * result + myExternalSystemId.hashCode();
        result = 31 * result + myExternalProjectConfigPath.hashCode();
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

        IntegrationKey that = (IntegrationKey) o;

        return myExternalSystemId.equals(that.myExternalSystemId)
            && myIdeProjectLocationHash.equals(that.myIdeProjectLocationHash)
            && myIdeProjectName.equals(that.myIdeProjectName)
            && myExternalProjectConfigPath.equals(that.myExternalProjectConfigPath);
    }

    @Override
    public String toString() {
        return String.format("%s project '%s'", myExternalSystemId.toString().toLowerCase(), myIdeProjectName);
    }
}
