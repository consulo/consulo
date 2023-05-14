/*
 * Copyright 2013-2019 consulo.io
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

import consulo.ide.impl.idea.openapi.ui.ThreeComponentsSplitter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class DesktopThreeComponentSplitLayoutImpl extends SwingComponentDelegate<ThreeComponentsSplitter> implements ThreeComponentSplitLayout {
  public DesktopThreeComponentSplitLayoutImpl(SplitLayoutPosition position) {
    initialize(new ThreeComponentsSplitter(position == SplitLayoutPosition.VERTICAL));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setFirstComponent(@Nullable Component component) {
    toAWTComponent().setFirstComponent((JComponent)TargetAWT.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setCenterComponent(@Nullable Component component) {
    toAWTComponent().setInnerComponent((JComponent)TargetAWT.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setSecondComponent(@Nullable Component component) {
    toAWTComponent().setLastComponent((JComponent)TargetAWT.to(component));
    return this;
  }
}
