// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.application.Application;

import jakarta.annotation.Nonnull;

/**
 * Subscribes to VFS events and processes them further on a dedicated pooled thread to {@link AsyncVfsEventsListener}s. <br/><br/>
 * <p>
 * If your event processing code might be slow (in particular, if it calls {@link VFileEvent#getPath()}), this listener is preferred
 * over original ones. Please also consider a safer {@link AsyncFileListener}.<br/><br/>
 *
 * <b>NB:</b> All listeners are executed on a pooled thread, without read action,
 * so the VFS state is unreliable without additional checks. <br/><br/>
 *
 * @see AsyncVfsEventsListener
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AsyncVfsEventsPostProcessor {

  /**
   * Subscribes the given listener to get the VFS events on a pooled thread.
   * The listener is automatically unsubscribed when the {@code disposable} is disposed.<br/><br/>
   * <p>
   * The caller should properly synchronize the call to {@code addListener()} with the {@code dispose()} of the given Disposable.
   */
  void addListener(@Nonnull AsyncVfsEventsListener listener, @Nonnull Disposable disposable);

  @Nonnull
  static AsyncVfsEventsPostProcessor getInstance() {
    return Application.get().getInstance(AsyncVfsEventsPostProcessor.class);
  }
}
