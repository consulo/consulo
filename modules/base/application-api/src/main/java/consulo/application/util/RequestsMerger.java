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
package consulo.application.util;

import consulo.logging.Logger;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * For exactly same refresh requests buffering:
 * <p/>
 * - refresh requests can be merged into one, but general principle is that each request should be reliably followed by refresh action
 * - at the moment only one refresh action is being done
 * - if request had been submitted while refresh action was in progress, new refresh action is initiated right after first refresh action finishes
 */
public class RequestsMerger {
  private static final Logger LOG = Logger.getInstance(RequestsMerger.class);

  private final MyWorker myWorker;

  private final Object myLock = new Object();

  private MyState myState;
  private final Consumer<? super Runnable> myAlarm;

  private final List<Runnable> myWaitingStartListeners = new ArrayList<>();
  private final List<Runnable> myWaitingFinishListeners = new ArrayList<>();

  public RequestsMerger(Runnable runnable, Consumer<? super Runnable> alarm) {
    myAlarm = alarm;
    myWorker = new MyWorker(runnable);

    myState = MyState.empty;
  }

  public void request() {
    LOG.debug("ext: request");
    doAction(MyAction.request);
  }

  public boolean isEmpty() {
    synchronized (myLock) {
      return MyState.empty.equals(myState);
    }
  }

  public void waitRefresh(Runnable runnable) {
    LOG.debug("ext: wait refresh");
    synchronized (myLock) {
      myWaitingStartListeners.add(runnable);
    }
    request();
  }

  private class MyWorker implements Runnable {
    private volatile boolean myInitialized;
    private final Runnable myRunnable;

    private MyWorker(Runnable runnable) {
      myRunnable = runnable;
    }

    @Override
    public void run() {
      LOG.debug("worker: started refresh");
      try {
        doAction(MyAction.start);
        myRunnable.run();
        myInitialized = true;
      }
      finally {
        doAction(MyAction.finish);
      }
    }

    public boolean isInitialized() {
      return myInitialized;
    }
  }

  private void doAction(MyAction action) {
    LOG.debug("doAction: START " + action.name());
    MyExitAction[] exitActions;
    List<Runnable> toBeCalled = null;
    synchronized (myLock) {
      MyState oldState = myState;
      myState = myState.transition(action);
      if (oldState.equals(myState)) return;
      exitActions = MyTransitionAction.getExit(oldState, myState);

      LOG.debug("doAction: oldState: " + oldState.name() + ", newState: " + myState.name());

      if (LOG.isDebugEnabled() && exitActions != null) {
        String debugExitActions = StringUtil.join(exitActions, exitAction -> exitAction.name(), " ");
        LOG.debug("exit actions: " + debugExitActions);
      }
      if (exitActions != null) {
        for (MyExitAction exitAction : exitActions) {
          if (MyExitAction.markStart.equals(exitAction)) {
            myWaitingFinishListeners.addAll(myWaitingStartListeners);
            myWaitingStartListeners.clear();
          }
          else if (MyExitAction.markEnd.equals(exitAction)) {
            toBeCalled = new ArrayList<>(myWaitingFinishListeners);
            myWaitingFinishListeners.clear();
          }
        }
      }
    }
    if (exitActions != null) {
      for (MyExitAction exitAction : exitActions) {
        if (MyExitAction.submitRequestToExecutor.equals(exitAction)) {
          myAlarm.accept(myWorker);
          //myAlarm.addRequest(myWorker, ourDelay);
          //ApplicationManager.getApplication().executeOnPooledThread(myWorker);
        }
      }
    }
    if (toBeCalled != null) {
      for (Runnable runnable : toBeCalled) {
        runnable.run();
      }
    }
    LOG.debug("doAction: END " + action.name());
  }

  private enum MyState {
    empty() {
      @Override
      @Nonnull
      public MyState transition(MyAction action) {
        if (MyAction.request.equals(action)) {
          return MyState.requestSubmitted;
        }
        logWrongAction(this, action);
        return this;
      }
    },
    inProgress() {
      @Override
      @Nonnull
      public MyState transition(MyAction action) {
        if (MyAction.finish.equals(action)) {
          return empty;
        }
        else if (MyAction.request.equals(action)) {
          return MyState.inProgressRequestSubmitted;
        }
        logWrongAction(this, action);
        return this;
      }
    },
    inProgressRequestSubmitted() {
      @Override
      @Nonnull
      public MyState transition(MyAction action) {
        if (MyAction.finish.equals(action)) {
          return MyState.requestSubmitted;
        }
        if (MyAction.start.equals(action)) {
          logWrongAction(this, action);
        }
        return this;
      }
    },
    requestSubmitted() {
      @Override
      @Nonnull
      public MyState transition(MyAction action) {
        if (MyAction.start.equals(action)) {
          return inProgress;
        }
        else if (MyAction.finish.equals(action)) {
          // to be able to be started by another request
          logWrongAction(this, action);
          return empty;
        }
        return this;
      }
    };

    // under lock
    @Nonnull
    public abstract MyState transition(MyAction action);

    private static void logWrongAction(MyState state, MyAction action) {
      LOG.info("Wrong action: state=" + state.name() + ", action=" + action.name());
    }
  }

  private static class MyTransitionAction {
    private static final Map<Couple<MyState>, MyExitAction[]> myMap = new HashMap<>();

    static {
      add(MyState.empty, MyState.requestSubmitted, MyExitAction.submitRequestToExecutor);
      add(MyState.requestSubmitted, MyState.inProgress, MyExitAction.markStart);
      add(MyState.inProgress, MyState.empty, MyExitAction.markEnd);
      add(MyState.inProgressRequestSubmitted, MyState.requestSubmitted, MyExitAction.submitRequestToExecutor, MyExitAction.markEnd);

      //... and not real but to be safe:
      add(MyState.inProgressRequestSubmitted, MyState.empty, MyExitAction.markEnd);
      add(MyState.inProgress, MyState.requestSubmitted, MyExitAction.markEnd);
    }

    private static void add(MyState from, MyState to, MyExitAction... action) {
      myMap.put(Couple.of(from, to), action);
    }

    @Nullable
    public static MyExitAction[] getExit(MyState from, MyState to) {
      return myMap.get(Couple.of(from, to));
    }
  }

  private enum MyExitAction {
    empty,
    submitRequestToExecutor,
    markStart,
    markEnd
  }

  private enum MyAction {
    request,
    start,
    finish
  }
}
