// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.util;

import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicLong;

public class ProfilingInfo {
  private final long myCreatedTimeStamp;
  private volatile long myDisposedTimeStamp = -1;

  private final AtomicLong myUseCount = new AtomicLong();
  @Nonnull
  private final StackTraceElement myOrigin;

  public ProfilingInfo(@Nonnull StackTraceElement origin) {
    myCreatedTimeStamp = currentTime();
    myOrigin = origin;
  }

  public synchronized void valueDisposed() {
    if (myDisposedTimeStamp != 0) {
      myDisposedTimeStamp = currentTime();
    }
  }

  public void valueUsed() {
    myUseCount.incrementAndGet();
  }

  public long getCreatedTimeStamp() {
    return myCreatedTimeStamp;
  }

  public long getDisposedTimeStamp() {
    return myDisposedTimeStamp;
  }

  public long getUseCount() {
    return myUseCount.get();
  }

  public long getLifetime() {
    long disposedTime = myDisposedTimeStamp;
    if (disposedTime == -1) disposedTime = currentTime();

    return disposedTime - myCreatedTimeStamp;
  }

  @Nonnull
  public StackTraceElement getOrigin() {
    return myOrigin;
  }

  private static long currentTime() {
    return System.currentTimeMillis();
  }
}
