package com.intellij.util;

import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author peter
 */
public class ProcessingContext {
  private Map<Object, Object> myMap;
  private SharedProcessingContext mySharedContext;

  public ProcessingContext() {
  }

  public ProcessingContext(final SharedProcessingContext sharedContext) {
    mySharedContext = sharedContext;
  }

  @Nonnull
  public SharedProcessingContext getSharedContext() {
    if (mySharedContext == null) {
      return mySharedContext = new SharedProcessingContext();
    }
    return mySharedContext;
  }

  @SuppressWarnings({"ConstantConditions"})
  public Object get(@Nonnull @NonNls final Object key) {
    return myMap == null? null : myMap.get(key);
  }

  public void put(@Nonnull @NonNls final Object key, @Nonnull final Object value) {
    checkMapInitialized();
    myMap.put(key, value);
  }

  public <T> void put(Key<T> key, T value) {
    checkMapInitialized();
    myMap.put(key, value);
  }

  @SuppressWarnings({"ConstantConditions"})
  public <T> T get(Key<T> key) {
    return myMap == null ? null : (T)myMap.get(key);
  }

  private void checkMapInitialized() {
    if (myMap == null) myMap = new HashMap<Object, Object>(1);
  }

}
