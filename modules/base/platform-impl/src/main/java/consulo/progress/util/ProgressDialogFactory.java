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
package consulo.progress.util;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public interface ProgressDialogFactory {
  static ProgressDialogFactory getInstance() {
    return ServiceManager.getService(ProgressDialogFactory.class);
  }

  @Nonnull
  ProgressDialog create(ProgressWindow progressWindow, boolean shouldShowBackground, JComponent parent, Project project, String cancelText);
}
