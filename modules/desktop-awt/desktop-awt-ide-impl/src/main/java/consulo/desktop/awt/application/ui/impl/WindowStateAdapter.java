// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.application.ui.impl;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

final class WindowStateAdapter extends WindowAdapter implements ComponentListener {
  @Nonnull
  static WindowStateBean getState(@Nonnull Window window) {
    return getAdapter(window).myWindowState;
  }

  @Nonnull
  private static WindowStateAdapter getAdapter(@Nonnull Window window) {
    for (ComponentListener listener : window.getComponentListeners()) {
      if (listener instanceof WindowStateAdapter) {
        return (WindowStateAdapter)listener;
      }
    }
    return new WindowStateAdapter(window);
  }


  private final WindowStateBean myWindowState = new WindowStateBean();

  private WindowStateAdapter(@Nonnull Window window) {
    myWindowState.applyFrom(window);
    window.addComponentListener(this);
    window.addWindowListener(this);
    window.addWindowStateListener(this);
  }

  @Override
  public void windowOpened(WindowEvent event) {
    update(event);
  }

  @Override
  public void windowStateChanged(WindowEvent event) {
    update(event);
  }

  @Override
  public void componentMoved(ComponentEvent event) {
    update(event);
  }

  @Override
  public void componentResized(ComponentEvent event) {
    update(event);
  }

  @Override
  public void componentShown(ComponentEvent event) {
  }

  @Override
  public void componentHidden(ComponentEvent event) {
  }

  private void update(@Nullable ComponentEvent event) {
    Object source = event == null ? null : event.getSource();
    if (source instanceof Window) myWindowState.applyFrom((Window)source);
  }
}
