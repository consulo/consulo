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
package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author nik
 */
public class PlaceInProjectStructureBase extends PlaceInProjectStructure {
  private final Function<Project,AsyncResult<Void>> myNavigator;
  private final ProjectStructureElement myElement;

  public PlaceInProjectStructureBase(@RequiredUIAccess Function<Project, AsyncResult<Void>> navigator, ProjectStructureElement element) {
    myNavigator = navigator;
    myElement = element;
  }

  @Override
  public String getPlacePath() {
    return null;
  }

  @Nonnull
  @Override
  public ProjectStructureElement getContainingElement() {
    return myElement;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> navigate(@Nonnull Project project) {
    return myNavigator.apply(project);
  }
}
