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
package com.intellij.ui.messages;

import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ModalityHelper;
import com.intellij.ui.mac.foundation.MacUtil;
import consulo.awt.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 10/07/2021
 */
public class SheetMessageUtil {
  public static consulo.ui.Window getForemostWindow(final Window window) {
    Window _window = null;
    IdeFocusManager ideFocusManager = IdeFocusManager.getGlobalInstance();

    Component focusOwner = IdeFocusManager.findInstance().getFocusOwner();
    // Let's ask for a focused component first
    if (focusOwner != null) {
      _window = SwingUtilities.getWindowAncestor(focusOwner);
    }

    if (_window == null) {
      // Looks like ide lost focus, let's ask about the last focused component
      focusOwner = ideFocusManager.getLastFocusedFor(ideFocusManager.getLastFocusedFrame());
      if (focusOwner != null) {
        _window = SwingUtilities.getWindowAncestor(focusOwner);
      }
    }

    if (_window == null) {
      _window = WindowManager.getInstance().findVisibleFrame();
    }

    if (_window == null && window != null) {
      // It might be we just has not opened a frame yet.
      // So let's ask AWT
      focusOwner = window.getMostRecentFocusOwner();
      if (focusOwner != null) {
        _window = SwingUtilities.getWindowAncestor(focusOwner);
      }
    }

    if (_window != null) {
      // We have successfully found the window
      // Let's check that we have not missed a blocker
      if (ModalityHelper.isModalBlocked(_window)) {
        _window = ModalityHelper.getModalBlockerFor(_window);
      }
    }

    while (_window != null && MacUtil.getWindowTitle(_window) == null) {
      _window = _window.getOwner();
      //At least our frame should have a title
    }

    while (Registry.is("skip.untitled.windows.for.mac.messages") && _window != null && _window instanceof JDialog && !((JDialog)_window).isModal()) {
      _window = _window.getOwner();
    }

    return TargetAWT.from(_window);
  }
}
