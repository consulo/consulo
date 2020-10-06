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
package consulo.ui.desktop.internal.taskBar;

import com.intellij.openapi.wm.impl.X11UiUtil;
import consulo.awt.TargetAWT;
import consulo.ui.TaskBar;
import consulo.ui.Window;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-06
 */
public class XTaskBarImpl implements TaskBar {
  @Override
  public void requestAttention(@Nonnull Window window, boolean critical) {
    java.awt.Window awtWindow = TargetAWT.to(window);
    if (awtWindow != null) X11UiUtil.requestAttention(awtWindow);
  }

  @Override
  public void requestFocus(@Nonnull Window window) {
    java.awt.Window awtWindow = TargetAWT.to(window);
    if (awtWindow != null) X11UiUtil.activate(awtWindow);
  }
}
