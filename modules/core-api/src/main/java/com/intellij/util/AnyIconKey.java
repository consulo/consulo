/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.util;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class AnyIconKey<T> {
  private final T myObject;
  private final Project myProject;
  private final int myFlags;

  public AnyIconKey(@NotNull T object, final Project project, int flags) {
    myObject = object;
    myProject = project;
    myFlags = flags;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof AnyIconKey)) return false;

    final AnyIconKey that = (AnyIconKey)o;

    if (myFlags != that.myFlags) return false;
    if (!myObject.equals(that.myObject)) return false;
    if (myProject != null ? !myProject.equals(that.myProject) : that.myProject != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myObject.hashCode();
    result = 31 * result + (myProject != null ? myProject.hashCode() : 0);
    result = 31 * result + myFlags;
    return result;
  }

  public T getObject() {
    return myObject;
  }

  public Project getProject() {
    return myProject;
  }

  public int getFlags() {
    return myFlags;
  }
}