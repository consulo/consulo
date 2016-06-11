/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui;

import consulo.ui.internal.WGwtCheckBoxImpl;
import consulo.ui.internal.WGwtDockPanelImpl;
import consulo.ui.layout.DockLayout;
import consulo.web.servlet.ui.UIAccessHelper;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
class _UIInternalsImpl extends _UIInternals {
  @Override
  public CheckBox _Components_checkBox(@NotNull String text, boolean selected) {
    return new WGwtCheckBoxImpl(selected, text);
  }

  @Override
  public DockLayout _Layouts_dock() {
    return new WGwtDockPanelImpl();
  }

  @Override
  protected boolean isUIThread() {
    return UIAccessHelper.ourInstance.isUIThread();
  }
}
