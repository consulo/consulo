/*
 * Copyright 2013-2020 consulo.io
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

import consulo.logging.Logger;

import javax.swing.plaf.basic.BasicScrollPaneUI;
import java.lang.reflect.Field;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class BasicScrollPaneUIHacking {
  private static final Logger LOG = Logger.getInstance(BasicScrollPaneUIHacking.class);

  public static Object getMouseScrollListener(BasicScrollPaneUI ui) {
    try {
      Field field = BasicScrollPaneUI.class.getDeclaredField("mouseScrollListener");
      field.setAccessible(true);
      return field.get(ui);
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
    return null;
  }

  public static void setMouseScrollListener(BasicScrollPaneUI ui, Object value) {
    try {
      Field field = BasicScrollPaneUI.class.getDeclaredField("mouseScrollListener");
      field.setAccessible(true);
      field.set(ui, value);
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
  }
}
