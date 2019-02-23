/*
 * Copyright 2013-2016 consulo.io
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
package consulo.builtInServer.impl.net.json;

import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.BitUtil;
import consulo.awt.TargetAWT;
import consulo.builtInServer.json.JsonGetRequestHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public class RequestFocusHttpRequestHandler extends JsonGetRequestHandler {
  public static boolean activateFrame(@Nullable final IdeFrame ideFrame) {
    if (ideFrame == null) {
      return false;
    }

    Window awtWindow = TargetAWT.to(ideFrame.getWindow());
    return awtWindow instanceof Frame && activateFrame((Frame)awtWindow);
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
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(frame);
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

  @Nonnull
  @Override
  public JsonResponse handle() {
    final IdeFrame frame = IdeFocusManager.findInstance().getLastFocusedFrame();
    return activateFrame(frame) ? JsonResponse.asSuccess(null) : JsonResponse.asError("No Frame");
  }
}
