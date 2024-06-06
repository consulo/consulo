// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.platform.Platform;
import consulo.ui.ex.awt.internal.JreHiDpiUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static consulo.ui.ex.awt.JBUI.ScaleType.*;

/**
 * @author Konstantin Bulenkov
 * @author tav
 */
@SuppressWarnings("UseJBColor")
public class JBUI {

  public static final String USER_SCALE_FACTOR_PROPERTY = JBUIScale.USER_SCALE_FACTOR_PROPERTY;

  /**
   * The IDE supports two different HiDPI modes:
   * <p/>
   * 1) IDE-managed HiDPI mode.
   * <p/>
   * Supported for backward compatibility until complete transition to the JRE-managed HiDPI mode happens.
   * In this mode there's a single coordinate space and the whole UI is scaled by the IDE guided by the
   * user scale factor ({@link #USR_SCALE}).
   * <p/>
   * 2) JRE-managed HiDPI mode.
   * <p/>
   * In this mode the JRE scales graphics prior to drawing it on the device. So, there're two coordinate
   * spaces: the user space and the device space. The system scale factor ({@link #SYS_SCALE}) defines the
   * transform b/w the spaces. The UI size metrics (windows, controls, fonts height) are in the user
   * coordinate space. Though, the raster images should be aware of the device scale in order to meet
   * HiDPI. (For instance, JRE on a Mac Retina monitor device works in the JRE-managed HiDPI mode,
   * transforming graphics to the double-scaled device coordinate space)
   * <p/>
   * The IDE operates the scale factors of the following types:
   * <p/>
   * 1) The user scale factor: {@link #USR_SCALE}
   * 2) The system (monitor device) scale factor: {@link #SYS_SCALE}
   * 3) The object (UI instance specific) scale factor: {@link #OBJ_SCALE}
   * 4) The pixel scale factor: {@link #PIX_SCALE}
   *
   * @see UIUtil#isJreHiDPIEnabled()
   * @see UIUtil#isJreHiDPI()
   * @see UIUtil#isJreHiDPI(GraphicsConfiguration)
   * @see UIUtil#isJreHiDPI(Graphics2D)
   * @see JBUI#isUsrHiDPI()
   * @see JBUI#isPixHiDPI(GraphicsConfiguration)
   * @see JBUI#isPixHiDPI(Graphics2D)
   * @see UIUtil#drawImage(Graphics, Image, Rectangle, Rectangle, ImageObserver)
   * @see UIUtil#createImage(Graphics, int, int, int)
   * @see UIUtil#createImage(GraphicsConfiguration, int, int, int)
   * @see UIUtil#createImage(int, int, int)
   * @see ScaleContext
   */
  public enum ScaleType {
    /**
     * The user scale factor is set and managed by the IDE. Currently it's derived from the UI font size,
     * specified in the IDE Settings.
     * <p/>
     * The user scale value depends on which HiDPI mode is enabled. In the IDE-managed HiDPI mode the
     * user scale "includes" the default system scale and simply equals it with the default UI font size.
     * In the JRE-managed HiDPI mode the user scale is independent of the system scale and equals 1.0
     * with the default UI font size. In case the default UI font size changes, the user scale changes
     * proportionally in both the HiDPI modes.
     * <p/>
     * In the IDE-managed HiDPI mode the user scale completely defines the UI scale. In the JRE-managed
     * HiDPI mode the user scale can be considered a supplementary scale taking effect in cases like
     * the IDE Presentation Mode and when the default UI scale is changed by the user.
     *
     * @see #setUserScaleFactor(float)
     * @see #scale(float)
     * @see #scale(int)
     */
    USR_SCALE,
    /**
     * The system scale factor is defined by the device DPI and/or the system settings. For instance,
     * Mac Retina monitor device has the system scale 2.0 by default. As there can be multiple devices
     * (multi-monitor configuration) there can be multiple system scale factors, appropriately. However,
     * there's always a single default system scale factor corresponding to the default device. And it's
     * the only system scale available in the IDE-managed HiDPI mode.
     * <p/>
     * In the JRE-managed HiDPI mode, the system scale defines the scale of the transform b/w the user
     * and the device coordinate spaces performed by the JRE.
     *
     * @see #sysScale()
     * @see #sysScale(GraphicsConfiguration)
     * @see #sysScale(Graphics2D)
     * @see #sysScale(Component)
     */
    SYS_SCALE,
    /**
     * An extra scale factor of a particular UI object, which doesn't affect any other UI object, as opposed
     * to the user scale and the system scale factors. Doesn't depend on the HiDPI mode and is 1.0 by default.
     */
    OBJ_SCALE,
    /**
     * The pixel scale factor "combines" all the other scale factors (user, system and object) and defines the
     * effective scale of a particular UI object.
     * <p/>
     * For instance, on Mac Retina monitor (JRE-managed HiDPI) in the Presentation mode (which, say,
     * doubles the UI scale) the pixel scale would equal 4.0 (provided the object scale is 1.0). The value
     * is the product of the user scale 2.0 and the system scale 2.0. In the IDE-managed HiDPI mode,
     * the pixel scale is the product of the user scale and the object scale.
     *
     * @see #pixScale()
     * @see #pixScale(GraphicsConfiguration)
     * @see #pixScale(Graphics2D)
     * @see #pixScale(Component)
     * @see #pixScale(GraphicsConfiguration, float)
     * @see #pixScale(float)
     */
    PIX_SCALE;

