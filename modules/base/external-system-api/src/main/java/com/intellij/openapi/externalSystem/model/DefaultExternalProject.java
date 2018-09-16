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
package com.intellij.openapi.externalSystem.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 7/14/2014
 */
public class DefaultExternalProject implements ExternalProject {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private String myName;
  @Nonnull
  private String myQName;
  @Nullable
  private String myDescription;
  @Nonnull
  private String myGroup;
  @Nonnull
  private String myVersion;
  @Nonnull
  private Map<String, ExternalProject> myChildProjects;
  @Nonnull
  private File myProjectDir;
  @Nonnull
  private File myBuildDir;
  @Nullable
  private File myBuildFile;
  @Nonnull
  private Map<String, ExternalTask> myTasks;
  @Nonnull
  private Map<String, ?> myProperties;
  @Nonnull
  private Map<String, ExternalSourceSet> mySourceSets;
  @Nonnull
  private String myExternalSystemId;
  @Nonnull
  private Map<String, ExternalPlugin> myPlugins;

  public DefaultExternalProject() {
    myChildProjects = new HashMap<String, ExternalProject>();
    myTasks = new HashMap<String, ExternalTask>();
    myProperties = new HashMap<String, Object>();
    mySourceSets = new HashMap<String, ExternalSourceSet>();
    myPlugins = new HashMap<String, ExternalPlugin>();
  }

  public DefaultExternalProject(@Nonnull ExternalProject externalProject) {
    this();
    myName = externalProject.getName();
    myQName = externalProject.getQName();
    myVersion = externalProject.getVersion();
    myGroup = externalProject.getGroup();
    myDescription = externalProject.getDescription();
    myProjectDir = externalProject.getProjectDir();
    myBuildDir = externalProject.getBuildDir();
    myBuildFile = externalProject.getBuildFile();
    myExternalSystemId = externalProject.getExternalSystemId();

    for (Map.Entry<String, ExternalProject> entry : externalProject.getChildProjects().entrySet()) {
      myChildProjects.put(entry.getKey(), new DefaultExternalProject(entry.getValue()));
    }

    for (Map.Entry<String, ExternalTask> entry : externalProject.getTasks().entrySet()) {
      myTasks.put(entry.getKey(), new DefaultExternalTask(entry.getValue()));
    }
    for (Map.Entry<String, ExternalSourceSet> entry : externalProject.getSourceSets().entrySet()) {
      mySourceSets.put(entry.getKey(), new DefaultExternalSourceSet(entry.getValue()));
    }
    for (Map.Entry<String, ExternalPlugin> entry : externalProject.getPlugins().entrySet()) {
      myPlugins.put(entry.getKey(), new DefaultExternalPlugin(entry.getValue()));
    }
  }


  @Nonnull
  @Override
  public String getExternalSystemId() {
    return myExternalSystemId;
  }

  public void setExternalSystemId(@Nonnull String externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Nonnull
  @Override
  public String getQName() {
    return myQName;
  }

  public void setQName(@Nonnull String QName) {
    myQName = QName;
  }

  @Nullable
  @Override
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(@Nullable String description) {
    myDescription = description;
  }

  @Nonnull
  @Override
  public String getGroup() {
    return myGroup;
  }

  public void setGroup(@Nonnull String group) {
    myGroup = group;
  }

  @Nonnull
  @Override
  public String getVersion() {
    return myVersion;
  }

  public void setVersion(@Nonnull String version) {
    myVersion = version;
  }

  @Nonnull
  @Override
  public Map<String, ExternalProject> getChildProjects() {
    return myChildProjects;
  }

  public void setChildProjects(@Nonnull Map<String, ExternalProject> childProjects) {
    myChildProjects = childProjects;
  }

  @Nonnull
  @Override
  public File getProjectDir() {
    return myProjectDir;
  }

  public void setProjectDir(@Nonnull File projectDir) {
    myProjectDir = projectDir;
  }

  @Nonnull
  @Override
  public File getBuildDir() {
    return myBuildDir;
  }

  public void setBuildDir(@Nonnull File buildDir) {
    myBuildDir = buildDir;
  }

  @Nullable
  @Override
  public File getBuildFile() {
    return myBuildFile;
  }

  public void setBuildFile(@Nullable File buildFile) {
    myBuildFile = buildFile;
  }

  @Nonnull
  @Override
  public Map<String, ExternalTask> getTasks() {
    return myTasks;
  }

  public void setTasks(@Nonnull Map<String, ExternalTask> tasks) {
    myTasks = tasks;
  }

  @Nonnull
  @Override
  public Map<String, ExternalPlugin> getPlugins() {
    return myPlugins;
  }

  public void setPlugins(@Nonnull Map<String, ExternalPlugin> plugins) {
    myPlugins = plugins;
  }

  @Nonnull
  @Override
  public Map<String, ?> getProperties() {
    return myProperties;
  }

  public void setProperties(@Nonnull Map<String, ?> properties) {
    myProperties = properties;
  }

  @Nullable
  @Override
  public Object getProperty(String name) {
    return myProperties.get(name);
  }

  @Nonnull
  @Override
  public Map<String, ExternalSourceSet> getSourceSets() {
    return mySourceSets;
  }

  public void setSourceSets(@Nonnull Map<String, ExternalSourceSet> sourceSets) {
    mySourceSets = sourceSets;
  }
}
