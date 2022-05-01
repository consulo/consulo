/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.ui.errorTreeView;

import consulo.ide.impl.idea.ide.errorTreeView.NewErrorTreeViewPanelImpl;
import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanelFactory;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 28-Apr-22
 */
@Singleton
public class DesktopAwtNewErrorTreeViewPanelFactoryImpl implements NewErrorTreeViewPanelFactory {
  @Nonnull
  @Override
  public NewErrorTreeViewPanel createPanel(ComponentManager project, String helpId, boolean createExitAction, boolean createToolbar, @Nullable Runnable rerunAction) {
    return new NewErrorTreeViewPanelImpl((Project)project, helpId, createExitAction, createToolbar, rerunAction);
  }
}
