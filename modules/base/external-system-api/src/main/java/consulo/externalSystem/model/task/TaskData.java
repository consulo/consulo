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
package consulo.externalSystem.model.task;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.AbstractExternalEntityData;
import consulo.externalSystem.service.project.ExternalConfigPathAware;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Container for external system task information.
 * 
 * @author Denis Zhdanov
 * @since 2013-05-15
 */
public class TaskData extends AbstractExternalEntityData implements ExternalConfigPathAware, Comparable<TaskData> {
  private static final long serialVersionUID = 1L;

  private final String myName;
  
  private final String myLinkedExternalProjectPath;

  private final @Nullable String myDescription;
  private @Nullable String myGroup;
  private boolean myInherited;
  private boolean myTest;

  public TaskData(ProjectSystemId owner, String name, String path, @Nullable String description) {
    super(owner);
    myName = name;
    myLinkedExternalProjectPath = path;
    myDescription = description;
  }

  public String getName() {
    return myName;
  }

  @Override
  public String getLinkedExternalProjectPath() {
    return myLinkedExternalProjectPath;
  }

  public @Nullable String getDescription() {
    return myDescription;
  }

  public @Nullable String getGroup() {
    return myGroup;
  }

  public void setGroup(@Nullable String group) {
    myGroup = group;
  }

  /** Whether this task is inherited from a parent project (not defined locally). */
  public boolean isInherited() {
    return myInherited;
  }

  public void setInherited(boolean inherited) {
    myInherited = inherited;
  }

  public boolean isTest() {
    return myTest;
  }

  public void setTest(boolean test) {
    myTest = test;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myName.hashCode();
    result = 31 * result + myLinkedExternalProjectPath.hashCode();
    result = 31 * result + Objects.hashCode(myDescription);
    return result;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    TaskData that = (TaskData)o;

    return Objects.equals(myDescription, that.myDescription)
      && myLinkedExternalProjectPath.equals(that.myLinkedExternalProjectPath)
      && myName.equals(that.myName);
  }

  @Override
  public int compareTo(TaskData that) {
    return myName.compareTo(that.getName());
  }

  @Override
  public String toString() {
    return myName;
  }
}
