// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.application.AllIcons;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.awt.AlphaComposite.SrcAtop;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Sergey.Malenkov
 */
public class AnimatedIcon implements Icon, Image {
  /**
   * This key is used to allow animated icons in lists, tables and trees.
   * If the corresponding client property is set to {@code true} the corresponding component
   * will be automatically repainted to update an animated icon painted by the renderer of the component.
   * Note, that animation may cause a performance problems and should not be used everywhere.
   *
   * @see UIUtil#putClientProperty
   */
//  @ApiStatus.Internal
  public static final Key<Boolean> ANIMATION_IN_RENDERER_ALLOWED = Key.create("ANIMATION_IN_RENDERER_ALLOWED");

  public interface Frame {
    @Nonnull
    Icon getIcon();

    int getDelay();
  }

  public static class Default extends AnimatedIcon {

    public Default() {
      super(DELAY, ICONS.toArray(new Icon[0]));
    }

    public static final int DELAY = 130;
    public static final List<Image> ICONS =
            List.of(AllIcons.Process.Step_1, AllIcons.Process.Step_2, AllIcons.Process.Step_3, AllIcons.Process.Step_4, AllIcons.Process.Step_5, AllIcons.Process.Step_6, AllIcons.Process.Step_7,
                    AllIcons.Process.Step_8);

    public static final AnimatedIcon INSTANCE = new Default();
  }

  public static class Big extends AnimatedIcon {
    public Big() {
      super(DELAY, ICONS.toArray(new Icon[0]));
    }

    public static final int DELAY = 130;
    public static final List<Icon> ICONS =
            immutableList(AllIcons.Process.Big.Step_1, AllIcons.Process.Big.Step_2, AllIcons.Process.Big.Step_3, AllIcons.Process.Big.Step_4, AllIcons.Process.Big.Step_5, AllIcons.Process.Big.Step_6,
                          AllIcons.Process.Big.Step_7, AllIcons.Process.Big.Step_8);
  }

  public static class Recording extends AnimatedIcon {
    public Recording() {
      super(DELAY, ICONS.toArray(new Icon[0]));
    }

    public static final int DELAY = 250;
    public static final List<Icon> ICONS = immutableList(AllIcons.Ide.Macro.Recording_1, AllIcons.Ide.Macro.Recording_2, AllIcons.Ide.Macro.Recording_3, AllIcons.Ide.Macro.Recording_4);
  }

  private static List<Icon> immutableList(Image... images) {
      return List.copyOf(Arrays.stream(images).map(TargetAWT::to).collect(Collectors.toList()));
  }

  //@ApiStatus.Internal
  public static class Blinking extends AnimatedIcon {
    public Blinking(@Nonnull Image icon) {
      this(1000, icon);
    }

    public Blinking(int delay, @Nonnull Image icon) {
      super(delay, icon, ImageEffects.grayed(icon));
    }
  }

  //@ApiStatus.Internal
  public static class Fading extends AnimatedIcon {
    public Fading(@Nonnull Icon icon) {
      this(1000, icon);
    }

    public Fading(int period, @Nonnull Icon icon) {
      super(50, new Icon() {
        private final long time = System.currentTimeMillis();

        @Override
        public int getIconWidth() {
          return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
          return icon.getIconHeight();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
          assert period > 0 : "unexpected";
          long time = (System.currentTimeMillis() - this.time) % period;
          float alpha = (float)((Math.cos(2 * Math.PI * time / period) + 1) / 2);
          if (alpha > 0) {
            if (alpha < 1 && g instanceof Graphics2D) {
              Graphics2D g2d = (Graphics2D)g.create();
              try {
                g2d.setComposite(SrcAtop.derive(alpha));
                icon.paintIcon(c, g2d, x, y);
              }
              finally {
                g2d.dispose();
              }
            }
            else {
              icon.paintIcon(c, g, x, y);
            }
          }
        }
      });
    }
  }


  private final Frame[] frames;
  private final Set<Component> requested = Collections.newSetFromMap(new IdentityHashMap<>());
  private long time;
  private int index;

  public AnimatedIcon(int delay, @Nonnull Icon... icons) {
    this(getFrames(delay, icons));
  }

  public AnimatedIcon(int delay, @Nonnull Image... icons) {
    this(getFrames(delay, Arrays.stream(icons).map(TargetAWT::to).toArray(Icon[]::new)));
  }

  public AnimatedIcon(@Nonnull Frame... frames) {
    this.frames = frames;
    assert frames.length > 0 : "empty array";
    for (Frame frame : frames) assert frame != null : "null animation frame";
    time = System.currentTimeMillis();
  }

  private static Frame[] getFrames(int delay, @Nonnull Icon... icons) {
    int length = icons.length;
    assert length > 0 : "empty array";
    Frame[] frames = new Frame[length];
    for (int i = 0; i < length; i++) {
      Icon icon = icons[i];
      assert icon != null : "null icon";
      frames[i] = new Frame() {
        @Nonnull
        @Override
        public Icon getIcon() {
          return icon;
        }

        @Override
        public int getDelay() {
          return delay;
        }
      };
    }
    return frames;
  }

  private void updateFrameAt(long current) {
    int next = index + 1;
    index = next < frames.length ? next : 0;
    time = current;
  }

  @Nonnull
  private Icon getUpdatedIcon() {
    int index = getCurrentIndex();
    return frames[index].getIcon();
  }

  private int getCurrentIndex() {
    long current = System.currentTimeMillis();
    Frame frame = frames[index];
    if (frame.getDelay() <= (current - time)) updateFrameAt(current);
    return index;
  }

  private void requestRefresh(@Nullable Component c, @Nonnull UIAccess uiAccess) {
    if (c != null && !requested.contains(c) && canRefresh(c)) {
      Frame frame = frames[index];
      int delay = frame.getDelay();
      if (delay > 0) {
        requested.add(c);
        uiAccess.getScheduler().schedule(() -> {
          requested.remove(c);
          if (canRefresh(c)) {
            doRefresh(c);
          }
        }, delay, MILLISECONDS);
      }
      else {
        doRefresh(c);
      }
    }
  }

  @Override
  @RequiredUIAccess
  public final void paintIcon(Component c, Graphics g, int x, int y) {
    UIAccess uiAccess = UIAccess.current();

    Icon icon = getUpdatedIcon();
    CellRendererPane pane = ComponentUtil.getParentOfType(CellRendererPane.class, c);
    requestRefresh(pane == null ? c : getRendererOwner(pane.getParent()), uiAccess);
    icon.paintIcon(c, g, x, y);
  }

  @Override
  public final int getIconWidth() {
    return getUpdatedIcon().getIconWidth();
  }

  @Override
  public final int getIconHeight() {
    return getUpdatedIcon().getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  protected boolean canRefresh(@Nonnull Component component) {
    return component.isShowing();
  }

  protected void doRefresh(@Nonnull Component component) {
    component.repaint();
  }

  @Nullable
  protected Component getRendererOwner(@Nullable Component component) {
    return UIUtil.isClientPropertyTrue(component, ANIMATION_IN_RENDERER_ALLOWED) ? component : null;
  }
}
