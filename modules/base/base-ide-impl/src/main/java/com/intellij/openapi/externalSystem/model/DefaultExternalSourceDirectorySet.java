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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 * @since 7/14/2014
 */
public class DefaultExternalSourceDirectorySet implements ExternalSourceDirectorySet {
  private static final long serialVersionUID = 1L;

  @Nonnull
  private String myName;
  @Nonnull
  private Set<File> mySrcDirs;
  @Nonnull
  private File myOutputDir;
  @Nonnull
  private Set<String> myExcludes;
  @Nonnull
  private Set<String> myIncludes;
  @Nonnull
  private List<ExternalFilter> myFilters;

  public DefaultExternalSourceDirectorySet() {
    mySrcDirs = new HashSet<File>();
    myExcludes = new HashSet<String>();
    myIncludes = new HashSet<String>();
    myFilters = new ArrayList<ExternalFilter>();
  }

  public DefaultExternalSourceDirectorySet(ExternalSourceDirectorySet sourceDirectorySet) {
    this();
    myName = sourceDirectorySet.getName();
    mySrcDirs = new HashSet<File>(sourceDirectorySet.getSrcDirs());
    myOutputDir = sourceDirectorySet.getOutputDir();
    myExcludes = new HashSet<String>(sourceDirectorySet.getExcludes());
    myIncludes = new HashSet<String>(sourceDirectorySet.getIncludes());
    for (ExternalFilter filter : sourceDirectorySet.getFilters()) {
      myFilters.add(new DefaultExternalFilter(filter));
    }
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
  public Set<File> getSrcDirs() {
    return mySrcDirs;
  }

  public void setSrcDirs(@Nonnull Set<File> srcDirs) {
    mySrcDirs = srcDirs;
  }

  @Nonnull
  @Override
  public File getOutputDir() {
    return myOutputDir;
  }

  @Nonnull
  @Override
  public Set<String> getIncludes() {
    return myIncludes;
  }

  public void setIncludes(@Nonnull Set<String> includes) {
    myIncludes = includes;
  }

  @Nonnull
  @Override
  public Set<String> getExcludes() {
    return myExcludes;
  }

  public void setExcludes(@Nonnull Set<String> excludes) {
    myExcludes = excludes;
  }

  @Nonnull
  @Override
  public List<ExternalFilter> getFilters() {
    return myFilters;
  }

  public void setFilters(@Nonnull List<ExternalFilter> filters) {
    myFilters = filters;
  }

  public void setOutputDir(@Nonnull File outputDir) {
    myOutputDir = outputDir;
  }
}
