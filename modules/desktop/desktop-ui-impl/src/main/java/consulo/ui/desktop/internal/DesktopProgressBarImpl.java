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
package consulo.ui.desktop.internal;

import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.ProgressBar;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
class DesktopProgressBarImpl extends SwingComponentDelegate<JProgressBar> implements ProgressBar {
  private class ProgressBar extends JProgressBar implements FromSwingComponentWrapper {
    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopProgressBarImpl.this;
    }
  }

  public DesktopProgressBarImpl() {
    initialize(new ProgressBar());
  }

  @Override
  public void setIndeterminate(boolean value) {
    toAWTComponent().setIndeterminate(value);
  }

  @Override
  public boolean isIndeterminate() {
    return toAWTComponent().isIndeterminate();
  }

  @Override
  public void setMinimum(int value) {
    toAWTComponent().setMinimum(value);
  }

  @Override
  public void setMaximum(int value) {
    toAWTComponent().setMaximum(value);
  }

  @Override
  public void setValue(int value) {
    toAWTComponent().setValue(value);
  }
}
