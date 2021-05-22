package com.intellij.util;

import com.intellij.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author peter
 */
public class SharedProcessingContext {
  private final Map<Object, Object> myMap = ContainerUtil.newConcurrentMap();

  public Object get(@Nonnull @NonNls final String key) {
    return myMap.get(key);
  }

  public void put(@Nonnull @NonNls final String key, @Nonnull final Object value) {
    myMap.put(key, value);
  }

  public <T> void put(Key<T> key, T value) {
    myMap.put(key, value);
  }

  public <T> T get(Key<T> key) {
    return (T)myMap.get(key);
  }

  @Nullable
  public <T> T get(@Nonnull Key<T> key, Object element) {
    Map map = (Map)myMap.get(key);
    if (map == null) {
      return null;
    }
    else {
      return (T) map.get(element);
    }
  }

  public <T> void put(@Nonnull Key<T> key, Object element, T value) {
    Map map = (Map)myMap.get(key);
    if (map == null) {
      map = new HashMap();
      myMap.put(key, map);
    }
    map.put(element, value);
  }
}