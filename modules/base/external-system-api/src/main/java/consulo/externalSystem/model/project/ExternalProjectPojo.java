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
package consulo.externalSystem.model.project;

import consulo.externalSystem.service.project.ExternalConfigPathAware;
import consulo.externalSystem.service.project.Identifiable;
import consulo.externalSystem.service.project.Named;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Denis Zhdanov
 * @since 2013-05-18
 */
public class ExternalProjectPojo implements Comparable<ExternalProjectPojo> {
    private String myName;

    private String myPath;

    @SuppressWarnings("UnusedDeclaration")
    public ExternalProjectPojo() {
        // Used by IJ serialization
        this("___DUMMY___", "___DUMMY___");
    }

    public ExternalProjectPojo(String name, String path) {
        myName = name;
        myPath = path;
    }

    public static <T extends Named & ExternalConfigPathAware & Identifiable> ExternalProjectPojo from(T data) {
        String projectUniqueName = StringUtil.isEmpty(data.getId()) ? data.getExternalName() : data.getId();
        return new ExternalProjectPojo(projectUniqueName, data.getLinkedExternalProjectPath());
    }

    public String getName() {
        return myName;
    }

    public void setName(String name) {
        myName = name;
    }

    public String getPath() {
        return myPath;
    }

    public void setPath(String path) {
        myPath = path;
    }

    @Override
    public int compareTo(ExternalProjectPojo that) {
        return myName.compareTo(that.myName);
    }

    @Override
    public int hashCode() {
        return 31 * myName.hashCode() + myPath.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExternalProjectPojo that = (ExternalProjectPojo) o;

        return myName.equals(that.myName)
            && myPath.equals(that.myPath);
    }

    @Override
    public String toString() {
        return myName;
    }
}
