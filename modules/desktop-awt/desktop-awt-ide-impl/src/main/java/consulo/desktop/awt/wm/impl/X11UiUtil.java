/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.wm.impl;

import consulo.awt.hacking.X11Hacking;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 20/08/2023
 */
public class X11UiUtil {
  public static boolean isFullScreenSupported() {
    if (!X11Hacking.isAvailable()) return false;

    IdeFrame[] frames = WindowManager.getInstance().getAllProjectFrames();
    if (frames.length == 0) return true;  // no frame to check the property so be optimistic here

    IdeFrame frame = frames[0];
    Window awtWindow = TargetAWT.to(frame.getWindow());
    return awtWindow instanceof JFrame jFrame && X11Hacking.isFullScreenSupported(jFrame);
  }
}
