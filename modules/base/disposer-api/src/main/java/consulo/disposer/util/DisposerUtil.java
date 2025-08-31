package consulo.disposer.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import java.util.Collection;

public class DisposerUtil {
  public static <T> void add(T element, @Nonnull Collection<T> result, @Nonnull Disposable parentDisposable) {
    if (result.add(element)) {
      Disposer.register(parentDisposable, () -> result.remove(element));
    }
  }
}
