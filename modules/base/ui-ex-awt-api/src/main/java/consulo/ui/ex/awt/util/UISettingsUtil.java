/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.util;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.SystemProperties;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 21-Feb-22
 *
 * AWT part from UISettings
 */
public class UISettingsUtil {
  public static final boolean FORCE_USE_FRACTIONAL_METRICS = SystemProperties.getBooleanProperty("idea.force.use.fractional.metrics", false);

  /* This method must not be used for set up antialiasing for editor components
   */
  public static void setupAntialiasing(final Graphics g) {
    Graphics2D g2d = (Graphics2D)g;
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_LCD_CONTRAST, UIUtil.getLcdContrastValue());

    Application application = ApplicationManager.getApplication();
    if (application == null) {
      // We cannot use services while Application has not been loaded yet
      // So let's apply the default hints.
      UIUtil.applyRenderingHints(g);
      return;
    }

    UISettings uiSettings = UISettings.getInstanceOrNull();

    if (uiSettings != null) {
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false));
    }
    else {
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    setupFractionalMetrics(g2d);
  }

  /**
   * @see #setupComponentAntialiasing(JComponent)
   */
  public static void setupComponentAntialiasing(JComponent component) {
    GraphicsUtil.setAntialiasingType(component, DesktopAntialiasingTypeUtil.getAntialiasingTypeForSwingComponent());
  }

  public static void setupEditorAntialiasing(JComponent component) {
    UISettings settings = UISettings.getInstanceOrNull();
    if (settings != null) {
      GraphicsUtil.setAntialiasingType(component, settings.EDITOR_AA_TYPE);
    }
  }

  /**
   * Returns the default font size scaled by #defFontScale
   *
   * @return the default scaled font size
   */
  public static float getDefFontSize() {
    return Math.round(UIUtil.DEF_SYSTEM_FONT_SIZE * getDefFontScale());
  }

  /**
   * Returns the default font scale, which depends on the HiDPI mode (see JBUI#ScaleType).
   * <p>
   * The font is represented:
   * - in relative (dpi-independent) points in the JRE-managed HiDPI mode, so the method returns 1.0f
   * - in absolute (dpi-dependent) points in the IDE-managed HiDPI mode, so the method returns the default screen scale
   *
   * @return the system font scale
   */
  public static float getDefFontScale() {
    return UIUtil.isJreHiDPIEnabled() ? 1f : JBUI.sysScale();
  }

  public static void setupFractionalMetrics(final Graphics2D g2d) {
    if (FORCE_USE_FRACTIONAL_METRICS) {
      g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
  }
}
