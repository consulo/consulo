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
package consulo.ui.util;

import consulo.ui.Component;
import consulo.ui.Window;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 6/9/18
 */
public class TraverseUtil {
  /**
   * Return {@code true} if a component {@code a} descends from a component {@code b}
   *
   * @param a the first component
   * @param b the second component
   * @return {@code true} if a component {@code a} descends from a component {@code b}
   */
  public static boolean isDescendingFrom(Component a, Component b) {
    if (a == b) return true;
    for (Component p = a.getParent(); p != null; p = p.getParent()) {
      if (p == b) return true;
    }
    return false;
  }

  @Nullable
  public static Component findUltimateParent(@Nullable Component c) {
    if (c == null) return null;

    Component eachParent = c;
    while (true) {
      if (eachParent.getParent() == null) return eachParent;
      eachParent = eachParent.getParent();
    }
  }
  
  @Nullable
  @Contract("null -> null")
  public static Window getWindowAncestor(@Nullable Component c) {
    if (c == null) {
      return null;
    }

    if (c instanceof Window) {
      return (Window)c;
    }

    for (Component p = c.getParent(); p != null; p = p.getParent()) {
      if (p instanceof Window) {
        return (Window)p;
      }
    }
    return null;
  }
}
