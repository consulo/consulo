/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.util.ui;

import java.awt.*;

/**
 * AWT components hierarchy visitor
 *
 * @author spleaner
 */
public abstract class AwtVisitor {

  protected AwtVisitor(Component component) {
    _accept(component);
  }

  private boolean _accept(Component component) {
    boolean shouldStop = visit(component);
    if (shouldStop) return shouldStop;

    if (component instanceof Container) {
      Component[] kids = ((Container) component).getComponents();
      for (Component each : kids) {
        shouldStop = _accept(each);
        if (shouldStop) return shouldStop;
      }
    }

    return false;
  }

  public abstract boolean visit(Component component);

}
