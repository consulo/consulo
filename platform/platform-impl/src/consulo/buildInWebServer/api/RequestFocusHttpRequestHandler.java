/*
 * Copyright 2013-2015 must-be.org
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
package consulo.buildInWebServer.api;

import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public class RequestFocusHttpRequestHandler extends JsonGetRequestHandler {
  public static boolean activateFrame(@Nullable final IdeFrame frame) {
    return frame instanceof Frame && activateFrame((Frame)frame);
  }

  public static boolean activateFrame(@Nullable final Frame frame) {
    if (frame != null) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          int extendedState = frame.getExtendedState();
          if (BitUtil.isSet(extendedState, Frame.ICONIFIED)) {
            extendedState = BitUtil.set(extendedState, Frame.ICONIFIED, false);
            frame.setExtendedState(extendedState);
          }

          // fixme [vistall] dirty hack - show frame on top
          frame.setAlwaysOnTop(true);
          frame.setAlwaysOnTop(false);
          frame.requestFocus();
        }
      };
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(runnable);
      return true;
    }
    return false;
  }

  public RequestFocusHttpRequestHandler() {
    super("requestFocus");
  }

  @NotNull
  @Override
  public JsonResponse handle() {
    final IdeFrame frame = IdeFocusManager.findInstance().getLastFocusedFrame();
    return activateFrame(frame) ? JsonResponse.asSuccess(null) : JsonResponse.asError("No Frame");
  }
}
