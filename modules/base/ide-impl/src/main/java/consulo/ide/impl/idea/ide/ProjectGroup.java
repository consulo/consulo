/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.util.lang.StringUtil;
import java.util.HashSet;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectGroup {
  private @Nonnull
  String myName = "";
  private String myProjectPaths = "";
  private boolean myExpanded = false;
  private boolean myTutorials = false; //used in different places, i.e. closing tutorials group should hide all nested items too

  public ProjectGroup(@Nonnull String name) {
    myName = name;
  }

  public ProjectGroup() {}

  @Nonnull
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  //do not remove. bean getter
  public String getProjectPaths() {
    return myProjectPaths;
  }

  //do not remove. bean setter
  public void setProjectPaths(String projectPaths) {
    myProjectPaths = projectPaths;
  }

  public void addProject(String path) {
    final List<String> projects = getProjects();
    projects.add(path);
    save(projects);
  }

  protected void save(List<String> projects) {
    myProjectPaths = StringUtil.join(projects, File.pathSeparator);
  }

  public List<String> getProjects() {
    return new ArrayList<String>(new HashSet<String>(StringUtil.split(myProjectPaths, File.pathSeparator)));
  }

  public void removeProject(String path) {
    final List<String> projects = getProjects();
    projects.remove(path);
    save(projects);
  }

  public boolean isExpanded() {
    return myExpanded;
  }

  public void setExpanded(boolean expanded) {
    myExpanded = expanded;
  }

  public boolean isTutorials() {
    return myTutorials;
  }

  public void setTutorials(boolean tutorials) {
    myTutorials = tutorials;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProjectGroup group = (ProjectGroup)o;

    if (!myName.equals(group.myName)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
