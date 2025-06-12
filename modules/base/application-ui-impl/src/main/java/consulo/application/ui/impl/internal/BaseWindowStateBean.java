// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.ui.impl.internal;

import consulo.application.ui.WindowState;
import consulo.component.util.ModificationTracker;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public abstract class BaseWindowStateBean implements ModificationTracker, WindowState {
  private final AtomicLong myModificationCount = new AtomicLong();
  private volatile Point2D myLocation;
  private volatile Size2D mySize;
  private volatile int myExtendedState;
  private volatile boolean myFullScreen;

  @Override
  public long getModificationCount() {
    return myModificationCount.get();
  }

  @Override
  @Nullable
  public Point2D getLocation() {
    return myLocation;
  }

  public void setLocation(@Nullable Point2D location) {
    if (Objects.equals(myLocation, location)) return;
    myLocation = location;
    myModificationCount.getAndIncrement();
  }

  @Override
  @Nullable
  public Size2D getSize() {
    return mySize;
  }

  public void setSize(@Nullable Size2D size) {
    if (Objects.equals(mySize, size)) return;
    mySize = size;
    myModificationCount.getAndIncrement();
  }

  @Override
  public int getExtendedState() {
    return myExtendedState;
  }

  public abstract void setMaximized(boolean maximized);

  public void setExtendedState(int extendedState) {
    if (myExtendedState == extendedState) return;
    myExtendedState = extendedState;
    myModificationCount.getAndIncrement();
  }

  @Override
  public boolean isFullScreen() {
    return myFullScreen;
  }

  public void setFullScreen(boolean fullScreen) {
    if (myFullScreen == fullScreen) return;
    myFullScreen = fullScreen;
    myModificationCount.getAndIncrement();
  }

  @Nullable
  private static <T, R> R apply(@Nonnull Function<T, R> function, @Nullable T value) {
    return value == null ? null : function.apply(value);
  }
}
