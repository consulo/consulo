// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util;

import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import consulo.awt.TargetAWT;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

final class WindowStateBean implements ModificationTracker, WindowState {
  private final AtomicLong myModificationCount = new AtomicLong();
  private volatile Point myLocation;
  private volatile Dimension mySize;
  private volatile int myExtendedState;
  private volatile boolean myFullScreen;

  @Override
  public long getModificationCount() {
    return myModificationCount.get();
  }

  @Override
  @Nullable
  public Point getLocation() {
    return apply(Point::new, myLocation);
  }

  void setLocation(@Nullable Point location) {
    if (Objects.equals(myLocation, location)) return;
    myLocation = apply(Point::new, location);
    myModificationCount.getAndIncrement();
  }


  @Override
  @Nullable
  public Dimension getSize() {
    return apply(Dimension::new, mySize);
  }

  void setSize(@Nullable Dimension size) {
    if (Objects.equals(mySize, size)) return;
    mySize = apply(Dimension::new, size);
    myModificationCount.getAndIncrement();
  }


  @Override
  public int getExtendedState() {
    return myExtendedState;
  }

  void setExtendedState(int extendedState) {
    if (myExtendedState == extendedState) return;
    myExtendedState = extendedState;
    myModificationCount.getAndIncrement();
  }


  @Override
  public boolean isFullScreen() {
    return myFullScreen;
  }

  void setFullScreen(boolean fullScreen) {
    if (myFullScreen == fullScreen) return;
    myFullScreen = fullScreen;
    myModificationCount.getAndIncrement();
  }


  @Override
  public void applyTo(@Nonnull Window window) {
    Point location = getLocation();
    Dimension size = getSize();
    int extendedState = getExtendedState();

    Frame frame = window instanceof Frame ? (Frame)window : null;
    if (frame != null && Frame.NORMAL != frame.getExtendedState()) {
      frame.setExtendedState(Frame.NORMAL);
    }

    Rectangle bounds = window.getBounds();
    if (location != null) bounds.setLocation(location);
    if (size != null) bounds.setSize(size);
    if (bounds.isEmpty()) bounds.setSize(window.getPreferredSize());
    window.setBounds(bounds);

    if (frame != null && Frame.NORMAL != extendedState) {
      frame.setExtendedState(extendedState);
    }
  }

  void applyFrom(@Nonnull Window window) {
    if (window.isVisible()) {
      boolean windowFullScreen = isFullScreen(window);
      setFullScreen(windowFullScreen);

      Frame frame = window instanceof Frame ? (Frame)window : null;
      int windowExtendedState = frame == null ? Frame.NORMAL : frame.getExtendedState();
      setExtendedState(windowExtendedState);

      if (!windowFullScreen && windowExtendedState == Frame.NORMAL) {
        setLocation(window.getLocation());
        setSize(window.getSize());
      }
    }
  }


  private static boolean isFullScreen(@Nonnull Window window) {
    consulo.ui.Window uiWindow = TargetAWT.from(window);

    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
    if(ideFrame != null && WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && ideFrame.isInFullScreen()) {
      return true;
    }

    return false;
  }

  @Nullable
  private static <T, R> R apply(@Nonnull Function<T, R> function, @Nullable T value) {
    return value == null ? null : function.apply(value);
  }
}
