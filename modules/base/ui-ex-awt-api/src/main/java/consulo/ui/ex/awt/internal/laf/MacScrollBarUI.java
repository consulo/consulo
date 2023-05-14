// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.internal.laf;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import consulo.application.util.mac.foundation.ID;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ComponentUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static consulo.application.util.mac.foundation.Foundation.*;

public final class MacScrollBarUI extends ConfigurableScrollBarUI {
  private static final UIElementWeakStorage<ConfigurableScrollBarUI> UI = new UIElementWeakStorage<>();
  private final Alarm myAlarm = new Alarm();
  private boolean myTrackHovered;

  public MacScrollBarUI() {
    super(12, 12, 12);
  }

  @Override
  protected void processReferences(ConfigurableScrollBarUI toAdd, ConfigurableScrollBarUI toRemove, List<? super ConfigurableScrollBarUI> list) {
    UI.processReferences(toAdd, toRemove, list);
  }

  @Override
  boolean isAbsolutePositioning(MouseEvent event) {
    return Behavior.JumpToSpot == Behavior.CURRENT.get();
  }

  @Override
  boolean isTrackClickable() {
    return isOpaque(myScrollBar) || (myTrack.animator.myValue > 0 && myThumb.animator.myValue > 0);
  }

  @Override
  boolean isTrackExpandable() {
    return !isOpaque(myScrollBar);
  }

  @Override
  void onTrackHover(boolean hover) {
    myTrackHovered = hover;
    if (myScrollBar != null && isOpaque(myScrollBar)) {
      myTrack.animator.start(hover);
      myThumb.animator.start(hover);
    }
    else if (hover) {
      myTrack.animator.start(true);
    }
    else {
      myThumb.animator.start(false);
    }
  }

  @Override
  void onThumbHover(boolean hover) {
  }

  @Override
  void paintTrack(Graphics2D g, JComponent c) {
    if (myTrack.animator.myValue > 0 && myThumb.animator.myValue > 0 || isOpaque(c)) super.paintTrack(g, c);
  }

  @Override
  void paintThumb(Graphics2D g, JComponent c) {
    if (isOpaque(c)) {
      paint(myThumb, g, c, true);
    }
    else if (myThumb.animator.myValue > 0) {
      paint(myThumb, g, c, false);
    }
  }

  @Override
  void onThumbMove() {
    if (myScrollBar != null && myScrollBar.isShowing() && !isOpaque(myScrollBar)) {
      if (!myTrackHovered && myThumb.animator.myValue == 0) myTrack.animator.rewind(false);
      myThumb.animator.rewind(true);
      myAlarm.cancelAllRequests();
      if (!myTrackHovered) {
        myAlarm.addRequest(() -> myThumb.animator.start(false), 700);
      }
    }
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    AWTEventListener listener = MOVEMENT_LISTENER.getAndSet(null); // add only one movement listener
    if (listener != null) Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
  }

  @Nonnull
  @Override
  public Style getCurrentStyle() {
    return CURRENT_STYLE.get();
  }

  @Override
  protected void cancelAllRequests() {
    myAlarm.cancelAllRequests();
  }

  /**
   * The movement listener that is intended to do not hide shown thumb while mouse is moving.
   */
  private static final AtomicReference<AWTEventListener> MOVEMENT_LISTENER = new AtomicReference<>(new AWTEventListener() {
    @Override
    public void eventDispatched(AWTEvent event) {
      if (event != null && MouseEvent.MOUSE_MOVED == event.getID()) {
        Object source = event.getSource();
        if (source instanceof Component) {
          JScrollPane pane = ComponentUtil.getParentOfType((Class<? extends JScrollPane>)JScrollPane.class, (Component)source);
          if (pane != null) {
            pauseThumbAnimation(pane.getHorizontalScrollBar());
            pauseThumbAnimation(pane.getVerticalScrollBar());
          }
        }
      }
    }

    /**
     * Pauses animation of the thumb if it is shown.
     *
     * @param bar the scroll bar with custom UI
     */
    private void pauseThumbAnimation(JScrollBar bar) {
      Object object = bar == null ? null : bar.getUI();
      if (object instanceof MacScrollBarUI) {
        MacScrollBarUI ui = (MacScrollBarUI)object;
        if (0 < ui.myThumb.animator.myValue) ui.onThumbMove();
      }
    }
  });

