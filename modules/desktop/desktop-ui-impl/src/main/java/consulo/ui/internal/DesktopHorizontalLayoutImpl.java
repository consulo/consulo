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

import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.HorizontalLayout;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopHorizontalLayoutImpl extends JPanel implements HorizontalLayout, SwingWrapper {
  public DesktopHorizontalLayoutImpl() {
    super(new com.intellij.ui.components.panels.HorizontalLayout(0));
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public HorizontalLayout add(@NotNull Component component) {
    add(TargetAWT.to(component));
    return this;
  }
}
