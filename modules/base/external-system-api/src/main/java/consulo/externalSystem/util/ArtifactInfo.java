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
package consulo.externalSystem.util;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Denis Zhdanov
 * @since 2013-01-16
 */
public class ArtifactInfo {
    private final @Nullable String myName;
    private final @Nullable String myGroup;
    private final @Nullable String myVersion;

    public ArtifactInfo(@Nullable String name, @Nullable String group, @Nullable String version) {
        assert name != null || group != null || version != null;
        myName = name;
        myGroup = group;
        myVersion = version;
    }

    public @Nullable String getName() {
        return myName;
    }

    // Commented to apply to green code policy. Un-comment if required.

    //public @Nullable String getGroup() {
    //    return myGroup;
    //}

    public @Nullable String getVersion() {
        return myVersion;
    }

    @Override
    public int hashCode() {
        int result = 31 * Objects.hashCode(myName) + Objects.hashCode(myGroup);
        return 31 * result + Objects.hashCode(myVersion);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtifactInfo info = (ArtifactInfo) o;

        return Objects.equals(myGroup, info.myGroup)
            && Objects.equals(myName, info.myName)
            && Objects.equals(myVersion, info.myVersion);
    }

    @Override
    public String toString() {
        return String.format(
            "%s:%s:%s",
            myName == null ? "<no-name>" : myName,
            myGroup == null ? "<no-group>" : myGroup,
            myVersion == null ? "<no-version>" : myVersion
        );
    }
}
