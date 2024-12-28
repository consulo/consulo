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
package consulo.externalSystem.rt.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 * @since 7/14/2014
 */
public class DefaultExternalProject implements ExternalProject {

  private static final long serialVersionUID = 1L;

  private String myName;
  
  private String myQName;
 
  private String myDescription;
  
  private String myGroup;
  
  private String myVersion;
  
  private Map<String, ExternalProject> myChildProjects;
  
  private File myProjectDir;
  
  private File myBuildDir;
 
  private File myBuildFile;
  
  private Map<String, ExternalTask> myTasks;
  
  private Map<String, ?> myProperties;
  
  private Map<String, ExternalSourceSet> mySourceSets;
  
  private String myExternalSystemId;
  
  private Map<String, ExternalPlugin> myPlugins;

  public DefaultExternalProject() {
    myChildProjects = new HashMap<String, ExternalProject>();
    myTasks = new HashMap<String, ExternalTask>();
    myProperties = new HashMap<String, Object>();
    mySourceSets = new HashMap<String, ExternalSourceSet>();
    myPlugins = new HashMap<String, ExternalPlugin>();
  }

  public DefaultExternalProject( ExternalProject externalProject) {
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


  
  @Override
  public String getExternalSystemId() {
    return myExternalSystemId;
  }

  public void setExternalSystemId( String externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  
  @Override
  public String getName() {
    return myName;
  }

  public void setName( String name) {
    myName = name;
  }

  
  @Override
  public String getQName() {
    return myQName;
  }

  public void setQName( String QName) {
    myQName = QName;
  }

 
  @Override
  public String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  
  @Override
  public String getGroup() {
    return myGroup;
  }

  public void setGroup( String group) {
    myGroup = group;
  }

  @Override
  public String getVersion() {
    return myVersion;
  }
  public void setVersion( String version) {
    myVersion = version;
  }

  @Override
  public Map<String, ExternalProject> getChildProjects() {
    return myChildProjects;
  }

  public void setChildProjects( Map<String, ExternalProject> childProjects) {
    myChildProjects = childProjects;
  }

  @Override
  public File getProjectDir() {
    return myProjectDir;
  }

  public void setProjectDir( File projectDir) {
    myProjectDir = projectDir;
  }

  
  @Override
  public File getBuildDir() {
    return myBuildDir;
  }

  public void setBuildDir( File buildDir) {
    myBuildDir = buildDir;
  }

 
  @Override
  public File getBuildFile() {
    return myBuildFile;
  }

  public void setBuildFile(File buildFile) {
    myBuildFile = buildFile;
  }

  @Override
  public Map<String, ExternalTask> getTasks() {
    return myTasks;
  }

  public void setTasks( Map<String, ExternalTask> tasks) {
    myTasks = tasks;
  }

  @Override
  public Map<String, ExternalPlugin> getPlugins() {
    return myPlugins;
  }

  public void setPlugins( Map<String, ExternalPlugin> plugins) {
    myPlugins = plugins;
  }

  @Override
  public Map<String, ?> getProperties() {
    return myProperties;
  }

  public void setProperties( Map<String, ?> properties) {
    myProperties = properties;
  }

  @Override
  public Object getProperty(String name) {
    return myProperties.get(name);
  }

  @Override
  public Map<String, ExternalSourceSet> getSourceSets() {
    return mySourceSets;
  }
  public void setSourceSets( Map<String, ExternalSourceSet> sourceSets) {
    mySourceSets = sourceSets;
  }
}
