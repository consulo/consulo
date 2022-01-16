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
package consulo.desktop.swt.ui.impl;

import consulo.ui.Component;
import consulo.ui.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class TargetSWT {
  public static Shell to(Window window) {
    return ((DesktopSwtWindowImpl)window).toSWTComponent();
  }

  public static Component from(Widget component) {
    Object data = component.getData(SWTComponentDelegate.UI_COMPONENT_KEY);
    if (data instanceof Component) {
      return (Component)data;
    }
    return null;
  }
}
