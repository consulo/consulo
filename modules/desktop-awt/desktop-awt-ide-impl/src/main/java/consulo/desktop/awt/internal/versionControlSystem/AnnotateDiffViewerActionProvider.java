/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.internal.versionControlSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.action.AnnotateToggleActionProvider;

@ExtensionImpl(order = "first")
public class AnnotateDiffViewerActionProvider implements AnnotateToggleActionProvider {
  @Override
  public boolean isEnabled(AnActionEvent e) {
    return AnnotateDiffViewerAction.isEnabled(e);
  }

  @Override
  public boolean isSuspended(AnActionEvent e) {
    return AnnotateDiffViewerAction.isSuspended(e);
  }

  @Override
  public boolean isAnnotated(AnActionEvent e) {
    return AnnotateDiffViewerAction.isAnnotated(e);
  }

  @Override
  public void perform(AnActionEvent e, boolean selected) {
    AnnotateDiffViewerAction.perform(e, selected);
  }
}
