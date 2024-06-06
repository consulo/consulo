// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.internal.laf;

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.util.registry.Registry;
import consulo.colorScheme.EditorColorKey;
import consulo.platform.Platform;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.EditorColorsUtil;
import consulo.ui.ex.awt.MixedColorProducer;
import consulo.ui.ex.awt.RegionPainter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.paint.RectanglePainter;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * This is an internal implementation for drawing opaque and transparent scroll bars.
 * It is public only to provide the ability to edit colors in the Settings/Preferences.
 * Due to the fact that the colors are animated, the constants given in the class
 * represent some key points for drawing scrollbars in different modes.
 *
 * @see consulo.ide.impl.idea.openapi.options.colors.pages.GeneralColorsPage
 */
public abstract class ScrollBarPainter implements RegionPainter<Float> {
  public final Rectangle bounds = new Rectangle();
  public final TwoWayAnimator animator;

  /**
   * The background of the JScrollBar component.
   * It makes sense for opaque scroll bars only.
   */
  public static final EditorColorKey BACKGROUND = key(0xFFF5F5F5, 0xFF3F4244, "ScrollBar.background");

  /**
   * The scroll track background on opaque scroll bar.
   */
  public static final EditorColorKey TRACK_OPAQUE_BACKGROUND = Platform.current().os().isMac() ? key(0x00808080, 0x00808080, "ScrollBar.Mac.trackColor") : key(0x00808080, 0x00808080, "ScrollBar.trackColor");
  /**
   * The scroll track background on opaque scroll bar when it is hovered.
   */
  public static final EditorColorKey TRACK_OPAQUE_HOVERED_BACKGROUND =
          Platform.current().os().isMac() ? key(0x00808080, 0x00808080, "ScrollBar.Mac.hoverTrackColor") : key(0x00808080, 0x00808080, "ScrollBar.hoverTrackColor");
  /**
   * The scroll track background on transparent scroll bar.
   */
  public static final EditorColorKey TRACK_BACKGROUND =
          Platform.current().os().isMac() ? key(0x00808080, 0x00808080, "ScrollBar.Mac.Transparent.trackColor") : key(0x00808080, 0x00808080, "ScrollBar.Transparent.trackColor");
  /**
   * The scroll track background on transparent scroll bar when it is hovered.
   */
  public static final EditorColorKey TRACK_HOVERED_BACKGROUND =
          Platform.current().os().isMac() ? key(0x1A808080, 0x1A808080, "ScrollBar.Mac.Transparent.hoverTrackColor") : key(0x1A808080, 0x1A808080, "ScrollBar.Transparent.hoverTrackColor");

  /**
   * The scroll thumb border color on opaque scroll bar.
   */
  public static final EditorColorKey THUMB_OPAQUE_FOREGROUND =
          Platform.current().os().isMac() ? key(0x33000000, 0x59262626, "ScrollBar.Mac.thumbBorderColor") : key(0x33595959, 0x47383838, "ScrollBar.thumbBorderColor");
  /**
   * The scroll thumb background on opaque scroll bar.
   */
  public static final EditorColorKey THUMB_OPAQUE_BACKGROUND = Platform.current().os().isMac() ? key(0x33000000, 0x59808080, "ScrollBar.Mac.thumbColor") : key(0x33737373, 0x47A6A6A6, "ScrollBar.thumbColor");
  /**
   * The scroll thumb border color on opaque scroll bar when it is hovered.
   */
  public static final EditorColorKey THUMB_OPAQUE_HOVERED_FOREGROUND =
          Platform.current().os().isMac() ? key(0x80000000, 0x8C262626, "ScrollBar.Mac.hoverThumbBorderColor") : key(0x47595959, 0x59383838, "ScrollBar.hoverThumbBorderColor");
  /**
   * The scroll thumb background on opaque scroll bar when it is hovered.
   */
  public static final EditorColorKey THUMB_OPAQUE_HOVERED_BACKGROUND =
          Platform.current().os().isMac() ? key(0x80000000, 0x8C808080, "ScrollBar.Mac.hoverThumbColor") : key(0x47737373, 0x59A6A6A6, "ScrollBar.hoverThumbColor");
  /**
   * The scroll thumb border color on transparent scroll bar.
   */
  public static final EditorColorKey THUMB_FOREGROUND =
          Platform.current().os().isMac() ? key(0x00000000, 0x00262626, "ScrollBar.Mac.Transparent.thumbBorderColor") : key(0x33595959, 0x47383838, "ScrollBar.Transparent.thumbBorderColor");
  /**
   * The scroll thumb background on transparent scroll bar.
   */
  public static final EditorColorKey THUMB_BACKGROUND =
          Platform.current().os().isMac() ? key(0x00000000, 0x00808080, "ScrollBar.Mac.Transparent.thumbColor") : key(0x33737373, 0x47A6A6A6, "ScrollBar.Transparent.thumbColor");
  /**
   * The scroll thumb border color on transparent scroll bar when it is hovered.
   */
  public static final EditorColorKey THUMB_HOVERED_FOREGROUND =
          Platform.current().os().isMac() ? key(0x80000000, 0x8C262626, "ScrollBar.Mac.Transparent.hoverThumbBorderColor") : key(0x47595959, 0x59383838, "ScrollBar.Transparent.hoverThumbBorderColor");
  /**
   * The scroll thumb background on transparent scroll bar when it is hovered.
   */
  public static final EditorColorKey THUMB_HOVERED_BACKGROUND =
          Platform.current().os().isMac() ? key(0x80000000, 0x8C808080, "ScrollBar.Mac.Transparent.hoverThumbColor") : key(0x47737373, 0x59A6A6A6, "ScrollBar.Transparent.hoverThumbColor");

