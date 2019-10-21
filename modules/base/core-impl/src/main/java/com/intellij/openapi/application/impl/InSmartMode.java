/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.constraints.ConstrainedExecution;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

/**
 * from kotlin
 */
public class InSmartMode implements ConstrainedExecution.ContextConstraint {
  private final Project myProject;

  public InSmartMode(Project project) {
    myProject = project;
  }

  @Override
  public boolean isCorrectContext() {
    return !DumbService.getInstance(myProject).isDumb();
  }

  @Override
  public void schedule(Runnable runnable) {
    DumbService.getInstance(myProject).runWhenSmart(runnable);
  }

  @Override
  public String toString() {
    return "inSmartMode";
  }
}
