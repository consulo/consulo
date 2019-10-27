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
package consulo.desktop.ui.popup.impl;

import consulo.ui.Window;
import consulo.ui.desktop.internal.window.JWindowAsUIWindow;
import consulo.ui.popup.JWindowPopupFactory;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-07-27
 */
public class JWindowPopupFactoryImpl implements JWindowPopupFactory {
  @Override
  public JWindow create(Window window) {
    return new JWindowAsUIWindow(window);
  }
}
