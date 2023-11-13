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
package consulo.desktop.awt.wm.mac;

import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.application.util.mac.foundation.MacUtil;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
public class MacNativeUIUtil {
  private static final Logger LOG = Logger.getInstance(MacUtil.class);
  public static final String MAC_NATIVE_WINDOW_SHOWING = "MAC_NATIVE_WINDOW_SHOWING";

  public static synchronized void startModal(JComponent component, String key) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        EventQueue theQueue = component.getToolkit().getSystemEventQueue();

        while (component.getClientProperty(key) == Boolean.TRUE) {
          AWTEvent event = theQueue.getNextEvent();
          Object source = event.getSource();
          if (event instanceof ActiveEvent) {
            ((ActiveEvent)event).dispatch();
          }
          else if (source instanceof Component) {
            ((Component)source).dispatchEvent(event);
          }
          else if (source instanceof MenuComponent) {
            ((MenuComponent)source).dispatchEvent(event);
          }
          else {
            LOG.debug("Unable to dispatch: " + event);
          }
        }
      }
      else {
        assert false : "Should be called from Event-Dispatch Thread only!";
        while (component.getClientProperty(key) == Boolean.TRUE) {
          // TODO:
          //wait();
        }
      }
    }
    catch (InterruptedException ignored) {
    }
  }

  public static synchronized void startModal(JComponent component) {
    startModal(component, MAC_NATIVE_WINDOW_SHOWING);
  }

  @Nullable
  public static String getWindowTitle(Window documentRoot) {
    String windowTitle = null;
    if (documentRoot instanceof Frame) {
      windowTitle = ((Frame)documentRoot).getTitle();
    }
    else if (documentRoot instanceof Dialog) {
      windowTitle = ((Dialog)documentRoot).getTitle();
    }
    return windowTitle;
  }

  @Nonnull
  public static Color colorFromNative(ID color) {
    final ID colorSpace = Foundation.invoke("NSColorSpace", "genericRGBColorSpace");
    final ID colorInSpace = Foundation.invoke(color, "colorUsingColorSpace:", colorSpace);
    final long red = Foundation.invoke(colorInSpace, "redComponent").longValue();
    final long green = Foundation.invoke(colorInSpace, "greenComponent").longValue();
    final long blue = Foundation.invoke(colorInSpace, "blueComponent").longValue();
    final long alpha = Foundation.invoke(colorInSpace, "alphaComponent").longValue();
    final double realAlpha = alpha != 0 && (int)((alpha >> 52) & 0x7ffL) == 0 ? 1.0 : Double.longBitsToDouble(alpha);
    //noinspection UseJBColor
    return new Color((float)Double.longBitsToDouble(red),
                     (float)Double.longBitsToDouble(green),
                     (float)Double.longBitsToDouble(blue),
                     (float)realAlpha);
  }
}
