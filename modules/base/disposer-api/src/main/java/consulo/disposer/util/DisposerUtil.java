package consulo.disposer.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2019-03-07
 */
public class DisposerUtil {
  public static <T> void add(final T element, @Nonnull final Collection<T> result, @Nonnull final Disposable parentDisposable) {
    if (result.add(element)) {
      Disposer.register(parentDisposable, () -> result.remove(element));
    }
  }
}
