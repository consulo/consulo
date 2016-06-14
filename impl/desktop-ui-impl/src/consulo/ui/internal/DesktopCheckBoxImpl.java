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

import com.intellij.ui.components.JBCheckBox;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopCheckBoxImpl extends JBCheckBox implements CheckBox {
  public DesktopCheckBoxImpl(String text, boolean selected) {
    super(text, selected);
  }

  @NotNull
  @Override
  public Boolean getValue() {
    return isSelected();
  }

  @Override
  public void setValue(@Nullable Boolean value) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

    setSelected(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Override
  public void dispose() {

  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    addItemListener(new DesktopValueListenerAsItemListenerImpl<Boolean>(valueListener, false));

  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    removeItemListener(new DesktopValueListenerAsItemListenerImpl<Boolean>(valueListener, false));
  }
}
