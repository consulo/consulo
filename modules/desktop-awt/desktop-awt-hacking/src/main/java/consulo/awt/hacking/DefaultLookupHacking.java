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
package consulo.awt.hacking;

import sun.swing.DefaultLookup;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 19/03/2021
 */
public class DefaultLookupHacking {
  public static Color getColor(JComponent c, ComponentUI ui, String key) {
    return DefaultLookup.getColor(c, ui, key);
  }

  public static Color getColor(JComponent c, ComponentUI ui, String key, Color defaultValue) {
    return DefaultLookup.getColor(c, ui, key, defaultValue);
  }
}
