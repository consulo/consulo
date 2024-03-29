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

import consulo.disposer.Disposable;
import consulo.proxy.EventDispatcher;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class UIDataObject extends UserDataHolderBase {
  private final Map<Class, EventDispatcher> myListeners = new ConcurrentHashMap<>();

  @Nullable
  private Map<BorderPosition, BorderInfo> myBorders;

  private final Supplier<List<Function<Key<?>, Object>>> myUserDataProviders = LazyValue.atomicNotNull(Lists::newLockFreeCopyOnWriteList);

  @SuppressWarnings("unchecked")
  public <T extends EventListener> Disposable addListener(Class<T> clazz, T value) {
    EventDispatcher<T> eventDispatcher = myListeners.computeIfAbsent(clazz, EventDispatcher::create);
    eventDispatcher.addListener(value);
    return () -> eventDispatcher.removeListener(value);
  }

  @SuppressWarnings("unchecked")
  public <T extends EventListener> T getDispatcher(Class<T> clazz) {
    return (T)myListeners.computeIfAbsent(clazz, EventDispatcher::create).getMulticaster();
  }

  @Nonnull
  public <T> Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    myUserDataProviders.get().add(function);
    return () -> myUserDataProviders.get().remove(function);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getUserData(@Nonnull Key<T> key) {
    List<Function<Key<?>, Object>> value = myUserDataProviders.get();
    for (Function<Key<?>, Object> function : value) {
      Object funcValue = function.apply(key);
      if (funcValue != null) {
        return (T)funcValue;
      }
    }
    return super.getUserData(key);
  }

  public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, ColorValue colorValue, int width) {
    if (myBorders == null) {
      myBorders = new ConcurrentHashMap<>();
    }

    BorderInfo borderInfo = new BorderInfo(borderPosition, borderStyle, colorValue, width);
    myBorders.put(borderPosition, borderInfo);
  }

  public void removeBorder(BorderPosition borderPosition) {
    if (myBorders == null) {
      return;
    }

    myBorders.remove(borderPosition);
  }

  @Nonnull
  public Map<BorderPosition, BorderInfo> getBorders() {
    return myBorders == null ? Map.of() : myBorders;
  }

  public void dispose() {
    myListeners.clear();
    myBorders = null;
  }
}
