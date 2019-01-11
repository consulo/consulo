/*
 * Copyright 2013-2018 consulo.io
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

import com.intellij.openapi.Disposable;
import com.intellij.ui.ColorPanel;
import consulo.awt.TargetAWT;
import consulo.ui.ColorBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.ActionListener;

/**
 * @author VISTALL
 * @since 6/9/18
 */
public class DesktopColorBoxImpl extends ColorPanel implements SwingWrapper, ColorBox {
  public DesktopColorBoxImpl(ColorValue colorValue) {
    setSelectedColor(TargetAWT.to(colorValue));
  }

  @Nonnull
  @Override
  public Disposable addValueListener(@Nonnull ValueListener<ColorValue> valueListener) {
    ActionListener actionListener = e -> valueListener.valueChanged(new ValueEvent<>(this, TargetAWT.from(getSelectedColor())));
    addActionListener(actionListener);
    return () -> removeActionListener(actionListener);
  }

  @Nullable
  @Override
  public ColorValue getValue() {
    return TargetAWT.from(getSelectedColor());
  }

  @RequiredUIAccess
  @Override
  public void setValue(ColorValue value, boolean fireEvents) {
    setSelectedColor(TargetAWT.to(value));
  }
}