    @Nonnull
    public Scale of(double value) {
      return Scale.create(value, this);
    }}

  /**
   * A scale factor of a particular type.
   */
  public static class Scale {
    private final double value;
    private final ScaleType type;

    // The cache radically reduces potentially thousands of equal Scale instances.
    private static final ThreadLocal<EnumMap<ScaleType, Map<Double, Scale>>> cache = ThreadLocal.withInitial(() -> new EnumMap<>(ScaleType.class));

    @Nonnull
    public static Scale create(double value, @Nonnull ScaleType type) {
      EnumMap<ScaleType, Map<Double, Scale>> emap = cache.get();
      Map<Double, Scale> map = emap.get(type);
      if (map == null) {
        emap.put(type, map = new HashMap<>());
      }
      Scale scale = map.get(value);
      if (scale != null) return scale;
      map.put(value, scale = new Scale(value, type));
      return scale;
    }

    private Scale(double value, @Nonnull ScaleType type) {
      this.value = value;
      this.type = type;
    }

    public double value() {
      return value;
    }

    @Nonnull
    public ScaleType type() {
      return type;
    }

    @Nonnull
    Scale newOrThis(double value) {
      if (this.value == value) return this;
      return type.of(value);
    }

    @Override
    public String toString() {
      return "[" + type.name() + " " + value + "]";
    }
  }

