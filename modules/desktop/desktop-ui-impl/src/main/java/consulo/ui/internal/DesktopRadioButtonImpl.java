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

import consulo.ui.RadioButton;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class DesktopRadioButtonImpl extends JRadioButton implements RadioButton, SwingWrapper {
  public DesktopRadioButtonImpl(String text, boolean selected) {
    super(text, selected);
  }

  @Override
  public void addValueListener(@NotNull ValueListener<Boolean> valueListener) {
    addItemListener(new DesktopValueListenerAsItemListenerImpl<>(valueListener, false));
  }

  @Override
  public void removeValueListener(@NotNull ValueListener<Boolean> valueListener) {
    removeItemListener(new DesktopValueListenerAsItemListenerImpl<>(valueListener, false));
  }

  @NotNull
  @Override
  public Boolean getValue() {
    return isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@NotNull Boolean value, boolean fireEvents) {
    setSelected(value);
  }
}
