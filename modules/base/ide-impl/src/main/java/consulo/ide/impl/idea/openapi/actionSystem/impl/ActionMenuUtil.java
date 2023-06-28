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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.ide.impl.desktop.DesktopIdeFrameUtil;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

/**
 * @author VISTALL
 * @since 26/06/2023
 */
public class ActionMenuUtil {
  public static void showDescriptionInStatusBar(boolean isIncluded, Component component, String description) {
    IdeFrame ideFrame = null;
    if (component instanceof Window) {
      ideFrame = TargetAWT.from((Window)component).getUserData(IdeFrame.KEY);
    }

    if (ideFrame == null) {
      ideFrame = DesktopIdeFrameUtil.findIdeFrameFromParent(component);
    }

    StatusBar statusBar;
    if (ideFrame != null && (statusBar = ideFrame.getStatusBar()) != null) {
      statusBar.setInfo(isIncluded ? description : null);
    }
  }
}
