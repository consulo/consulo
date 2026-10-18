// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal;

import consulo.application.ApplicationManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;

public class FileWatchRequestsManager {
  private final FileWatchRequestModifier myModifier;
  private @Nullable Future<?> myFuture;
  private volatile boolean myDisposed;

  public FileWatchRequestsManager(Project project, NewMappings newMappings) {
    this(project, newMappings, LocalFileSystem.getInstance());
  }

  public FileWatchRequestsManager(Project project, NewMappings newMappings, LocalFileSystem localFileSystem) {
    myModifier = new FileWatchRequestModifier(project, newMappings, localFileSystem);
    Disposer.register(newMappings, () -> {
      myDisposed = true;
      cancel();
    });
  }

  public void ping() {
    if (myDisposed || ApplicationManager.getApplication().isUnitTestMode()) {
      myModifier.run();
    }
    else {
      synchronized (this) {
        cancel();
        myFuture = AppExecutorUtil.getAppExecutorService().submit(myModifier);
      }
    }
  }

  private synchronized void cancel() {
    Future<?> future = myFuture;
    if (future != null) {
      future.cancel(false);
    }
  }
}
