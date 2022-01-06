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
package consulo.awt.impl;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.BitUtil;
import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.awt.TargetAWTFacade;
import consulo.container.StartupError;
import consulo.desktop.util.awt.MorphColor;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.desktop.internal.DesktopFontImpl;
import consulo.ui.desktop.internal.image.libraryImage.DesktopImageKeyImpl;
import consulo.ui.desktop.internal.window.DummyWindow;
import consulo.ui.desktop.internal.window.WindowOverAWTWindow;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;


/**
 * @author VISTALL
 * @since 2019-02-16
 */
public class TargetAWTFacadeImpl implements TargetAWTFacade {
  static class LoggerHolder {
    static final Logger LOG = Logger.getInstance(TargetAWTFacadeImpl.class);
  }

  private final static String popupHeavyWeightWindow = "javax.swing.Popup$HeavyWeightWindow";
  private final static String popupDefaultFrame = "javax.swing.Popup$DefaultFrame";

  static class StubWindow extends WindowOverAWTWindow {
    public StubWindow(java.awt.Window window) {
      super(window);
    }

    @RequiredUIAccess
    @Override
    public void setTitle(@Nonnull String title) {
    }
  }

  private StubWindow mySharedOwnerFrame;
  // stub for popup default frame. Popup create it when, no target window. just hack it
  private DummyWindow myPopupDefaultFrame;

  public TargetAWTFacadeImpl() {
    if (!GraphicsEnvironment.isHeadless()) {
      myPopupDefaultFrame = new DummyWindow();

      JDialog stubDialog = new JDialog((Frame)null);
      java.awt.Window sharedOwnerFrame = stubDialog.getOwner();
      // of dialog have owner - we need stub it
      if (sharedOwnerFrame != null) {
        mySharedOwnerFrame = new StubWindow(sharedOwnerFrame);
      }
    }
  }

  @Nonnull
  @Override
  public Component wrap(@Nonnull java.awt.Component component) {
    return new TempComponentWrapper(component);
  }

  @Override
  @Nonnull
  public java.awt.Dimension to(@Nonnull Size size) {
    return JBUI.size(size.getWidth(), size.getHeight());
  }

  @Override
  @Nonnull
  public java.awt.Color to(@Nonnull RGBColor color) {
    return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
  }

  @Override
  @Contract("null -> null")
  public java.awt.Color to(@Nullable ColorValue colorValue) {
    return colorValue == null ? null : MorphColor.of(() -> to(colorValue.toRGB()));
  }

  @Override
  @Contract("null -> null")
  public java.awt.Rectangle to(@Nullable Rectangle2D rectangle2D) {
    if (rectangle2D == null) {
      return null;
    }
    return new java.awt.Rectangle(rectangle2D.getCoordinate().getX(), rectangle2D.getCoordinate().getY(), rectangle2D.getSize().getWidth(), rectangle2D.getSize().getHeight());
  }

  @Override
  @Contract("null -> null")
  public java.awt.Component to(@Nullable Component component) {
    if (component == null) {
      return null;
    }

    if (component instanceof ToSwingComponentWrapper) {
      return ((ToSwingComponentWrapper)component).toAWTComponent();
    }

    if (component instanceof ToSwingWindowWrapper) {
      return ((ToSwingWindowWrapper)component).toAWTWindow();
    }

    throw new IllegalArgumentException(component + " is not SwingComponentWrapper or ToSwingWindowWrapper");
  }

  @Override
  @Contract("null -> null")
  public Component from(@Nullable java.awt.Component component) {
    if (component == null) {
      return null;
    }

    if (component instanceof FromSwingComponentWrapper) {
      return ((FromSwingComponentWrapper)component).toUIComponent();
    }
    throw new IllegalArgumentException(component + " is not FromSwingComponentWrapper");
  }

  @Override
  @Contract("null -> null")
  public java.awt.Window to(@Nullable Window window) {
    if (window == null) {
      return null;
    }

    if (myPopupDefaultFrame == window) {
      return null;
    }

    if (window instanceof ToSwingWindowWrapper) {
      return ((ToSwingWindowWrapper)window).toAWTWindow();
    }

    if (window == mySharedOwnerFrame) {
      return mySharedOwnerFrame.toAWTWindow();
    }
    throw new IllegalArgumentException(window + " is not SwingWindowWrapper");
  }

