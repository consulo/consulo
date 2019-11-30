package com.intellij.util.io;

import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * @author Dmitry Avdeev
 */
public interface PersistentMap<K, V> extends KeyValueStore<K, V> {
  boolean processKeys(@Nonnull Processor<? super K> processor) throws IOException;

  boolean isClosed();

  boolean isDirty();

  void markDirty() throws IOException;
}
