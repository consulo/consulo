/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.progress.util;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorExBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.StandardProgressIndicator;
import consulo.application.progress.WrappedProgressIndicator;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.Semaphore;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SmoothProgressAdapter extends AbstractProgressIndicatorExBase implements ProgressIndicatorEx, WrappedProgressIndicator,
                                                                                      StandardProgressIndicator {
  private static final int SHOW_DELAY = 500;

  private Future<?> myStartupAlarm = CompletableFuture.completedFuture(null);

  private final ProgressIndicator myOriginal;
  private final Project myProject;

  private volatile boolean myOriginalStarted;

  private DialogWrapper myDialog;

  private final Runnable myShowRequest = new Runnable() {
    @Override
    public void run() {
      synchronized(SmoothProgressAdapter.this){
        if (!isRunning()) {
          return;
        }

        myOriginal.start();
        myOriginalStarted = true;

        myOriginal.setText(getText());
        myOriginal.setFraction(getFraction());
        myOriginal.setText2(getText2());
      }
    }
  };

  public SmoothProgressAdapter(@Nonnull ProgressIndicator original, @Nonnull Project project){
    myOriginal = original;
    myProject = project;
    if (myOriginal.isModal()) {
      myOriginal.setModalityProgress(this);
      setModalityProgress(this);
    }
  }

  @Nonnull
  @Override
  public ProgressIndicator getOriginalProgressIndicator() {
    return myOriginal;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
    super.setIndeterminate(indeterminate);
    myOriginal.setIndeterminate(indeterminate);
  }

  @Override
  public boolean isIndeterminate() {
    return myOriginal.isIndeterminate();
  }

  @Override
  public synchronized void start() {
    if (isRunning()) return;

    super.start();
    myOriginalStarted = false;
    myStartupAlarm = AppExecutorUtil.getAppScheduledExecutorService().schedule(myShowRequest, SHOW_DELAY, TimeUnit.MILLISECONDS);
  }

  public void startBlocking() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    start();
    if (isModal()) {
      showDialog();
    }
  }

  private void showDialog(){
    if (myDialog == null){
      //System.out.println("showDialog()");
      myDialog = new DialogWrapper(myProject, false) {
        {
          getWindow().setBounds(0, 0, 1, 1);
          setResizable(false);
        }

        @Override
        protected boolean isProgressDialog() {
          return true;
        }

        @Override
        protected JComponent createCenterPanel() {
          return null;
        }
      };
      myDialog.setModal(true);
      myDialog.setUndecorated(true);
      myDialog.show();
    }
  }

  @Override
  public synchronized void stop() {
    if (myOriginal.isRunning()) {
      myOriginal.stop();
    }
    else {
      myStartupAlarm.cancel(false);

      if (!myOriginalStarted && myOriginal instanceof Disposable) {
        // dispose original because start & stop were not called so original progress might not have released its resources 
        Disposer.dispose(((Disposable)myOriginal));
      }
    }

    // needed only for correct assertion of !progress.isRunning() in ApplicationImpl.runProcessWithProgressSynchroniously
    final Semaphore semaphore = new Semaphore();
    semaphore.down();

    SwingUtilities.invokeLater(
            new Runnable() {
              @Override
              public void run() {
                semaphore.waitFor();
                if (myDialog != null){
                  //System.out.println("myDialog.destroyProcess()");
                  myDialog.close(DialogWrapper.OK_EXIT_CODE);
                  myDialog = null;
                }
              }
            }
    );

    try {
      super.stop(); // should be last to not leaveModal before closing the dialog
    }
    finally {
      semaphore.up();
    }
  }

  @Override
  public synchronized void setTextValue(@Nonnull LocalizeValue text) {
    super.setTextValue(text);
    if (myOriginal.isRunning()) {
      myOriginal.setTextValue(text);
    }
  }

  @Override
  public synchronized void setFraction(double fraction) {
    super.setFraction(fraction);
    if (myOriginal.isRunning()) {
      myOriginal.setFraction(fraction);
    }
  }

  @Override
  public synchronized void setText2Value(LocalizeValue text) {
    super.setText2Value(text);
    if (myOriginal.isRunning()) {
      myOriginal.setText2Value(text);
    }
  }

  @Override
  public final void cancel() {
    super.cancel();
    myOriginal.cancel();
  }

  @Override
  public final boolean isCanceled() {
    return super.isCanceled() || myOriginalStarted && myOriginal.isCanceled();
  }
}
