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
package com.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ImageLoader;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import sun.awt.AWTAccessor;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

/**
 * @author yole
 */
public class AppUIUtil {
  @SuppressWarnings("deprecation")
  public static void updateWindowIcon(@Nonnull Window window) {
    ApplicationInfoEx appInfo = ApplicationInfoImpl.getShadowInstance();
    List<Image> images = ContainerUtil.newArrayListWithCapacity(2);

    images.add(ImageLoader.loadFromResource(appInfo.getIconUrl()));
    images.add(ImageLoader.loadFromResource(appInfo.getSmallIconUrl()));

    for (int i = 0; i < images.size(); i++) {
      Image image = images.get(i);
      if (image instanceof JBHiDPIScaledImage) {
        images.set(i, ((JBHiDPIScaledImage)image).getDelegate());
      }
    }

    window.setIconImages(images);
  }

  public static void invokeLaterIfProjectAlive(@Nonnull final Project project, @Nonnull final Runnable runnable) {
    final Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, new Condition() {
        @Override
        public boolean value(Object o) {
          return !project.isOpen() || project.isDisposed();
        }
      });
    }
  }

  public static void invokeOnEdt(Runnable runnable) {
    invokeOnEdt(runnable, null);
  }

  public static void invokeOnEdt(Runnable runnable, @Nullable Condition condition) {
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

  public static void updateFrameClass() {
    try {
      final Toolkit toolkit = Toolkit.getDefaultToolkit();
      final Class<? extends Toolkit> aClass = toolkit.getClass();
      if ("sun.awt.X11.XToolkit".equals(aClass.getName())) {
        final Field awtAppClassName = aClass.getDeclaredField("awtAppClassName");
        awtAppClassName.setAccessible(true);
        awtAppClassName.set(toolkit, getFrameClass());
      }
    }
    catch (Exception ignore) {
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

  public static void registerBundledFonts() {
    registerFont("/fonts/Inconsolata.ttf");
    registerFont("/fonts/SourceCodePro-Regular.ttf");
    registerFont("/fonts/SourceCodePro-Bold.ttf");
    registerFont("/fonts/FiraCode-Bold.ttf");
    registerFont("/fonts/FiraCode-Light.ttf");
    registerFont("/fonts/FiraCode-Medium.ttf");
    registerFont("/fonts/FiraCode-Regular.ttf");
    registerFont("/fonts/FiraCode-Retina.ttf");
  }

  private static void registerFont(@NonNls String name) {
    try {
      URL url = AppUIUtil.class.getResource(name);
      if (url == null) {
        throw new IOException("Resource missing: " + name);
      }

      InputStream is = url.openStream();
      try {
        Font font = Font.createFont(Font.TRUETYPE_FONT, is);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
      }
      finally {
        is.close();
      }
    }
    catch (Exception e) {
      Logger.getInstance(AppUIUtil.class).error("Cannot register font: " + name, e);
    }
  }

  public static void hideToolWindowBalloon(@Nonnull final String id, @Nonnull final Project project) {
    invokeLaterIfProjectAlive(project, new Runnable() {
      @Override
      public void run() {
        Balloon balloon = ToolWindowManager.getInstance(project).getToolWindowBalloon(id);
        if (balloon != null) {
          balloon.hide();
        }
      }
    });
  }

  public static JTextField createUndoableTextField() {
    JTextField field = new JTextField();
    new TextComponentUndoProvider(field);
    return field;
  }

  private static final int MIN_ICON_SIZE = 32;

  @Nullable
  public static String findIcon(final String iconsPath) {
    final File iconsDir = new File(iconsPath);

    // 1. look for .svg icon
    for (String child : iconsDir.list()) {
      if (child.endsWith(".svg")) {
        return iconsPath + '/' + child;
      }
    }

    // 2. look for .png icon of max size
    int max = 0;
    String iconPath = null;
    for (String child : iconsDir.list()) {
      if (!child.endsWith(".png")) continue;
      final String path = iconsPath + '/' + child;
      final Icon icon = new ImageIcon(path);
      final int size = icon.getIconHeight();
      if (size >= MIN_ICON_SIZE && size > max && size == icon.getIconWidth()) {
        max = size;
        iconPath = path;
      }
    }

    return iconPath;
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
    AWTAccessor.getComponentAccessor().setGraphicsConfiguration(comp, gc);
  }
}
