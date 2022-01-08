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
package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 5/18/13 10:51 PM
 */
public class ExternalProjectPojo implements Comparable<ExternalProjectPojo> {

  @Nonnull
  private String myName;
  @Nonnull
  private String myPath;

  @SuppressWarnings("UnusedDeclaration")
  public ExternalProjectPojo() {
    // Used by IJ serialization
    this("___DUMMY___", "___DUMMY___");
  }

  public ExternalProjectPojo(@Nonnull String name, @Nonnull String path) {
    myName = name;
    myPath = path;
  }

  @Nonnull
  public static <T extends Named & ExternalConfigPathAware & Identifiable> ExternalProjectPojo from(@Nonnull T data) {
    String projectUniqueName = StringUtil.isEmpty(data.getId()) ? data.getExternalName() : data.getId();
    return new ExternalProjectPojo(projectUniqueName, data.getLinkedExternalProjectPath());
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Nonnull
  public String getPath() {
    return myPath;
  }

  public void setPath(@Nonnull String path) {
    myPath = path;
  }

  @Override
  public int compareTo(@Nonnull ExternalProjectPojo that) {
    return myName.compareTo(that.myName);
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myPath.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalProjectPojo pojo = (ExternalProjectPojo)o;

    if (!myName.equals(pojo.myName)) return false;
    if (!myPath.equals(pojo.myPath)) return false;

    return true;
  }

  @Override
  public String toString() {
    return myName;
  }
}
