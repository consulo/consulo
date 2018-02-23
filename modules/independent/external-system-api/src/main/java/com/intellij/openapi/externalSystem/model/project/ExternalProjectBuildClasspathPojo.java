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
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 1/14/14
 */
public class ExternalProjectBuildClasspathPojo {

  /**
   * Common for all project modules build classpath. E.g. it can be build system SDK libraries, configured at project level.
   */
  @Nonnull
  private List<String> myProjectBuildClasspath;
  @Nonnull
  private Map<String, ExternalModuleBuildClasspathPojo> myModulesBuildClasspath;
  @Nonnull
  private String myName;

  @SuppressWarnings("UnusedDeclaration")
  public ExternalProjectBuildClasspathPojo() {
    // Used by IJ serialization
    this("___DUMMY___", ContainerUtil.<String>newArrayList(), ContainerUtil.<String, ExternalModuleBuildClasspathPojo>newHashMap());
  }

  public ExternalProjectBuildClasspathPojo(@Nonnull String name,
                                           @Nonnull List<String> projectBuildClasspath,
                                           @Nonnull Map<String, ExternalModuleBuildClasspathPojo> modulesBuildClasspath) {
    myName = name;
    myProjectBuildClasspath = projectBuildClasspath;
    myModulesBuildClasspath = modulesBuildClasspath;
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Nonnull
  public Map<String, ExternalModuleBuildClasspathPojo> getModulesBuildClasspath() {
    return myModulesBuildClasspath;
  }

  public void setModulesBuildClasspath(@Nonnull Map<String, ExternalModuleBuildClasspathPojo> modulesBuildClasspath) {
    myModulesBuildClasspath = modulesBuildClasspath;
  }

  @Nonnull
  public List<String> getProjectBuildClasspath() {
    return myProjectBuildClasspath;
  }

  public void setProjectBuildClasspath(@Nonnull List<String> projectBuildClasspath) {
    myProjectBuildClasspath = projectBuildClasspath;
  }

  @Override
  public int hashCode() {
    int result = myModulesBuildClasspath.hashCode();
    result = 31 * result + myModulesBuildClasspath.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalProjectBuildClasspathPojo pojo = (ExternalProjectBuildClasspathPojo)o;

    if (!myModulesBuildClasspath.equals(pojo.myModulesBuildClasspath)) return false;
    if (!myName.equals(pojo.myName)) return false;

    return true;
  }

  @Override
  public String toString() {
    return myName;
  }
}