package consulo.ide.impl.idea.openapi.util;

import consulo.component.util.ModificationTracker;

import java.util.concurrent.atomic.AtomicLong;

public class DefaultModificationTracker implements ModificationTracker {
  private volatile AtomicLong myCount = new AtomicLong();

  @Override
  public long getModificationCount() {
    return myCount.get();
  }

  public void incModificationCount() {
    myCount.incrementAndGet();
  }
}
