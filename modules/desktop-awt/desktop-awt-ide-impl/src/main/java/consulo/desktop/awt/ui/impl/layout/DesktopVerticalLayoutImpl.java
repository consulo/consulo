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
package consulo.desktop.awt.ui.impl.layout;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class DesktopVerticalLayoutImpl extends DesktopLayoutBase<JPanel> implements VerticalLayout {
  public DesktopVerticalLayoutImpl(int vGap) {
    initDefaultPanel(new consulo.ui.ex.awt.VerticalLayout(JBUI.scale(vGap)));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public VerticalLayout add(@Nonnull Component component) {
    add(component, null);
    return this;
  }
}
