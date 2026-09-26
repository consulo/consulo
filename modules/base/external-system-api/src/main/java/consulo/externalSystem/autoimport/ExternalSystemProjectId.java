// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.util.io.PathUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class ExternalSystemProjectId {
    private final ProjectSystemId mySystemId;
    private final String myExternalProjectPath;
    private final String myProjectName;

    public ExternalSystemProjectId(ProjectSystemId systemId, String externalProjectPath) {
        mySystemId = systemId;
        myExternalProjectPath = externalProjectPath;
        myProjectName = PathUtil.getFileName(externalProjectPath);
    }

    public ProjectSystemId getSystemId() {
        return mySystemId;
    }

    public String getExternalProjectPath() {
        return myExternalProjectPath;
    }

    public String getProjectName() {
        return myProjectName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExternalSystemProjectId that)) {
            return false;
        }
        return mySystemId.equals(that.mySystemId) && myExternalProjectPath.equals(that.myExternalProjectPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySystemId, myExternalProjectPath);
    }

    @Override
    public String toString() {
        return mySystemId.getReadableName().get() + " (" + myProjectName + ")";
    }
}
