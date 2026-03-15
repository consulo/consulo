// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.proxy.EventDispatcher;
import consulo.util.collection.JBTreeTraverser;

import org.jspecify.annotations.Nullable;
import java.awt.*;
import java.util.Arrays;
import java.util.EventListener;

import static consulo.ui.ex.awt.UIUtil.uiTraverser;

/**
 * Pushes events down the UI components hierarchy.
 *
 * @author gregsh
 */
public class ComponentTreeEventDispatcher<T extends EventListener> {

  private final Class<T> myListenerClass;
  private final T myMulticaster;

  public static <T extends EventListener> ComponentTreeEventDispatcher<T> create(Class<T> listenerClass) {
    return create(null, listenerClass);
  }

  public static <T extends EventListener> ComponentTreeEventDispatcher<T> create(@Nullable Component root, Class<T> listenerClass) {
    return new ComponentTreeEventDispatcher<>(root, listenerClass);
  }

  private ComponentTreeEventDispatcher(@Nullable Component root, Class<T> listenerClass) {
    myListenerClass = listenerClass;
    myMulticaster = EventDispatcher.createMulticaster(listenerClass, null, () -> {
      JBTreeTraverser<Component> traverser = uiTraverser(root);
      if (root == null) traverser = traverser.withRoots(Arrays.asList(Window.getWindows()));
      return traverser.postOrderDfsTraversal().filter(myListenerClass);
    });
  }

  
  public T getMulticaster() {
    return myMulticaster;
  }
}
