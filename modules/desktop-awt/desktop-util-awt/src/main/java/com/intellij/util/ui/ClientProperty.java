// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.ui;

import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public final class ClientProperty {
  /**
   * Sets the value for the client property of the component.
   * This is a convenient way to specify a value as a lambda.
   *
   * @param component a Swing component that may hold a client property value
   * @param key       a typed key corresponding to a client property
   * @param value     new value for the client property
   * @see JComponent#putClientProperty(Object, Object)
   */
  public static <T> void put(@Nonnull JComponent component, @Nonnull Key<T> key, @Nullable T value) {
    component.putClientProperty(key, value);
  }

  /**
   * Sets the value for the client property of the window.
   * This is a convenient way to specify a value as a lambda.
   *
   * @param window a Swing window that may hold a client property value
   * @param key    a typed key corresponding to a client property
   * @param value  new value for the client property
   * @return {@code true} if property is set to the corresponding root pane, {@code false} otherwise
   */
  public static <T> boolean put(@Nonnull Window window, @Nonnull Key<T> key, @Nullable T value) {
    return put(window, (Object)key, value);
  }

  /**
   * Sets the value for the client property of the window.
   * All these properties will be put into the corresponding root pane.
   *
   * @param window a Swing window that may hold a client property value
   * @param key    a key corresponding to a client property
   * @param value  new value for the client property
   * @return {@code true} if property is set to the corresponding root pane, {@code false} otherwise
   */
  public static boolean put(@Nullable Window window, @Nonnull @NonNls Object key, @Nullable Object value) {
    JComponent holder = getPropertiesHolder(window);
    if (holder != null) holder.putClientProperty(key, value);
    return holder != null;
  }

  @Nullable
  private static JComponent getPropertiesHolder(@Nullable Component component) {
    if (component instanceof JComponent) return (JComponent)component;
    if (component instanceof Window && component instanceof RootPaneContainer) {
      RootPaneContainer container = (RootPaneContainer)component;
      return container.getRootPane(); // store window properties in its root pane
    }
    return null;
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @return the property value from the specified component or {@code null}
   */
  @Nullable
  public static Object get(@Nullable Component component, @Nonnull @NonNls Object key) {
    JComponent holder = getPropertiesHolder(component);
    return holder == null ? null : holder.getClientProperty(key);
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @return the property value from the specified component or {@code null}
   */
  @Nullable
  public static Object findInHierarchy(@Nullable Component component, @Nonnull @NonNls Object key) {
    while (component != null) {
      Object value = get(component, key);
      if (value != null) return value;
      if (component instanceof Window) break;
      component = component.getParent();
    }
    return null;
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a typed key corresponding to a client property
   * @return the property value from the specified component or {@code null}
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T get(@Nullable Component component, @Nonnull Key<T> key) {
    Object value = get(component, (Object)key);
    return value != null ? (T)value : null;
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a typed key corresponding to a client property
   * @return the property value from the specified component or {@code null}
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T findInHierarchy(@Nullable Component component, @Nonnull Key<T> key) {
    Object value = findInHierarchy(component, (Object)key);
    return value != null ? (T)value : null;
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @return {@code true} if the property value is not null, or {@code false} otherwise
   */
  public static boolean isSet(@Nullable Component component, @Nonnull @NonNls Object key) {
    return null != get(component, key);
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @return {@code true} if the property value is not null, or {@code false} otherwise
   */
  public static boolean isSetInHierarchy(@Nullable Component component, @Nonnull @NonNls Object key) {
    return null != findInHierarchy(component, key);
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @param value     the expected value
   * @return {@code true} if the property value is equal to the {@code expected}, or {@code false} otherwise
   */
  public static boolean isSet(@Nullable Component component, @Nonnull @NonNls Object key, @Nonnull Object value) {
    return value.equals(get(component, key));
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a client property
   * @param value     the expected value
   * @return {@code true} if the property value is equal to the {@code expected}, or {@code false} otherwise
   */
  public static boolean isSetInHierarchy(@Nullable Component component, @Nonnull @NonNls Object key, @Nonnull Object value) {
    return value.equals(findInHierarchy(component, key));
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a boolean client property
   * @return {@code true} if the property value is {@link Boolean#TRUE}, or {@code false} otherwise
   */
  public static boolean isTrue(@Nullable Component component, @Nonnull Object key) {
    return isSet(component, key, Boolean.TRUE);
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a boolean client property
   * @return {@code true} if the property value is {@link Boolean#TRUE}, or {@code false} otherwise
   */
  public static boolean isTrueInHierarchy(@Nullable Component component, @Nonnull Object key) {
    return isSetInHierarchy(component, key, Boolean.TRUE);
  }


  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a boolean client property
   * @return {@code true} if the property value is {@link Boolean#FALSE}, or {@code false} otherwise
   */
  public static boolean isFalse(@Nullable Component component, @Nonnull Object key) {
    return isSet(component, key, Boolean.FALSE);
  }

  /**
   * @param component a Swing component that may hold a client property value
   * @param key       a key corresponding to a boolean client property
   * @return {@code true} if the property value is {@link Boolean#FALSE}, or {@code false} otherwise
   */
  public static boolean isFalseInHierarchy(@Nullable Component component, @Nonnull Object key) {
    return isSetInHierarchy(component, key, Boolean.FALSE);
  }
}
