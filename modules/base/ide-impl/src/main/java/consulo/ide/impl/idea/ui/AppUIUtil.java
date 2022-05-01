/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.ApplicationNamesInfo;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.popup.Balloon;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * @author yole
 */
public class AppUIUtil {

  public static void invokeLaterIfProjectAlive(@Nonnull final Project project, @Nonnull final Runnable runnable) {
    final Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, o -> !project.isOpen() || project.isDisposed());
    }
  }

  public static void invokeOnEdt(Runnable runnable) {
    invokeOnEdt(runnable, null);
  }

  public static void invokeOnEdt(Runnable runnable, @Nullable BooleanSupplier condition) {
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else if (condition == null) {
      application.invokeLater(runnable);
    }
    else {
      application.invokeLater(runnable, condition);
    }
  }

  public static String getFrameClass() {
    String name = ApplicationNamesInfo.getInstance().getProductName().toLowerCase();
    String wmClass = StringUtil.replaceChar(name, ' ', '-');
    if ("true".equals(System.getProperty("idea.debug.mode"))) {
      wmClass += "-debug";
    }
    return wmClass;
  }

  public static void hideToolWindowBalloon(@Nonnull final String id, @Nonnull final Project project) {
    invokeLaterIfProjectAlive(project, () -> {
      Balloon balloon = ToolWindowManager.getInstance(project).getToolWindowBalloon(id);
      if (balloon != null) {
        balloon.hide();
      }
    });
  }

  /**
   * Targets the component to a (screen) device before showing.
   * In case the component is already a part of UI hierarchy (and is thus bound to a device)
   * the method does nothing.
   * <p>
   * The prior targeting to a device is required when there's a need to calculate preferred
   * size of a compound component (such as JEditorPane, for instance) which is not yet added
   * to a hierarchy. The calculation in that case may involve device-dependent metrics
   * (such as font metrics) and thus should refer to a particular device in multi-monitor env.
   * <p>
   * Note that if after calling this method the component is added to another hierarchy,
   * bound to a different device, AWT will throw IllegalArgumentException. To avoid that,
   * the device should be reset by calling {@code targetToDevice(comp, null)}.
   *
   * @param target the component representing the UI hierarchy and the target device
   * @param comp the component to target
   */
  public static void targetToDevice(@Nonnull Component comp, @Nullable Component target) {
    if (comp.isShowing()) return;
    GraphicsConfiguration gc = target != null ? target.getGraphicsConfiguration() : null;
    setGraphicsConfiguration(comp, gc);
  }

  public static void setGraphicsConfiguration(@Nonnull Component comp, @Nullable GraphicsConfiguration gc) {
    AWTAccessorHacking.setGraphicsConfiguration(comp, gc);
  }
}
