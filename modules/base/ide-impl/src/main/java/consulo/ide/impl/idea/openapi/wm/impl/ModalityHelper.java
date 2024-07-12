/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl;

import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.awt.hacking.WindowHacking;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Fokin
 */
public class ModalityHelper {
  public static boolean isModalBlocked(final Window window) {
    return WindowHacking.isModalBlocked(window);
  }

  public static JDialog getModalBlockerFor(final Window window) {
    return WindowHacking.getModalBlockerFor(window);
  }

  public static JDialog getBlockerForFrame(final FocusableFrame ideFrame) {
    if (ideFrame == null) return null;
    Component c = ideFrame.getComponent();
    if (c == null) return null;
    Window window = SwingUtilities.getWindowAncestor(c);
    if (window == null) return null;
    if (!isModalBlocked(window)) return null;
    return getModalBlockerFor(window);
  }

  public static JDialog getBlockerForFocusedFrame() {
    return getBlockerForFrame(IdeFocusManager.getGlobalInstance().getLastFocusedFrame());
  }

}
