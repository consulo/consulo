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

package com.intellij.ide.todo;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author yole
 */
public class TodoToolWindowFactory implements ToolWindowFactory {
  private final Application myApplication;
  private final ContentFactory myContentFactory;

  @Inject
  public TodoToolWindowFactory(Application application, ContentFactory contentFactory) {
    myApplication = application;
    myContentFactory = contentFactory;
  }

  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    DumbService.getInstance(project).runWhenSmart(() -> {
      UIAccess lastUIAccess = myApplication.getLastUIAccess();

      lastUIAccess.give(() -> TodoView.getInstance(project).initToolWindow(myContentFactory, toolWindow));
    });
  }
}
