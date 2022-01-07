/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.projectView;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.wm.ToolWindow;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public interface ProjectViewEx extends ProjectView {
  @RequiredUIAccess
  void setupToolWindow(@Nonnull ToolWindow toolWindow, final boolean loadPaneExtensions);
}
