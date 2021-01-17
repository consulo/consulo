// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.hover;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import com.intellij.util.ui.UIUtil;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//@ApiStatus.Experimental
public abstract class HoverListener {
  private static final Key<List<HoverListener>> HOVER_LISTENER_LIST_KEY = Key.create("HoverListenerList");

  public abstract void mouseEntered(@Nonnull Component component, int x, int y);

  public abstract void mouseMoved(@Nonnull Component component, int x, int y);

  public abstract void mouseExited(@Nonnull Component component);


  public final void addTo(@Nonnull JComponent component, @Nonnull Disposable parent) {
    addTo(component);
    Disposer.register(parent, () -> removeFrom(component));
  }

  public final void addTo(@Nonnull JComponent component) {
    List<HoverListener> list = UIUtil.getClientProperty(component, HOVER_LISTENER_LIST_KEY);
    if (list == null) component.putClientProperty(HOVER_LISTENER_LIST_KEY, list = new CopyOnWriteArrayList<>());
    list.add(0, this);
  }

  public final void removeFrom(@Nonnull JComponent component) {
    List<HoverListener> list = UIUtil.getClientProperty(component, HOVER_LISTENER_LIST_KEY);
    if (list != null) list.remove(this);
  }

  public static
  @Nonnull
  List<HoverListener> getAll(@Nonnull Component component) {
    List<HoverListener> list = UIUtil.getClientProperty(component, HOVER_LISTENER_LIST_KEY);
    return list != null ? list : Collections.emptyList();
  }
}
