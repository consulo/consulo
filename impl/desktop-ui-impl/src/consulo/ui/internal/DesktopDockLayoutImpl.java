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
package consulo.ui.internal;

import consulo.ui.Component;
import consulo.ui.DockLayout;
import consulo.ui.shared.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopDockLayoutImpl extends JPanel implements DockLayout {
  public DesktopDockLayoutImpl() {
    super(new BorderLayout());
  }

  @Override
  public void setSize(@NotNull Size size) {
    setSize(new Dimension(size.getWidth(), size.getHeight()));
  }

  @NotNull
  @Override
  public DockLayout top(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.NORTH);
    return this;
  }

  @NotNull
  @Override
  public DockLayout bottom(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.SOUTH);
    return this;
  }

  @NotNull
  @Override
  public DockLayout center(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.CENTER);
    return this;
  }

  @NotNull
  @Override
  public DockLayout left(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.WEST);
    return this;
  }

  @NotNull
  @Override
  public DockLayout right(@NotNull Component component) {
    add((java.awt.Component)component, BorderLayout.EAST);
    return this;
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Override
  public void dispose() {

  }
}