  /**
   * Adds property change listener. Supported properties:
   * {@link #USER_SCALE_FACTOR_PROPERTY}
   */
  public static void addPropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener) {
    JBUIScale.addPropertyChangeListener(propertyName, listener);
  }

  /**
   * Removes property change listener
   */
  public static void removePropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener) {
    JBUIScale.removePropertyChangeListener(propertyName, listener);
  }

  /**
   * Returns the system scale factor, corresponding to the default monitor device.
   */
  public static float sysScale() {
    return JBUIScale.sysScale();
  }

  /**
   * Returns the system scale factor, corresponding to the graphics configuration.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable GraphicsConfiguration gc) {
    return JBUIScale.sysScale(gc);
  }

  /**
   * Returns the system scale factor, corresponding to the graphics.
   * For BufferedImage's graphics, the scale is taken from the graphics itself.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable Graphics2D g) {
    return JBUIScale.sysScale(g);
  }

  /**
   * Returns the system scale factor, corresponding to the device the component is tied to.
   * In the IDE-managed HiDPI mode defaults to {@link #sysScale()}
   */
  public static float sysScale(@Nullable Component comp) {
    if (comp != null) {
      return sysScale(comp.getGraphicsConfiguration());
    }
    return sysScale();
  }

  public static double sysScale(@Nullable ScaleContext ctx) {
    if (ctx != null) {
      return ctx.getScale(SYS_SCALE);
    }
    return sysScale();
  }

  /**
   * Returns the pixel scale factor, corresponding to the default monitor device.
   */
  public static float pixScale() {
    return JreHiDpiUtil.isJreHiDPIEnabled() ? sysScale() * scale(1f) : scale(1f);
  }

  /**
   * Returns "f" scaled by pixScale().
   */
  public static float pixScale(float f) {
    return pixScale() * f;
  }

  /**
   * Returns "f" scaled by pixScale(gc).
   */
  public static float pixScale(@Nullable GraphicsConfiguration gc, float f) {
    return pixScale(gc) * f;
  }

  /**
   * Returns the pixel scale factor, corresponding to the provided configuration.
   * In the IDE-managed HiDPI mode defaults to {@link #pixScale()}
   */
  public static float pixScale(@Nullable GraphicsConfiguration gc) {
    return JreHiDpiUtil.isJreHiDPIEnabled() ? sysScale(gc) * scale(1f) : scale(1f);
  }

  /**
   * Returns the pixel scale factor, corresponding to the provided graphics.
   * In the IDE-managed HiDPI mode defaults to {@link #pixScale()}
   */
  public static float pixScale(@Nullable Graphics2D g) {
    return JreHiDpiUtil.isJreHiDPIEnabled() ? sysScale(g) * scale(1f) : scale(1f);
  }

  /**
   * Returns the pixel scale factor, corresponding to the device the provided component is tied to.
   * In the IDE-managed HiDPI mode defaults to {@link #pixScale()}
   */
  public static float pixScale(@Nullable Component comp) {
    return pixScale(comp != null ? comp.getGraphicsConfiguration() : null);
  }

  public static <T extends BaseScaleContext> double pixScale(@Nullable T ctx) {
    if (ctx != null) {
      double usrScale = ctx.getScale(USR_SCALE);
      return JreHiDpiUtil.isJreHiDPIEnabled() ? ctx.getScale(SYS_SCALE) * usrScale : usrScale;
    }
    return pixScale();
  }

  public static float setUserScaleFactor(float scale) {
    return JBUIScale.setUserScaleFactor(scale);
  }

  /**
   * @return 'f' scaled by the user scale factor
   */
  public static float scale(float f) {
    return JBUIScale.scale(f);
  }

  /**
   * @return 'i' scaled by the user scale factor
   */
  public static int scale(int i) {
    return JBUIScale.scale(i);
  }

  @Nonnull
  public static Font scale(@Nonnull Font font) {
    return font.deriveFont((float)scaleFontSize(font.getSize()));
  }

  public static int scaleFontSize(float fontSize) {
    return JBUIScale.scaleFontSize(fontSize);
  }

  /**
   * @return the scale factor of {@code fontSize} relative to the standard font size (currently 12pt)
   */
  public static float getFontScale(float fontSize) {
    return JBUIScale.getFontScale(fontSize);
  }

  @Nonnull
  public static JBValue value(float value) {
    return new JBValue.Float(value);
  }

  @Nonnull
  public static JBValue uiIntValue(@Nonnull String key, int defValue) {
    return new JBValue.UIInteger(key, defValue);
  }

  public static JBDimension size(int width, int height) {
    return new JBDimension(width, height);
  }

  @Nonnull
  public static JBDimension size(int widthAndHeight) {
    return new JBDimension(widthAndHeight, widthAndHeight);
  }

  @Nonnull
  public static JBDimension size(Dimension size) {
    if (size instanceof JBDimension) {
      JBDimension newSize = ((JBDimension)size).newSize();
      return size instanceof UIResource ? newSize.asUIResource() : newSize;
    }
    return new JBDimension(size.width, size.height);
  }

  @Nonnull
  public static JBInsets insets(int top, int left, int bottom, int right) {
    return new JBInsets(top, left, bottom, right);
  }

  @Nonnull
  public static JBInsets insets(int all) {
    return insets(all, all, all, all);
  }

  @Nonnull
  public static JBInsets insets(String propName, JBInsets defaultValue) {
    Insets i = UIManager.getInsets(propName);
    return i != null ? JBInsets.create(i) : defaultValue;
  }

  @Nonnull
  public static JBInsets insets(int topBottom, int leftRight) {
    return insets(topBottom, leftRight, topBottom, leftRight);
  }

  @Nonnull
  public static JBInsets emptyInsets() {
    return new JBInsets(0, 0, 0, 0);
  }

  @Nonnull
  public static JBInsets insetsTop(int t) {
    return insets(t, 0, 0, 0);
  }

  @Nonnull
  public static JBInsets insetsLeft(int l) {
    return insets(0, l, 0, 0);
  }

  @Nonnull
  public static JBInsets insetsBottom(int b) {
    return insets(0, 0, b, 0);
  }

  @Nonnull
  public static JBInsets insetsRight(int r) {
    return insets(0, 0, 0, r);
  }

  /**
   * @deprecated use JBUI.scale(EmptyIcon.create(size)) instead
   */
  @Nonnull
  public static EmptyIcon emptyIcon(int size) {
    return scale(EmptyIcon.create(size));
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T extends JBIcon> T scale(@Nonnull T icon) {
    return (T)icon.withIconPreScaled(false);
  }

  @Nonnull
  public static JBDimension emptySize() {
    return new JBDimension(0, 0);
  }

  @Nonnull
  public static JBInsets insets(@Nonnull Insets insets) {
    return JBInsets.create(insets);
  }

  /**
   * @deprecated use {@link #isUsrHiDPI()} instead
   */
  @Deprecated
  public static boolean isHiDPI() {
    return isUsrHiDPI();
  }

  /**
   * Returns whether the {@link ScaleType#USR_SCALE} scale factor assumes HiDPI-awareness.
   * An equivalent of {@code isHiDPI(scale(1f))}
   */
  public static boolean isUsrHiDPI() {
    return isHiDPI(scale(1f));
  }

  /**
   * Returns whether the {@link ScaleType#PIX_SCALE} scale factor assumes HiDPI-awareness in the provided graphics config.
   * An equivalent of {@code isHiDPI(pixScale(gc))}
   */
  public static boolean isPixHiDPI(@Nullable GraphicsConfiguration gc) {
    return isHiDPI(pixScale(gc));
  }

  /**
   * Returns whether the {@link ScaleType#PIX_SCALE} scale factor assumes HiDPI-awareness in the provided graphics.
   * An equivalent of {@code isHiDPI(pixScale(g))}
   */
  public static boolean isPixHiDPI(@Nullable Graphics2D g) {
    return isHiDPI(pixScale(g));
  }

  /**
   * Returns whether the {@link ScaleType#PIX_SCALE} scale factor assumes HiDPI-awareness in the provided component's device.
   * An equivalent of {@code isHiDPI(pixScale(comp))}
   */
  public static boolean isPixHiDPI(@Nullable Component comp) {
    return isHiDPI(pixScale(comp));
  }

  /**
   * Returns whether the provided scale assumes HiDPI-awareness.
   */
  public static boolean isHiDPI(double scale) {
    return JBUIScale.isHiDPI(scale);
  }

  public static class Fonts {
    @Nonnull
    public static JBFont label() {
      return JBFont.create(UIManager.getFont("Label.font"), false);
    }

    @Nonnull
    public static JBFont label(float size) {
      return label().deriveFont(scale(size));
    }

    @Nonnull
    public static JBFont biggerFont() {
      return label().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.BIGGER));
    }

    @Nonnull
    public static JBFont smallFont() {
      return label().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.SMALL));
    }

    @Nonnull
    public static JBFont miniFont() {
      return label().deriveFont(UIUtil.getFontSize(UIUtil.FontSize.MINI));
    }

    @Nonnull
    public static JBFont create(String fontFamily, int size) {
      return JBFont.create(new Font(fontFamily, Font.PLAIN, size));
    }

    @Nonnull
    public static JBFont toolbarSmallComboBoxFont() {
      return label(11);
    }

    @Nonnull
    public static JBFont toolbarFont() {
      return Platform.current().os().isMac() ? smallFont() : label();
    }
  }

  private static final JBEmptyBorder SHARED_EMPTY_INSTANCE = new JBEmptyBorder(0);

  @SuppressWarnings("UseDPIAwareBorders")
  public static class Borders {
    public static JBEmptyBorder empty(int top, int left, int bottom, int right) {
      if (top == 0 && left == 0 && bottom == 0 && right == 0) {
        return SHARED_EMPTY_INSTANCE;
      }
      return new JBEmptyBorder(top, left, bottom, right);
    }

    @Nonnull
    public static JBEmptyBorder empty(int topAndBottom, int leftAndRight) {
      return empty(topAndBottom, leftAndRight, topAndBottom, leftAndRight);
    }

    @Nonnull
    public static JBEmptyBorder emptyTop(int offset) {
      return empty(offset, 0, 0, 0);
    }

    @Nonnull
    public static JBEmptyBorder emptyLeft(int offset) {
      return empty(0, offset, 0, 0);
    }

    @Nonnull
    public static JBEmptyBorder emptyBottom(int offset) {
      return empty(0, 0, offset, 0);
    }

    @Nonnull
    public static JBEmptyBorder emptyRight(int offset) {
      return empty(0, 0, 0, offset);
    }

    @Nonnull
    public static JBEmptyBorder empty() {
      return empty(0, 0, 0, 0);
    }

    @Nonnull
    public static JBEmptyBorder empty(@Nonnull Insets insets) {
      return empty(insets.top, insets.left, insets.bottom, insets.right);
    }

    @Nonnull
    public static Border empty(int offsets) {
      return empty(offsets, offsets, offsets, offsets);
    }

    @Nonnull
    public static Border customLine(Color color, int top, int left, int bottom, int right) {
      return new CustomLineBorder(color, insets(top, left, bottom, right));
    }

    @Nonnull
    public static Border customLine(Color color, int thickness) {
      return customLine(color, thickness, thickness, thickness, thickness);
    }

    @Nonnull
    public static Border customLine(Color color) {
      return customLine(color, 1);
    }

    @Nonnull
    public static Border customLineRight(Color color) {
      return customLine(color, 0, 0, 0, 1);
    }

    @Nonnull
    public static Border customLineLeft(Color color) {
      return customLine(color, 0, 1, 0, 0);
    }

    @Nonnull
    public static Border merge(@Nullable Border source, @Nonnull Border extra, boolean extraIsOutside) {
      if (source == null) return extra;
      return new CompoundBorder(extraIsOutside ? extra : source, extraIsOutside ? source : extra);
    }
  }

  public static class Panels {
    @Nonnull
    public static VerticalLayoutPanel verticalPanel() {
      return new VerticalLayoutPanel();
    }

    @Nonnull
    public static VerticalLayoutPanel verticalPanel(int hgap, int vgap) {
      return new VerticalLayoutPanel(hgap, vgap);
    }

    @Nonnull
    public static BorderLayoutPanel simplePanel() {
      return new BorderLayoutPanel();
    }

    @Nonnull
    public static BorderLayoutPanel simplePanel(Component comp) {
      return simplePanel().addToCenter(comp);
    }

    @Nonnull
    public static BorderLayoutPanel simplePanel(int hgap, int vgap) {
      return new BorderLayoutPanel(hgap, vgap);
    }
  }

  /**
   * A wrapper over a user scale supplier, representing a state of a UI element
   * in which its initial size is either pre-scaled (according to {@link #currentScale()})
   * or not (given in a standard resolution, e.g. 16x16 for an icon).
   */
  public abstract static class Scaler {
    protected double initialScale = currentScale();

    private double alignedScale() {
      return currentScale() / initialScale;
    }

    protected boolean isPreScaled() {
      return initialScale != 1d;
    }

    protected void setPreScaled(boolean preScaled) {
      initialScale = preScaled ? currentScale() : 1d;
    }

    /**
     * @param value the value (e.g. a size of the associated UI object) to scale
     * @return the scaled result, taking into account the pre-scaled state and {@link #currentScale()}
     */
    public double scaleVal(double value) {
      return value * alignedScale();
    }

    /**
     * Supplies the Scaler with the current user scale. This can be the current global user scale or
     * the context scale ({@link BaseScaleContext#usrScale}) or something else.
     */
    protected abstract double currentScale();

    /**
     * Synchronizes the state with the provided scaler.
     *
     * @return whether the state has been updated
     */
    public boolean update(@Nonnull Scaler scaler) {
      boolean updated = initialScale != scaler.initialScale;
      initialScale = scaler.initialScale;
      return updated;
    }
  }

  /**
   * Represents a snapshot of the scale factors (see {@link ScaleType}), except the system scale.
   * The context can be associated with a UI object (see {@link ScaleContextAware}) to define its HiDPI behaviour.
   * Unlike {@link ScaleContext}, BaseScaleContext is system scale independent and is thus used for vector-based painting.
   *
   * @author tav
   * @see ScaleContextAware
   * @see ScaleContext
   */
  public static class BaseScaleContext {
    protected Scale usrScale = USR_SCALE.of(scale(1f));
    protected Scale objScale = OBJ_SCALE.of(1d);
    protected Scale pixScale = PIX_SCALE.of(usrScale.value);

    private List<UpdateListener> listeners;

    private BaseScaleContext() {
    }

    /**
     * Creates a context with all scale factors set to 1.
     */
    public static BaseScaleContext createIdentity() {
      return create(USR_SCALE.of(1));
    }

    /**
     * Creates a context with the provided scale factors (system scale is ignored)
     */
    @Nonnull
    public static BaseScaleContext create(@Nonnull Scale... scales) {
      BaseScaleContext ctx = create();
      for (Scale s : scales) ctx.update(s);
      return ctx;
    }

    /**
     * Creates a default context with the current user scale
     */
    @Nonnull
    public static BaseScaleContext create() {
      return new BaseScaleContext();
    }

    protected double derivePixScale() {
      return usrScale.value * objScale.value;
    }

    /**
     * @return the context scale factor of the provided type (1d for system scale)
     */
    public double getScale(@Nonnull ScaleType type) {
      switch (type) {
        case USR_SCALE:
          return usrScale.value;
        case SYS_SCALE:
          return 1d;
        case OBJ_SCALE:
          return objScale.value;
        case PIX_SCALE:
          return pixScale.value;
      }
      return 1f; // unreachable
    }

    protected boolean onUpdated(boolean updated) {
      if (updated) {
        update(pixScale, derivePixScale());
        notifyUpdateListeners();
      }
      return updated;
    }

    /**
     * Updates the user scale with the current global user scale if necessary.
     *
     * @return whether any of the scale factors has been updated
     */
    public boolean update() {
      return onUpdated(update(usrScale, scale(1f)));
    }

    /**
     * Updates the provided scale if necessary (system scale is ignored)
     *
     * @param scale the new scale
     * @return whether the scale factor has been updated
     */
    public boolean update(@Nonnull Scale scale) {
      boolean updated = false;
      switch (scale.type) {
        case USR_SCALE:
          updated = update(usrScale, scale.value);
          break;
        case SYS_SCALE:
          break;
        case OBJ_SCALE:
          updated = update(objScale, scale.value);
          break;
        case PIX_SCALE:
          break;
      }
      return onUpdated(updated);
    }

    /**
     * Updates the context with the state of the provided one.
     *
     * @param ctx the new context
     * @return whether any of the scale factors has been updated
     */
    public boolean update(@Nullable BaseScaleContext ctx) {
      if (ctx == null) return update();
      return onUpdated(updateAll(ctx));
    }

    protected <T extends BaseScaleContext> boolean updateAll(@Nonnull T ctx) {
      boolean updated = update(usrScale, ctx.usrScale.value);
      return update(objScale, ctx.objScale.value) || updated;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (!(obj instanceof BaseScaleContext)) return false;

      BaseScaleContext that = (BaseScaleContext)obj;
      return that.usrScale.value == usrScale.value && that.objScale.value == objScale.value;
    }

    /**
     * Clears the links.
     */
    public void dispose() {
      listeners = null;
    }

    /**
     * A context update listener. Used to listen to possible external context updates.
     */
    public interface UpdateListener {
      void contextUpdated();
    }

    public void addUpdateListener(@Nonnull UpdateListener l) {
      if (listeners == null) listeners = new ArrayList<UpdateListener>(1);
      listeners.add(l);
    }

    public void removeUpdateListener(@Nonnull UpdateListener l) {
      if (listeners != null) listeners.remove(l);
    }

    protected void notifyUpdateListeners() {
      if (listeners == null) return;
      for (UpdateListener l : listeners) {
        l.contextUpdated();
      }
    }

    protected boolean update(@Nonnull Scale scale, double value) {
      Scale newScale = scale.newOrThis(value);
      if (newScale == scale) return false;
      switch (scale.type) {
        case USR_SCALE:
          usrScale = newScale;
          break;
        case OBJ_SCALE:
          objScale = newScale;
          break;
        case PIX_SCALE:
          pixScale = newScale;
          break;
      }
      return true;
    }

    @Nonnull
    public <T extends BaseScaleContext> T copy() {
      BaseScaleContext ctx = createIdentity();
      ctx.updateAll(this);
      //noinspection unchecked
      return (T)ctx;
    }

    @Override
    public String toString() {
      return usrScale + ", " + objScale + ", " + pixScale;
    }

    /**
     * A cache for the last usage of a data object matching a scale context.
     *
     * @param <D> the data type
     * @param <S> the context type
     */
    public static class Cache<D, S extends BaseScaleContext> {
      private final Function<S, D> myDataProvider;
      private final AtomicReference<Pair<Double, D>> myData = new AtomicReference<Pair<Double, D>>(null);

      /**
       * @param dataProvider provides a data object matching the passed scale context
       */
      public Cache(@Nonnull Function<S, D> dataProvider) {
        this.myDataProvider = dataProvider;
      }

      /**
       * Retunrs the data object from the cache if it matches the {@code ctx},
       * otherwise provides the new data via the provider and caches it.
       */
      @Nullable
      public D getOrProvide(@Nonnull S ctx) {
        Pair<Double, D> data = myData.get();
        double scale = ctx.getScale(PIX_SCALE);
        if (data == null || Double.compare(scale, data.first) != 0) {
          myData.set(data = Pair.create(scale, myDataProvider.apply(ctx)));
        }
        return data.second;
      }

      /**
       * Clears the cache.
       */
      public void clear() {
        myData.set(null);
      }
    }
  }

  /**
   * Extends {@link BaseScaleContext} with the system scale, and is thus used for raster-based painting.
   * The context is created via a context provider. If the provider is {@link Component}, the context's
   * system scale can be updated via a call to {@link #update()}, reflecting the current component's
   * system scale (which may change as the component moves b/w devices).
   *
   * @author tav
   * @see ScaleContextAware
   */
  public static class ScaleContext extends BaseScaleContext {
    protected Scale sysScale = SYS_SCALE.of(sysScale());

    @Nullable
    private WeakReference<Component> compRef;

    private ScaleContext() {
      update(pixScale, derivePixScale());
    }

    private ScaleContext(@Nonnull Scale scale) {
      switch (scale.type) {
        case USR_SCALE:
          update(usrScale, scale.value);
          break;
        case SYS_SCALE:
          update(sysScale, scale.value);
          break;
        case OBJ_SCALE:
          update(objScale, scale.value);
          break;
        case PIX_SCALE:
          break;
      }
      update(pixScale, derivePixScale());
    }

    /**
     * Creates a context with all scale factors set to 1.
     */
    @Nonnull
    public static ScaleContext createIdentity() {
      return create(USR_SCALE.of(1), SYS_SCALE.of(1));
    }

    /**
     * Creates a context based on the comp's system scale and sticks to it via the {@link #update()} method.
     */
    @Nonnull
    public static ScaleContext create(@Nullable Component comp) {
      final ScaleContext ctx = new ScaleContext(SYS_SCALE.of(sysScale(comp)));
      if (comp != null) ctx.compRef = new WeakReference<Component>(comp);
      return ctx;
    }

    /**
     * Creates a context based on the component's (or graphics's) scale and sticks to it via the {@link #update()} method.
     */
    @Nonnull
    public static ScaleContext create(@Nullable Component component, @Nullable Graphics2D graphics) {
      // Component is preferable to Graphics as a scale provider, as it lets the context stick
      // to the comp's actual scale via the update method.
      if (component != null) {
        GraphicsConfiguration gc = component.getGraphicsConfiguration();
        if (gc == null || gc.getDevice().getType() == GraphicsDevice.TYPE_IMAGE_BUFFER || gc.getDevice().getType() == GraphicsDevice.TYPE_PRINTER) {
          // can't rely on gc in this case as it may provide incorrect transform or scale
          component = null;
        }
      }
      if (component != null) {
        return create(component);
      }
      return create(graphics);
    }

    /**
     * Creates a context based on the gc's system scale
     */
    @Nonnull
    public static ScaleContext create(@Nullable GraphicsConfiguration gc) {
      return new ScaleContext(SYS_SCALE.of(sysScale(gc)));
    }

    /**
     * Creates a context based on the g's system scale
     */
    @Nonnull
    public static ScaleContext create(Graphics2D g) {
      return new ScaleContext(SYS_SCALE.of(sysScale(g)));
    }

    /**
     * Creates a context with the provided scale
     */
    @Nonnull
    public static ScaleContext create(@Nonnull Scale scale) {
      return new ScaleContext(scale);
    }

    /**
     * Creates a context with the provided scale factors
     */
    @Nonnull
    public static ScaleContext create(@Nonnull Scale... scales) {
      ScaleContext ctx = create();
      for (Scale s : scales) ctx.update(s);
      return ctx;
    }

    /**
     * Creates a default context with the default screen scale and the current user scale
     */
    @Nonnull
    public static ScaleContext create() {
      return new ScaleContext();
    }

    @Override
    protected double derivePixScale() {
      return JreHiDpiUtil.isJreHiDPIEnabled() ? sysScale.value * super.derivePixScale() : super.derivePixScale();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getScale(@Nonnull ScaleType type) {
      if (type == SYS_SCALE) return sysScale.value;
      return super.getScale(type);
    }

    /**
     * {@inheritDoc}
     * Also updates the system scale (if the context was created from Component) if necessary.
     */
    @Override
    public boolean update() {
      boolean updated = update(usrScale, scale(1f));
      if (compRef != null) {
        Component comp = compRef.get();
        if (comp != null) updated = update(sysScale, sysScale(comp)) || updated;
      }
      return onUpdated(updated);
    }

    /**
     * {@inheritDoc}
     * Also includes the system scale.
     */
    @Override
    public boolean update(@Nonnull Scale scale) {
      if (scale.type == SYS_SCALE) return onUpdated(update(sysScale, scale.value));
      return super.update(scale);
    }

    @Override
    protected <T extends BaseScaleContext> boolean updateAll(@Nonnull T ctx) {
      boolean updated = super.updateAll(ctx);
      if (!(ctx instanceof ScaleContext)) return updated;
      ScaleContext context = (ScaleContext)ctx;

      if (compRef != null) compRef.clear();
      compRef = context.compRef;

      return update(sysScale, context.sysScale.value) || updated;
    }

    @Override
    protected boolean update(@Nonnull Scale scale, double value) {
      if (scale.type == SYS_SCALE) {
        Scale newScale = scale.newOrThis(value);
        if (newScale == scale) return false;
        sysScale = newScale;
        return true;
      }
      return super.update(scale, value);
    }

    @Override
    public boolean equals(Object obj) {
      if (super.equals(obj) && obj instanceof ScaleContext) {
        ScaleContext that = (ScaleContext)obj;
        return that.sysScale.value == sysScale.value;
      }
      return false;
    }

    @Override
    public void dispose() {
      super.dispose();
      if (compRef != null) {
        compRef.clear();
      }
    }

    @Nonnull
    @Override
    public <T extends BaseScaleContext> T copy() {
      ScaleContext ctx = createIdentity();
      ctx.updateAll(this);
      //noinspection unchecked
      return (T)ctx;
    }

    @Override
    public String toString() {
      return usrScale + ", " + sysScale + ", " + objScale + ", " + pixScale;
    }

    public static class Cache<D> extends BaseScaleContext.Cache<D, ScaleContext> {
      public Cache(@Nonnull Function<ScaleContext, D> dataProvider) {
        super(dataProvider);
      }
    }
  }

  /**
   * Provides ScaleContext awareness of a UI object.
   *
   * @author tav
   * @see ScaleContextSupport
   */
  public interface ScaleContextAware<T extends BaseScaleContext> {
    /**
     * @return the scale context
     */
    @Nonnull
    T getScaleContext();

    /**
     * Updates the current context with the state of the provided context.
     * If {@code ctx} is null, then updates the current context via {@link ScaleContext#update()}
     * and returns the result.
     *
     * @param ctx the new scale context
     * @return whether any of the scale factors has been updated
     */
    boolean updateScaleContext(@Nullable T ctx);

    /**
     * @return the scale of the provided type from the context
     */
    double getScale(@Nonnull ScaleType type);

    /**
     * Updates the provided scale in the context
     *
     * @return whether the provided scale has been changed
     */
    boolean updateScale(@Nonnull Scale scale);
  }

  public static class ScaleContextSupport<T extends BaseScaleContext> implements ScaleContextAware<T> {
    @Nonnull
    private final T myScaleContext;

    public ScaleContextSupport(@Nonnull T ctx) {
      myScaleContext = ctx;
    }

    @Nonnull
    @Override
    public T getScaleContext() {
      return myScaleContext;
    }

    @Override
    public boolean updateScaleContext(@Nullable T ctx) {
      return myScaleContext.update(ctx);
    }

    @Override
    public double getScale(@Nonnull ScaleType type) {
      return getScaleContext().getScale(type);
    }

    @Override
    public boolean updateScale(@Nonnull Scale scale) {
      return getScaleContext().update(scale);
    }
  }

  /**
   * A {@link BaseScaleContext} aware Icon, assuming vector-based painting, system scale independent.
   *
   * @author tav
   */
  public abstract static class JBIcon extends ScaleContextSupport<BaseScaleContext> implements Icon {
    private final Scaler myScaler = new Scaler() {
      @Override
      protected double currentScale() {
        if (autoUpdateScaleContext) getScaleContext().update();
        return getScale(USR_SCALE);
      }
    };
    private boolean autoUpdateScaleContext = true;

    protected JBIcon() {
      super(BaseScaleContext.create());
    }

    protected JBIcon(@Nonnull JBIcon icon) {
      this();
      updateScaleContext(icon.getScaleContext());
      myScaler.update(icon.myScaler);
      autoUpdateScaleContext = icon.autoUpdateScaleContext;
    }

    protected boolean isIconPreScaled() {
      return myScaler.isPreScaled();
    }

    protected void setIconPreScaled(boolean preScaled) {
      myScaler.setPreScaled(preScaled);
    }

    /**
     * The pre-scaled state of the icon indicates whether the initial size of the icon
     * is pre-scaled (by the global user scale) or not. If the size is not pre-scaled,
     * then there're two approaches to deal with it:
     * 1) scale its initial size right away and store;
     * 2) scale its initial size every time it's requested.
     * The 2nd approach is preferable because of the the following. Scaling of the icon may
     * involve not only USR_SCALE but OBJ_SCALE as well. In which case applying all the scale
     * factors and then rounding (the size is integer, the scale factors are not) gives more
     * accurate result than rounding and then scaling.
     * <p/>
     * For example, say we have an icon of 15x15 initial size, USR_SCALE is 1.5f, OBJ_SCALE is 1,5f.
     * Math.round(Math.round(15 * USR_SCALE) * OBJ_SCALE) = 35
     * Math.round(15 * USR_SCALE * OBJ_SCALE) = 34
     * <p/>
     * Thus, JBUI.scale(MyIcon.create(w, h)) is preferable to MyIcon.create(JBUI.scale(w), JBUI.scale(h)).
     * Here [w, h] is "raw" unscaled size.
     *
     * @param preScaled whether the icon is pre-scaled
     * @return the icon in the provided pre-scaled state
     * @see JBUI#scale(JBIcon)
     */
    @Nonnull
    public JBIcon withIconPreScaled(boolean preScaled) {
      setIconPreScaled(preScaled);
      return this;
    }

    /**
     * See {@link Scaler#scaleVal(double)}
     */
    protected double scaleVal(double value) {
      return myScaler.scaleVal(value);
    }

    /**
     * Sets whether the scale context should be auto-updated by the {@link Scaler}.
     * This ensures that {@link #scaleVal(double)} always uses up-to-date scale.
     * This is useful when the icon doesn't need to recalculate its internal sizes
     * on the scale context update and so it doesn't need the result of the update
     * and/or it doesn't listen for updates. Otherwise, the value should be set to
     * false and the scale context should be updated manually.
     * <p/>
     * By default the value is true.
     */
    protected void setAutoUpdateScaleContext(boolean autoUpdate) {
      autoUpdateScaleContext = autoUpdate;
    }

    @Override
    public String toString() {
      return getClass().getName() + " " + getIconWidth() + "x" + getIconHeight();
    }
  }

  /**
   * A {@link JBIcon} implementing {@link ScalableIcon}
   *
   * @author tav
   */
  public abstract static class ScalableJBIcon extends JBIcon implements ScalableIcon {
    protected ScalableJBIcon() {
    }

    protected ScalableJBIcon(@Nonnull ScalableJBIcon icon) {
      super(icon);
    }

    @Override
    public float getScale() {
      return (float)getScale(OBJ_SCALE); // todo: float -> double
    }

    @Override
    @Nonnull
    public Icon scale(float scale) {
      updateScale(OBJ_SCALE.of(scale));
      return this;
    }

    /**
     * An equivalent of scaleVal(value, PIX_SCALE)
     */
    @Override
    protected double scaleVal(double value) {
      return scaleVal(value, PIX_SCALE);
    }

    /**
     * Updates the context and scales the provided value according to the provided type
     */
    protected double scaleVal(double value, @Nonnull ScaleType type) {
      switch (type) {
        case USR_SCALE:
          return super.scaleVal(value);
        case SYS_SCALE:
          return value * getScale(SYS_SCALE);
        case OBJ_SCALE:
          return value * getScale(OBJ_SCALE);
        case PIX_SCALE:
          return super.scaleVal(value * getScale(OBJ_SCALE));
      }
      return value; // unreachable
    }
  }

  /**
   * A {@link ScalableJBIcon} providing an immutable caching implementation of the {@link ScalableIcon#scale(float)} method.
   *
   * @author tav
   * @author Aleksey Pivovarov
   */
  public abstract static class CachingScalableJBIcon<T extends CachingScalableJBIcon> extends ScalableJBIcon {
    private CachingScalableJBIcon myScaledIconCache;

    protected CachingScalableJBIcon() {
    }

    protected CachingScalableJBIcon(@Nonnull CachingScalableJBIcon icon) {
      super(icon);
    }

    /**
     * @return a new scaled copy of this icon, or the cached instance of the provided scale
     */
    @Override
    @Nonnull
    public Icon scale(float scale) {
      if (scale == getScale()) return this;

      if (myScaledIconCache == null || myScaledIconCache.getScale() != scale) {
        myScaledIconCache = copy();
        myScaledIconCache.updateScale(OBJ_SCALE.of(scale));
      }
      return myScaledIconCache;
    }

    /**
     * @return a copy of this icon instance
     */
    @Nonnull
    protected abstract T copy();
  }

  /**
   * A {@link ScaleContext} aware Icon, assuming raster-based painting, system scale dependant.
   *
   * @author tav
   */
  public abstract static class RasterJBIcon extends ScaleContextSupport<ScaleContext> implements Icon {
    public RasterJBIcon() {
      super(ScaleContext.create());
    }
  }

  public static int getInt(@Nonnull String propertyName, int defaultValue) {
    Object value = UIManager.get(propertyName);
    return value instanceof Integer ? (Integer)value : defaultValue;
  }

  public static float getFloat(String propertyName, float defaultValue) {
    Object value = UIManager.get(propertyName);
    if (value instanceof Float) return (Float)value;
    if (value instanceof Double) return ((Double)value).floatValue();
    if (value instanceof String) {
      try {
        return Float.parseFloat((String)value);
      }
      catch (NumberFormatException ignore) {
      }
    }
    return defaultValue;
  }

  @Nonnull
  public static Icon getIcon(@Nonnull String propertyName, @Nonnull Icon defaultIcon) {
    Icon icon = UIManager.getIcon(propertyName);
    return icon == null ? defaultIcon : icon;
  }

  @Nonnull
  public static Border getBorder(@Nonnull String propertyName, @Nonnull Border defaultBorder) {
    Border border = UIManager.getBorder(propertyName);
    return border == null ? defaultBorder : border;
  }
}