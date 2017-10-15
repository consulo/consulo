/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui.impl;

import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.EventDispatcher;
import com.intellij.util.containers.ConcurrentMultiMap;
import com.intellij.util.containers.MultiMap;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class UIDataObject extends UserDataHolderBase {
  private final Map<Class, EventDispatcher> myListeners = new ConcurrentHashMap<>();

  @Nullable
  private Map<BorderPosition, BorderInfo> myBorders;

  private final AtomicNotNullLazyValue<MultiMap<Key, Supplier>> myUserDataProviders = AtomicNotNullLazyValue.createValue(ConcurrentMultiMap::new);

  @SuppressWarnings("unchecked")
  public <T extends EventListener> Runnable addListener(Class<T> clazz, T value) {
    EventDispatcher<T> eventDispatcher = myListeners.computeIfAbsent(clazz, EventDispatcher::create);
    eventDispatcher.addListener(value);
    return () -> eventDispatcher.removeListener(value);
  }

  @SuppressWarnings("unchecked")
  public <T extends EventListener> T getDispatcher(Class<T> clazz) {
    return (T)myListeners.computeIfAbsent(clazz, EventDispatcher::create).getMulticaster();
  }

  @NotNull
  public <T> Runnable addUserDataProvider(@NotNull Key<T> key, @NotNull Supplier<T> supplier) {
    myUserDataProviders.getValue().putValue(key, supplier);
    return () -> myUserDataProviders.getValue().remove(key, supplier);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUserData(@NotNull Key<T> key) {
    Collection<Supplier> collection = myUserDataProviders.getValue().get(key);
    for (Supplier supplier : collection) {
      Object o = supplier.get();
      if (o != null) {
        return (T)o;
      }
    }
    return super.getUserData(key);
  }

  public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    if (myBorders == null) {
      myBorders = new ConcurrentHashMap<>();
    }

    BorderInfo borderInfo = new BorderInfo(borderPosition, borderStyle, colorKey, width);
    myBorders.put(borderPosition, borderInfo);
  }

  public void removeBorder(BorderPosition borderPosition) {
    if (myBorders == null) {
      return;
    }

    myBorders.remove(borderPosition);
  }

  @NotNull
  public Collection<BorderInfo> getBorders() {
    if (myBorders == null) {
      return Collections.emptyList();
    }
    return myBorders.values();
  }
}
