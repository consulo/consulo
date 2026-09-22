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
package consulo.externalSystem.model.project;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 2014-01-14
 */
public class ExternalProjectBuildClasspathPojo {
    /**
     * Common for all project modules build classpath. E.g. it can be build system SDK libraries, configured at project level.
     */
    private List<String> myProjectBuildClasspath;

    private Map<String, ExternalModuleBuildClasspathPojo> myModulesBuildClasspath;

    private String myName;

    @SuppressWarnings("UnusedDeclaration")
    public ExternalProjectBuildClasspathPojo() {
        // Used by IJ serialization
        this("___DUMMY___", new ArrayList<>(), new HashMap<>());
    }

    public ExternalProjectBuildClasspathPojo(
        String name,
        List<String> projectBuildClasspath,
        Map<String, ExternalModuleBuildClasspathPojo> modulesBuildClasspath
    ) {
        myName = name;
        myProjectBuildClasspath = projectBuildClasspath;
        myModulesBuildClasspath = modulesBuildClasspath;
    }

    public String getName() {
        return myName;
    }

    public void setName(String name) {
        myName = name;
    }

    public Map<String, ExternalModuleBuildClasspathPojo> getModulesBuildClasspath() {
        return myModulesBuildClasspath;
    }

    public void setModulesBuildClasspath(Map<String, ExternalModuleBuildClasspathPojo> modulesBuildClasspath) {
        myModulesBuildClasspath = modulesBuildClasspath;
    }

    public List<String> getProjectBuildClasspath() {
        return myProjectBuildClasspath;
    }

    public void setProjectBuildClasspath(List<String> projectBuildClasspath) {
        myProjectBuildClasspath = projectBuildClasspath;
    }

    @Override
    public int hashCode() {
        return 31 * myModulesBuildClasspath.hashCode() + myName.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExternalProjectBuildClasspathPojo that = (ExternalProjectBuildClasspathPojo) o;

        return myModulesBuildClasspath.equals(that.myModulesBuildClasspath)
            && myName.equals(that.myName);
    }

    @Override
    public String toString() {
        return myName;
    }
}
