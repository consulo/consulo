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
package com.intellij.openapi.externalSystem.model.project;

import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author Vladislav.Soroka
 * @since 1/14/14
 */
public class ExternalModuleBuildClasspathPojo {

  @Nonnull
  private List<String> myEntries;
  @Nonnull
  private String myPath;

  @SuppressWarnings("UnusedDeclaration")
  public ExternalModuleBuildClasspathPojo() {
    // Used by IJ serialization
    this("___DUMMY___", ContainerUtil.<String>newArrayList());
  }

  public ExternalModuleBuildClasspathPojo(@Nonnull String path, @Nonnull List<String> entries) {
    myPath = path;
    myEntries = entries;
  }

  @Nonnull
  public String getPath() {
    return myPath;
  }

  public void setPath(@Nonnull String path) {
    myPath = path;
  }

  @Nonnull
  public List<String> getEntries() {
    return myEntries;
  }

  public void setEntries(@Nonnull List<String> entries) {
    myEntries = entries;
  }

  @Override
  public int hashCode() {
    int result = myEntries.hashCode();
    result = 31 * result + myPath.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalModuleBuildClasspathPojo pojo = (ExternalModuleBuildClasspathPojo)o;

    if (!myEntries.equals(pojo.myEntries)) return false;
    if (!myPath.equals(pojo.myPath)) return false;

    return true;
  }

  @Override
  public String toString() {
    return myPath;
  }
}
