package consulo.index.io;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Predicate;

/**
 * @author Dmitry Avdeev
 */
public interface PersistentMap<K, V> extends KeyValueStore<K, V> {
  boolean processKeys(@Nonnull Predicate<? super K> processor) throws IOException;

  boolean isClosed();

  boolean isDirty();

  void markDirty() throws IOException;
}
