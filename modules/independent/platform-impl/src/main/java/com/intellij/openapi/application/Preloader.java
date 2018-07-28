/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.application;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.annotation.inject.NotLazy;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.concurrent.Executor;

/**
 * @author peter
 */
@Singleton
@NotLazy
public class Preloader implements Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.application.Preloader");
  private final Executor myExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("com.intellij.openapi.application.Preloader pool");
  private final ProgressIndicator myIndicator = new ProgressIndicatorBase();
  private final ProgressIndicator myWrappingIndicator = new AbstractProgressIndicatorBase() {
    @Override
    public void checkCanceled() {
      checkHeavyProcessRunning();
      myIndicator.checkCanceled();
    }

    @Override
    public boolean isCanceled() {
      return myIndicator.isCanceled();
    }
  };

  private static void checkHeavyProcessRunning() {
    if (HeavyProcessLatch.INSTANCE.isRunning()) {
      TimeoutUtil.sleep(1);
    }
  }

  @PostConstruct
  public void initComponent() {
    if (ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isHeadlessEnvironment()) {
      return;
    }

    final ProgressManager progressManager = ProgressManager.getInstance();
    for (final PreloadingActivity activity : PreloadingActivity.EP_NAME.getExtensions()) {
      myExecutor.execute(new Runnable() {
        @Override
        public void run() {
          if (myIndicator.isCanceled()) return;

          checkHeavyProcessRunning();
          if (myIndicator.isCanceled()) return;

          progressManager.runProcess(new Runnable() {
            @Override
            public void run() {
              try {
                activity.preload(myWrappingIndicator);
              }
              catch (ProcessCanceledException ignore) {
              }
              LOG.info("Finished preloading " + activity);
            }
          }, myIndicator);
        }
      });
    }
  }

  @Override
  public void dispose() {
    myIndicator.cancel();
  }
}
