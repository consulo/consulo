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
package com.intellij.util.ui;

import com.intellij.BundleBase;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.ui.*;
import com.intellij.ui.mac.foundation.Foundation;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.ui.paint.PaintUtil;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.JBTreeTraverser;
import consulo.annotation.DeprecationInfo;
import consulo.desktop.awt.util.DarkThemeCalculator;
import consulo.desktop.util.awt.MorphColor;
import consulo.desktop.util.awt.StringHtmlUtil;
import consulo.desktop.util.awt.laf.BuildInLookAndFeel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.image.ImageKey;
import consulo.util.dataholder.Key;
import org.intellij.lang.annotations.JdkConstants;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.PixelGrabber;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author max
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public class UIUtil {
  @Deprecated
  @DeprecationInfo("StyleManager.get().getCurrentStyle().isDark(), and don't call this method inside swing paint code")
  public static boolean isUnderDarkTheme() {
    return DarkThemeCalculator.isDark();
  }

  public static final Key<Iterable<? extends Component>> NOT_IN_HIERARCHY_COMPONENTS = Key.create("NOT_IN_HIERARCHY_COMPONENTS");
  private static final Function<Component, Iterable<Component>> COMPONENT_CHILDREN = new Function<Component, Iterable<Component>>() {
    @Nonnull
    @Override
    public JBIterable<Component> fun(@Nonnull Component c) {
      JBIterable<Component> result;
      if (c instanceof JMenu) {
        result = JBIterable.of(((JMenu)c).getMenuComponents());
      }
      else if (c instanceof Container) {
        result = JBIterable.of(((Container)c).getComponents());
      }
      else {
        result = JBIterable.empty();
      }
      if (c instanceof JComponent) {
        JComponent jc = (JComponent)c;
        Iterable<? extends Component> orphans = getClientProperty(jc, NOT_IN_HIERARCHY_COMPONENTS);
        if (orphans != null) {
          result = result.append(orphans);
        }
        JPopupMenu jpm = jc.getComponentPopupMenu();
        if (jpm != null && jpm.isVisible() && jpm.getInvoker() == jc) {
          result = result.append(Collections.singletonList(jpm));
        }
      }
      return result;
    }
  };

  private static final Function.Mono<Component> COMPONENT_PARENT = new Function.Mono<Component>() {
    @Override
    public Component fun(Component c) {
      return c.getParent();
    }
  };

  public static int getMultiClickInterval() {
    Object property = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
    if (property instanceof Integer) {
      return (Integer)property;
    }
    return 500;
  }

  private static Key<UndoManager> UNDO_MANAGER = Key.create("undoManager");
  private static final AbstractAction REDO_ACTION = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      UndoManager manager = source instanceof JComponent ? getClientProperty((JComponent)source, UNDO_MANAGER) : null;
      if (manager != null && manager.canRedo()) {
        manager.redo();
      }
    }
  };
  private static final AbstractAction UNDO_ACTION = new AbstractAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      Object source = e.getSource();
      UndoManager manager = source instanceof JComponent ? getClientProperty((JComponent)source, UNDO_MANAGER) : null;
      if (manager != null && manager.canUndo()) {
        manager.undo();
      }
    }
  };

  private static final AtomicNotNullLazyValue<Boolean> X_RENDER_ACTIVE = new AtomicNotNullLazyValue<Boolean>() {
    @Nonnull
    @Override
    protected Boolean compute() {
      if (!SystemInfo.isXWindow) {
        return false;
      }
      try {
        final Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("sun.awt.X11GraphicsEnvironment");
        final Method method = clazz.getMethod("isXRenderAvailable");
        return (Boolean)method.invoke(null);
      }
      catch (Throwable e) {
        return false;
      }
    }
  };


  public static final String CHECKBOX_ROLLOVER_PROPERTY = "JCheckBox.rollOver.rectangle";
  public static final String CHECKBOX_PRESSED_PROPERTY = "JCheckBox.pressed.rectangle";


  /**
   * Alt+click does copy text from tooltip or balloon to clipboard.
   * We collect this text from components recursively and this generic approach might 'grab' unexpected text fragments.
   * To provide more accurate text scope you should mark dedicated component with putClientProperty(TEXT_COPY_ROOT, Boolean.TRUE)
   * Note, main(root) components of BalloonImpl and AbstractPopup are already marked with this key
   */
  public static final Key<Boolean> TEXT_COPY_ROOT = Key.create("TEXT_COPY_ROOT");

  private static final String[] STANDARD_FONT_SIZES = {"8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"};

  public static final String BORDER_LINE = "<hr size=1 noshade>";
  public static final String BR = "<br/>";
  public static final String HR = "<hr/>";
  public static final String LINE_SEPARATOR = "\n";

  /**
   * The comment to display when the cursor is over the component,
   * also known as a "value tip", "flyover help", or "flyover label".
   *
   * @see JComponent#TOOL_TIP_TEXT_KEY
   */
  public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

  public static void applyStyle(@Nonnull ComponentStyle componentStyle, @Nonnull Component comp) {
    if (!(comp instanceof JComponent)) return;

    JComponent c = (JComponent)comp;

    if (isUnderAquaBasedLookAndFeel()) {
      c.putClientProperty("JComponent.sizeVariant", StringUtil.toLowerCase(componentStyle.name()));
    }
    FontSize fontSize = componentStyle == ComponentStyle.MINI ? FontSize.MINI : componentStyle == ComponentStyle.SMALL ? FontSize.SMALL : FontSize.NORMAL;
    c.setFont(getFont(fontSize, c.getFont()));
    Container p = c.getParent();
    if (p != null) {
      SwingUtilities.updateComponentTreeUI(p);
    }
  }

  public static Cursor getTextCursor(final Color backgroundColor) {
    return SystemInfo.isMac && ColorUtil.isDark(backgroundColor) ? MacUIUtil.getInvertedTextCursor() : Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
  }

  /**
   * Draws two horizontal lines, the first at {@code topY}, the second at {@code bottomY}.
   * The purpose of this method (and the ground of the name) is to draw two lines framing a horizontal filled rectangle.
   *
   * @param g       Graphics context to draw with.
   * @param startX  x-start point.
   * @param endX    x-end point.
   * @param topY    y-coordinate of the first line.
   * @param bottomY y-coordinate of the second line.
   * @param color   color of the lines.
   */
  public static void drawFramingLines(@Nonnull Graphics2D g, int startX, int endX, int topY, int bottomY, @Nonnull Color color) {
    drawLine(g, startX, topY, endX, topY, null, color);
    drawLine(g, startX, bottomY, endX, bottomY, null, color);
  }

  private static final GrayFilter DEFAULT_GRAY_FILTER = new GrayFilter(true, 65);
  private static final GrayFilter DARCULA_GRAY_FILTER = new GrayFilter(true, 30);

  public static GrayFilter getGrayFilter() {
    return getGrayFilter(isUnderDarkTheme());
  }

  public static GrayFilter getGrayFilter(boolean dark) {
    return dark ? DARCULA_GRAY_FILTER : DEFAULT_GRAY_FILTER;
  }

  public static boolean isAppleRetina() {
    return isRetina() && SystemInfo.isAppleJvm;
  }

  @Nonnull
  public static JBIterable<Component> uiParents(@Nullable Component c, boolean strict) {
    return strict ? JBIterable.generate(c, COMPONENT_PARENT).skip(1) : JBIterable.generate(c, COMPONENT_PARENT);
  }

  @Nonnull
  public static JBIterable<Component> uiChildren(@Nullable Component component) {
    if (!(component instanceof Container)) return JBIterable.empty();
    Container container = (Container)component;
    return JBIterable.of(container.getComponents());
  }

  @Nonnull
  public static JBTreeTraverser<Component> uiTraverser() {
    return new JBTreeTraverser<Component>(COMPONENT_CHILDREN);
  }

  @Nonnull
  public static JBTreeTraverser<Component> uiTraverser(@Nullable Component component) {
    return new JBTreeTraverser<Component>(COMPONENT_CHILDREN).withRoot(component);
  }

  public enum FontSize {
    BIGGER,
    NORMAL,
    SMALL,
    MINI
  }

  public enum ComponentStyle {
    LARGE,
    REGULAR,
    SMALL,
    MINI
  }

  public enum FontColor {
    NORMAL,
    BRIGHTER
  }

  public static final char MNEMONIC = BundleBase.MNEMONIC;
  @NonNls
  public static final String HTML_MIME = "text/html";
  @NonNls
  public static final String JSLIDER_ISFILLED = "JSlider.isFilled";
  @NonNls
  public static final String ARIAL_FONT_NAME = "Arial";
  @NonNls
  public static final String TABLE_FOCUS_CELL_BACKGROUND_PROPERTY = "Table.focusCellBackground";
  /**
   * Prevent component DataContext from returning parent editor
   * Useful for components that are manually painted over the editor to prevent shortcuts from falling-through to editor
   * <p>
   * Usage: {@code component.putClientProperty(HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, Boolean.TRUE)}
   */
  @NonNls
  public static final String HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY = "AuxEditorComponent";
  @NonNls
  public static final String CENTER_TOOLTIP_DEFAULT = "ToCenterTooltip";
  @NonNls
  public static final String CENTER_TOOLTIP_STRICT = "ToCenterTooltip.default";

  public static final Pattern CLOSE_TAG_PATTERN = Pattern.compile("<\\s*([^<>/ ]+)([^<>]*)/\\s*>", Pattern.CASE_INSENSITIVE);

  @NonNls
  public static final String FOCUS_PROXY_KEY = "isFocusProxy";

  public static Key<Integer> KEEP_BORDER_SIDES = Key.create("keepBorderSides");

  private static final Color ACTIVE_HEADER_COLOR = JBColor.namedColor("HeaderColor.active", 0xa0bad5);
  private static final Color INACTIVE_HEADER_COLOR = JBColor.namedColor("HeaderColor.inactive", Gray._128);

  public static final Color CONTRAST_BORDER_COLOR = JBColor.namedColor("Borders.ContrastBorderColor", new JBColor(0xC9C9C9, 0x323232));

  public static final Color AQUA_SEPARATOR_FOREGROUND_COLOR = Gray._190;
  public static final Color AQUA_SEPARATOR_BACKGROUND_COLOR = Gray._240;
  public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

  public static final int DEFAULT_HGAP = 10;
  public static final int DEFAULT_VGAP = 4;
  public static final int LARGE_VGAP = 12;

  public static final Insets PANEL_REGULAR_INSETS = new Insets(8, 12, 8, 12);
  public static final Insets PANEL_SMALL_INSETS = new Insets(5, 8, 5, 8);


  public static final Border DEBUG_MARKER_BORDER = new Border() {
    private final Insets empty = new Insets(0, 0, 0, 0);

    @Override
    public Insets getBorderInsets(Component c) {
      return empty;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics g2 = g.create();
      try {
        g2.setColor(JBColor.RED);
        drawDottedRectangle(g2, x, y, x + width - 1, y + height - 1);
      }
      finally {
        g2.dispose();
      }
    }

    @Override
    public boolean isBorderOpaque() {
      return true;
    }
  };

  public static float DEF_SYSTEM_FONT_SIZE = 12f; // TODO: consider 12 * 1.33 to compensate JDK's 72dpi font scale

  @NonNls
  private static final String ROOT_PANE = "JRootPane.future";

  private static final Ref<Boolean> ourRetina = Ref.create(SystemInfo.isMac ? null : false);

  private static volatile StyleSheet ourDefaultHtmlKitCss;

  private UIUtil() {
  }

  public static void configureHtmlKitStylesheet(@Nonnull Supplier<? extends StyleSheet> styleSheetFactory) {
    if (ourDefaultHtmlKitCss != null) {
      return;
    }


    // save the default JRE CSS and ..
    HTMLEditorKit kit = new HTMLEditorKit();
    ourDefaultHtmlKitCss = kit.getStyleSheet();
    // .. erase global ref to this CSS so no one can alter it
    kit.setStyleSheet(null);

    // Applied to all JLabel instances, including subclasses. Supported in JBR only.
    UIManager.getDefaults().put("javax.swing.JLabel.userStyleSheet", styleSheetFactory.get());
  }

  public static StyleSheet getDefaultHtmlKitCss() {
    return ourDefaultHtmlKitCss;
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the default monitor device is HiDPI.
   * (analogue of {@link #isRetina()} on macOS)
   */
  public static boolean isJreHiDPI() {
    return isJreHiDPI((GraphicsConfiguration)null);
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the graphics configuration represents a HiDPI device.
   * (analogue of {@link #isRetina(Graphics2D)} on macOS)
   */
  public static boolean isJreHiDPI(@Nullable GraphicsConfiguration gc) {
    return isJreHiDPIEnabled() && JBUI.isHiDPI(JBUI.sysScale(gc));
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the graphics represents a HiDPI device.
   * (analogue of {@link #isRetina(Graphics2D)} on macOS)
   */
  public static boolean isJreHiDPI(@Nullable Graphics2D g) {
    return isJreHiDPIEnabled() && JBUI.isHiDPI(JBUI.sysScale(g));
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the provided component is tied to a HiDPI device.
   */
  public static boolean isJreHiDPI(@Nullable Component comp) {
    return isJreHiDPI(comp != null ? comp.getGraphicsConfiguration() : null);
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled and the provided system scale context is HiDPI.
   */
  public static boolean isJreHiDPI(@Nullable JBUI.ScaleContext ctx) {
    return isJreHiDPIEnabled() && JBUI.isHiDPI(JBUI.sysScale(ctx));
  }

  /**
   * Returns whether the JRE-managed HiDPI mode is enabled.
   * (True for macOS JDK >= 7.10 versions)
   *
   * @see JBUI.ScaleType
   */
  @Deprecated
  public static boolean isJreHiDPIEnabled() {
    return JreHiDpiUtil.isJreHiDPIEnabled();
  }

  public static boolean isRetina(Graphics2D graphics) {
    return SystemInfo.isMac ? DetectRetinaKit.isMacRetina(graphics) : isRetina();
  }

  public static boolean isRetina() {
    if (GraphicsEnvironment.isHeadless()) return false;

    //Temporary workaround for HiDPI on Windows/Linux
    if ("true".equalsIgnoreCase(System.getProperty("is.hidpi"))) {
      return true;
    }

    if (Registry.is("new.retina.detection", false)) {
      return DetectRetinaKit.isRetina();
    }
    else {
      synchronized (ourRetina) {
        if (ourRetina.isNull()) {
          ourRetina.set(false); // in case HiDPIScaledImage.drawIntoImage is not called for some reason

          try {
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice device = env.getDefaultScreenDevice();
            Integer scale = ReflectionUtil.getField(device.getClass(), device, int.class, "scale");
            if (scale != null && scale.intValue() == 2) {
              ourRetina.set(true);
              return true;
            }
          }
          catch (AWTError | Exception ignore) {
          }
          ourRetina.set(false);
        }

        return ourRetina.get();
      }
    }
  }

  public static boolean hasLeakingAppleListeners() {
    // in version 1.6.0_29 Apple introduced a memory leak in JViewport class - they add a PropertyChangeListeners to the CToolkit
    // but never remove them:
    // JViewport.java:
    // public JViewport() {
    //   ...
    //   final Toolkit toolkit = Toolkit.getDefaultToolkit();
    //   if(toolkit instanceof CToolkit)
    //   {
    //     final boolean isRunningInHiDPI = ((CToolkit)toolkit).runningInHiDPI();
    //     if(isRunningInHiDPI) setScrollMode(0);
    //     toolkit.addPropertyChangeListener("apple.awt.contentScaleFactor", new PropertyChangeListener() { ... });
    //   }
    // }

    return SystemInfo.isMac && System.getProperty("java.runtime.version").startsWith("1.6.0_29");
  }

  public static void removeLeakingAppleListeners() {
    if (!hasLeakingAppleListeners()) return;

    Toolkit toolkit = Toolkit.getDefaultToolkit();
    String name = "apple.awt.contentScaleFactor";
    for (PropertyChangeListener each : toolkit.getPropertyChangeListeners(name)) {
      toolkit.removePropertyChangeListener(name, each);
    }
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       the client property key
   * @return {@code true} if the property of the specified component is set to {@code true}
   */
  public static boolean isClientPropertyTrue(Object component, @Nonnull Object key) {
    return Boolean.TRUE.equals(getClientProperty(component, key));
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       the client property key that specifies a return type
   * @return the property value from the specified component or {@code null}
   */
  public static Object getClientProperty(Object component, @Nonnull Object key) {
    return component instanceof JComponent ? ((JComponent)component).getClientProperty(key) : null;
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param type      the client property key that specifies a return type
   * @return the property value from the specified component or {@code null}
   */
  public static <T> T getClientProperty(Object component, @Nonnull Class<T> type) {
    return ObjectUtils.tryCast(getClientProperty(component, (Object)type), type);
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       the client property key that specifies a return type
   * @return the property value from the specified component or {@code null}
   */
  public static <T> T getClientProperty(Object component, @Nonnull Key<T> key) {
    //noinspection unchecked
    return (T)getClientProperty(component, (Object)key);
  }

  public static <T> void putClientProperty(@Nonnull JComponent component, @Nonnull Key<T> key, T value) {
    component.putClientProperty(key, value);
  }

  public static String getHtmlBody(String text) {
    return getHtmlBody(new Html(text));
  }

  public static String getHtmlBody(Html html) {
    String text = html.getText();
    String result;
    if (!text.startsWith("<html>")) {
      result = text.replaceAll("\n", "<br>");
    }
    else {
      final int bodyIdx = text.indexOf("<body>");
      final int closedBodyIdx = text.indexOf("</body>");
      if (bodyIdx != -1 && closedBodyIdx != -1) {
        result = text.substring(bodyIdx + "<body>".length(), closedBodyIdx);
      }
      else {
        text = StringUtil.trimStart(text, "<html>").trim();
        text = StringUtil.trimEnd(text, "</html>").trim();
        text = StringUtil.trimStart(text, "<body>").trim();
        text = StringUtil.trimEnd(text, "</body>").trim();
        result = text;
      }
    }

    return html.isKeepFont() ? result : result.replaceAll("<font(.*?)>", "").replaceAll("</font>", "");
  }

  public static void drawLinePickedOut(Graphics graphics, int x, int y, int x1, int y1) {
    if (x == x1) {
      int minY = Math.min(y, y1);
      int maxY = Math.max(y, y1);
      graphics.drawLine(x, minY + 1, x1, maxY - 1);
    }
    else if (y == y1) {
      int minX = Math.min(x, x1);
      int maxX = Math.max(x, x1);
      graphics.drawLine(minX + 1, y, maxX - 1, y1);
    }
    else {
      drawLine(graphics, x, y, x1, y1);
    }
  }

  public static boolean isReallyTypedEvent(KeyEvent e) {
    char c = e.getKeyChar();
    if (c < 0x20 || c == 0x7F) return false;

    if (SystemInfo.isMac) {
      return !e.isMetaDown() && !e.isControlDown();
    }

    return !e.isAltDown() && !e.isControlDown();
  }

  public static int getStringY(@Nonnull final String string, @Nonnull final Rectangle bounds, @Nonnull final Graphics2D g) {
    final int centerY = bounds.height / 2;
    final Font font = g.getFont();
    final FontRenderContext frc = g.getFontRenderContext();
    final Rectangle stringBounds = font.getStringBounds(string, frc).getBounds();

    return (int)(centerY - stringBounds.height / 2.0 - stringBounds.y);
  }

  public static void drawLabelDottedRectangle(final @Nonnull JLabel label, final @Nonnull Graphics g) {
    drawLabelDottedRectangle(label, g, null);
  }

  public static void drawLabelDottedRectangle(final @Nonnull JLabel label, final @Nonnull Graphics g, @Nullable Rectangle bounds) {
    if (bounds == null) {
      bounds = getLabelTextBounds(label);
    }
    // JLabel draws the text relative to the baseline. So, we must ensure
    // we draw the dotted rectangle relative to that same baseline.
    FontMetrics fm = label.getFontMetrics(label.getFont());
    int baseLine = label.getUI().getBaseline(label, label.getWidth(), label.getHeight());
    int textY = baseLine - fm.getLeading() - fm.getAscent();
    int textHeight = fm.getHeight();
    drawDottedRectangle(g, bounds.x, textY, bounds.x + bounds.width - 1, textY + textHeight - 1);
  }

  @Nonnull
  public static Rectangle getLabelTextBounds(final @Nonnull JLabel label) {
    final Dimension size = label.getPreferredSize();
    Icon icon = label.getIcon();
    final Point point = new Point(0, 0);
    final Insets insets = label.getInsets();
    if (icon != null) {
      if (label.getHorizontalTextPosition() == SwingConstants.TRAILING) {
        point.x += label.getIconTextGap();
        point.x += icon.getIconWidth();
      }
      else if (label.getHorizontalTextPosition() == SwingConstants.LEADING) {
        size.width -= icon.getIconWidth();
      }
    }
    point.x += insets.left;
    point.y += insets.top;
    size.width -= point.x;
    size.width -= insets.right;
    size.height -= insets.bottom;

    return new Rectangle(point, size);
  }

  /**
   * @param string   {@code String} to examine
   * @param font     {@code Font} that is used to render the string
   * @param graphics {@link Graphics} that should be used to render the string
   * @return height of the tallest glyph in a string. If string is empty, returns 0
   */
  public static int getHighestGlyphHeight(@Nonnull String string, @Nonnull Font font, @Nonnull Graphics graphics) {
    FontRenderContext frc = ((Graphics2D)graphics).getFontRenderContext();
    GlyphVector gv = font.createGlyphVector(frc, string);
    int maxHeight = 0;
    for (int i = 0; i < string.length(); i++) {
      maxHeight = Math.max(maxHeight, (int)gv.getGlyphMetrics(i).getBounds2D().getHeight());
    }
    return maxHeight;
  }

  public static void setEnabled(Component component, boolean enabled, boolean recursively) {
    component.setEnabled(enabled);
    if (component instanceof JLabel) {
      Color color = enabled ? getLabelForeground() : getLabelDisabledForeground();
      if (color != null) {
        component.setForeground(color);
      }
    }
    if (recursively && enabled == component.isEnabled()) {
      if (component instanceof Container) {
        final Container container = (Container)component;
        final int subComponentCount = container.getComponentCount();
        for (int i = 0; i < subComponentCount; i++) {
          setEnabled(container.getComponent(i), enabled, recursively);
        }
      }
    }
  }

  /**
   * @deprecated Use {@link LinePainter2D#paint(Graphics2D, double, double, double, double)} instead.
   */
  public static void drawLine(Graphics g, int x1, int y1, int x2, int y2) {
    LinePainter2D.paint((Graphics2D)g, x1, y1, x2, y2);
  }

  public static void drawLine(Graphics2D g, int x1, int y1, int x2, int y2, @Nullable Color bgColor, @Nullable Color fgColor) {
    Color oldFg = g.getColor();
    Color oldBg = g.getBackground();
    if (fgColor != null) {
      g.setColor(fgColor);
    }
    if (bgColor != null) {
      g.setBackground(bgColor);
    }
    drawLine(g, x1, y1, x2, y2);
    if (fgColor != null) {
      g.setColor(oldFg);
    }
    if (bgColor != null) {
      g.setBackground(oldBg);
    }
  }

  public static void drawWave(Graphics2D g, Rectangle rectangle) {
    GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    Stroke oldStroke = g.getStroke();
    try {
      g.setStroke(new BasicStroke(0.7F));
      double cycle = 4;
      final double wavedAt = rectangle.y + (double)rectangle.height / 2 - .5;
      GeneralPath wavePath = new GeneralPath();
      wavePath.moveTo(rectangle.x, wavedAt - Math.cos(rectangle.x * 2 * Math.PI / cycle));
      for (int x = rectangle.x + 1; x <= rectangle.x + rectangle.width; x++) {
        wavePath.lineTo(x, wavedAt - Math.cos(x * 2 * Math.PI / cycle));
      }
      g.draw(wavePath);
    }
    finally {
      config.restore();
      g.setStroke(oldStroke);
    }
  }

  @Nonnull
  public static String[] splitText(String text, FontMetrics fontMetrics, int widthLimit, char separator) {
    ArrayList<String> lines = new ArrayList<String>();
    String currentLine = "";
    StringBuilder currentAtom = new StringBuilder();

    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      currentAtom.append(ch);

      if (ch == separator) {
        currentLine += currentAtom.toString();
        currentAtom.setLength(0);
      }

      String s = currentLine + currentAtom.toString();
      int width = fontMetrics.stringWidth(s);

      if (width >= widthLimit - fontMetrics.charWidth('w')) {
        if (!currentLine.isEmpty()) {
          lines.add(currentLine);
          currentLine = "";
        }
        else {
          lines.add(currentAtom.toString());
          currentAtom.setLength(0);
        }
      }
    }

    String s = currentLine + currentAtom.toString();
    if (!s.isEmpty()) {
      lines.add(s);
    }

    return ArrayUtil.toStringArray(lines);
  }

  public static void setActionNameAndMnemonic(@Nonnull String text, @Nonnull Action action) {
    assignMnemonic(text, action);

    text = text.replaceAll("&", "");
    action.putValue(Action.NAME, text);
  }

  public static void assignMnemonic(@Nonnull String text, @Nonnull Action action) {
    int mnemoPos = text.indexOf('&');
    if (mnemoPos >= 0 && mnemoPos < text.length() - 2) {
      String mnemoChar = text.substring(mnemoPos + 1, mnemoPos + 2).trim();
      if (mnemoChar.length() == 1) {
        action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemoChar.charAt(0)));
      }
    }
  }

  public static Font getLabelFont(@Nonnull FontSize size) {
    return getFont(size, null);
  }

  @Nonnull
  public static Font getFont(@Nonnull FontSize size, @Nullable Font base) {
    if (base == null) base = getLabelFont();

    return base.deriveFont(getFontSize(size));
  }

  public static float getFontSize(FontSize size) {
    int defSize = getLabelFont().getSize();
    switch (size) {
      case BIGGER:
        return Math.max(defSize + JBUI.scale(2f), JBUI.scale(13f));
      case SMALL:
        return Math.max(defSize - JBUI.scale(2f), JBUI.scale(11f));
      case MINI:
        return Math.max(defSize - JBUI.scale(4f), JBUI.scale(9f));
      default:
        return defSize;
    }
  }

  public static Color getLabelFontColor(FontColor fontColor) {
    Color defColor = getLabelForeground();
    if (fontColor == FontColor.BRIGHTER) {
      return new JBColor(new Color(Math.min(defColor.getRed() + 50, 255), Math.min(defColor.getGreen() + 50, 255), Math.min(defColor.getBlue() + 50, 255)), defColor.darker());
    }
    return defColor;
  }

  public static int getScrollBarWidth() {
    return UIManager.getInt("ScrollBar.width");
  }

  public static Font getLabelFont() {
    return UIManager.getFont("Label.font");
  }

  public static Color getLabelBackground() {
    return UIManager.getColor("Label.background");
  }

  @Nonnull
  public static Color getContextHelpForeground() {
    return JBColor.namedColor("Label.infoForeground", new JBColor(Gray.x78, Gray.x8C));
  }

  public static Color getLabelForeground() {
    return JBColor.namedColor("Label.foreground", new JBColor(Gray._0, Gray.xBB));
  }

  public static Color getErrorForeground() {
    return JBColor.namedColor("Label.errorForeground", new JBColor(new Color(0xC7222D), JBColor.RED));
  }

  public static Color getLabelDisabledForeground() {
    final Color color = UIManager.getColor("Label.disabledForeground");
    if (color != null) return color;
    return UIManager.getColor("Label.disabledText");
  }

  @Nonnull
  public static String removeMnemonic(@Nonnull String s) {
    if (s.indexOf('&') != -1) {
      s = StringUtil.replace(s, "&", "");
    }
    if (s.indexOf('_') != -1) {
      s = StringUtil.replace(s, "_", "");
    }
    if (s.indexOf(MNEMONIC) != -1) {
      s = StringUtil.replace(s, String.valueOf(MNEMONIC), "");
    }
    return s;
  }

  public static int getDisplayMnemonicIndex(@Nonnull String s) {
    int idx = s.indexOf('&');
    if (idx >= 0 && idx != s.length() - 1 && idx == s.lastIndexOf('&')) return idx;

    idx = s.indexOf(MNEMONIC);
    if (idx >= 0 && idx != s.length() - 1 && idx == s.lastIndexOf(MNEMONIC)) return idx;

    return -1;
  }

  public static String replaceMnemonicAmpersand(final String value) {
    return BundleBase.replaceMnemonicAmpersand(value);
  }

  public static Color getTableHeaderBackground() {
    return UIManager.getColor("TableHeader.background");
  }

  public static Color getTreeTextForeground() {
    return MorphColor.of(new NotNullProducer<Color>() {
      @Nonnull
      @Override
      public Color produce() {
        return UIManager.getColor("Tree.textForeground");
      }
    });
  }

  public static Color getTreeTextBackground() {
    return UIManager.getColor("Tree.textBackground");
  }

  /**
   * @deprecated use {@link #getListSelectionForeground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getListSelectionForeground() {
    return getListSelectionForeground(true);
  }

  @Nonnull
  public static Color getListSelectionForeground(boolean focused) {
    Color foreground = UIManager.getColor(focused ? "List.selectionForeground" : "List.selectionInactiveForeground");
    if (focused && foreground == null) foreground = UIManager.getColor("List[Selected].textForeground");  // Nimbus
    return foreground != null ? foreground : getListForeground();
  }

  public static Color getFieldForegroundColor() {
    return UIManager.getColor("field.foreground");
  }

  public static Color getTableSelectionBackground() {
    if (isUnderNimbusLookAndFeel()) {
      Color color = UIManager.getColor("Table[Enabled+Selected].textBackground");
      if (color != null) return color;
      color = UIManager.getColor("nimbusSelectionBackground");
      if (color != null) return color;
    }
    return UIManager.getColor("Table.selectionBackground");
  }

  public static Color getActiveTextColor() {
    return UIManager.getColor("textActiveText");
  }

  public static Color getInactiveTextColor() {
    return UIManager.getColor("textInactiveText");
  }

  public static Color getSlightlyDarkerColor(Color c) {
    float[] hsl = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), new float[3]);
    return new Color(Color.HSBtoRGB(hsl[0], hsl[1], hsl[2] - .08f > 0 ? hsl[2] - .08f : hsl[2]));
  }

  /**
   * @deprecated use com.intellij.util.ui.UIUtil#getTextFieldBackground()
   */
  public static Color getActiveTextFieldBackgroundColor() {
    return getTextFieldBackground();
  }

  public static Color getInactiveTextFieldBackgroundColor() {
    return UIManager.getColor("TextField.inactiveBackground");
  }

  public static Font getTreeFont() {
    return UIManager.getFont("Tree.font");
  }

  public static Font getListFont() {
    return UIManager.getFont("List.font");
  }

  /**
   * @see RenderingUtil#getForeground(JTree, boolean)
   */
  @Nonnull
  public static Color getTreeForeground(boolean selected, boolean focused) {
    return !selected ? getTreeForeground() : getTreeSelectionForeground(focused);
  }

  /**
   * @deprecated use {@link #getTreeSelectionForeground(boolean)}
   */
  @Deprecated
  @Nonnull
  public static Color getTreeSelectionForeground() {
    return getTreeSelectionForeground(true);
  }

  /**
   * @see RenderingUtil#getForeground(JTree)
   */
  @Nonnull
  public static Color getTreeForeground() {
    return JBUI.CurrentTheme.Tree.FOREGROUND;
  }

  /**
   * @see RenderingUtil#getSelectionForeground(JTree)
   */
  @Nonnull
  public static Color getTreeSelectionForeground(boolean focused) {
    return JBUI.CurrentTheme.Tree.Selection.foreground(focused);
  }

  /**
   * @deprecated use com.intellij.util.ui.UIUtil#getInactiveTextColor()
   */
  public static Color getTextInactiveTextColor() {
    return getInactiveTextColor();
  }

  public static void installPopupMenuColorAndFonts(final JComponent contentPane) {
    LookAndFeel.installColorsAndFont(contentPane, "PopupMenu.background", "PopupMenu.foreground", "PopupMenu.font");
  }

  public static void installPopupMenuBorder(final JComponent contentPane) {
    LookAndFeel.installBorder(contentPane, "PopupMenu.border");
  }

  public static Color getTreeSelectionBorderColor() {
    return UIManager.getColor("Tree.selectionBorderColor");
  }

  public static int getTreeRightChildIndent() {
    return UIManager.getInt("Tree.rightChildIndent");
  }

  public static int getTreeLeftChildIndent() {
    return UIManager.getInt("Tree.leftChildIndent");
  }

  public static Color getToolTipBackground() {
    return JBColor.namedColor("ToolTip.background", new JBColor(Gray.xF2, new Color(0x3c3f41)));
  }

  public static Color getToolTipForeground() {
    return UIManager.getColor("ToolTip.foreground");
  }

  public static Color getComboBoxDisabledForeground() {
    return UIManager.getColor("ComboBox.disabledForeground");
  }

  public static Color getComboBoxDisabledBackground() {
    return UIManager.getColor("ComboBox.disabledBackground");
  }

  public static Color getButtonSelectColor() {
    return UIManager.getColor("Button.select");
  }

  public static Integer getPropertyMaxGutterIconWidth(final String propertyPrefix) {
    return (Integer)UIManager.get(propertyPrefix + ".maxGutterIconWidth");
  }

  public static Color getMenuItemDisabledForeground() {
    return UIManager.getColor("MenuItem.disabledForeground");
  }

  public static Color getMenuItemSelectedBackground() {
    return UIManager.getColor("MenuItem.selectedBackground");
  }

  public static Object getMenuItemDisabledForegroundObject() {
    return UIManager.get("MenuItem.disabledForeground");
  }

  public static Object getTabbedPanePaintContentBorder(final JComponent c) {
    return c.getClientProperty("TabbedPane.paintContentBorder");
  }

  public static boolean isMenuCrossMenuMnemonics() {
    return UIManager.getBoolean("Menu.crossMenuMnemonic");
  }

  public static Color getTableBackground() {
    // Under GTK+ L&F "Table.background" often has main panel color, which looks ugly
    return isUnderGTKLookAndFeel() ? getTreeTextBackground() : UIManager.getColor("Table.background");
  }

  public static Color getTableBackground(final boolean isSelected) {
    return isSelected ? getTableSelectionBackground() : getTableBackground();
  }

  public static Color getTableSelectionForeground() {
    if (isUnderNimbusLookAndFeel()) {
      return UIManager.getColor("Table[Enabled+Selected].textForeground");
    }
    return UIManager.getColor("Table.selectionForeground");
  }

  public static Color getTableForeground() {
    return UIManager.getColor("Table.foreground");
  }

  public static Color getTableForeground(final boolean isSelected) {
    return isSelected ? getTableSelectionForeground() : getTableForeground();
  }

  public static Color getTableGridColor() {
    return UIManager.getColor("Table.gridColor");
  }

  public static Color getListBackground() {
    if (isUnderNimbusLookAndFeel()) {
      final Color color = UIManager.getColor("List.background");
      //noinspection UseJBColor
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }
    // Under GTK+ L&F "Table.background" often has main panel color, which looks ugly
    return isUnderGTKLookAndFeel() ? getTreeTextBackground() : UIManager.getColor("List.background");
  }

  public static Color getListBackground(boolean isSelected) {
    return isSelected ? getListSelectionBackground() : getListBackground();
  }

  public static Color getListForeground() {
    return UIManager.getColor("List.foreground");
  }

  public static Color getListForeground(boolean isSelected) {
    return isSelected ? getListSelectionForeground() : getListForeground();
  }

  @Nonnull
  public static Color getListForeground(boolean selected, boolean focused) {
    return !selected ? getListForeground() : getListSelectionForeground(focused);
  }

  public static Color getPanelBackground() {
    return UIManager.getColor("Panel.background");
  }

  public static Color getEditorPaneBackground() {
    return UIManager.getColor("EditorPane.background");
  }

  public static Color getTreeBackground() {
    return UIManager.getColor("Tree.background");
  }

  public static Color getTableFocusCellBackground() {
    return UIManager.getColor(TABLE_FOCUS_CELL_BACKGROUND_PROPERTY);
  }

  private static final class UnfocusedSelection {
    private static final Color BACKGROUND = new JBColor(0xD4D4D4, 0x0D293E);
    private static final Color LIST_BACKGROUND = JBColor.namedColor("List.selectionInactiveBackground", BACKGROUND);
    private static final Color TREE_BACKGROUND = JBColor.namedColor("Tree.selectionInactiveBackground", BACKGROUND);
    private static final Color TABLE_BACKGROUND = JBColor.namedColor("Table.selectionInactiveBackground", BACKGROUND);
  }

  private static final class FocusedSelection {
    private static final Color BACKGROUND = new JBColor(0x3875D6, 0x2F65CA);
    private static final Color LIST_BACKGROUND = JBColor.namedColor("List.selectionBackground", BACKGROUND);
    private static final Color TREE_BACKGROUND = JBColor.namedColor("Tree.selectionBackground", BACKGROUND);
    private static final Color TABLE_BACKGROUND = JBColor.namedColor("Table.selectionBackground", BACKGROUND);
  }

  private static final JBValue SELECTED_ITEM_ALPHA = new JBValue.UIInteger("List.selectedItemAlpha", 75);

  @Nonnull
  public static Color getListSelectionBackground(boolean focused) {
    if (!focused) return UnfocusedSelection.LIST_BACKGROUND;
    Color color = UIManager.getColor("List.selectionBackground");
    double alpha = SELECTED_ITEM_ALPHA.getFloat() / 100.0;
    //noinspection UseJBColor
    return isUnderDefaultMacTheme() && alpha >= 0 && alpha <= 1.0 ? ColorUtil.mix(Color.WHITE, color, alpha) : color;
  }

  @Nonnull
  public static Color getListBackground(boolean selected, boolean focused) {
    return !selected ? getListBackground() : getListSelectionBackground(focused);
  }

  /**
   * @deprecated use {@link #getListSelectionBackground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getListSelectionBackground() {
    return getListSelectionBackground(true);
  }

  /**
   * @deprecated use {@link #getListSelectionBackground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getListUnfocusedSelectionBackground() {
    return getListSelectionBackground(false);
  }

  /**
   * @deprecated use {@link #getTreeSelectionBackground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getTreeSelectionBackground() {
    return getTreeSelectionBackground(true);
  }

  @Nonnull
  public static Color getTreeSelectionBackground(boolean focused) {
    return focused ? FocusedSelection.TREE_BACKGROUND : UnfocusedSelection.TREE_BACKGROUND;
  }

  /**
   * @deprecated use {@link #getTreeSelectionBackground(boolean)}
   */
  @Nonnull
  @Deprecated
  public static Color getTreeUnfocusedSelectionBackground() {
    return getTreeSelectionBackground(false);
  }

  public static Color getTextFieldForeground() {
    return UIManager.getColor("TextField.foreground");
  }

  public static Color getTextFieldBackground() {
    return isUnderGTKLookAndFeel() ? UIManager.getColor("EditorPane.background") : UIManager.getColor("TextField.background");
  }

  public static Font getButtonFont() {
    return UIManager.getFont("Button.font");
  }

  public static Font getToolTipFont() {
    return UIManager.getFont("ToolTip.font");
  }

  public static Color getTabbedPaneBackground() {
    return UIManager.getColor("TabbedPane.background");
  }

  public static void setSliderIsFilled(final JSlider slider, final boolean value) {
    slider.putClientProperty("JSlider.isFilled", Boolean.valueOf(value));
  }

  public static Color getLabelTextForeground() {
    return UIManager.getColor("Label.textForeground");
  }

  public static Color getControlColor() {
    return UIManager.getColor("control");
  }

  public static Font getOptionPaneMessageFont() {
    return UIManager.getFont("OptionPane.messageFont");
  }

  public static Font getMenuFont() {
    return UIManager.getFont("Menu.font");
  }

  public static Color getSeparatorForeground() {
    return MorphColor.of(new NotNullProducer<Color>() {
      @Nonnull
      @Override
      public Color produce() {
        return UIManager.getColor("Separator.foreground");
      }
    });
  }

  public static Color getSeparatorBackground() {
    return UIManager.getColor("Separator.background");
  }

  public static Color getSeparatorShadow() {
    return UIManager.getColor("Separator.shadow");
  }

  public static Color getSeparatorHighlight() {
    return UIManager.getColor("Separator.highlight");
  }

  public static Color getSeparatorColorUnderNimbus() {
    return UIManager.getColor("nimbusBlueGrey");
  }

  public static Color getSeparatorColor() {
    Color separatorColor = getSeparatorForeground();
    if (isUnderNimbusLookAndFeel()) {
      separatorColor = getSeparatorColorUnderNimbus();
    }
    //under GTK+ L&F colors set hard
    if (isUnderGTKLookAndFeel()) {
      separatorColor = Gray._215;
    }
    return separatorColor;
  }

  public static Border getTableFocusCellHighlightBorder() {
    return UIManager.getBorder("Table.focusCellHighlightBorder");
  }

  public static void setLineStyleAngled(final ClientPropertyHolder component) {
    component.putClientProperty("JTree.lineStyle", "Angled");
  }

  public static void setLineStyleAngled(final JTree component) {
    component.putClientProperty("JTree.lineStyle", "Angled");
  }

  public static Color getTableFocusCellForeground() {
    return UIManager.getColor("Table.focusCellForeground");
  }

  /**
   * @deprecated use com.intellij.util.ui.UIUtil#getPanelBackground() instead
   */
  public static Color getPanelBackgound() {
    return getPanelBackground();
  }

  public static Border getTextFieldBorder() {
    return UIManager.getBorder("TextField.border");
  }

  public static Border getButtonBorder() {
    return UIManager.getBorder("Button.border");
  }

  public static consulo.ui.image.Image getErrorIcon() {
    return wrapToImageIfNeed("OptionPane.errorIcon");
  }

  public static consulo.ui.image.Image getInformationIcon() {
    return wrapToImageIfNeed("OptionPane.informationIcon");
  }

  public static consulo.ui.image.Image getQuestionIcon() {
    return wrapToImageIfNeed("OptionPane.questionIcon");
  }

  public static consulo.ui.image.Image getWarningIcon() {
    return wrapToImageIfNeed("OptionPane.warningIcon");
  }

  private static consulo.ui.image.Image wrapToImageIfNeed(String imageIcon) {
    Icon icon = UIManager.getIcon(imageIcon);
    if (icon instanceof consulo.ui.image.Image) {
      return (consulo.ui.image.Image)icon;
    }

    return new ImageWrapper(icon);
  }

  private static class ImageWrapper implements Icon, consulo.ui.image.Image {
    private final Icon myDelegate;

    private ImageWrapper(Icon delegate) {
      myDelegate = delegate;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      myDelegate.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return getWidth();
    }

    @Override
    public int getIconHeight() {
      return getHeight();
    }

    @Override
    public int getHeight() {
      return myDelegate.getIconHeight();
    }

    @Override
    public int getWidth() {
      return myDelegate.getIconWidth();
    }
  }

  public static Icon getRadioButtonIcon() {
    return UIManager.getIcon("RadioButton.icon");
  }

  public static Icon getTreeNodeIcon(boolean expanded, boolean selected, boolean focused) {
    boolean selectedAndFocused = selected && focused;

    Icon selectedIcon = getTreeSelectedExpandedIcon();
    Icon notSelectedIcon = getTreeExpandedIcon();

    int width = Math.max(selectedIcon.getIconWidth(), notSelectedIcon.getIconWidth());
    int height = Math.max(selectedIcon.getIconWidth(), notSelectedIcon.getIconWidth());

    return new CenteredIcon(expanded ? (selectedAndFocused ? getTreeSelectedExpandedIcon() : getTreeExpandedIcon()) : (selectedAndFocused ? getTreeSelectedCollapsedIcon() : getTreeCollapsedIcon()),
                            width, height, false);
  }

  public static Icon getTreeCollapsedIcon() {
    return UIManager.getIcon("Tree.collapsedIcon");
  }

  public static Icon getTreeExpandedIcon() {
    return UIManager.getIcon("Tree.expandedIcon");
  }

  public static Icon getTreeIcon(boolean expanded) {
    return expanded ? getTreeExpandedIcon() : getTreeCollapsedIcon();
  }

  private static final ImageKey selectedCollapsedIcon = ImageKey.fromString("consulo.platform.desktop.laf.LookAndFeelIconGroup@components.treeCollapsedSelected", 9, 11);

  public static Icon getTreeSelectedCollapsedIcon() {
    Icon icon = UIManager.getIcon("Tree.collapsedSelectedIcon");
    if (icon != null) {
      return icon;
    }
    return isUnderAquaBasedLookAndFeel() || isUnderGTKLookAndFeel() || isUnderBuildInLaF() ? (Icon)selectedCollapsedIcon : getTreeCollapsedIcon();
  }

  private static final ImageKey selectedExpandedIcon = ImageKey.fromString("consulo.platform.desktop.laf.LookAndFeelIconGroup@components.treeExpandedSelected", 11, 9);

  public static Icon getTreeSelectedExpandedIcon() {
    Icon icon = UIManager.getIcon("Tree.expandedSelectedIcon");
    if (icon != null) {
      return icon;
    }

    return isUnderAquaBasedLookAndFeel() || isUnderGTKLookAndFeel() || isUnderBuildInLaF() ? (Icon)selectedExpandedIcon : getTreeExpandedIcon();
  }

  public static Border getTableHeaderCellBorder() {
    return UIManager.getBorder("TableHeader.cellBorder");
  }

  public static Color getWindowColor() {
    return UIManager.getColor("window");
  }

  public static Color getTextAreaForeground() {
    return UIManager.getColor("TextArea.foreground");
  }

  public static Color getOptionPaneBackground() {
    return UIManager.getColor("OptionPane.background");
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Deprecated
  @DeprecationInfo(value = "Look & Feel is not supported", until = "3.0")
  public static boolean isUnderWindowsLookAndFeel() {
    return UIManager.getLookAndFeel().getName().equals("Windows");
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Deprecated
  @DeprecationInfo(value = "Look & Feel is not supported", until = "3.0")
  public static boolean isUnderWindowsClassicLookAndFeel() {
    return UIManager.getLookAndFeel().getName().equals("Windows Classic");
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Deprecated
  @DeprecationInfo(value = "Look & Feel is not supported", until = "3.0")
  public static boolean isUnderNimbusLookAndFeel() {
    return false;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static boolean isUnderAquaLookAndFeel() {
    return SystemInfo.isMac && UIManager.getLookAndFeel().getName().contains("Mac OS X");
  }

  public static boolean isUnderAquaBasedLookAndFeel() {
    return SystemInfo.isMac && (isUnderAquaLookAndFeel() || isUnderDarkBuildInLaf());
  }

  @Deprecated
  @DeprecationInfo("macOS specific option")
  public static boolean isGraphite() {
    if (!SystemInfo.isMac) return false;
    try {
      // https://developer.apple.com/library/mac/documentation/Cocoa/Reference/ApplicationKit/Classes/NSCell_Class/index.html#//apple_ref/doc/c_ref/NSGraphiteControlTint
      // NSGraphiteControlTint = 6
      return Foundation.invoke("NSColor", "currentControlTint").intValue() == 6;
    }
    catch (Exception e) {
      return false;
    }
  }

  @Deprecated
  @DeprecationInfo(value = "Use #isUnderDarkTheme()", until = "2.0")
  public static boolean isUnderDarcula() {
    return isUnderDarkTheme();
  }

  public static boolean isUnderIntelliJLaF() {
    return !isUnderDarkBuildInLaf();
  }

  public static boolean isUnderDefaultMacTheme() {
    return SystemInfo.isMac && isUnderIntelliJLaF();
  }

  @Deprecated
  @DeprecationInfo(value = "Use #isUnderDarkTheme()", until = "2.0")
  public static boolean isUnderDarkBuildInLaf() {
    return isUnderDarkTheme();
  }

  public static boolean isUnderBuildInLaF() {
    LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
    return lookAndFeel instanceof BuildInLookAndFeel;
  }

  public static boolean isUnderGTKLookAndFeel() {
    return UIManager.getLookAndFeel().getName().contains("GTK");
  }

  public static final Color GTK_AMBIANCE_TEXT_COLOR = new Color(223, 219, 210);
  public static final Color GTK_AMBIANCE_BACKGROUND_COLOR = new Color(67, 66, 63);

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Nullable
  public static String getGtkThemeName() {
    final LookAndFeel laf = UIManager.getLookAndFeel();
    if (laf != null && "GTKLookAndFeel".equals(laf.getClass().getSimpleName())) {
      try {
        final Method method = laf.getClass().getDeclaredMethod("getGtkThemeName");
        method.setAccessible(true);
        final Object theme = method.invoke(laf);
        if (theme != null) {
          return theme.toString();
        }
      }
      catch (Exception ignored) {
      }
    }
    return null;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static boolean isMurrineBasedTheme() {
    final String gtkTheme = getGtkThemeName();
    return "Ambiance".equalsIgnoreCase(gtkTheme) || "Radiance".equalsIgnoreCase(gtkTheme) || "Dust".equalsIgnoreCase(gtkTheme) || "Dust Sand".equalsIgnoreCase(gtkTheme);
  }

  public static Color shade(final Color c, final double factor, final double alphaFactor) {
    assert factor >= 0 : factor;
    return new Color(Math.min((int)Math.round(c.getRed() * factor), 255), Math.min((int)Math.round(c.getGreen() * factor), 255), Math.min((int)Math.round(c.getBlue() * factor), 255),
                     Math.min((int)Math.round(c.getAlpha() * alphaFactor), 255));
  }

  public static Color mix(final Color c1, final Color c2, final double factor) {
    assert 0 <= factor && factor <= 1.0 : factor;
    final double backFactor = 1.0 - factor;
    return new Color(Math.min((int)Math.round(c1.getRed() * backFactor + c2.getRed() * factor), 255), Math.min((int)Math.round(c1.getGreen() * backFactor + c2.getGreen() * factor), 255),
                     Math.min((int)Math.round(c1.getBlue() * backFactor + c2.getBlue() * factor), 255));
  }

  public static boolean isFullRowSelectionLAF() {
    return false;
  }

  public static boolean isUnderNativeMacLookAndFeel() {
    return SystemInfo.isMac;
  }

  public static int getListCellHPadding() {
    return 5;
  }

  public static int getListCellVPadding() {
    return 3;
  }

  public static Insets getListCellPadding() {
    return JBInsets.create(getListCellVPadding(), getListCellHPadding());
  }

  public static Insets getListViewportPadding() {
    return JBUI.emptyInsets();
  }

  public static boolean isToUseDottedCellBorder() {
    return !isUnderNativeMacLookAndFeel();
  }

  public static boolean isControlKeyDown(MouseEvent mouseEvent) {
    return SystemInfo.isMac ? mouseEvent.isMetaDown() : mouseEvent.isControlDown();
  }

  public static String[] getValidFontNames(final boolean familyName) {
    Set<String> result = new TreeSet<String>();

    // adds fonts that can display symbols at [A, Z] + [a, z] + [0, 9]
    for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
      try {
        if (isValidFont(font)) {
          result.add(familyName ? font.getFamily() : font.getName());
        }
      }
      catch (Exception ignore) {
        // JRE has problems working with the font. Just skip.
      }
    }

    // add label font (if isn't listed among above)
    Font labelFont = getLabelFont();
    if (labelFont != null && isValidFont(labelFont)) {
      result.add(familyName ? labelFont.getFamily() : labelFont.getName());
    }

    return ArrayUtil.toStringArray(result);
  }

  public static String[] getStandardFontSizes() {
    return STANDARD_FONT_SIZES;
  }

  public static boolean isValidFont(@Nonnull Font font) {
    try {
      return font.canDisplay('a') && font.canDisplay('z') && font.canDisplay('A') && font.canDisplay('Z') && font.canDisplay('0') && font.canDisplay('1');
    }
    catch (Exception e) {
      // JRE has problems working with the font. Just skip.
      return false;
    }
  }

  public static void setupEnclosingDialogBounds(final JComponent component) {
    component.revalidate();
    component.repaint();
    final Window window = SwingUtilities.windowForComponent(component);
    if (window != null && (window.getSize().height < window.getMinimumSize().height || window.getSize().width < window.getMinimumSize().width)) {
      window.pack();
    }
  }

  public static String displayPropertiesToCSS(Font font, Color fg) {
    @NonNls StringBuilder rule = new StringBuilder("body {");
    if (font != null) {
      rule.append(" font-family: ");
      rule.append(font.getFamily());
      rule.append(" ; ");
      rule.append(" font-size: ");
      rule.append(font.getSize());
      rule.append("pt ;");
      if (font.isBold()) {
        rule.append(" font-weight: 700 ; ");
      }
      if (font.isItalic()) {
        rule.append(" font-style: italic ; ");
      }
    }
    if (fg != null) {
      rule.append(" color: #");
      appendColor(fg, rule);
      rule.append(" ; ");
    }
    rule.append(" }");
    return rule.toString();
  }

  public static void appendColor(final Color color, final StringBuilder sb) {
    if (color.getRed() < 16) sb.append('0');
    sb.append(Integer.toHexString(color.getRed()));
    if (color.getGreen() < 16) sb.append('0');
    sb.append(Integer.toHexString(color.getGreen()));
    if (color.getBlue() < 16) sb.append('0');
    sb.append(Integer.toHexString(color.getBlue()));
  }

  /**
   * @param g  graphics.
   * @param x  top left X coordinate.
   * @param y  top left Y coordinate.
   * @param x1 right bottom X coordinate.
   * @param y1 right bottom Y coordinate.
   */
  public static void drawDottedRectangle(Graphics g, int x, int y, int x1, int y1) {
    int i1;
    for (i1 = x; i1 <= x1; i1 += 2) {
      drawLine(g, i1, y, i1, y);
    }

    for (i1 = i1 != x1 + 1 ? y + 2 : y + 1; i1 <= y1; i1 += 2) {
      drawLine(g, x1, i1, x1, i1);
    }

    for (i1 = i1 != y1 + 1 ? x1 - 2 : x1 - 1; i1 >= x; i1 -= 2) {
      drawLine(g, i1, y1, i1, y1);
    }

    for (i1 = i1 != x - 1 ? y1 - 2 : y1 - 1; i1 >= y; i1 -= 2) {
      drawLine(g, x, i1, x, i1);
    }
  }

  /**
   * Should be invoked only in EDT.
   *
   * @param g       Graphics surface
   * @param startX  Line start X coordinate
   * @param endX    Line end X coordinate
   * @param lineY   Line Y coordinate
   * @param bgColor Background color (optional)
   * @param fgColor Foreground color (optional)
   * @param opaque  If opaque the image will be dr
   */
  public static void drawBoldDottedLine(final Graphics2D g, final int startX, final int endX, final int lineY, final Color bgColor, final Color fgColor, final boolean opaque) {
    if ((SystemInfo.isMac && !isRetina()) || SystemInfo.isLinux) {
      drawAppleDottedLine(g, startX, endX, lineY, bgColor, fgColor, opaque);
    }
    else {
      drawBoringDottedLine(g, startX, endX, lineY, bgColor, fgColor, opaque);
    }
  }

  public static void drawSearchMatch(final Graphics2D g, final int startX, final int endX, final int height) {
    Color c1 = new Color(255, 234, 162);
    Color c2 = new Color(255, 208, 66);
    drawSearchMatch(g, startX, endX, height, c1, c2);
  }

  public static void drawSearchMatch(Graphics2D g, int startX, int endX, int height, Color c1, Color c2) {
    final boolean drawRound = endX - startX > 4;

    final Composite oldComposite = g.getComposite();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
    g.setPaint(getGradientPaint(startX, 2, c1, startX, height - 5, c2));

    if (isRetina()) {
      g.fillRoundRect(startX - 1, 2, endX - startX + 1, height - 4, 5, 5);
      g.setComposite(oldComposite);
      return;
    }

    g.fillRect(startX, 3, endX - startX, height - 5);

    if (drawRound) {
      g.drawLine(startX - 1, 4, startX - 1, height - 4);
      g.drawLine(endX, 4, endX, height - 4);

      g.setColor(new Color(100, 100, 100, 50));
      g.drawLine(startX - 1, 4, startX - 1, height - 4);
      g.drawLine(endX, 4, endX, height - 4);

      g.drawLine(startX, 3, endX - 1, 3);
      g.drawLine(startX, height - 3, endX - 1, height - 3);
    }

    g.setComposite(oldComposite);
  }

  public static void drawRectPickedOut(Graphics2D g, int x, int y, int w, int h) {
    g.drawLine(x + 1, y, x + w - 1, y);
    g.drawLine(x + w, y + 1, x + w, y + h - 1);
    g.drawLine(x + w - 1, y + h, x + 1, y + h);
    g.drawLine(x, y + 1, x, y + h - 1);
  }

  private static void drawBoringDottedLine(final Graphics2D g, final int startX, final int endX, final int lineY, final Color bgColor, final Color fgColor, final boolean opaque) {
    final Color oldColor = g.getColor();

    // Fill 2 lines with background color
    if (opaque && bgColor != null) {
      g.setColor(bgColor);

      drawLine(g, startX, lineY, endX, lineY);
      drawLine(g, startX, lineY + 1, endX, lineY + 1);
    }

    // Draw dotted line:
    //
    // CCC CCC CCC ...
    // CCC CCC CCC ...
    //
    // (where "C" - colored pixel, " " - white pixel)

    final int step = 4;
    final int startPosCorrection = startX % step < 3 ? 0 : 1;

    g.setColor(fgColor != null ? fgColor : oldColor);
    // Now draw bold line segments
    for (int dotXi = (startX / step + startPosCorrection) * step; dotXi < endX; dotXi += step) {
      g.drawLine(dotXi, lineY, dotXi + 1, lineY);
      g.drawLine(dotXi, lineY + 1, dotXi + 1, lineY + 1);
    }

    // restore color
    g.setColor(oldColor);
  }

  public static void drawGradientHToolbarBackground(final Graphics g, final int width, final int height) {
    final Graphics2D g2d = (Graphics2D)g;
    g2d.setPaint(getGradientPaint(0, 0, Gray._215, 0, height, Gray._200));
    g2d.fillRect(0, 0, width, height);
  }

  public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean drawTopLine) {
    drawHeader(g, x, width, height, active, false, drawTopLine, true);
  }

  public static void drawHeader(Graphics g, int x, int width, int height, boolean active, boolean toolWindow, boolean drawTopLine, boolean drawBottomLine) {
    g.setColor(getPanelBackground());
    g.fillRect(x, 0, width, height);

    ((Graphics2D)g).setPaint(new Color(0, 0, 0, 5));
    g.fillRect(x, 0, width, height);

    g.setColor(new Color(0, 0, 0, toolWindow ? 90 : 50));
    if (drawTopLine) g.drawLine(x, 0, width, 0);
    if (drawBottomLine) g.drawLine(x, height - JBUI.scale(1), width, height - JBUI.scale(1));

    g.setColor(isUnderDarkBuildInLaf() ? Gray._255.withAlpha(30) : new Color(255, 255, 255, 100));
    g.drawLine(x, drawTopLine ? JBUI.scale(1) : 0, width, drawTopLine ? JBUI.scale(1) : 0);

    if (active) {
      g.setColor(ColorUtil.toAlpha(UIManager.getColor("Hyperlink.linkColor"), toolWindow ? 100 : 30));//new Color(100, 150, 230, toolWindow ? 50 : 30));
      g.fillRect(x, 0, width, height);
    }
  }

  public static void drawDoubleSpaceDottedLine(final Graphics2D g, final int start, final int end, final int xOrY, final Color fgColor, boolean horizontal) {

    g.setColor(fgColor);
    for (int dot = start; dot < end; dot += 3) {
      if (horizontal) {
        g.drawLine(dot, xOrY, dot, xOrY);
      }
      else {
        g.drawLine(xOrY, dot, xOrY, dot);
      }
    }
  }

  private static void drawAppleDottedLine(final Graphics2D g, final int startX, final int endX, final int lineY, final Color bgColor, final Color fgColor, final boolean opaque) {
    final Color oldColor = g.getColor();

    // Fill 3 lines with background color
    if (opaque && bgColor != null) {
      g.setColor(bgColor);

      drawLine(g, startX, lineY, endX, lineY);
      drawLine(g, startX, lineY + 1, endX, lineY + 1);
      drawLine(g, startX, lineY + 2, endX, lineY + 2);
    }

    AppleBoldDottedPainter painter = AppleBoldDottedPainter.forColor(ObjectUtils.notNull(fgColor, oldColor));
    painter.paint(g, startX, endX, lineY);
  }

  public static void applyRenderingHints(final Graphics g) {
    Toolkit tk = Toolkit.getDefaultToolkit();
    //noinspection HardCodedStringLiteral
    Map map = (Map)tk.getDesktopProperty("awt.font.desktophints");
    if (map != null) {
      ((Graphics2D)g).addRenderingHints(map);
    }
  }

  /**
   * Creates a HiDPI-aware BufferedImage in device scale.
   *
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in device scale
   */
  @Nonnull
  public static BufferedImage createImage(int width, int height, int type) {
    if (isJreHiDPI()) {
      return RetinaImage.create(width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }

  /**
   * Creates a HiDPI-aware BufferedImage in the graphics config scale.
   *
   * @param gc     the graphics config
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in the graphics scale
   */
  @Nonnull
  public static BufferedImage createImage(GraphicsConfiguration gc, int width, int height, int type) {
    if (isJreHiDPI(gc)) {
      return RetinaImage.create(gc, width, height, type);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(width, height, type);
  }

  /**
   * Creates a HiDPI-aware BufferedImage in the graphics device scale.
   *
   * @param g      the graphics of the target device
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in the graphics scale
   */
  @Nonnull
  public static BufferedImage createImage(Graphics g, int width, int height, int type) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D)g;
      if (isJreHiDPI(g2d)) {
        return RetinaImage.create(g2d, width, height, type);
      }
      //noinspection UndesirableClassUsage
      return new BufferedImage(width, height, type);
    }
    return createImage(width, height, type);
  }


  /**
   * @see #createImage(GraphicsConfiguration, double, double, int, RoundingMode)
   */
  @Nonnull
  public static BufferedImage createImage(Graphics g, double width, double height, int type, @Nonnull PaintUtil.RoundingMode rm) {
    if (g instanceof Graphics2D) {
      Graphics2D g2d = (Graphics2D)g;
      if (isJreHiDPI(g2d)) {
        return RetinaImage.create(g2d, width, height, type, rm);
      }
      //noinspection UndesirableClassUsage
      return new BufferedImage(rm.round(width), rm.round(height), type);
    }
    return createImage(rm.round(width), rm.round(height), type);
  }

  /**
   * @see #createImage(GraphicsConfiguration, double, double, int, RoundingMode)
   */
  @Nonnull
  public static BufferedImage createImage(JBUI.ScaleContext ctx, double width, double height, int type, PaintUtil.RoundingMode rm) {
    if (isJreHiDPI(ctx)) {
      return RetinaImage.create(ctx, width, height, type, rm);
    }
    //noinspection UndesirableClassUsage
    return new BufferedImage(rm.round(width), rm.round(height), type);
  }

  /**
   * Creates a HiDPI-aware BufferedImage in the component scale.
   *
   * @param comp   the component associated with the target graphics device
   * @param width  the width in user coordinate space
   * @param height the height in user coordinate space
   * @param type   the type of the image
   * @return a HiDPI-aware BufferedImage in the component scale
   */
  @Nonnull
  public static BufferedImage createImage(Component comp, int width, int height, int type) {
    return comp != null ? createImage(comp.getGraphicsConfiguration(), width, height, type) : createImage(width, height, type);
  }

  /**
   * A hidpi-aware wrapper over {@link Graphics#drawImage(Image, int, int, ImageObserver)}.
   *
   * @see #drawImage(Graphics, Image, Rectangle, Rectangle, ImageObserver)
   */
  public static void drawImage(@Nonnull Graphics g, @Nonnull Image image, int x, int y, @Nullable ImageObserver observer) {
    drawImage(g, image, new Rectangle(x, y, -1, -1), null, null, observer);
  }

  /**
   * A hidpi-aware wrapper over {@link Graphics#drawImage(Image, int, int, int, int, ImageObserver)}.
   * <p/>
   * Note, the method interprets [x,y,width,height] as the destination and source bounds which doesn't conform
   * to the {@link Graphics#drawImage(Image, int, int, int, int, ImageObserver)} method contract. This works
   * just fine for the general-purpose one-to-one drawing, however when the dst and src bounds need to be specific,
   * use {@link #drawImage(Graphics, Image, Rectangle, Rectangle, BufferedImageOp, ImageObserver)}.
   */
  @Deprecated
  public static void drawImage(@Nonnull Graphics g, @Nonnull Image image, int x, int y, int width, int height, @Nullable ImageObserver observer) {
    drawImage(g, image, x, y, width, height, null, observer);
  }

  private static void drawImage(Graphics g, Image image, int x, int y, int width, int height, @Nullable BufferedImageOp op, ImageObserver observer) {
    Rectangle srcBounds = width >= 0 && height >= 0 ? new Rectangle(x, y, width, height) : null;
    drawImage(g, image, new Rectangle(x, y, width, height), srcBounds, op, observer);
  }

  /**
   * A hidpi-aware wrapper over {@link Graphics#drawImage(Image, int, int, int, int, ImageObserver)}.
   *
   * @see #drawImage(Graphics, Image, Rectangle, Rectangle, BufferedImageOp, ImageObserver)
   */
  public static void drawImage(@Nonnull Graphics g, @Nonnull Image image, @Nullable Rectangle dstBounds, @Nullable ImageObserver observer) {
    drawImage(g, image, dstBounds, null, null, observer);
  }

  /**
   * @see #drawImage(Graphics, Image, Rectangle, Rectangle, BufferedImageOp, ImageObserver)
   */
  public static void drawImage(@Nonnull Graphics g, @Nonnull Image image, @Nullable Rectangle dstBounds, @Nullable Rectangle srcBounds, @Nullable ImageObserver observer) {
    drawImage(g, image, dstBounds, srcBounds, null, observer);
  }

  /**
   * A hidpi-aware wrapper over {@link Graphics#drawImage(Image, int, int, int, int, int, int, int, int, ImageObserver)}.
   * <p/>
   * The {@code dstBounds} and {@code srcBounds} are in the user space (just like the width/height of the image).
   * If {@code dstBounds} is null or if its width/height is set to (-1) the image bounds or the image width/height is used.
   * If {@code srcBounds} is null or if its width/height is set to (-1) the image bounds or the image right/bottom area to the provided x/y is used.
   */
  public static void drawImage(@Nonnull Graphics g,
                               @Nonnull Image image,
                               @Nullable Rectangle dstBounds,
                               @Nullable Rectangle srcBounds,
                               @Nullable BufferedImageOp op,
                               @Nullable ImageObserver observer) {
    Graphics2D invG = null;
    double scale = 1;
    int userWidth = ImageUtil.getUserWidth(image);
    int userHeight = ImageUtil.getUserHeight(image);

    int dx = 0;
    int dy = 0;
    int dw = -1;
    int dh = -1;
    if (dstBounds != null) {
      dx = dstBounds.x;
      dy = dstBounds.y;
      dw = dstBounds.width;
      dh = dstBounds.height;
    }
    boolean hasDstSize = dw >= 0 && dh >= 0;

    if (image instanceof JBHiDPIScaledImage) {
      JBHiDPIScaledImage hidpiImage = (JBHiDPIScaledImage)image;
      Image delegate = hidpiImage.getDelegate();
      if (delegate != null) image = delegate;
      scale = hidpiImage.getScale();

      AffineTransform tx = ((Graphics2D)g).getTransform();
      if (scale == tx.getScaleX()) {
        // The image has the same original scale as the graphics scale. However, the real image
        // scale - userSize/realSize - can suffer from inaccuracy due to the image user size
        // rounding to int (userSize = (int)realSize/originalImageScale). This may case quality
        // loss if the image is drawn via Graphics.drawImage(image, <srcRect>, <dstRect>)
        // due to scaling in Graphics. To avoid that, the image should be drawn directly via
        // Graphics.drawImage(image, 0, 0) on the unscaled Graphics.
        double gScaleX = tx.getScaleX();
        double gScaleY = tx.getScaleY();
        tx.scale(1 / gScaleX, 1 / gScaleY);
        tx.translate(dx * gScaleX, dy * gScaleY);
        dx = dy = 0;
        g = invG = (Graphics2D)g.create();
        invG.setTransform(tx);
      }
    }
    final double _scale = scale;
    Function<Integer, Integer> size = new Function<Integer, Integer>() {
      @Override
      public Integer fun(Integer size) {
        return (int)Math.round(size * _scale);
      }
    };
    try {
      if (op != null && image instanceof BufferedImage) {
        image = op.filter((BufferedImage)image, null);
      }
      if (invG != null && hasDstSize) {
        dw = size.fun(dw);
        dh = size.fun(dh);
      }
      if (srcBounds != null) {
        int sx = size.fun(srcBounds.x);
        int sy = size.fun(srcBounds.y);
        int sw = srcBounds.width >= 0 ? size.fun(srcBounds.width) : size.fun(userWidth) - sx;
        int sh = srcBounds.height >= 0 ? size.fun(srcBounds.height) : size.fun(userHeight) - sy;
        if (!hasDstSize) {
          dw = size.fun(userWidth);
          dh = size.fun(userHeight);
        }
        g.drawImage(image, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, observer);
      }
      else if (hasDstSize) {
        g.drawImage(image, dx, dy, dw, dh, observer);
      }
      else if (invG == null) {
        g.drawImage(image, dx, dy, userWidth, userHeight, observer);
      }
      else {
        g.drawImage(image, dx, dy, observer);
      }
    }
    finally {
      if (invG != null) invG.dispose();
    }
  }

  /**
   * @see #drawImage(Graphics, Image, int, int, ImageObserver)
   */
  public static void drawImage(@Nonnull Graphics g, @Nonnull BufferedImage image, @Nullable BufferedImageOp op, int x, int y) {
    drawImage(g, image, x, y, -1, -1, op, null);
  }

  public static void paintWithXorOnRetina(@Nonnull Dimension size, @Nonnull Graphics g, Consumer<Graphics2D> paintRoutine) {
    paintWithXorOnRetina(size, g, true, paintRoutine);
  }

  /**
   * Direct painting into component's graphics with XORMode is broken on retina-mode so we need to paint into an intermediate buffer first.
   */
  public static void paintWithXorOnRetina(@Nonnull Dimension size, @Nonnull Graphics g, boolean useRetinaCondition, Consumer<Graphics2D> paintRoutine) {
    if (!useRetinaCondition || !isRetina() || Registry.is("ide.mac.retina.disableDrawingFix", false)) {
      paintRoutine.consume((Graphics2D)g);
    }
    else {
      Rectangle rect = g.getClipBounds();
      if (rect == null) rect = new Rectangle(size);

      //noinspection UndesirableClassUsage
      Image image = new BufferedImage(rect.width * 2, rect.height * 2, BufferedImage.TYPE_INT_RGB);
      Graphics2D imageGraphics = (Graphics2D)image.getGraphics();

      imageGraphics.scale(2, 2);
      imageGraphics.translate(-rect.x, -rect.y);
      imageGraphics.setClip(rect.x, rect.y, rect.width, rect.height);

      paintRoutine.consume(imageGraphics);
      image.flush();
      imageGraphics.dispose();

      ((Graphics2D)g).scale(0.5, 0.5);
      g.drawImage(image, rect.x * 2, rect.y * 2, null);
    }
  }

  /**
   * Configures composite to use for drawing text with the given graphics container.
   * <p/>
   * The whole idea is that <a href="http://en.wikipedia.org/wiki/X_Rendering_Extension">XRender-based</a> pipeline doesn't support
   * {@link AlphaComposite#SRC} and we should use {@link AlphaComposite#SRC_OVER} instead.
   *
   * @param g target graphics container
   */
  public static void setupComposite(@Nonnull Graphics2D g) {
    g.setComposite(X_RENDER_ACTIVE.getValue() ? AlphaComposite.SrcOver : AlphaComposite.Src);
  }

  @TestOnly
  public static void dispatchAllInvocationEvents() {
    assert SwingUtilities.isEventDispatchThread() : Thread.currentThread();
    final EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    while (true) {
      AWTEvent event = eventQueue.peekEvent();
      if (event == null) break;
      try {
        AWTEvent event1 = eventQueue.getNextEvent();
        if (event1 instanceof InvocationEvent) {
          ((InvocationEvent)event1).dispatch();
        }
      }
      catch (Exception e) {
        getLogger().error(e);
      }
    }
  }

  @TestOnly
  public static void pump() {
    assert !SwingUtilities.isEventDispatchThread();
    final BlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        queue.offer(queue);
      }
    });
    try {
      queue.take();
    }
    catch (InterruptedException e) {
      getLogger().error(e);
    }
  }

  public static void addAwtListener(final AWTEventListener listener, long mask, consulo.disposer.Disposable parent) {
    Toolkit.getDefaultToolkit().addAWTEventListener(listener, mask);
    consulo.disposer.Disposer.register(parent, () -> Toolkit.getDefaultToolkit().removeAWTEventListener(listener));
  }

  public static void addParentChangeListener(@Nonnull Component component, @Nonnull PropertyChangeListener listener) {
    component.addPropertyChangeListener("ancestor", listener);
  }

  public static void removeParentChangeListener(@Nonnull Component component, @Nonnull PropertyChangeListener listener) {
    component.removePropertyChangeListener("ancestor", listener);
  }

  public static void drawVDottedLine(Graphics2D g, int lineX, int startY, int endY, @Nullable final Color bgColor, final Color fgColor) {
    if (bgColor != null) {
      g.setColor(bgColor);
      drawLine(g, lineX, startY, lineX, endY);
    }

    g.setColor(fgColor);
    for (int i = (startY / 2) * 2; i < endY; i += 2) {
      g.drawRect(lineX, i, 0, 0);
    }
  }

  public static void drawHDottedLine(Graphics2D g, int startX, int endX, int lineY, @Nullable final Color bgColor, final Color fgColor) {
    if (bgColor != null) {
      g.setColor(bgColor);
      drawLine(g, startX, lineY, endX, lineY);
    }

    g.setColor(fgColor);

    for (int i = (startX / 2) * 2; i < endX; i += 2) {
      g.drawRect(i, lineY, 0, 0);
    }
  }

  public static void drawDottedLine(Graphics2D g, int x1, int y1, int x2, int y2, @Nullable final Color bgColor, final Color fgColor) {
    if (x1 == x2) {
      drawVDottedLine(g, x1, y1, y2, bgColor, fgColor);
    }
    else if (y1 == y2) {
      drawHDottedLine(g, x1, x2, y1, bgColor, fgColor);
    }
    else {
      throw new IllegalArgumentException("Only vertical or horizontal lines are supported");
    }
  }

  public static void drawStringWithHighlighting(Graphics g, String s, int x, int y, Color foreground, Color highlighting) {
    g.setColor(highlighting);
    for (int i = x - 1; i <= x + 1; i++) {
      for (int j = y - 1; j <= y + 1; j++) {
        g.drawString(s, i, j);
      }
    }
    g.setColor(foreground);
    g.drawString(s, x, y);
  }

  /**
   * Draws a centered string in the passed rectangle.
   *
   * @param g            the {@link Graphics} instance to draw to
   * @param rect         the {@link Rectangle} to use as bounding box
   * @param str          the string to draw
   * @param horzCentered if true, the string will be centered horizontally
   * @param vertCentered if true, the string will be centered vertically
   */
  public static void drawCenteredString(Graphics2D g, Rectangle rect, String str, boolean horzCentered, boolean vertCentered) {
    FontMetrics fm = g.getFontMetrics(g.getFont());
    int textWidth = fm.stringWidth(str) - 1;
    int x = horzCentered ? Math.max(rect.x, rect.x + (rect.width - textWidth) / 2) : rect.x;
    int y = vertCentered ? Math.max(rect.y, rect.y + rect.height / 2 + fm.getAscent() * 2 / 5) : rect.y;
    Shape oldClip = g.getClip();
    g.clip(rect);
    g.drawString(str, x, y);
    g.setClip(oldClip);
  }

  /**
   * Draws a centered string in the passed rectangle.
   *
   * @param g    the {@link Graphics} instance to draw to
   * @param rect the {@link Rectangle} to use as bounding box
   * @param str  the string to draw
   */
  public static void drawCenteredString(Graphics2D g, Rectangle rect, String str) {
    drawCenteredString(g, rect, str, true, true);
  }

  public static boolean isFocusAncestor(@Nonnull final Component component) {
    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (owner == null) return false;
    if (owner == component) return true;
    return SwingUtilities.isDescendingFrom(owner, component);
  }

  /**
   * @param component to check whether it can be focused or not
   * @return {@code true} if component is not {@code null} and can be focused
   * @see Component#isRequestFocusAccepted(boolean, boolean, sun.awt.CausedFocusEvent.Cause)
   */
  public static boolean isFocusable(@Nullable Component component) {
    return component != null && component.isFocusable() && component.isEnabled() && component.isShowing();
  }

  public static boolean isCloseClick(MouseEvent e) {
    return isCloseClick(e, MouseEvent.MOUSE_PRESSED);
  }

  public static boolean isCloseClick(MouseEvent e, int effectiveType) {
    if (e.isPopupTrigger() || e.getID() != effectiveType) return false;
    return e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON1 && e.isShiftDown();
  }

  public static boolean isActionClick(MouseEvent e) {
    return isActionClick(e, MouseEvent.MOUSE_PRESSED);
  }

  public static boolean isActionClick(MouseEvent e, int effectiveType) {
    return isActionClick(e, effectiveType, false);
  }

  public static boolean isActionClick(MouseEvent e, int effectiveType, boolean allowShift) {
    if (!allowShift && isCloseClick(e) || e.isPopupTrigger() || e.getID() != effectiveType) return false;
    return e.getButton() == MouseEvent.BUTTON1;
  }

  @Nonnull
  public static Color getBgFillColor(@Nonnull Component c) {
    final Component parent = findNearestOpaque(c);
    return parent == null ? c.getBackground() : parent.getBackground();
  }

  @Nullable
  public static Component findNearestOpaque(@Nullable Component c) {
    Component eachParent = c;
    while (eachParent != null) {
      if (eachParent.isOpaque()) return eachParent;
      eachParent = eachParent.getParent();
    }

    return null;
  }

  @Nullable
  public static Component findParentByCondition(@Nullable Component c, Condition<Component> condition) {
    Component eachParent = c;
    while (eachParent != null) {
      if (condition.value(eachParent)) return eachParent;
      eachParent = eachParent.getParent();
    }
    return null;
  }


  @NonNls
  public static String getCssFontDeclaration(final Font font) {
    return getCssFontDeclaration(font, null, null, null);
  }

  @Language("HTML")
  @NonNls
  public static String getCssFontDeclaration(final Font font, @Nullable Color fgColor, @Nullable Color linkColor, @Nullable String liImg) {
    URL resource = liImg != null ? SystemInfo.class.getResource(liImg) : null;

    @NonNls String fontFamilyAndSize = "font-family:" + font.getFamily() + "; font-size:" + font.getSize() + ";";
    @NonNls @Language("HTML") String body = "body, div, td, p {" + fontFamilyAndSize + " " + (fgColor != null ? "color:" + ColorUtil.toHex(fgColor) : "") + "}";
    if (resource != null) {
      body += "ul {list-style-image: " + resource.toExternalForm() + "}";
    }
    @NonNls String link = linkColor != null ? "a {" + fontFamilyAndSize + " color:" + ColorUtil.toHex(linkColor) + "}" : "";
    return "<style> " + body + " " + link + "</style>";
  }

  public static boolean isWinLafOnVista() {
    return SystemInfo.isWinVistaOrNewer && "Windows".equals(UIManager.getLookAndFeel().getName());
  }

  public static Color getFocusedFillColor() {
    return toAlpha(getListSelectionBackground(), 100);
  }

  public static Color getFocusedBoundsColor() {
    return getBoundsColor();
  }

  public static Color getBoundsColor() {
    return getBorderColor();
  }

  public static Color getBoundsColor(boolean focused) {
    return focused ? getFocusedBoundsColor() : getBoundsColor();
  }

  public static Color toAlpha(final Color color, final int alpha) {
    Color actual = color != null ? color : Color.black;
    return new Color(actual.getRed(), actual.getGreen(), actual.getBlue(), alpha);
  }

  @Deprecated
  public static void requestFocus(@Nonnull final JComponent c) {
    if (c.isShowing()) {
      c.requestFocus();
    }
    else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          c.requestFocus();
        }
      });
    }
  }

  //todo maybe should do for all kind of listeners via the AWTEventMulticaster class

  public static void dispose(final Component c) {
    if (c == null) return;

    final MouseListener[] mouseListeners = c.getMouseListeners();
    for (MouseListener each : mouseListeners) {
      c.removeMouseListener(each);
    }

    final MouseMotionListener[] motionListeners = c.getMouseMotionListeners();
    for (MouseMotionListener each : motionListeners) {
      c.removeMouseMotionListener(each);
    }

    final MouseWheelListener[] mouseWheelListeners = c.getMouseWheelListeners();
    for (MouseWheelListener each : mouseWheelListeners) {
      c.removeMouseWheelListener(each);
    }
  }

  public static void disposeProgress(final JProgressBar progress) {
    if (!isUnderNativeMacLookAndFeel()) return;

    SwingUtilities.invokeLater(() -> progress.setUI(null));
  }

  @Nullable
  public static Component findUltimateParent(Component c) {
    if (c == null) return null;

    Component eachParent = c;
    while (true) {
      if (eachParent.getParent() == null) return eachParent;
      eachParent = eachParent.getParent();
    }
  }

  public static Color getHeaderActiveColor() {
    return JBUI.CurrentTheme.Focus.focusColor();
  }

  public static Color getHeaderInactiveColor() {
    return INACTIVE_HEADER_COLOR;
  }

  public static Color getBorderColor() {
    return JBColor.border();
  }

  public static Font getTitledBorderFont() {
    Font defFont = getLabelFont();
    return defFont.deriveFont(Math.max(defFont.getSize() - 2f, 11f));
  }

  /**
   * @deprecated use getBorderColor instead
   */
  public static Color getBorderInactiveColor() {
    return getBorderColor();
  }

  /**
   * @deprecated use getBorderColor instead
   */
  public static Color getBorderActiveColor() {
    return getBorderColor();
  }

  /**
   * @deprecated use getBorderColor instead
   */
  public static Color getBorderSeparatorColor() {
    return getBorderColor();
  }


  @Nullable
  public static StyleSheet loadStyleSheet(@Nullable URL url) {
    if (url == null) return null;
    try {
      StyleSheet styleSheet = new StyleSheet();
      styleSheet.loadRules(new InputStreamReader(url.openStream(), CharsetToolkit.UTF8), url);
      return styleSheet;
    }
    catch (IOException e) {
      getLogger().warn(url + " loading failed", e);
      return null;
    }
  }

  @Nonnull
  public static FontUIResource getFontWithFallback(@Nonnull Font font) {
    return getFontWithFallback(font.getFamily(), font.getStyle(), font.getSize());
  }

  public static FontUIResource getFontWithFallback(@Nonnull String familyName, @JdkConstants.FontStyle int style, int size) {
    Font fontWithFallback = new StyleContext().getFont(familyName, style, size);
    return fontWithFallback instanceof FontUIResource ? (FontUIResource)fontWithFallback : new FontUIResource(fontWithFallback);
  }

  public static void removeScrollBorder(final Component c) {
    for (JScrollPane scrollPane : uiTraverser(c).filter(JScrollPane.class)) {
      if (!uiParents(scrollPane, true).takeWhile(Conditions.notEqualTo(c)).filter(Conditions.not(Conditions.instanceOf(JPanel.class, JLayeredPane.class))).isEmpty()) continue;

      Integer keepBorderSides = getClientProperty(scrollPane, KEEP_BORDER_SIDES);
      if (keepBorderSides != null) {
        if (scrollPane.getBorder() instanceof LineBorder) {
          Color color = ((LineBorder)scrollPane.getBorder()).getLineColor();
          scrollPane.setBorder(new SideBorder(color, keepBorderSides.intValue()));
        }
        else {
          scrollPane.setBorder(new SideBorder(getBoundsColor(), keepBorderSides.intValue()));
        }
      }
      else {
        scrollPane.setBorder(new SideBorder(getBoundsColor(), SideBorder.NONE));
      }
    }
  }

  public static boolean hasNonPrimitiveParents(Component stopParent, Component c) {
    Component eachParent = c.getParent();
    while (true) {
      if (eachParent == null || eachParent == stopParent) return false;
      if (!isPrimitive(eachParent)) return true;
      eachParent = eachParent.getParent();
    }
  }

  public static boolean isPrimitive(Component c) {
    return c instanceof JPanel || c instanceof JLayeredPane;
  }

  public static Point getCenterPoint(Dimension container, Dimension child) {
    return getCenterPoint(new Rectangle(new Point(), container), child);
  }

  public static Point getCenterPoint(Rectangle container, Dimension child) {
    Point result = new Point();

    Point containerLocation = container.getLocation();
    Dimension containerSize = container.getSize();

    result.x = containerLocation.x + containerSize.width / 2 - child.width / 2;
    result.y = containerLocation.y + containerSize.height / 2 - child.height / 2;

    return result;
  }

  public static String toHtml(String html) {
    return toHtml(html, 0);
  }

  @NonNls
  public static String toHtml(String html, final int hPadding) {
    html = CLOSE_TAG_PATTERN.matcher(html).replaceAll("<$1$2></$1>");
    Font font = getLabelFont();
    @NonNls String family = font != null ? font.getFamily() : "Tahoma";
    int size = font != null ? font.getSize() : 11;
    return "<html><style>body { font-family: " + family + "; font-size: " + size + ";} ul li {list-style-type:circle;}</style>" + addPadding(html, hPadding) + "</html>";
  }

  public static String addPadding(final String html, int hPadding) {
    return String.format("<p style=\"margin: 0 %dpx 0 %dpx;\">%s</p>", hPadding, hPadding, html);
  }

  public static String convertSpace2Nbsp(String html) {
    @NonNls StringBuilder result = new StringBuilder();
    int currentPos = 0;
    int braces = 0;
    while (currentPos < html.length()) {
      String each = html.substring(currentPos, currentPos + 1);
      if ("<".equals(each)) {
        braces++;
      }
      else if (">".equals(each)) {
        braces--;
      }

      if (" ".equals(each) && braces == 0) {
        result.append("&nbsp;");
      }
      else {
        result.append(each);
      }
      currentPos++;
    }

    return result.toString();
  }

  public static void invokeLaterIfNeeded(@Nonnull Runnable runnable) {
    if (EDT.isCurrentThreadEdt()) {
      runnable.run();
    }
    else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  /**
   * Invoke and wait in the event dispatch thread
   * or in the current thread if the current thread
   * is event queue thread.
   * DO NOT INVOKE THIS METHOD FROM UNDER READ ACTION.
   *
   * @param runnable a runnable to invoke
   * @see #invokeAndWaitIfNeeded(com.intellij.util.ThrowableRunnable)
   */
  public static void invokeAndWaitIfNeeded(@Nonnull Runnable runnable) {
    if (EDT.isCurrentThreadEdt()) {
      runnable.run();
    }
    else {
      try {
        SwingUtilities.invokeAndWait(runnable);
      }
      catch (Exception e) {
        getLogger().error(e);
      }
    }
  }

  public static <T> T invokeAndWaitIfNeeded(@Nonnull final Computable<T> computable) {
    final Ref<T> result = Ref.create();
    invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        result.set(computable.compute());
      }
    });
    return result.get();
  }

  public static void invokeAndWaitIfNeeded(@Nonnull final ThrowableRunnable runnable) throws Throwable {
    if (EDT.isCurrentThreadEdt()) {
      runnable.run();
    }
    else {
      final Ref<Throwable> ref = new Ref<Throwable>();
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          try {
            runnable.run();
          }
          catch (Throwable throwable) {
            ref.set(throwable);
          }
        }
      });
      if (!ref.isNull()) throw ref.get();
    }
  }

  public static boolean isFocusProxy(@Nullable Component c) {
    return c instanceof JComponent && Boolean.TRUE.equals(((JComponent)c).getClientProperty(FOCUS_PROXY_KEY));
  }

  public static void setFocusProxy(JComponent c, boolean isProxy) {
    c.putClientProperty(FOCUS_PROXY_KEY, isProxy ? Boolean.TRUE : null);
  }

  public static void maybeInstall(InputMap map, String action, KeyStroke stroke) {
    if (map.get(stroke) == null) {
      map.put(stroke, action);
    }
  }

  /**
   * Avoid blinking while changing background.
   *
   * @param component  component.
   * @param background new background.
   */
  public static void changeBackGround(final Component component, final Color background) {
    final Color oldBackGround = component.getBackground();
    if (background == null || !background.equals(oldBackGround)) {
      component.setBackground(background);
    }
  }

  public static void addKeyboardShortcut(final JComponent target, final AbstractButton button, final KeyStroke keyStroke) {
    target.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (button.isEnabled()) {
          button.doClick();
        }
      }
    }, keyStroke, JComponent.WHEN_FOCUSED);
  }

  public static void installComboBoxCopyAction(JComboBox comboBox) {
    final Component editorComponent = comboBox.getEditor().getEditorComponent();
    if (!(editorComponent instanceof JTextComponent)) return;
    final InputMap inputMap = ((JTextComponent)editorComponent).getInputMap();
    for (KeyStroke keyStroke : inputMap.allKeys()) {
      if (DefaultEditorKit.copyAction.equals(inputMap.get(keyStroke))) {
        comboBox.getInputMap().put(keyStroke, DefaultEditorKit.copyAction);
      }
    }
    comboBox.getActionMap().put(DefaultEditorKit.copyAction, new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (!(e.getSource() instanceof JComboBox)) return;
        final JComboBox comboBox = (JComboBox)e.getSource();
        final String text;
        final Object selectedItem = comboBox.getSelectedItem();
        if (selectedItem instanceof String) {
          text = (String)selectedItem;
        }
        else {
          final Component component = comboBox.getRenderer().getListCellRendererComponent(new JList(), selectedItem, 0, false, false);
          if (component instanceof JLabel) {
            text = ((JLabel)component).getText();
          }
          else if (component != null) {
            final String str = component.toString();
            // skip default Component.toString and handle SimpleColoredComponent case
            text = str == null || str.startsWith(component.getClass().getName() + "[") ? null : str;
          }
          else {
            text = null;
          }
        }
        if (text != null) {
          final JTextField textField = new JTextField(text);
          textField.selectAll();
          textField.copy();
        }
      }
    });
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public static void fixFormattedField(JFormattedTextField field) {
    if (SystemInfo.isMac) {
      final Toolkit toolkit = Toolkit.getDefaultToolkit();
      final int commandKeyMask = toolkit.getMenuShortcutKeyMask();
      final InputMap inputMap = field.getInputMap();
      final KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, commandKeyMask);
      inputMap.put(copyKeyStroke, "copy-to-clipboard");
      final KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, commandKeyMask);
      inputMap.put(pasteKeyStroke, "paste-from-clipboard");
      final KeyStroke cutKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_X, commandKeyMask);
      inputMap.put(cutKeyStroke, "cut-to-clipboard");
    }
  }

  public static boolean isPrinting(Graphics g) {
    return g instanceof PrintGraphics;
  }

  public static int getSelectedButton(ButtonGroup group) {
    Enumeration<AbstractButton> enumeration = group.getElements();
    int i = 0;
    while (enumeration.hasMoreElements()) {
      AbstractButton button = enumeration.nextElement();
      if (group.isSelected(button.getModel())) {
        return i;
      }
      i++;
    }
    return -1;
  }

  public static void setSelectedButton(ButtonGroup group, int index) {
    Enumeration<AbstractButton> enumeration = group.getElements();
    int i = 0;
    while (enumeration.hasMoreElements()) {
      AbstractButton button = enumeration.nextElement();
      group.setSelected(button.getModel(), index == i);
      i++;
    }
  }

  public static boolean isSelectionButtonDown(MouseEvent e) {
    return e.isShiftDown() || e.isControlDown() || e.isMetaDown();
  }

  @SuppressWarnings("deprecation")
  public static void setComboBoxEditorBounds(int x, int y, int width, int height, JComponent editor) {
    if (SystemInfo.isMac && isUnderAquaLookAndFeel()) {
      // fix for too wide combobox editor, see AquaComboBoxUI.layoutContainer:
      // it adds +4 pixels to editor width. WTF?!
      editor.reshape(x, y, width - 4, height - 1);
    }
    else {
      editor.reshape(x, y, width, height);
    }
  }

  @Deprecated
  @DeprecationInfo("Use #getListFixedCellHeight()")
  public static final int LIST_FIXED_CELL_HEIGHT = 20;

  public static int getListFixedCellHeight() {
    return JBUI.scale(LIST_FIXED_CELL_HEIGHT);
  }

  @Deprecated
  public static int fixComboBoxHeight(final int height) {
    return height;
  }

  /**
   * The main difference from javax.swing.SwingUtilities#isDescendingFrom(Component, Component) is that this method
   * uses getInvoker() instead of getParent() when it meets JPopupMenu
   *
   * @param child  child component
   * @param parent parent component
   * @return true if parent if a top parent of child, false otherwise
   * @see javax.swing.SwingUtilities#isDescendingFrom(java.awt.Component, java.awt.Component)
   */
  public static boolean isDescendingFrom(@Nullable Component child, @Nonnull Component parent) {
    while (child != null && child != parent) {
      child = child instanceof JPopupMenu ? ((JPopupMenu)child).getInvoker() : child.getParent();
    }
    return child == parent;
  }

  @Nullable
  public static <T> T getParentOfType(Class<? extends T> cls, Component c) {
    Component eachParent = c;
    while (eachParent != null) {
      if (cls.isAssignableFrom(eachParent.getClass())) {
        @SuppressWarnings({"unchecked"}) final T t = (T)eachParent;
        return t;
      }

      eachParent = eachParent.getParent();
    }

    return null;
  }

  public static void scrollListToVisibleIfNeeded(@Nonnull final JList list) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final int selectedIndex = list.getSelectedIndex();
        if (selectedIndex >= 0) {
          final Rectangle visibleRect = list.getVisibleRect();
          final Rectangle cellBounds = list.getCellBounds(selectedIndex, selectedIndex);
          if (!visibleRect.contains(cellBounds)) {
            list.scrollRectToVisible(cellBounds);
          }
        }
      }
    });
  }

  @Nullable
  public static <T extends JComponent> T findComponentOfType(JComponent parent, Class<T> cls) {
    if (parent == null || cls.isAssignableFrom(parent.getClass())) {
      @SuppressWarnings({"unchecked"}) final T t = (T)parent;
      return t;
    }
    for (Component component : parent.getComponents()) {
      if (component instanceof JComponent) {
        T comp = findComponentOfType((JComponent)component, cls);
        if (comp != null) return comp;
      }
    }
    return null;
  }

  public static <T extends JComponent> List<T> findComponentsOfType(JComponent parent, Class<T> cls) {
    final ArrayList<T> result = new ArrayList<T>();
    findComponentsOfType(parent, cls, result);
    return result;
  }

  private static <T extends JComponent> void findComponentsOfType(JComponent parent, Class<T> cls, ArrayList<T> result) {
    if (parent == null) return;
    if (cls.isAssignableFrom(parent.getClass())) {
      @SuppressWarnings({"unchecked"}) final T t = (T)parent;
      result.add(t);
    }
    for (Component c : parent.getComponents()) {
      if (c instanceof JComponent) {
        findComponentsOfType((JComponent)c, cls, result);
      }
    }
  }

  public static class TextPainter {
    private final List<Pair<String, LineInfo>> myLines = new ArrayList<Pair<String, LineInfo>>();
    private boolean myDrawShadow;
    private Color myShadowColor;
    private float myLineSpacing;

    public TextPainter() {
      myDrawShadow = /*isUnderAquaLookAndFeel() ||*/ isUnderDarkBuildInLaf();
      myShadowColor = isUnderDarkBuildInLaf() ? Gray._0.withAlpha(100) : Gray._220;
      myLineSpacing = 1.0f;
    }

    public TextPainter withShadow(final boolean drawShadow) {
      myDrawShadow = drawShadow;
      return this;
    }

    public TextPainter withShadow(final boolean drawShadow, final Color shadowColor) {
      myDrawShadow = drawShadow;
      myShadowColor = shadowColor;
      return this;
    }

    public TextPainter withLineSpacing(final float lineSpacing) {
      myLineSpacing = lineSpacing;
      return this;
    }

    public TextPainter appendLine(final String text) {
      if (text == null || text.isEmpty()) return this;
      myLines.add(Pair.create(text, new LineInfo()));
      return this;
    }

    public TextPainter underlined(@Nullable final Color color) {
      if (!myLines.isEmpty()) {
        final LineInfo info = myLines.get(myLines.size() - 1).getSecond();
        info.underlined = true;
        info.underlineColor = color;
      }

      return this;
    }

    public TextPainter withBullet(final char c) {
      if (!myLines.isEmpty()) {
        final LineInfo info = myLines.get(myLines.size() - 1).getSecond();
        info.withBullet = true;
        info.bulletChar = c;
      }

      return this;
    }

    public TextPainter withBullet() {
      return withBullet('\u2022');
    }

    public TextPainter underlined() {
      return underlined(null);
    }

    public TextPainter smaller() {
      if (!myLines.isEmpty()) {
        myLines.get(myLines.size() - 1).getSecond().smaller = true;
      }

      return this;
    }

    public TextPainter center() {
      if (!myLines.isEmpty()) {
        myLines.get(myLines.size() - 1).getSecond().center = true;
      }

      return this;
    }

    /**
     * _position(block width, block height) => (x, y) of the block
     */
    public void draw(@Nonnull final Graphics g, final PairFunction<Integer, Integer, Couple<Integer>> _position) {
      final int[] maxWidth = {0};
      final int[] height = {0};
      final int[] maxBulletWidth = {0};
      ContainerUtil.process(myLines, new Processor<Pair<String, LineInfo>>() {
        @Override
        public boolean process(final Pair<String, LineInfo> pair) {
          final LineInfo info = pair.getSecond();
          Font old = null;
          if (info.smaller) {
            old = g.getFont();
            g.setFont(old.deriveFont(old.getSize() * 0.70f));
          }

          final FontMetrics fm = g.getFontMetrics();

          final int bulletWidth = info.withBullet ? fm.stringWidth(" " + info.bulletChar) : 0;
          maxBulletWidth[0] = Math.max(maxBulletWidth[0], bulletWidth);

          maxWidth[0] = Math.max(fm.stringWidth(pair.getFirst().replace("<shortcut>", "").replace("</shortcut>", "") + bulletWidth), maxWidth[0]);
          height[0] += (fm.getHeight() + fm.getLeading()) * myLineSpacing;

          if (old != null) {
            g.setFont(old);
          }

          return true;
        }
      });

      final Pair<Integer, Integer> position = _position.fun(maxWidth[0] + 20, height[0]);
      assert position != null;

      final int[] yOffset = {position.getSecond()};
      ContainerUtil.process(myLines, new Processor<Pair<String, LineInfo>>() {
        @Override
        public boolean process(final Pair<String, LineInfo> pair) {
          final LineInfo info = pair.getSecond();
          String text = pair.first;
          String shortcut = "";
          if (pair.first.contains("<shortcut>")) {
            shortcut = text.substring(text.indexOf("<shortcut>") + "<shortcut>".length(), text.indexOf("</shortcut>"));
            text = text.substring(0, text.indexOf("<shortcut>"));
          }

          Font old = null;
          if (info.smaller) {
            old = g.getFont();
            g.setFont(old.deriveFont(old.getSize() * 0.70f));
          }

          final int x = position.getFirst() + maxBulletWidth[0] + 10;

          final FontMetrics fm = g.getFontMetrics();
          int xOffset = x;
          if (info.center) {
            xOffset = x + (maxWidth[0] - fm.stringWidth(text)) / 2;
          }

          if (myDrawShadow) {
            int xOff = isUnderDarkBuildInLaf() ? 1 : 0;
            int yOff = 1;
            Color oldColor = g.getColor();
            g.setColor(myShadowColor);

            if (info.withBullet) {
              g.drawString(info.bulletChar + " ", x - fm.stringWidth(" " + info.bulletChar) + xOff, yOffset[0] + yOff);
            }

            g.drawString(text, xOffset + xOff, yOffset[0] + yOff);
            g.setColor(oldColor);
          }

          if (info.withBullet) {
            g.drawString(info.bulletChar + " ", x - fm.stringWidth(" " + info.bulletChar), yOffset[0]);
          }

          g.drawString(text, xOffset, yOffset[0]);
          if (!StringUtil.isEmpty(shortcut)) {
            Color oldColor = g.getColor();
            if (isUnderDarkBuildInLaf()) {
              g.setColor(new Color(60, 118, 249));
            }
            g.drawString(shortcut, xOffset + fm.stringWidth(text + (isUnderDarkBuildInLaf() ? " " : "")), yOffset[0]);
            g.setColor(oldColor);
          }

          if (info.underlined) {
            Color c = null;
            if (info.underlineColor != null) {
              c = g.getColor();
              g.setColor(info.underlineColor);
            }

            g.drawLine(x - maxBulletWidth[0] - 10, yOffset[0] + fm.getDescent(), x + maxWidth[0] + 10, yOffset[0] + fm.getDescent());
            if (c != null) {
              g.setColor(c);

            }

            if (myDrawShadow) {
              c = g.getColor();
              g.setColor(myShadowColor);
              g.drawLine(x - maxBulletWidth[0] - 10, yOffset[0] + fm.getDescent() + 1, x + maxWidth[0] + 10, yOffset[0] + fm.getDescent() + 1);
              g.setColor(c);
            }
          }

          yOffset[0] += (fm.getHeight() + fm.getLeading()) * myLineSpacing;

          if (old != null) {
            g.setFont(old);
          }

          return true;
        }
      });
    }

    private static class LineInfo {
      private boolean underlined;
      private boolean withBullet;
      private char bulletChar;
      private Color underlineColor;
      private boolean smaller;
      private boolean center;
    }
  }

  @Nullable
  public static JRootPane getRootPane(Component c) {
    JRootPane root = getParentOfType(JRootPane.class, c);
    if (root != null) return root;
    Component eachParent = c;
    while (eachParent != null) {
      if (eachParent instanceof JComponent) {
        @SuppressWarnings({"unchecked"}) WeakReference<JRootPane> pane = (WeakReference<JRootPane>)((JComponent)eachParent).getClientProperty(ROOT_PANE);
        if (pane != null) return pane.get();
      }
      eachParent = eachParent.getParent();
    }

    return null;
  }

  public static void setFutureRootPane(JComponent c, JRootPane pane) {
    c.putClientProperty(ROOT_PANE, new WeakReference<JRootPane>(pane));
  }

  public static boolean isMeaninglessFocusOwner(@Nullable Component c) {
    if (c == null || !c.isShowing()) return true;

    return c instanceof JFrame || c instanceof JDialog || c instanceof JWindow || c instanceof JRootPane || isFocusProxy(c);
  }

  @Nonnull
  public static Timer createNamedTimer(@NonNls @Nonnull final String name, int delay, @Nonnull ActionListener listener) {
    return new Timer(delay, listener) {
      @Override
      public String toString() {
        return name;
      }
    };
  }

  @Nonnull
  public static Timer createNamedTimer(@NonNls @Nonnull final String name, int delay) {
    return new Timer(delay, null) {
      @Override
      public String toString() {
        return name;
      }
    };
  }

  public static boolean isDialogRootPane(JRootPane rootPane) {
    if (rootPane != null) {
      final Object isDialog = rootPane.getClientProperty("DIALOG_ROOT_PANE");
      return isDialog instanceof Boolean && ((Boolean)isDialog).booleanValue();
    }
    return false;
  }

  @Nullable
  public static JComponent mergeComponentsWithAnchor(PanelWithAnchor... panels) {
    return mergeComponentsWithAnchor(Arrays.asList(panels));
  }

  @Nullable
  public static JComponent mergeComponentsWithAnchor(Collection<? extends PanelWithAnchor> panels) {
    JComponent maxWidthAnchor = null;
    int maxWidth = 0;
    for (PanelWithAnchor panel : panels) {
      JComponent anchor = panel != null ? panel.getAnchor() : null;
      if (anchor != null) {
        int anchorWidth = anchor.getPreferredSize().width;
        if (maxWidth < anchorWidth) {
          maxWidth = anchorWidth;
          maxWidthAnchor = anchor;
        }
      }
    }
    for (PanelWithAnchor panel : panels) {
      if (panel != null) {
        panel.setAnchor(maxWidthAnchor);
      }
    }
    return maxWidthAnchor;
  }

  public static void setNotOpaqueRecursively(@Nonnull Component component) {
    if (!isUnderAquaLookAndFeel()) return;

    if (component.getBackground().equals(getPanelBackground()) || component instanceof JScrollPane || component instanceof JViewport) {
      if (component instanceof JComponent) {
        ((JComponent)component).setOpaque(false);
      }
      if (component instanceof Container) {
        for (Component c : ((Container)component).getComponents()) {
          setNotOpaqueRecursively(c);
        }
      }
    }
  }


  public static void setBackgroundRecursively(@Nonnull Component component, @Nonnull Color bg) {
    component.setBackground(bg);
    if (component instanceof Container) {
      for (Component c : ((Container)component).getComponents()) {
        setBackgroundRecursively(c, bg);
      }
    }
  }

  /**
   * Adds an empty border with the specified insets to the specified component.
   * If the component already has a border it will be preserved.
   *
   * @param component the component to which border added
   * @param top       the inset from the top
   * @param left      the inset from the left
   * @param bottom    the inset from the bottom
   * @param right     the inset from the right
   */
  public static void addInsets(@Nonnull JComponent component, int top, int left, int bottom, int right) {
    addBorder(component, BorderFactory.createEmptyBorder(top, left, bottom, right));
  }

  public static void addInsets(@Nonnull JComponent component, @Nonnull Insets insets) {
    if (component.getBorder() != null) {
      component.setBorder(new CompoundBorder(new EmptyBorder(insets), component.getBorder()));
    }
    else {
      component.setBorder(new EmptyBorder(insets));
    }
  }

  public static Dimension addInsets(@Nonnull Dimension dimension, @Nonnull Insets insets) {
    Dimension ans = new Dimension(dimension);
    ans.width += insets.left;
    ans.width += insets.right;
    ans.height += insets.top;
    ans.height += insets.bottom;

    return ans;
  }

  public static void adjustWindowToMinimumSize(final Window window) {
    if (window == null) return;
    final Dimension minSize = window.getMinimumSize();
    final Dimension size = window.getSize();
    final Dimension newSize = new Dimension(Math.max(size.width, minSize.width), Math.max(size.height, minSize.height));

    if (!newSize.equals(size)) {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (window.isShowing()) {
            window.setSize(newSize);
          }
        }
      });
    }
  }

  @Nullable
  public static Color getColorAt(final Icon icon, final int x, final int y) {
    if (0 <= x && x < icon.getIconWidth() && 0 <= y && y < icon.getIconHeight()) {
      final BufferedImage image = createImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
      icon.paintIcon(null, image.getGraphics(), 0, 0);

      final int[] pixels = new int[1];
      final PixelGrabber pixelGrabber = new PixelGrabber(image, x, y, 1, 1, pixels, 0, 1);
      try {
        pixelGrabber.grabPixels();
        return new Color(pixels[0]);
      }
      catch (InterruptedException ignored) {
      }
    }

    return null;
  }

  public static int getLcdContrastValue() {
    int lcdContrastValue = Registry.get("lcd.contrast.value").asInteger();

    // Evaluate the value depending on our current theme
    if (lcdContrastValue == 0) {
      if (SystemInfo.isMacIntel64) {
        lcdContrastValue = isUnderDarcula() ? 140 : 200;
      }
      else {
        Map map = (Map)Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");

        if (map == null) {
          lcdContrastValue = 140;
        }
        else {
          Object o = map.get(RenderingHints.KEY_TEXT_LCD_CONTRAST);
          lcdContrastValue = (o == null) ? 140 : ((Integer)o);
        }
      }
    }

    if (lcdContrastValue < 100 || lcdContrastValue > 250) {
      // the default value
      lcdContrastValue = 140;
    }

    return lcdContrastValue;
  }

  public static void addBorder(JComponent component, Border border) {
    if (component == null) return;

    if (component.getBorder() != null) {
      component.setBorder(new CompoundBorder(border, component.getBorder()));
    }
    else {
      component.setBorder(border);
    }
  }

  private static final Color DECORATED_ROW_BG_COLOR = new JBColor(new Color(242, 245, 249), new Color(79, 83, 84));

  public static Color getDecoratedRowColor() {
    return DECORATED_ROW_BG_COLOR;
  }

  @Nonnull
  public static Paint getGradientPaint(float x1, float y1, @Nonnull Color c1, float x2, float y2, @Nonnull Color c2) {
    return (Registry.is("ui.no.bangs.and.whistles", false)) ? ColorUtil.mix(c1, c2, .5) : new GradientPaint(x1, y1, c1, x2, y2, c2);
  }

  @Nullable
  public static Point getLocationOnScreen(@Nonnull JComponent component) {
    int dx = 0;
    int dy = 0;
    for (Container c = component; c != null; c = c.getParent()) {
      if (c.isShowing()) {
        Point locationOnScreen = c.getLocationOnScreen();
        locationOnScreen.translate(dx, dy);
        return locationOnScreen;
      }
      else {
        Point location = c.getLocation();
        dx += location.x;
        dy += location.y;
      }
    }
    return null;
  }

  @Nonnull
  public static Window getActiveWindow() {
    Window[] windows = Window.getWindows();
    for (Window each : windows) {
      if (each.isVisible() && each.isActive()) return each;
    }
    return JOptionPane.getRootFrame();
  }

  public static void suppressFocusStealing(Window window) {
    // Focus stealing is not a problem on Mac
    if (SystemInfo.isMac) return;
    if (Registry.is("suppress.focus.stealing")) {
      setAutoRequestFocus(window, false);
    }
  }

  public static void setAutoRequestFocus(final Window onWindow, final boolean set) {
    if (!SystemInfo.isMac) {
      try {
        onWindow.getClass().getMethod("setAutoRequestFocus", boolean.class).invoke(onWindow, set);
      }
      catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        getLogger().debug(e);
      }
    }
  }

  @Nonnull
  public static String rightArrow() {
    return FontUtil.rightArrow(getLabelFont());
  }

  public static EmptyBorder getTextAlignBorder(@Nonnull JToggleButton alignSource) {
    ButtonUI ui = alignSource.getUI();
    int leftGap = alignSource.getIconTextGap();
    Border border = alignSource.getBorder();
    if (border != null) {
      leftGap += border.getBorderInsets(alignSource).left;
    }
    if (ui instanceof BasicRadioButtonUI) {
      leftGap += ((BasicRadioButtonUI)alignSource.getUI()).getDefaultIcon().getIconWidth();
    }
    else {
      Method method = ReflectionUtil.getMethod(ui.getClass(), "getDefaultIcon", JComponent.class);
      if (method != null) {
        try {
          Object o = method.invoke(ui, alignSource);
          if (o instanceof Icon) {
            leftGap += ((Icon)o).getIconWidth();
          }
        }
        catch (IllegalAccessException e) {
          e.printStackTrace();
        }
        catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
    return new EmptyBorder(0, leftGap, 0, 0);
  }

  /**
   * It is your responsibility to set correct horizontal align (left in case of UI Designer)
   */
  public static void configureNumericFormattedTextField(@Nonnull JFormattedTextField textField) {
    NumberFormat format = NumberFormat.getIntegerInstance();
    format.setParseIntegerOnly(true);
    format.setGroupingUsed(false);
    NumberFormatter numberFormatter = new NumberFormatter(format);
    numberFormatter.setMinimum(0);
    textField.setFormatterFactory(new DefaultFormatterFactory(numberFormatter));
    textField.setHorizontalAlignment(SwingConstants.TRAILING);

    textField.setColumns(4);
  }


  /**
   * Returns the first window ancestor of the component.
   * Note that this method returns the component itself if it is a window.
   *
   * @param component the component used to find corresponding window
   * @return the first window ancestor of the component; or {@code null}
   * if the component is not a window and is not contained inside a window
   */
  @Nullable
  public static Window getWindow(@Nullable Component component) {
    return component == null ? null : component instanceof Window ? (Window)component : SwingUtilities.getWindowAncestor(component);
  }

  public static boolean isAncestor(@Nonnull Component ancestor, @Nullable Component descendant) {
    while (descendant != null) {
      if (descendant == ancestor) {
        return true;
      }
      descendant = descendant.getParent();
    }
    return false;
  }

  public static void resetUndoRedoActions(@Nonnull JTextComponent textComponent) {
    UndoManager undoManager = getClientProperty(textComponent, UNDO_MANAGER);
    if (undoManager != null) {
      undoManager.discardAllEdits();
    }
  }

  //May have no usages but it's useful in runtime (Debugger "watches", some logging etc.)
  public static String getDebugText(Component c) {
    StringBuilder builder = new StringBuilder();
    getAllTextsRecursivelyImpl(c, builder);
    return builder.toString();
  }

  private static void getAllTextsRecursivelyImpl(Component component, StringBuilder builder) {
    String candidate = "";
    if (component instanceof JLabel) candidate = ((JLabel)component).getText();
    if (component instanceof JTextComponent) candidate = ((JTextComponent)component).getText();
    if (component instanceof AbstractButton) candidate = ((AbstractButton)component).getText();
    if (StringUtil.isNotEmpty(candidate)) {
      candidate = candidate.replaceAll("<a href=\"#inspection/[^)]+\\)", "");
      if (builder.length() > 0) builder.append(' ');
      builder.append(StringHtmlUtil.removeHtmlTags(candidate).trim());
    }
    if (component instanceof Container) {
      Component[] components = ((Container)component).getComponents();
      for (Component child : components) {
        getAllTextsRecursivelyImpl(child, builder);
      }
    }
  }

  public static int getLineHeight(@Nonnull JTextComponent textComponent) {
    return textComponent.getFontMetrics(textComponent.getFont()).getHeight();
  }

  public static void addUndoRedoActions(@Nonnull final JTextComponent textComponent) {
    UndoManager undoManager = new UndoManager();
    textComponent.putClientProperty(UNDO_MANAGER, undoManager);
    textComponent.getDocument().addUndoableEditListener(undoManager);
    textComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK), "undoKeystroke");
    textComponent.getActionMap().put("undoKeystroke", UNDO_ACTION);
    textComponent.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, (SystemInfo.isMac ? InputEvent.META_MASK : InputEvent.CTRL_MASK) | InputEvent.SHIFT_MASK), "redoKeystroke");
    textComponent.getActionMap().put("redoKeystroke", REDO_ACTION);
  }

  /**
   * KeyEvents for specified keystrokes would be redispatched to target component
   */
  public static void redirectKeystrokes(@Nonnull Disposable disposable, @Nonnull final JComponent source, @Nonnull final JComponent target, @Nonnull final KeyStroke... keyStrokes) {
    final KeyAdapter keyAdapter = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        KeyStroke keyStrokeForEvent = KeyStroke.getKeyStrokeForEvent(e);
        for (KeyStroke stroke : keyStrokes) {
          if (!stroke.isOnKeyRelease() && stroke.equals(keyStrokeForEvent)) target.dispatchEvent(e);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        KeyStroke keyStrokeForEvent = KeyStroke.getKeyStrokeForEvent(e);
        for (KeyStroke stroke : keyStrokes) {
          if (stroke.isOnKeyRelease() && stroke.equals(keyStrokeForEvent)) target.dispatchEvent(e);
        }
      }
    };
    source.addKeyListener(keyAdapter);
    Disposer.register(disposable, new Disposable() {
      @Override
      public void dispose() {
        source.removeKeyListener(keyAdapter);
      }
    });
  }

  /**
   * Employs a common pattern to use {@code Graphics}. This is a non-distractive approach
   * all modifications on {@code Graphics} are metter only inside the {@code Consumer} block
   *
   * @param originGraphics  graphics to work with
   * @param drawingConsumer you can use the Graphics2D object here safely
   */
  public static void useSafely(@Nonnull Graphics originGraphics, @Nonnull Consumer<? super Graphics2D> drawingConsumer) {
    Graphics2D graphics = (Graphics2D)originGraphics.create();
    try {
      drawingConsumer.consume(graphics);
    }
    finally {
      graphics.dispose();
    }
  }

  public static void setCursor(@Nonnull Component component, Cursor cursor) {
    // cursor is updated by native code even if component has the same cursor, causing performance problems (IDEA-167733)
    if (component.isCursorSet() && component.getCursor() == cursor) return;
    component.setCursor(cursor);
  }

  /**
   * This method (as opposed to {@link JEditorPane#scrollToReference}) supports also targets using {@code id} HTML attribute.
   */
  public static void scrollToReference(@Nonnull JEditorPane editor, @Nonnull String reference) {
    Document document = editor.getDocument();
    if (document instanceof HTMLDocument) {
      Element elementById = ((HTMLDocument)document).getElement(reference);
      if (elementById != null) {
        try {
          int pos = elementById.getStartOffset();
          Rectangle r = editor.modelToView(pos);
          if (r != null) {
            r.height = editor.getVisibleRect().height;
            editor.scrollRectToVisible(r);
            editor.setCaretPosition(pos);
          }
        }
        catch (BadLocationException e) {
          getLogger().error(e);
        }
        return;
      }
    }
    editor.scrollToReference(reference);
  }

  public static void runWhenFocused(@Nonnull Component component, @Nonnull Runnable runnable) {
    assert component.isShowing();
    if (component.isFocusOwner()) {
      runnable.run();
    }
    else {
      Disposable disposable = Disposable.newDisposable();
      FocusListener focusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          Disposer.dispose(disposable);
          runnable.run();
        }
      };
      HierarchyListener hierarchyListener = new HierarchyListener() {
        @Override
        public void hierarchyChanged(HierarchyEvent e) {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !component.isShowing()) {
            Disposer.dispose(disposable);
          }
        }
      };
      component.addFocusListener(focusListener);
      component.addHierarchyListener(hierarchyListener);
      Disposer.register(disposable, () -> {
        component.removeFocusListener(focusListener);
        component.removeHierarchyListener(hierarchyListener);
      });
    }
  }

  @Nonnull
  public static Color getTooltipSeparatorColor() {
    return JBColor.namedColor("Tooltip.separatorColor", 0xd1d1d1, 0x545658);
  }

  public static boolean isHelpButton(Component button) {
    return button instanceof JButton && "help".equals(((JComponent)button).getClientProperty("JButton.buttonType"));
  }

  @Nonnull
  public static Color getToolTipActionBackground() {
    return JBColor.namedColor("ToolTip.Actions.background", new JBColor(Gray.xEB, new Color(0x43474a)));
  }

  public static void doNotScrollToCaret(@Nonnull JTextComponent textComponent) {
    textComponent.setCaret(new DefaultCaret() {
      @Override
      protected void adjustVisibility(Rectangle nloc) {
      }
    });
  }

  @Nonnull
  public static Dimension updateListRowHeight(@Nonnull Dimension size) {
    size.height = Math.max(size.height, UIManager.getInt("List.rowHeight"));
    return size;
  }

  //Escape error-prone HTML data (if any) when we use it in renderers, see IDEA-170768
  public static <T> T htmlInjectionGuard(T toRender) {
    if (toRender instanceof String && StringUtil.toLowerCase(((String)toRender)).startsWith("<html>")) {
      //noinspection unchecked
      return (T)("<html>" + StringUtil.escapeXmlEntities((String)toRender));
    }
    return toRender;
  }

  public static boolean isDialogFont(@Nonnull Font font) {
    return Font.DIALOG.equals(font.getFamily(Locale.US));
  }

  public static boolean haveCommonOwner(Component c1, Component c2) {
    if (c1 == null || c2 == null) return false;
    Window c1Ancestor = findWindowAncestor(c1);
    Window c2Ancestor = findWindowAncestor(c2);

    Set<Window> ownerSet = new HashSet<>();

    Window owner = c1Ancestor;

    while (owner != null && !(owner instanceof JDialog || owner instanceof JFrame)) {
      ownerSet.add(owner);
      owner = owner.getOwner();
    }

    owner = c2Ancestor;

    while (owner != null && !(owner instanceof JDialog || owner instanceof JFrame)) {
      if (ownerSet.contains(owner)) return true;
      owner = owner.getOwner();
    }

    return false;
  }

  public static void layoutRecursively(@Nonnull Component component) {
    if (component instanceof JComponent) {
      component.doLayout();
      for (Component child : ((JComponent)component).getComponents()) {
        layoutRecursively(child);
      }
    }
  }

  //x and y should be from {0, 0} to {parent.getWidth(), parent.getHeight()}
  @Nullable
  public static Component getDeepestComponentAt(@Nonnull Component parent, int x, int y) {
    Component component = SwingUtilities.getDeepestComponentAt(parent, x, y);
    if (component != null && component.getParent() instanceof JRootPane) {//GlassPane case
      JRootPane rootPane = (JRootPane)component.getParent();
      Point point = SwingUtilities.convertPoint(parent, new Point(x, y), rootPane.getLayeredPane());
      component = SwingUtilities.getDeepestComponentAt(rootPane.getLayeredPane(), point.x, point.y);
      if (component == null) {
        point = SwingUtilities.convertPoint(parent, new Point(x, y), rootPane.getContentPane());
        component = SwingUtilities.getDeepestComponentAt(rootPane.getContentPane(), point.x, point.y);
      }
    }
    return component;
  }

  public static boolean isWindowClientPropertyTrue(Window window, @Nonnull Object key) {
    return Boolean.TRUE.equals(getWindowClientProperty(window, key));
  }

  public static Object getWindowClientProperty(Window window, @Nonnull Object key) {
    if (window instanceof RootPaneContainer) {
      JRootPane pane = ((RootPaneContainer)window).getRootPane();
      if (pane != null) {
        return pane.getClientProperty(key);
      }
    }
    return null;
  }

  public static void putWindowClientProperty(Window window, @Nonnull Object key, Object value) {
    if (window instanceof RootPaneContainer) {
      JRootPane pane = ((RootPaneContainer)window).getRootPane();
      if (pane != null) {
        pane.putClientProperty(key, value);
      }
    }
  }

  // Here we setup dialog to be suggested in OwnerOptional as owner even if the dialog is not modal
  public static void markAsPossibleOwner(Dialog dialog) {
    putWindowClientProperty(dialog, "PossibleOwner", Boolean.TRUE);
  }

  public static boolean isPossibleOwner(@Nonnull Dialog dialog) {
    return isWindowClientPropertyTrue(dialog, "PossibleOwner");
  }

  private static Window findWindowAncestor(@Nonnull Component c) {
    return c instanceof Window ? (Window)c : SwingUtilities.getWindowAncestor(c);
  }

  /**
   * Places the specified window at the top of the stacking order and shows it in front of any other windows.
   * If the window is iconified it will be shown anyway.
   *
   * @param window the window to activate
   */
  public static void toFront(@Nullable Window window) {
    if (window instanceof Frame) {
      ((Frame)window).setState(Frame.NORMAL);
    }
    if (window != null) {
      window.toFront();
    }
  }

  /**
   * Calculates a component style from the corresponding client property.
   * The key "JComponent.sizeVariant" is used by Apple's L&F to scale components.
   *
   * @param component a component to process
   * @return a component style of the specified component
   */
  @Nonnull
  public static ComponentStyle getComponentStyle(Component component) {
    if (component instanceof JComponent) {
      Object property = ((JComponent)component).getClientProperty("JComponent.sizeVariant");
      if ("large".equals(property)) return ComponentStyle.LARGE;
      if ("small".equals(property)) return ComponentStyle.SMALL;
      if ("mini".equals(property)) return ComponentStyle.MINI;
    }
    return ComponentStyle.REGULAR;
  }

  // Here we setup window to be checked in IdeEventQueue and reset typeahead state when the window finally appears and gets focus
  public static void markAsTypeAheadAware(Window window) {
    ClientProperty.put(window, "TypeAheadAwareWindow", Boolean.TRUE);
  }

  @Nonnull
  public static Logger getLogger() {
    return Logger.getInstance(UIUtil.class);
  }
}
