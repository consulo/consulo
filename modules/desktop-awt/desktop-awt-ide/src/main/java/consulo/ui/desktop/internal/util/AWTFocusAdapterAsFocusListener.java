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
package consulo.ui.desktop.internal.util;

import consulo.ui.FocusableComponent;
import consulo.ui.event.FocusListener;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 2019-11-09
 */
public class AWTFocusAdapterAsFocusListener extends FocusAdapter {
  private final FocusableComponent myComponent;
  private final FocusListener myFocusListener;

  public AWTFocusAdapterAsFocusListener(FocusableComponent component, FocusListener focusListener) {
    myComponent = component;
    myFocusListener = focusListener;
  }

  @Override
  public void focusGained(FocusEvent e) {
    myFocusListener.focusGained(new consulo.ui.event.FocusEvent(myComponent));
  }

  @Override
  public void focusLost(FocusEvent e) {
    myFocusListener.focusLost(new consulo.ui.event.FocusEvent(myComponent));
  }
}
