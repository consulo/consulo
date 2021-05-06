/*
 * Copyright 2013-2020 consulo.io
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
package consulo.awt.hacking;

import consulo.awt.hacking.util.MethodInvocator;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class JBViewportHacking {
  private static final MethodInvocator ourCanUseWindowBlitterMethod = new MethodInvocator(JViewport.class, "canUseWindowBlitter");
  private static final MethodInvocator ourGetPaintManagerMethod = new MethodInvocator(RepaintManager.class, "getPaintManager");
  private static final MethodInvocator ourGetUseTrueDoubleBufferingMethod = new MethodInvocator(JRootPane.class, "getUseTrueDoubleBuffering");


  /**
   * Blit-acceleration copies as much of the rendered area as possible and then repaints only newly exposed region.
   * This helps to improve scrolling performance and to reduce CPU usage (especially if drawing is compute-intensive).
   * <p>
   * Generally, this requires that viewport must not be obscured by its ancestors and must be showing.
   */
  @Nullable
  public static Boolean isWindowBlitterAvailableFor(JViewport viewport) {
    if (ourCanUseWindowBlitterMethod.isAvailable()) {
      return (Boolean)ourCanUseWindowBlitterMethod.invoke(viewport);
    }

    return null;
  }

  /**
   * True double buffering is needed to eliminate tearing on blit-accelerated scrolling and to restore
   * frame buffer content without the usual repainting, even when the EDT is blocked.
   * <p>
   * Generally, this requires default RepaintManager, swing.bufferPerWindow = true and
   * no prior direct invocations of JComponent.getGraphics() within JRootPane.
   * <p>
   * Use a breakpoint in JRootPane.disableTrueDoubleBuffering() to detect direct getGraphics() calls.
   * <p>
   * See GraphicsUtil.safelyGetGraphics() for more info.
   */
  @Nullable
  public static Boolean isTrueDoubleBufferingAvailableFor(JComponent component) {
    if (ourGetPaintManagerMethod.isAvailable()) {
      Object paintManager = ourGetPaintManagerMethod.invoke(RepaintManager.currentManager(component));

      if (!"javax.swing.BufferStrategyPaintManager".equals(paintManager.getClass().getName())) {
        return false;
      }

      if (ourGetUseTrueDoubleBufferingMethod.isAvailable()) {
        JRootPane rootPane = component.getRootPane();

        if (rootPane != null) {
          return (Boolean)ourGetUseTrueDoubleBufferingMethod.invoke(rootPane);
        }
      }
    }

    return null;
  }
}
