package consulo.language.util;

import consulo.util.dataholder.Key;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author peter
 */
public class SharedProcessingContext {
  private final Map<Object, Object> myMap = new ConcurrentHashMap<>();

  public @Nullable Object get(String key) {
    return myMap.get(key);
  }

  public void put(String key, Object value) {
    myMap.put(key, value);
  }

  public <T> void put(Key<T> key, T value) {
    myMap.put(key, value);
  }

  public <T> @Nullable T get(Key<T> key) {
    return (T)myMap.get(key);
  }

  public <T> @Nullable T get(Key<T> key, Object element) {
    Map map = (Map)myMap.get(key);
    if (map == null) {
      return null;
    }
    else {
      return (T)map.get(element);
    }
  }

  public <T> void put(Key<T> key, Object element, T value) {
    Map map = (Map)myMap.get(key);
    if (map == null) {
      map = new HashMap();
      myMap.put(key, map);
    }
    map.put(element, value);
  }
}