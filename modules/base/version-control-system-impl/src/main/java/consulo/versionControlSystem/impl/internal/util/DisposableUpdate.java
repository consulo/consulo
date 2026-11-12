// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.util;

import consulo.application.internal.BackgroundTaskUtil;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.util.Update;

public abstract class DisposableUpdate extends Update {
  private final Disposable myParentDisposable;

  public DisposableUpdate(Disposable parentDisposable, Object identity) {
    super(identity);
    myParentDisposable = parentDisposable;
  }

  @Override
  public final void run() {
    BackgroundTaskUtil.runUnderDisposeAwareIndicator(myParentDisposable, this::doRun);
  }

  protected abstract void doRun();

  public static DisposableUpdate createDisposable(Disposable parentDisposable,
                                                  Object identity,
                                                  Runnable runnable) {
    return new DisposableUpdate(parentDisposable, identity) {
      @Override
      public void doRun() {
        runnable.run();
      }
    };
  }
}
