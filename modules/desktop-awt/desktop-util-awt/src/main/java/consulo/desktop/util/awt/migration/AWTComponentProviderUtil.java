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
package consulo.desktop.util.awt.migration;

import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2018-05-11
 * <p/>
 * JB guys love creating components via extending, and searching them via {@link com.intellij.util.ui.UIUtil#findComponentOfType(javax.swing.JComponent, Class)}
 * But when we using cross-ui, we can't extend UI component
 * <p/>
 * This impementation is AWT specific, but cant be converted to UI without any problems
 */
public class AWTComponentProviderUtil {
  private static final Key<AWTComponentProvider> KEY = Key.create("AWTComponentUtil.KEY");

  public static void putMark(JComponent component, AWTComponentProvider componentProvider) {
    component.putClientProperty(KEY, componentProvider);
  }

  @Nullable
  public static AWTComponentProvider getMark(@Nullable Component component) {
    return component instanceof JComponent ? (AWTComponentProvider)((JComponent)component).getClientProperty(KEY) : null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends AWTComponentProvider> T findChild(@Nullable Component child, @Nonnull Class<T> cls) {
    if (child == null) {
      return null;
    }

    AWTComponentProvider property = getMark(child);
    if (property != null && cls.isInstance(property) && property.getComponent() == child) {
      return (T)property;
    }

    if (child instanceof Container) {
      for (Component component : ((Container)child).getComponents()) {
        T comp = findChild(component, cls);
        if (comp != null) {
          return comp;
        }
      }
    }

    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <T extends AWTComponentProvider> T findParent(@Nullable Component parent, @Nonnull Class<T> cls) {
    if (parent == null) {
      return null;
    }

    AWTComponentProvider property = getMark(parent);
    if (property != null && cls.isInstance(property) && property.getComponent() == parent) {
      return (T)property;
    }

    Container nextParent = parent.getParent();
    return findParent(nextParent, cls);
  }
}
