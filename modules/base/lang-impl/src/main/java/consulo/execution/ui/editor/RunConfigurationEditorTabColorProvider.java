/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui.editor;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LightColors;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationEditorTabColorProvider implements EditorTabColorProvider, DumbAware {
  @Nullable
  @Override
  public Color getEditorTabColor(Project project, VirtualFile file) {
    if (file instanceof RunConfigurationVirtualFile) {
      return LightColors.SLIGHTLY_GREEN;
    }
    return null;
  }
}
