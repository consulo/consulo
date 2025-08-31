package consulo.language.util;

import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public class SharedProcessingContext {
  private final Map<Object, Object> myMap = new ConcurrentHashMap<>();

  public Object get(@Nonnull String key) {
    return myMap.get(key);
  }

  public void put(@Nonnull String key, @Nonnull Object value) {
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
      return (T)map.get(element);
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