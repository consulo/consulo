package com.intellij.util.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Sergey Simonchik
 */
public class Outcome<V> {

  private final V myData;
  private final boolean myCancelled;
  private final Exception myException;

  private Outcome(V data, boolean cancelled, Exception exception) {
    myData = data;
    myCancelled = cancelled;
    myException = exception;
  }

  @Nullable
  public V get() {
    return myData;
  }

  public boolean isCancelled() {
    return myCancelled;
  }

  @Nullable
  public Exception getException() {
    return myException;
  }

  @Nonnull
  public static <V> Outcome<V> createAsCancelled() {
    return new Outcome<V>(null, true, null);
  }

  @Nonnull
  public static <V> Outcome<V> createAsException(@Nonnull Exception ex) {
    return new Outcome<V>(null, false, ex);
  }

  @Nonnull
  public static <V> Outcome<V> createNormal(@Nonnull V data) {
    return new Outcome<V>(data, false, null);
  }

}