  @Override
  @Contract("null -> null")
  public Window from(@Nullable java.awt.Window window) {
    if (StartupError.hasStartupError) {
      return null;
    }

    if (window == null) {
      return null;
    }

    if (window instanceof FromSwingWindowWrapper) {
      return ((FromSwingWindowWrapper)window).toUIWindow();
    }

    if (mySharedOwnerFrame != null) {
      if (mySharedOwnerFrame.toAWTWindow() == window) {
        return mySharedOwnerFrame;
      }
    }

    String className = window.getClass().getName();
    if (popupDefaultFrame.equals(className)) {
      return myPopupDefaultFrame;
    }

    if (popupHeavyWeightWindow.equals(className)) {
      JWindow jWindow = (JWindow)window;

      JRootPane rootPane = jWindow.getRootPane();
      Object clientProperty = rootPane.getClientProperty(className);
      if (clientProperty != null) {
        return (Window)clientProperty;
      }
      else {
        StubWindow stubWindow = new StubWindow(window);
        rootPane.putClientProperty(className, stubWindow);
        return stubWindow;
      }
    }

    StringBuilder builder = new StringBuilder();
    builder.append("Window class: ");
    builder.append(window.getClass().getName());
    if (window instanceof RootPaneContainer) {
      JRootPane rootPane = ((RootPaneContainer)window).getRootPane();
      if (rootPane != null) {
        builder.append(", rootPane: ");
        builder.append(rootPane.getClass().getName());
        builder.append("/");
        builder.append(rootPane);
      }
    }
    builder.append(", window.toString(): ");
    builder.append(window.toString());
    builder.append(" is not FromSwingWindowWrapper");

    LoggerHolder.LOG.warn(new IllegalArgumentException(builder.toString()));
    return null;
  }

  @Override
  @Contract("null -> null")
  public Rectangle2D from(@Nullable java.awt.Rectangle rectangle) {
    if (rectangle == null) {
      return null;
    }
    return new Rectangle2D(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
  }

  @Override
  @Contract("null -> null")
  public ColorValue from(@Nullable java.awt.Color color) {
    if (color == null) {
      return null;
    }
    return new AWTColorValue(color);
  }

  @Override
  @Contract("null -> null")
  public javax.swing.Icon to(@Nullable Image image) {
    if (image == null) {
      return null;
    }

    if (image instanceof ToSwingIconWrapper) {
      return ((ToSwingIconWrapper)image).toSwingIcon();
    }
    else if (image instanceof javax.swing.Icon) {
      return (javax.swing.Icon)image;
    }

    throw new IllegalArgumentException(image + "' is not supported");
  }

  @Override
  @Contract("null -> null")
  public Image from(@Nullable javax.swing.Icon icon) {
    if (icon == null) {
      return null;
    }

    if (icon instanceof Image) {
      return (Image)icon;
    }

    throw new IllegalArgumentException(icon + "' is not supported");
  }

  @Nonnull
  @Override
  public Font to(@Nonnull consulo.ui.font.Font font) {
    if (font instanceof DesktopFontImpl) {
      return ((DesktopFontImpl)font).getFont();
    }
    throw new UnsupportedOperationException(font + " unsupported");
  }

  @Override
  public java.awt.Image toImage(@Nonnull ImageKey key, @Nullable JBUI.ScaleContext ctx) {
    DesktopImageKeyImpl desktopImageKey = (DesktopImageKeyImpl)key;
    return desktopImageKey.toAWTImage(ctx);
  }

  @Override
  public Cursor to(consulo.ui.cursor.Cursor cursor) {
    if (cursor instanceof StandardCursors systemCursor) {
      switch (systemCursor) {
        case ARROW:
          return Cursor.getDefaultCursor();
        case CROSSHAIR:
          return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        case TEXT:
          return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        case WAIT:
          return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        case HAND:
          return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      }
    }
    throw new UnsupportedOperationException(cursor.toString());
  }

  @Override
  public consulo.ui.cursor.Cursor from(Cursor cursor) {
    if (cursor == null) {
      return null;
    }

    switch (cursor.getType()) {
      case Cursor.DEFAULT_CURSOR:
        return StandardCursors.ARROW;
      case Cursor.CROSSHAIR_CURSOR:
        return StandardCursors.CROSSHAIR;
      case Cursor.TEXT_CURSOR:
        return StandardCursors.TEXT;
      case Cursor.WAIT_CURSOR:
        return StandardCursors.WAIT;
      case Cursor.HAND_CURSOR:
        return StandardCursors.HAND;

    }
    throw new UnsupportedOperationException(cursor.toString());
  }

  public static SimpleTextAttributes from(@Nonnull TextAttribute textAttribute) {
    int mask = 0;

    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_PLAIN, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_PLAIN));
    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_BOLD, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_BOLD));
    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_ITALIC, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_ITALIC));

    ColorValue backgroundColor = textAttribute.getBackgroundColor();
    ColorValue foregroundColor = textAttribute.getForegroundColor();
    return new SimpleTextAttributes(mask, TargetAWT.to(foregroundColor), TargetAWT.to(backgroundColor));
  }
}
