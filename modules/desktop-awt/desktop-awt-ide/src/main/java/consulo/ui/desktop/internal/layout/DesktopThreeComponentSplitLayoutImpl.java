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
package consulo.ui.desktop.internal.layout;

import com.intellij.openapi.ui.ThreeComponentsSplitter;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
