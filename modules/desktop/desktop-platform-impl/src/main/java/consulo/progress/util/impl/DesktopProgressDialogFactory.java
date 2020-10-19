/*
 * Copyright 2013-2020 consulo.io
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
package consulo.progress.util.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import consulo.progress.util.ProgressDialog;
import consulo.progress.util.ProgressDialogFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
@Singleton
public class DesktopProgressDialogFactory implements ProgressDialogFactory {
  private final Application myApplication;

  @Inject
  public DesktopProgressDialogFactory(Application application) {
    myApplication = application;
  }

  @Nonnull
  @Override
  public ProgressDialog create(ProgressWindow progressWindow, boolean shouldShowBackground, JComponent parentComponent, Project project, String cancelText) {
    Component parent = parentComponent;
    if (parent == null && project == null && !myApplication.isHeadlessEnvironment()) {
      parent = JOptionPane.getRootFrame();
    }

    return parent == null
           ? new com.intellij.openapi.progress.util.ProgressDialog(progressWindow, shouldShowBackground, project, cancelText)
           : new com.intellij.openapi.progress.util.ProgressDialog(progressWindow, shouldShowBackground, parent, cancelText);
  }
}