  private static ID createDelegate(String name, Pointer pointer, Callback callback) {
    ID delegateClass = allocateObjcClassPair(getObjcClass("NSObject"), name);
    if (!ID.NIL.equals(delegateClass)) {
      if (!addMethod(delegateClass, pointer, callback, "v@")) {
        throw new RuntimeException("Cannot add observer method");
      }
      registerObjcClassPair(delegateClass);
    }
    return invoke(name, "new");
  }

  private static <T> T callMac(Producer<? extends T> producer) {
    if (Platform.current().os().isMac()) {
      NSAutoreleasePool pool = new NSAutoreleasePool();
      try {
        return producer.produce();
      }
      catch (Throwable throwable) {
        Logger.getInstance(MacScrollBarUI.class).warn(throwable);
      }
      finally {
        pool.drain();
      }
    }
    return null;
  }

  private enum Behavior {
    NextPage,
    JumpToSpot;

    private static final Native<Behavior> CURRENT = new Native<>() {
      @Nonnull
      @Override
      public Behavior produce() {
        ID defaults = invoke("NSUserDefaults", "standardUserDefaults");
        invoke(defaults, "synchronize");
        ID behavior = invoke(defaults, "boolForKey:", nsString("AppleScrollerPagingBehavior"));
        Behavior value = 1 == behavior.intValue() ? JumpToSpot : NextPage;
        Logger.getInstance(MacScrollBarUI.class).debug("scroll bar behavior ", value, " from ", behavior);
        return value;
      }

      @Override
      public String toString() {
        return "scroll bar behavior";
      }

      @Override
      ID initialize() {
        return invoke(invoke("NSDistributedNotificationCenter", "defaultCenter"), "addObserver:selector:name:object:",
                      createDelegate("JBScrollBarBehaviorObserver", createSelector("handleBehaviorChanged:"), this), createSelector("handleBehaviorChanged:"),
                      nsString("AppleNoRedisplayAppearancePreferenceChanged"), ID.NIL, 2 // NSNotificationSuspensionBehaviorCoalesce
        );
      }
    };
  }

  private static final Native<Style> CURRENT_STYLE = new Native<>() {
    @Override
    public void run() {
      Style oldStyle = get();
      if (Platform.current().os().isMac() && !Registry.is("ide.mac.disableMacScrollbars", false)) {
        super.run();
      }
      Style newStyle = get();
      if (newStyle != oldStyle) {
        List<ConfigurableScrollBarUI> list = new ArrayList<>();
        UI.processReferences(null, null, list);
        for (ConfigurableScrollBarUI ui : list) {
          ui.updateStyle(newStyle);
        }
      }
    }

    @Nonnull
    @Override
    public Style produce() {
      ID style = invoke(getObjcClass("NSScroller"), "preferredScrollerStyle");
      Style value = 1 == style.intValue() ? Style.Overlay : Style.Legacy;
      Logger.getInstance(MacScrollBarUI.class).debug("scroll bar style ", value, " from ", style);
      return value;
    }

    @Override
    public String toString() {
      return "scroll bar style";
    }

    @Override
    ID initialize() {
      return invoke(invoke("NSNotificationCenter", "defaultCenter"), "addObserver:selector:name:object:",
                    createDelegate("JBScrollBarStyleObserver", createSelector("handleScrollerStyleChanged:"), this), createSelector("handleScrollerStyleChanged:"),
                    nsString("NSPreferredScrollerStyleDidChangeNotification"), ID.NIL);
    }
  };


  private static abstract class Native<T> implements Callback, Runnable, Producer<T> {
    private T myValue;

    Native() {
      Logger.getInstance(MacScrollBarUI.class).debug("initialize ", this);
      callMac(() -> initialize());
      UIUtil.invokeLaterIfNeeded(this);
    }

    abstract ID initialize();

    public T get() {
      return myValue;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void callback(ID self, Pointer selector, ID event) {
      Logger.getInstance(MacScrollBarUI.class).debug("update ", this);
      UIUtil.invokeLaterIfNeeded(this);
    }

    @Override
    public void run() {
      myValue = callMac(this);
    }
  }

  @FunctionalInterface
  public interface Producer<T> {
    T produce();
  }
}