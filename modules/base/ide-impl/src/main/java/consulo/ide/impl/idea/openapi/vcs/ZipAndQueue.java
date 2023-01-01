/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.vcs;

import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.progress.BackgroundTaskQueue;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.util.ZipperUpdater;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import javax.annotation.Nonnull;

/**
 * @author irengrig
 *         Date: 4/27/11
 *         Time: 10:38 AM
 */
public class ZipAndQueue {
  private final ZipperUpdater myZipperUpdater;
  private final BackgroundTaskQueue myQueue;
  private Runnable myInZipper;
  private Task.Backgroundable myInvokedOnQueue;

  public ZipAndQueue(final Project project, final int interval, final String title, final Runnable runnable) {
    final int correctedInterval = interval <= 0 ? 300 : interval;
    myZipperUpdater = new ZipperUpdater(correctedInterval, project);
    myQueue = new BackgroundTaskQueue(project, title);
    myInZipper = new Runnable() {
      @Override
      public void run() {
        myQueue.run(myInvokedOnQueue);
      }
    };
    myInvokedOnQueue = new Task.Backgroundable(project, title, false, BackgroundFromStartOption.getInstance()) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        runnable.run();
      }
    };
    Disposer.register(project, new Disposable() {
      @Override
      public void dispose() {
        myZipperUpdater.stop();
      }
    });
  }

  public void request() {
    myZipperUpdater.queue(myInZipper);
  }

  public void stop() {
    myZipperUpdater.stop();
  }
}
