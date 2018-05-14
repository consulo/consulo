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

import com.intellij.ui.components.JBCheckBox;
import consulo.ui.CheckBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ValueComponent;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopCheckBoxImpl extends JBCheckBox implements CheckBox, SwingWrapper {
  public DesktopCheckBoxImpl(String text, boolean selected) {
    super(text, selected);
  }

  @Nonnull
  @Override
  public Boolean getValue() {
    return isSelected();
  }

  @RequiredUIAccess
  @Override
  public void setValue(@javax.annotation.Nullable Boolean value, boolean fireEvents) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

    setSelected(value);
  }

  @Override
  public void addValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    addItemListener(new DesktopValueListenerAsItemListenerImpl<Boolean>(valueListener, false));
  }

  @Override
  public void removeValueListener(@Nonnull ValueComponent.ValueListener<Boolean> valueListener) {
    removeItemListener(new DesktopValueListenerAsItemListenerImpl<Boolean>(valueListener, false));
  }
}
