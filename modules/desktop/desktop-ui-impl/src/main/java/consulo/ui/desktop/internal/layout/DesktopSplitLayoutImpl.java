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
package consulo.ui.desktop.internal.layout;

import com.intellij.openapi.ui.Splitter;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.layout.SplitLayout;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class DesktopSplitLayoutImpl extends SwingComponentDelegate<Splitter> implements SplitLayout {
  public DesktopSplitLayoutImpl(boolean vertical) {
    myComponent = new Splitter(vertical);
  }

  @Override
  public void setProportion(int percent) {
    myComponent.setProportion(percent / 100f);
  }

  @RequiredUIAccess
  @Override
  public SplitLayout setFirstComponent(@Nonnull Component component) {
    myComponent.setFirstComponent((JComponent)TargetAWT.to(component));
    return this;
  }

  @RequiredUIAccess
  @Override
  public SplitLayout setSecondComponent(@Nonnull Component component) {
    myComponent.setSecondComponent((JComponent)TargetAWT.to(component));
    return this;
  }
}
