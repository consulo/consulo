/*
 * Copyright 2013-2021 consulo.io
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
import consulo.ui.PasswordBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.validableComponent.SwingValidableComponent;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 22/03/2021
 */
public class DesktopPasswordBoxImpl extends SwingValidableComponent<String, DesktopPasswordBoxImpl.MyPasswordField> implements PasswordBox {
  public class MyPasswordField extends JPasswordField implements FromSwingComponentWrapper {
    public MyPasswordField(String text) {
      super(text, 16);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopPasswordBoxImpl.this;
    }
  }

  public DesktopPasswordBoxImpl(String password) {
    initialize(new MyPasswordField(StringUtil.notNullize(password)));
  }

  @Nullable
  @Override
  public String getValue() {
    return StringUtil.nullize(toAWTComponent().getText());
  }

  @RequiredUIAccess
  @Override
  public void setValue(String value, boolean fireListeners) {
    toAWTComponent().setText(value);
  }
}