  private static final List<EditorColorKey> CONTRAST_ELEMENTS_KEYS =
          Arrays.asList(THUMB_OPAQUE_FOREGROUND, THUMB_OPAQUE_BACKGROUND, THUMB_OPAQUE_HOVERED_FOREGROUND, THUMB_OPAQUE_HOVERED_BACKGROUND, THUMB_FOREGROUND, THUMB_BACKGROUND,
                        THUMB_HOVERED_FOREGROUND, THUMB_HOVERED_BACKGROUND);

  private static final int LIGHT_ALPHA = Platform.current().os().isMac() ? 120 : 160;
  private static final int DARK_ALPHA = Platform.current().os().isMac() ? 255 : 180;

  ScrollBarPainter(@Nonnull Supplier<? extends Component> supplier) {
    animator = new TwoWayAnimator(getClass().getName(), 11, 150, 125, 300, 125) {
      @Override
      void onValueUpdate() {
        Component component = supplier.get();
        if (component != null) component.repaint();
      }
    };
  }

  @Nonnull
  private static EditorColorKey key(int light, int dark, @Nonnull String name) {
    return EditorColorsUtil.createColorKey(name, new JBColor(new Color(light, true), new Color(dark, true)));
  }

  @Nonnull
  private static Color getColor(@Nullable Component component, @Nonnull EditorColorKey key) {
    Color color = TargetAWT.to(EditorColorsUtil.getColor(component, key));
    assert color != null : "default color is not specified for " + key;

    boolean useContrastScrollbars = UISettings.getShadowInstance().getUseContrastScrollbars();
    if (useContrastScrollbars) color = updateTransparency(color, key);

    return color;
  }

  private static Color updateTransparency(Color color, EditorColorKey key) {
    if (!CONTRAST_ELEMENTS_KEYS.contains(key)) return color;

    int alpha = Registry.intValue("contrast.scrollbars.alpha.level");
    if (alpha > 0) {
      alpha = Integer.min(alpha, 255);
    }
    else {
      alpha = UIUtil.isUnderDarcula() ? DARK_ALPHA : LIGHT_ALPHA;
    }

    return ColorUtil.toAlpha(color, alpha);
  }

  static Color getColor(@Nonnull Supplier<? extends Component> supplier, @Nonnull EditorColorKey key) {
    return new JBColor(() -> getColor(supplier.get(), key));
  }

  static Color getColor(@Nonnull Supplier<? extends Component> supplier, @Nonnull EditorColorKey transparent, @Nonnull EditorColorKey opaque) {
    return new JBColor(() -> {
      Component component = supplier.get();
      return getColor(component, component != null && DefaultScrollBarUI.isOpaque(component) ? opaque : transparent);
    });
  }

  public static void setBackground(@Nonnull Component component) {
    if (!Application.get().isSwingApplication()) {
      return; // FIXME [VISTALL] hack due it will create in unified app, and will throw error  due conversion to awt which not supported
    }
    component.setBackground(new JBColor(() -> getColor(component, BACKGROUND)));
  }

  public static final class Track extends ScrollBarPainter {
    private final MixedColorProducer fillProducer;

    public Track(@Nonnull Supplier<? extends Component> supplier) {
      super(supplier);
      fillProducer = new MixedColorProducer(getColor(supplier, TRACK_BACKGROUND, TRACK_OPAQUE_BACKGROUND), getColor(supplier, TRACK_HOVERED_BACKGROUND, TRACK_OPAQUE_HOVERED_BACKGROUND));
    }

    @Override
    public void paint(@Nonnull Graphics2D g, int x, int y, int width, int height, @Nullable Float value) {
      double mixer = value == null ? 0 : value.doubleValue();
      Color fill = fillProducer.produce(mixer);
      if (0 >= fill.getAlpha()) return; // optimization

      g.setPaint(fill);
      RectanglePainter.FILL.paint(g, x, y, width, height, null);
    }
  }

  public static final class Thumb extends ScrollBarPainter {
    private final MixedColorProducer fillProducer;
    private final MixedColorProducer drawProducer;

    public Thumb(@Nonnull Supplier<? extends Component> supplier, boolean opaque) {
      super(supplier);
      fillProducer = new MixedColorProducer(opaque ? getColor(supplier, THUMB_OPAQUE_BACKGROUND) : getColor(supplier, THUMB_BACKGROUND, THUMB_OPAQUE_BACKGROUND),
                                            opaque ? getColor(supplier, THUMB_OPAQUE_HOVERED_BACKGROUND) : getColor(supplier, THUMB_HOVERED_BACKGROUND, THUMB_OPAQUE_HOVERED_BACKGROUND));
      drawProducer = new MixedColorProducer(opaque ? getColor(supplier, THUMB_OPAQUE_FOREGROUND) : getColor(supplier, THUMB_FOREGROUND, THUMB_OPAQUE_FOREGROUND),
                                            opaque ? getColor(supplier, THUMB_OPAQUE_HOVERED_FOREGROUND) : getColor(supplier, THUMB_HOVERED_FOREGROUND, THUMB_OPAQUE_HOVERED_FOREGROUND));
    }

    @Override
    public void paint(@Nonnull Graphics2D g, int x, int y, int width, int height, @Nullable Float value) {
      double mixer = value == null ? 0 : value.doubleValue();
      Color fill = fillProducer.produce(mixer);

      int arc = 8;
      int margin = 2;
      x += margin;
      y += margin;
      width -= margin + margin;
      height -= margin + margin;

      RectanglePainter.paint(g, x, y, width, height, arc, fill, null);
    }
  }
}
