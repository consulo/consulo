package consulo.language.util;

import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author peter
 */
public class ProcessingContext {
  private @Nullable Map<Object, Object> myMap = null;
  private @Nullable SharedProcessingContext mySharedContext;

  public ProcessingContext() {
    mySharedContext = null;
  }

  public ProcessingContext(SharedProcessingContext sharedContext) {
    mySharedContext = sharedContext;
  }

  public SharedProcessingContext getSharedContext() {
    if (mySharedContext == null) {
      return mySharedContext = new SharedProcessingContext();
    }
    return mySharedContext;
  }

  @SuppressWarnings({"ConstantConditions"})
  public @Nullable Object get(Object key) {
    return myMap == null ? null : myMap.get(key);
  }

  public void put(Object key, Object value) {
    getInitializedMap().put(key, value);
  }

  public <T> void put(Key<T> key, @Nullable T value) {
    getInitializedMap().put(key, value);
  }

  @SuppressWarnings({"ConstantConditions"})
  public <T> @Nullable T get(Key<T> key) {
    return myMap == null ? null : (T) myMap.get(key);
  }

  private Map<Object, Object> getInitializedMap() {
    Map<Object, Object> map = myMap;
    if (map == null) {
      myMap = map = new HashMap<>(1);
    }
    return map;
  }
}
