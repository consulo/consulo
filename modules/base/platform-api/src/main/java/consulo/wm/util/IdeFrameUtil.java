/*
 * Copyright 2013-2019 consulo.io
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
package consulo.wm.util;

import com.intellij.openapi.wm.IdeFrame;
import consulo.ui.Window;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-15
 */
public class IdeFrameUtil {
  public static boolean isRootFrame(@Nullable IdeFrame frame) {
    return frame != null && !(frame instanceof IdeFrame.Child);
  }

  public static boolean isRootIdeFrameWindow(@Nullable Window window) {
    if(window == null) {
      return false;
    }
    return isRootFrame(window.getUserData(IdeFrame.KEY));
  }

  /**
   * Return focused and root IdeFrame. Consulo can provide some frame which is not project-frame (child frame). It will return child if active
   * In this case it will return parent frame
   */
  @Nullable
  public static IdeFrame findFocusedRootIdeFrame() {
    return findRootIdeFrame(Window.getFocusedWindow());
  }

  /**
   * Return active and root IdeFrame. Consulo can provide some frame which is not project-frame (child frame). It will return child if active
   * In this case it will return parent frame
   */
  @Nullable
  public static IdeFrame findActiveRootIdeFrame() {
    return findRootIdeFrame(Window.getActiveWindow());
  }

  @Nullable
  public static IdeFrame findRootIdeFrame(@Nullable Window activeWindow) {
    if (activeWindow == null) {
      return null;
    }

    IdeFrame frame = null;
    while (frame == null) {
      if (activeWindow == null) {
        break;
      }

      frame = activeWindow.getUserData(IdeFrame.KEY);

      activeWindow = activeWindow.getParent();
    }

    if (frame == null) {
      return null;
    }

    while (frame instanceof IdeFrame.Child) {
      frame = ((IdeFrame.Child)frame).getParentFrame();
    }

    return frame;
  }
}
