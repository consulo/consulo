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
package com.intellij.openapi.externalSystem.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

/**
 * Unique key which encapsulates information about target ide and external projects.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 4/10/13 11:39 AM
 */
public class IntegrationKey {

  @Nonnull
  private final String          myIdeProjectName;
  @Nonnull
  private final String          myIdeProjectLocationHash;
  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nonnull
  private final String          myExternalProjectConfigPath;

  public IntegrationKey(@Nonnull Project ideProject, @Nonnull ProjectSystemId externalSystemId, @Nonnull String externalProjectConfigPath) {
    this(ideProject.getName(), ideProject.getLocationHash(), externalSystemId, externalProjectConfigPath);
  }

  public IntegrationKey(@Nonnull String ideProjectName,
                        @Nonnull String ideProjectLocationHash,
                        @Nonnull ProjectSystemId externalSystemId,
                        @Nonnull String externalProjectConfigPath)
  {
    myIdeProjectName = ideProjectName;
    myIdeProjectLocationHash = ideProjectLocationHash;
    myExternalSystemId = externalSystemId;
    myExternalProjectConfigPath = externalProjectConfigPath;
  }

  @Nonnull
  public String getIdeProjectName() {
    return myIdeProjectName;
  }

  @Nonnull
  public String getIdeProjectLocationHash() {
    return myIdeProjectLocationHash;
  }

  @Nonnull
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  @Nonnull
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IntegrationKey key = (IntegrationKey)o;

    if (!myExternalSystemId.equals(key.myExternalSystemId)) return false;
    if (!myIdeProjectLocationHash.equals(key.myIdeProjectLocationHash)) return false;
    if (!myIdeProjectName.equals(key.myIdeProjectName)) return false;
    if (!myExternalProjectConfigPath.equals(key.myExternalProjectConfigPath)) return false;

    return true;
  }

  @Override
  public String toString() {
    return String.format("%s project '%s'", myExternalSystemId.toString().toLowerCase(), myIdeProjectName);
  }
}
