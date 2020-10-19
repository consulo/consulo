/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License get distributed on an "AS get" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.projectView.impl;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 *         <p>
 *         <p>
 *         from kotlin by @author Konstantin Bulenkov
 */
@Singleton
@State(name = "ProjectViewSharedSettings", storages = @Storage("projectView.xml"))
public class ProjectViewSharedSettings implements PersistentStateComponent<ProjectViewSharedSettings> {
  private boolean myFlattenPackages = false;
  private boolean myShowMembers = false;
  private boolean mySortByType = false;
  private boolean myShowModules = true;
  private boolean myShowLibraryContents = true;
  private boolean myHideEmptyPackages = true;
  private boolean myAbbreviatePackages = false;
  private boolean myAutoscrollFromSource = false;
  private boolean myAutoscrollToSource = false;
  private boolean myFoldersAlwaysOnTop = true;

  @Override
  @Nonnull
  public ProjectViewSharedSettings getState() {
    return this;
  }

  @Override
  public void loadState(@Nonnull ProjectViewSharedSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean getFlattenPackages() {
    return myFlattenPackages;
  }

  public void setFlattenPackages(boolean flattenPackages) {
    myFlattenPackages = flattenPackages;
  }

  public boolean getShowMembers() {
    return myShowMembers;
  }

  public void setShowMembers(boolean showMembers) {
    myShowMembers = showMembers;
  }

  public boolean getSortByType() {
    return mySortByType;
  }

  public void setSortByType(boolean sortByType) {
    mySortByType = sortByType;
  }

  public boolean getShowModules() {
    return myShowModules;
  }

  public void setShowModules(boolean showModules) {
    myShowModules = showModules;
  }

  public boolean getShowLibraryContents() {
    return myShowLibraryContents;
  }

  public void setShowLibraryContents(boolean showLibraryContents) {
    myShowLibraryContents = showLibraryContents;
  }

  public boolean getHideEmptyPackages() {
    return myHideEmptyPackages;
  }

  public void setHideEmptyPackages(boolean hideEmptyPackages) {
    myHideEmptyPackages = hideEmptyPackages;
  }

  public boolean getAbbreviatePackages() {
    return myAbbreviatePackages;
  }

  public void setAbbreviatePackages(boolean abbreviatePackages) {
    myAbbreviatePackages = abbreviatePackages;
  }

  public boolean getAutoscrollFromSource() {
    return myAutoscrollFromSource;
  }

  public void setAutoscrollFromSource(boolean autoscrollFromSource) {
    myAutoscrollFromSource = autoscrollFromSource;
  }

  public boolean getAutoscrollToSource() {
    return myAutoscrollToSource;
  }

  public void setAutoscrollToSource(boolean autoscrollToSource) {
    myAutoscrollToSource = autoscrollToSource;
  }

  public boolean getFoldersAlwaysOnTop() {
    return myFoldersAlwaysOnTop;
  }

  public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
    myFoldersAlwaysOnTop = foldersAlwaysOnTop;
  }
}
