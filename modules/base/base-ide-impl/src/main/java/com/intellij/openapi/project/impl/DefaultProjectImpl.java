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
package com.intellij.openapi.project.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.ProjectManager;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class DefaultProjectImpl extends ProjectImpl {
  private static final String TEMPLATE_PROJECT_NAME = "Default (Template) Project";

  private Element myStateElement;
  private boolean myInitialized;

  DefaultProjectImpl(@Nonnull Application application, @Nonnull ProjectManager manager, @Nonnull String filePath, boolean optimiseTestLoadSpeed) {
    super(application, manager, filePath, optimiseTestLoadSpeed, TEMPLATE_PROJECT_NAME, false);
  }

  @Nullable
  public Element getStateElement() {
    return myStateElement;
  }

  public void setStateElement(@Nullable Element stateElement) {
    myStateElement = stateElement;
  }

  public void setInitialized() {
    myInitialized = true;
  }

  @Override
  public boolean isInitialized() {
    return myInitialized;
  }

  @Override
  public boolean isDefault() {
    return true;
  }
}
