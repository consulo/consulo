/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.welcomeScreen;

import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;

import java.awt.*;

public interface WelcomeScreenConstants {
  static Color getLinkNormalColor() {
    return new JBColor(Gray._0, Gray.xBB);
  }

  static Color getActionLinkSelectionColor() {
    return new JBColor(0xdbe5f5, 0x485875);
  }
}
