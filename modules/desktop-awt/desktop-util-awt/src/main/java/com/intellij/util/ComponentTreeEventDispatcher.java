// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.util.containers.JBTreeTraverser;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.EventListener;

import static com.intellij.util.ui.UIUtil.uiTraverser;

/**
 * Pushes events down the UI components hierarchy.
 *
 * @author gregsh
 */
public class ComponentTreeEventDispatcher<T extends EventListener> {

  private final Class<T> myListenerClass;
  private final T myMulticaster;

  public static <T extends EventListener> ComponentTreeEventDispatcher<T> create(@Nonnull Class<T> listenerClass) {
    return create(null, listenerClass);
  }

  public static <T extends EventListener> ComponentTreeEventDispatcher<T> create(@Nullable Component root, @Nonnull Class<T> listenerClass) {
    return new ComponentTreeEventDispatcher<>(root, listenerClass);
  }

  private ComponentTreeEventDispatcher(@Nullable final Component root, @Nonnull Class<T> listenerClass) {
    myListenerClass = listenerClass;
    myMulticaster = EventDispatcher.createMulticaster(listenerClass, null, () -> {
      JBTreeTraverser<Component> traverser = uiTraverser(root);
      if (root == null) traverser = traverser.withRoots(Arrays.asList(Window.getWindows()));
      return traverser.postOrderDfsTraversal().filter(myListenerClass);
    });
  }

  @Nonnull
  public T getMulticaster() {
    return myMulticaster;
  }

}
