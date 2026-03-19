package consulo.ide.util;

import org.jspecify.annotations.Nullable;

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

  public @Nullable V get() {
    return myData;
  }

  public boolean isCancelled() {
    return myCancelled;
  }

  public @Nullable Exception getException() {
    return myException;
  }

  
  public static <V> Outcome<V> createAsCancelled() {
    return new Outcome<V>(null, true, null);
  }

  
  public static <V> Outcome<V> createAsException(Exception ex) {
    return new Outcome<V>(null, false, ex);
  }

  
  public static <V> Outcome<V> createNormal(V data) {
    return new Outcome<V>(data, false, null);
  }

}
