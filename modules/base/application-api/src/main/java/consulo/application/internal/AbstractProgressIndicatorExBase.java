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
package consulo.application.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.TaskInfo;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.WeakList;

import jakarta.annotation.Nonnull;
import java.util.Collection;

public class AbstractProgressIndicatorExBase extends AbstractProgressIndicatorBase implements ProgressIndicatorEx {
  private final boolean myReusable;
  private volatile ProgressIndicatorEx[] myStateDelegates;
  private volatile WeakList<TaskInfo> myFinished;
  private volatile boolean myWasStarted;
  private TaskInfo myOwnerTask;

  public AbstractProgressIndicatorExBase(boolean reusable) {
    myReusable = reusable;
  }

  public AbstractProgressIndicatorExBase() {
    this(false);
  }

  @Override
  public void start() {
    synchronized (getLock()) {
      super.start();
      delegateRunningChange(ProgressIndicator::start);
      myWasStarted = true;
    }
  }


  @Override
  public void stop() {
    super.stop();
    delegateRunningChange(ProgressIndicator::stop);
  }

  @Override
  public void cancel() {
    super.cancel();
    delegateRunningChange(ProgressIndicator::cancel);
  }

  @Override
  public void finish(@Nonnull TaskInfo task) {
    WeakList<TaskInfo> finished = myFinished;
    if (finished == null) {
      synchronized (getLock()) {
        finished = myFinished;
        if (finished == null) {
          myFinished = finished = new WeakList<>();
        }
      }
    }
    if (!finished.addIfAbsent(task)) return;

    delegateRunningChange(each -> each.finish(task));
  }

  @Override
  public boolean isFinished(@Nonnull TaskInfo task) {
    Collection<TaskInfo> list = myFinished;
    return list != null && list.contains(task);
  }

  protected void setOwnerTask(TaskInfo owner) {
    myOwnerTask = owner;
  }

  @Override
  public void processFinish() {
    if (myOwnerTask != null) {
      finish(myOwnerTask);
      myOwnerTask = null;
    }
  }

  @Override
  public final void checkCanceled() {
    super.checkCanceled();

    delegate(ProgressIndicator::checkCanceled);
  }

  @Override
  public void setTextValue(LocalizeValue text) {
    super.setTextValue(text);

    delegateProgressChange(each -> each.setTextValue(text));
  }

  @Override
  public void setText2Value(LocalizeValue text) {
    super.setText2Value(text);

    delegateProgressChange(each -> each.setText2Value(text));
  }

  @Override
  public void setFraction(double fraction) {
    super.setFraction(fraction);

    delegateProgressChange(each -> each.setFraction(fraction));
  }

  @Override
  public void pushState() {
    synchronized (getLock()) {
      super.pushState();

      delegateProgressChange(ProgressIndicator::pushState);
    }
  }

  @Override
  public void popState() {
    synchronized (getLock()) {
      super.popState();

      delegateProgressChange(ProgressIndicator::popState);
    }
  }

  @Override
  protected boolean isReuseable() {
    return myReusable;
  }

  @Override
  public void setIndeterminate(boolean indeterminate) {
    super.setIndeterminate(indeterminate);

    delegateProgressChange(each -> each.setIndeterminate(indeterminate));
  }

  @Override
  public void addListener(@Nonnull ProgressIndicatorListener listener) {
    addStateDelegate(new AbstractProgressIndicatorExBase() {
      @Override
      public void cancel() {
        super.cancel();
        listener.canceled();
      }

      @Override
      public void finish(@Nonnull TaskInfo task) {
        super.finish(task);
        listener.finished();
      }

      @Override
      public void stop() {
        super.stop();
        listener.stopped();
      }

      @Override
      public void setFraction(double fraction) {
        super.setFraction(fraction);
        listener.onFractionChange(fraction);
      }
    });
  }

  @Override
  public final void addStateDelegate(@Nonnull ProgressIndicatorEx delegate) {
    synchronized (getLock()) {
      delegate.initStateFrom(this);
      ProgressIndicatorEx[] stateDelegates = myStateDelegates;
      if (stateDelegates == null) {
        myStateDelegates = stateDelegates = new ProgressIndicatorEx[1];
        stateDelegates[0] = delegate;
      }
      else {
        // hard throw is essential for avoiding deadlocks
        if (ArrayUtil.contains(delegate, stateDelegates)) {
          throw new IllegalArgumentException("Already registered: " + delegate);
        }
        myStateDelegates = ArrayUtil.append(stateDelegates, delegate, ProgressIndicatorEx.class);
      }
    }
  }

  protected void delegateProgressChange(@Nonnull IndicatorAction action) {
    delegate(action);
    onProgressChange();
  }

  protected void delegateRunningChange(@Nonnull IndicatorAction action) {
    delegate(action);
    onRunningChange();
  }

  private void delegate(@Nonnull IndicatorAction action) {
    ProgressIndicatorEx[] list = myStateDelegates;
    if (list != null) {
      for (ProgressIndicatorEx each : list) {
        action.execute(each);
      }
    }
  }

  protected void onProgressChange() {

  }

  protected void onRunningChange() {

  }

  @Override
  public boolean wasStarted() {
    return myWasStarted;
  }

  @FunctionalInterface
  protected interface IndicatorAction {
    void execute(@Nonnull ProgressIndicatorEx each);
  }
}
