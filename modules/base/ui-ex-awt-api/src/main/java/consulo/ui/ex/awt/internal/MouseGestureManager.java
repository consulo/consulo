/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.project.ui.wm.IdeFrame;

import jakarta.inject.Singleton;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class MouseGestureManager {
  public static MouseGestureManager getInstance() {
    return Application.get().getInstance(MouseGestureManager.class);
  }

  private static final Logger LOG = Logger.getInstance(MouseGestureManager.class);

  private Map<IdeFrame, Object> myListeners = new HashMap<>();
  private boolean HAS_TRACKPAD = false;

  public void add(IdeFrame frame) {
    if (!Registry.is("actionSystem.mouseGesturesEnabled")) return;

    if (SystemInfo.isMacOSSnowLeopard) {
      try {
        if (myListeners.containsKey(frame)) {
          remove(frame);
        }

        Object listener = new MacGestureAdapter(this, frame);

        myListeners.put(frame, listener);
      }
      catch (Throwable e) {
        LOG.debug(e);
      }
    }
  }

  protected void activateTrackpad() {
    HAS_TRACKPAD = true;
  }
  
  public boolean hasTrackpad() {
    return HAS_TRACKPAD;
  }

  public void remove(IdeFrame frame) {
    if (!Registry.is("actionSystem.mouseGesturesEnabled")) return;

    if (SystemInfo.isMacOSSnowLeopard) {
      try {
        Object listener = myListeners.get(frame);
        JComponent cmp = frame.getComponent();
        myListeners.remove(frame);
        if (listener != null && cmp != null && cmp.isShowing()) {
          ((MacGestureAdapter)listener).remove(cmp);
        }
      }
      catch (Throwable e) {
        LOG.debug(e);
      }
    }

  }
}
