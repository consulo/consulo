/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.shared.Size;
import consulo.ui.Tab;
import consulo.ui.TabbedLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class DesktopTabbedLayoutImpl extends JTabbedPane implements TabbedLayout, SwingWrapper {
  @NotNull
  @Override
  public TabbedLayout addTab(@NotNull Tab tab, @NotNull Component component) {
    return this;
  }

  @NotNull
  @Override
  public TabbedLayout addTab(@NotNull String tabName, @NotNull Component component) {
    addTab(tabName, (java.awt.Component)component);
    return this;
  }
}
