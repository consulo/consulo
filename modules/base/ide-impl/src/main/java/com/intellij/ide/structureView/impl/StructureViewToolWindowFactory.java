/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.structureView.impl;

import consulo.fileEditor.structureView.StructureViewFactory;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author yole
 */
public class StructureViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @RequiredUIAccess
  @Override
  public void createToolWindowContent(Project project, ToolWindow toolWindow) {
    StructureViewFactoryImpl factory = (StructureViewFactoryImpl)StructureViewFactory.getInstance(project);
    factory.initToolWindow((ToolWindowEx)toolWindow);
  }
}
